// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/队伍操作请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.team.队伍同步工具;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍世界数据;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：队伍操作请求
 *
 * 统一处理创建/离开/踢出/解散/强制拉人/转移队长
 * 操作完成后自动触发同步
 */
public class 队伍操作请求包 {

    /**
     * 操作类型枚举
     */
    public enum 操作类型 {
        创建,
        离开,
        踢出,
        解散,
        强制拉人,
        转移队长;

        public void writeTo(FriendlyByteBuf buf) {
            buf.writeEnum(this);
        }

        public static 操作类型 readFrom(FriendlyByteBuf buf) {
            return buf.readEnum(操作类型.class);
        }
    }

    private final 操作类型 操作;
    /** 目标玩家UUID，踢出/强制拉人/转移队长时使用 */
    private final UUID 目标UUID;

    /**
     * 无目标的操作（创建/离开/解散）
     */
    public 队伍操作请求包(操作类型 action) {
        this.操作 = action;
        this.目标UUID = null;
    }

    /**
     * 有目标的操作（踢出/强制拉人/转移队长）
     */
    public 队伍操作请求包(操作类型 action, UUID targetUUID) {
        this.操作 = action;
        this.目标UUID = targetUUID;
    }

    public static void encode(队伍操作请求包 packet, FriendlyByteBuf buf) {
        packet.操作.writeTo(buf);
        boolean hasTarget = packet.目标UUID != null;
        buf.writeBoolean(hasTarget);if (hasTarget) {
            buf.writeUUID(packet.目标UUID);
        }
    }

    public static 队伍操作请求包 decode(FriendlyByteBuf buf) {
        操作类型 action = 操作类型.readFrom(buf);
        boolean hasTarget = buf.readBoolean();
        UUID targetUUID = hasTarget ? buf.readUUID() : null;

        if (targetUUID != null) {
            return new 队伍操作请求包(action, targetUUID);
        }
        return new 队伍操作请求包(action);
    }

    public static void handle(队伍操作请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            switch (packet.操作) {
                case 创建 -> 处理创建(player, server);
                case 离开 -> 处理离开(player, server);
                case 踢出 -> 处理踢出(player, server, packet.目标UUID);
                case 解散 -> 处理解散(player, server);
                case 强制拉人 -> 处理强制拉人(player, server, packet.目标UUID);
                case 转移队长 -> 处理转移队长(player, server, packet.目标UUID);
            }
        });
        ctx.setPacketHandled(true);
    }

    // ==================== 操作处理 ====================

    private static void 处理创建(ServerPlayer player, MinecraftServer server) {
        队伍管理器.操作结果 result = 队伍管理器.创建队伍(player);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功()) {
            队伍同步工具.同步队伍数据(player);
        }
    }

    private static void 处理离开(ServerPlayer player, MinecraftServer server) {
        // 离开前记录队伍ID，用于通知剩余成员
        UUID oldTeamId = 队伍世界数据.获取(server).获取玩家队伍ID(player.getUUID());

        队伍管理器.操作结果 result = 队伍管理器.离开队伍(player);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功()) {
            // 通知自己：无队伍
            队伍同步工具.同步无队伍(player);

            // 通知剩余成员刷新
            if (oldTeamId != null) {
                队伍同步工具.同步给所有在线成员(server, oldTeamId);

                // 广播离开消息给剩余成员
                队伍同步工具.广播消息给队伍(
                        server, oldTeamId,
                        Component.translatable("message.puellamagi.team.broadcast.member_left",
                                player.getName()),
                        null
                );
            }
        }
    }

    private static void 处理踢出(ServerPlayer player, MinecraftServer server, UUID targetUUID) {
        if (targetUUID == null) {
            player.sendSystemMessage(Component.translatable("message.puellamagi.team.error.no_target"));
            return;
        }

        // 踢出前记录队伍ID
        UUID teamId = 队伍世界数据.获取(server).获取玩家队伍ID(player.getUUID());

        队伍管理器.操作结果 result = 队伍管理器.踢出成员(player, targetUUID);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功() && teamId != null) {
            // 通知被踢者（如果在线）
            ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                队伍同步工具.同步无队伍(target);
                target.sendSystemMessage(
                        Component.translatable("message.puellamagi.team.kicked")
                );
            }

            // 通知剩余成员刷新
            队伍同步工具.同步给所有在线成员(server, teamId);
        }
    }

    private static void 处理解散(ServerPlayer player, MinecraftServer server) {
        // 解散前记录成员列表
        队伍世界数据 worldData = 队伍世界数据.获取(server);
        UUID teamId = worldData.获取玩家队伍ID(player.getUUID());

        List<UUID> formerMembers = null;
        if (teamId != null) {
            var teamOpt = worldData.获取队伍(teamId);
            if (teamOpt.isPresent()) {
                formerMembers = teamOpt.get().获取所有成员UUID();
            }
        }

        队伍管理器.操作结果 result = 队伍管理器.解散队伍(player);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功() && formerMembers != null) {
            // 通知所有前成员：无队伍
            队伍同步工具.同步无队伍给多人(server, formerMembers);

            // 广播解散消息
            队伍同步工具.广播消息给玩家列表(
                    server, formerMembers,
                    Component.translatable("message.puellamagi.team.broadcast.disbanded"),
                    null
            );
        }
    }

    private static void 处理强制拉人(ServerPlayer player, MinecraftServer server, UUID targetUUID) {
        if (targetUUID == null) {
            player.sendSystemMessage(Component.translatable("message.puellamagi.team.error.no_target"));
            return;
        }

        // 权限检查：仅OP
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(
                    Component.translatable("message.puellamagi.team.error.no_permission")
            );
            return;
        }

        ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
        if (target == null) {
            player.sendSystemMessage(
                    Component.translatable("message.puellamagi.team.error.player_offline")
            );
            return;
        }

        UUID teamId = 队伍世界数据.获取(server).获取玩家队伍ID(player.getUUID());

        队伍管理器.操作结果 result = 队伍管理器.强制拉人(player, target);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功() && teamId != null) {
            // 同步给所有成员（包括新加入的）
            队伍同步工具.同步给所有在线成员(server, teamId);
        }
    }

    private static void 处理转移队长(ServerPlayer player, MinecraftServer server, UUID targetUUID) {
        if (targetUUID == null) {
            player.sendSystemMessage(Component.translatable("message.puellamagi.team.error.no_target"));
            return;
        }

        UUID teamId = 队伍世界数据.获取(server).获取玩家队伍ID(player.getUUID());

        队伍管理器.操作结果 result = 队伍管理器.转移队长(player, targetUUID);
        player.sendSystemMessage(Component.translatable(result.消息()));

        if (result.成功() && teamId != null) {
            // 通知目标（如果在线）
            ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                target.sendSystemMessage(
                        Component.translatable("message.puellamagi.team.transfer_received")
                );
            }

            // 同步给所有成员（职位变了）
            队伍同步工具.同步给所有在线成员(server, teamId);
        }
    }
}
