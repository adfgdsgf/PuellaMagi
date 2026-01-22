// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/契约能力同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端→ 客户端：同步契约状态
 * 使用完整NBT传输，支持灵魂宝石等扩展字段
 */
public class 契约能力同步包 {

    private final CompoundTag 数据;

    public 契约能力同步包(CompoundTag data) {
        this.数据 = data;
    }

    // ==================== 编码 ====================

    public static void encode(契约能力同步包 packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.数据);
    }

    // ==================== 解码 ====================

    public static 契约能力同步包 decode(FriendlyByteBuf buf) {
        CompoundTag data = buf.readNbt();
        return new 契约能力同步包(data != null ? data : new CompoundTag());
    }

    // ==================== 处理（客户端）====================

    public static void handle(契约能力同步包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            能力工具.获取契约能力(player).ifPresent(contract -> {
                contract.从NBT读取(packet.数据);
            });
        });
        ctx.setPacketHandled(true);
    }
}
