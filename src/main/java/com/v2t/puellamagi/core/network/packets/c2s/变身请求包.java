// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/变身请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.transformation.变身管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求变身或解除变身
 *
 * 变身时服务端从契约获取类型，客户端只需发送意图
 */
public class 变身请求包 {

    private final boolean 是变身请求;  // true=变身, false=解除

    /**
     * 变身请求
     */
    public 变身请求包(boolean isTransform) {
        this.是变身请求 = isTransform;
    }

    /**
     * 解除变身请求
     */
    public 变身请求包() {
        this.是变身请求 = false;
    }

    // ==================== 编码 ====================

    public static void encode(变身请求包 packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.是变身请求);
    }

    // ==================== 解码 ====================

    public static 变身请求包 decode(FriendlyByteBuf buf) {
        boolean isTransform = buf.readBoolean();
        return new 变身请求包(isTransform);
    }

    //==================== 处理（服务端）====================

    public static void handle(变身请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (packet.是变身请求) {
                // 从契约获取类型
                var typeOpt = 契约管理器.获取类型(player);
                if (typeOpt.isPresent()) {
                    变身管理器.尝试变身(player, typeOpt.get().获取ID());
                }
                // 未契约则静默忽略（客户端已检查）
            } else {
                变身管理器.解除变身(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
