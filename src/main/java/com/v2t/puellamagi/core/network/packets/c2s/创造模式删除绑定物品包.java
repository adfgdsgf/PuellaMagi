// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/创造模式删除绑定物品包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.api.item.I绑定物品;
import com.v2t.puellamagi.util.绑定物品工具;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 创造模式删除绑定物品通知包
 *
 * 客户端检测到绑定物品在创造模式背包界面被删除时发送
 * 通用设计，支持所有实现 I绑定物品 接口的物品
 *
 * 验证机制：
 * - 服务端验证玩家是否处于创造模式
 * - 服务端验证物品类型是否实现了 I绑定物品
 */
public class 创造模式删除绑定物品包 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/CreativeDelete");

    private final ResourceLocation 物品ID;
    private final UUID 所有者UUID;
    private final long 时间戳;

    public 创造模式删除绑定物品包(ResourceLocation itemId, UUID ownerUUID, long timestamp) {
        this.物品ID = itemId;
        this.所有者UUID = ownerUUID;
        this.时间戳 = timestamp;
    }

    //==================== 编解码 ====================

    public static void encode(创造模式删除绑定物品包 packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.物品ID);
        buf.writeUUID(packet.所有者UUID);
        buf.writeLong(packet.时间戳);
    }

    public static 创造模式删除绑定物品包 decode(FriendlyByteBuf buf) {
        return new 创造模式删除绑定物品包(
                buf.readResourceLocation(),
                buf.readUUID(),
                buf.readLong()
        );
    }

    // ==================== 处理 ====================

    public static void handle(创造模式删除绑定物品包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // 验证：发送者必须是创造模式
            if (!sender.isCreative()) {
                LOGGER.warn("非创造模式玩家 {} 发送了创造删除包，忽略",
                        sender.getName().getString());
                return;
            }

            // 获取物品类型
            Item item = ForgeRegistries.ITEMS.getValue(packet.物品ID);
            if (item == null) {
                LOGGER.warn("未知物品类型: {}", packet.物品ID);
                return;
            }

            // 验证：必须是绑定物品
            if (!(item instanceof I绑定物品 绑定物品)) {
                LOGGER.warn("物品 {} 不是绑定物品", packet.物品ID);
                return;
            }

            // 委托给绑定物品工具处理
            绑定物品工具.处理创造模式删除(
                    sender,
                    绑定物品,
                    packet.所有者UUID,
                    packet.时间戳
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
