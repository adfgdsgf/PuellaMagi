// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/队伍邀请请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.team.队伍同步工具;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍邀请管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：发送队伍邀请
 *
 * 客户端通过目标玩家UUID发起邀请
 * 服务端验证后创建邀请记录并通知目标
 */
public class 队伍邀请请求包 {

    private final UUID 目标UUID;

    public 队伍邀请请求包(UUID targetUUID) {
        this.目标UUID = targetUUID;
    }

    public static void encode(队伍邀请请求包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.目标UUID);
    }

    public static 队伍邀请请求包 decode(FriendlyByteBuf buf) {
        return new 队伍邀请请求包(buf.readUUID());
    }

    public static void handle(队伍邀请请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            // 查找目标玩家
            ServerPlayer target = server.getPlayerList().getPlayer(packet.目标UUID);
            if (target == null) {
                player.sendSystemMessage(
                        Component.translatable("message.puellamagi.team.error.player_offline")
                );
                return;
            }

            // 执行邀请
            队伍管理器.操作结果 result = 队伍邀请管理器.发送邀请(player, target);
            player.sendSystemMessage(Component.translatable(result.消息()));

            // 成功则通知目标玩家
            if (result.成功()) {
                队伍同步工具.发送邀请通知(
                        target,
                        player.getUUID(),
                        player.getName().getString()
                );

                // 给目标发聊天提示
                target.sendSystemMessage(
                        Component.translatable("message.puellamagi.team.invite_received",
                                player.getName())
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
