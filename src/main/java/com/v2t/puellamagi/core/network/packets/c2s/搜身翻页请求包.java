// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/搜身翻页请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.interaction.menu.搜身菜单;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 搜身翻页请求包
 *
 * 客户端请求翻页时发送到服务端
 * 确保客户端和服务端的菜单状态同步
 */
public class 搜身翻页请求包 {

    /**
     * 区域类型
     */
    public enum 区域 {
        主区域,
        侧边区域
    }

    private final 区域 目标区域;
    private final int 目标页码;

    public 搜身翻页请求包(区域 目标区域, int 目标页码) {
        this.目标区域 = 目标区域;
        this.目标页码 = 目标页码;
    }

    //==================== 编解码 ====================

    public static void encode(搜身翻页请求包 packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.目标区域);
        buf.writeVarInt(packet.目标页码);
    }

    public static 搜身翻页请求包 decode(FriendlyByteBuf buf) {
        区域 目标区域 = buf.readEnum(区域.class);
        int 目标页码 = buf.readVarInt();
        return new 搜身翻页请求包(目标区域, 目标页码);
    }

    // ==================== 处理 ====================

    public static void handle(搜身翻页请求包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 检查玩家当前打开的是搜身菜单
            if (!(player.containerMenu instanceof 搜身菜单 menu)) {
                return;
            }

            // 执行翻页
            if (packet.目标区域 == 区域.主区域) {
                menu.主区域跳转到页(packet.目标页码);
            } else {
                menu.侧边区域跳转到页(packet.目标页码);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
