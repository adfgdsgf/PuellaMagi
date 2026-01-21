// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/契约能力同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端→ 客户端：同步契约状态
 */
public class 契约能力同步包 {

    private final boolean 已契约;
    private final ResourceLocation 系列ID;
    private final ResourceLocation 类型ID;
    private final long 契约时间;

    public 契约能力同步包(boolean hasContract, ResourceLocation seriesId, ResourceLocation typeId, long contractTime) {
        this.已契约 = hasContract;
        this.系列ID = seriesId;
        this.类型ID = typeId;
        this.契约时间 = contractTime;
    }

    //==================== 编码 ====================

    public static void encode(契约能力同步包 packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.已契约);
        buf.writeBoolean(packet.系列ID != null);
        if (packet.系列ID != null) {
            buf.writeResourceLocation(packet.系列ID);
        }
        buf.writeBoolean(packet.类型ID != null);
        if (packet.类型ID != null) {
            buf.writeResourceLocation(packet.类型ID);
        }
        buf.writeLong(packet.契约时间);
    }

    // ==================== 解码 ====================

    public static 契约能力同步包 decode(FriendlyByteBuf buf) {
        boolean hasContract = buf.readBoolean();
        ResourceLocation seriesId = buf.readBoolean() ? buf.readResourceLocation() : null;
        ResourceLocation typeId = buf.readBoolean() ? buf.readResourceLocation() : null;
        long contractTime = buf.readLong();
        return new 契约能力同步包(hasContract, seriesId, typeId, contractTime);
    }

    // ==================== 处理（客户端）====================

    public static void handle(契约能力同步包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            能力工具.获取契约能力(player).ifPresent(contract -> {
                if (packet.已契约) {
                    contract.签订契约(packet.系列ID, packet.类型ID, packet.契约时间);
                } else {
                    contract.解除契约();
                }
            });
        });ctx.setPacketHandled(true);
    }
}
