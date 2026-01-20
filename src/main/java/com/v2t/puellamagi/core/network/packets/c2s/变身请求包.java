package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.transformation.变身管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求变身或解除变身
 */
public class 变身请求包 {

    private final boolean 是变身请求;// true=变身, false=解除
    private final ResourceLocation 少女类型;  // 变身时需要，解除时可为null

    /**
     * 变身请求
     */
    public 变身请求包(ResourceLocation girlType) {
        this.是变身请求 = true;
        this.少女类型 = girlType;
    }

    /**
     * 解除变身请求
     */
    public 变身请求包() {
        this.是变身请求 = false;
        this.少女类型 = null;
    }

    private 变身请求包(boolean isTransform, ResourceLocation girlType) {
        this.是变身请求 = isTransform;
        this.少女类型 = girlType;
    }

    //==================== 编码 ====================
    public static void encode(变身请求包 packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.是变身请求);
        buf.writeBoolean(packet.少女类型 != null);
        if (packet.少女类型 != null) {
            buf.writeResourceLocation(packet.少女类型);
        }
    }

    // ==================== 解码 ====================
    public static 变身请求包 decode(FriendlyByteBuf buf) {
        boolean isTransform = buf.readBoolean();
        boolean hasType = buf.readBoolean();
        ResourceLocation type = hasType ? buf.readResourceLocation() : null;
        return new 变身请求包(isTransform, type);
    }

    // ==================== 处理（服务端） ====================
    public static void handle(变身请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (packet.是变身请求) {
                if (packet.少女类型 != null) {
                    变身管理器.尝试变身(player, packet.少女类型);
                }
            } else {
                变身管理器.解除变身(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
