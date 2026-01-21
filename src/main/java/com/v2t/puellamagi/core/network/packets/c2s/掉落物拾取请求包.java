// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/掉落物拾取请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 掉落物拾取请求包（C2S）
 *
 * 时停中右键拾取掉落物
 */
public class 掉落物拾取请求包 {

    private final int entityId;

    public 掉落物拾取请求包(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(掉落物拾取请求包 packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static 掉落物拾取请求包 decode(FriendlyByteBuf buf) {
        return new 掉落物拾取请求包(buf.readInt());
    }

    public static void handle(掉落物拾取请求包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            TimeStop timeStop = (TimeStop) player.level();

            // 必须是时停者
            if (!timeStop.puellamagi$isTimeStopper(player)) {
                return;
            }

            // 获取目标实体
            Entity entity = player.level().getEntity(packet.entityId);
            if (!(entity instanceof ItemEntity item)) {
                return;
            }

            // 必须在时停范围内
            if (!timeStop.puellamagi$inTimeStopRange(item)) {
                return;
            }

            // 检查距离
            double pickupRange = player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
            pickupRange = Math.max(pickupRange, 3.0);

            double distance = player.distanceTo(item);
            if (distance > pickupRange + 1.0) {
                return;
            }

            // 执行拾取
            ItemStack stack = item.getItem().copy();
            int countBefore = stack.getCount();

            // 尝试添加到玩家背包
            if (player.getInventory().add(stack)) {
                // 全部拾取成功
                player.take(item, countBefore);
                item.discard();
            } else {
                // 背包满了，尝试部分拾取
                int countAfter = stack.getCount();
                if (countAfter < countBefore) {
                    // 部分拾取成功
                    player.take(item, countBefore - countAfter);
                    item.getItem().setCount(countAfter);
                }// 否则完全没拾取
            }
        });ctx.get().setPacketHandled(true);
    }
}
