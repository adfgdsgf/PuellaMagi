// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/队伍邀请响应包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.team.队伍同步工具;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍世界数据;
import com.v2t.puellamagi.system.team.队伍邀请管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：响应队伍邀请（接受/拒绝）
 */
public class 队伍邀请响应包 {

    private final UUID 邀请者UUID;
    private final boolean 接受;

    public 队伍邀请响应包(UUID inviterUUID, boolean accept) {
        this.邀请者UUID = inviterUUID;
        this.接受 = accept;
    }

    public static void encode(队伍邀请响应包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.邀请者UUID);
        buf.writeBoolean(packet.接受);
    }

    public static 队伍邀请响应包 decode(FriendlyByteBuf buf) {
        UUID inviterUUID = buf.readUUID();
        boolean accept = buf.readBoolean();
        return new 队伍邀请响应包(inviterUUID, accept);
    }

    public static void handle(队伍邀请响应包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            if (packet.接受) {
                处理接受(player, server, packet.邀请者UUID);
            } else {
                处理拒绝(player, server, packet.邀请者UUID);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void 处理接受(ServerPlayer player, MinecraftServer server, UUID inviterUUID) {
        队伍管理器.操作结果 result = 队伍邀请管理器.接受邀请(player, inviterUUID);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功()) {
            // 获取加入的队伍ID
            UUID teamId = 队伍世界数据.获取(server).获取玩家队伍ID(player.getUUID());

            if (teamId != null) {
                // 同步给所有成员（包括新加入的）
                队伍同步工具.同步给所有在线成员(server, teamId);

                // 广播加入消息
                队伍同步工具.广播消息给队伍(
                        server, teamId,
                        Component.translatable("message.puellamagi.team.broadcast.member_joined",
                                player.getName()),
                        player.getUUID()
                );
            }
        }
    }

    private static void 处理拒绝(ServerPlayer player, MinecraftServer server, UUID inviterUUID) {
        队伍管理器.操作结果 result = 队伍邀请管理器.拒绝邀请(player, inviterUUID);
        player.sendSystemMessage(Component.translatable(result.消息()));

        // 通知邀请者被拒绝
        if (result.成功()) {
            ServerPlayer inviter = server.getPlayerList().getPlayer(inviterUUID);
            if (inviter != null) {
                inviter.sendSystemMessage(
                        Component.translatable("message.puellamagi.team.invite_declined_notify",
                                player.getName())
                );
            }
        }
    }
}
