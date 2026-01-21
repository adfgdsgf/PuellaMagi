// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/投射物拾取请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.core.config.时停配置;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 投射物拾取请求包（C2S）
 *
 * 时停中右键拾取静止的投射物（主要是箭）
 */
public class 投射物拾取请求包 {

    private final int entityId;

    public 投射物拾取请求包(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(投射物拾取请求包 packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static 投射物拾取请求包 decode(FriendlyByteBuf buf) {
        return new 投射物拾取请求包(buf.readInt());
    }

    public static void handle(投射物拾取请求包 packet, Supplier<NetworkEvent.Context> ctx) {
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
            if (entity == null) {
                return;
            }

            // 必须在时停范围内
            if (!timeStop.puellamagi$inTimeStopRange(entity)) {
                return;
            }

            // 检查距离
            double pickupRange = player.getAttributeValue(
                    net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
            pickupRange = Math.max(pickupRange, 3.0);

            double distance = player.distanceTo(entity);
            if (distance > pickupRange + 1.0) {
                return;
            }

            // 只处理箭类投射物
            if (entity instanceof AbstractArrow arrow) {
                // 检查是否已静止（speedMultiplier <= 阈值）
                IProjectileAccess access = (IProjectileAccess) arrow;
                double stopThreshold = 时停配置.获取静止阈值();

                // 只有静止的箭才能捡（惯性结束或已经停止）
                if (access.puellamagi$getSpeedMultiplier() > stopThreshold
                        && access.puellamagi$isTimeStopCreated()) {
                    // 还在飞行中，不能捡
                    return;
                }

                // 尝试拾取
                if (arrow.pickup == AbstractArrow.Pickup.ALLOWED
                        || (arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY && player.isCreative())) {

                    ItemStack arrowStack = ((com.v2t.puellamagi.mixin.access.AbstractArrowAccessor)
                            arrow)
                            .puellamagi$getPickupItem();
                    if (!arrowStack.isEmpty() && player.getInventory().add(arrowStack)) {
                        player.take(arrow,1);
                        arrow.discard();
                    }
                } else if (arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY) {
                    // 创造模式直接移除
                    if (player.isCreative()) {
                        arrow.discard();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
