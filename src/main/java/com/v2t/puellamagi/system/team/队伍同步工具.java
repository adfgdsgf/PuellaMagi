// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍同步工具.java

package com.v2t.puellamagi.system.team;

import com.v2t.puellamagi.core.network.ModNetwork;
import com.v2t.puellamagi.core.network.packets.s2c.队伍数据同步包;
import com.v2t.puellamagi.core.network.packets.s2c.队伍成员更新包;
import com.v2t.puellamagi.core.network.packets.s2c.队伍邀请通知包;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 队伍网络同步工具
 *
 * 服务端调用，负责将队伍数据变更同步给相关客户端
 * 所有方法仅在服务端调用
 *
 * 同步策略：全量同步（队伍数据量小，简单可靠）
 */
public final class 队伍同步工具 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/TeamSync");

    private 队伍同步工具() {}

    // ==================== 队伍数据同步 ====================

    /**
     * 同步队伍数据给指定玩家
     * 自动判断该玩家是否有队伍，发送对应的同步包
     *
     * @param player 目标玩家
     */
    public static void 同步队伍数据(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        worldData.获取玩家队伍(player.getUUID()).ifPresentOrElse(
                team -> 发送包给玩家(player, new 队伍数据同步包(team)),
                () -> 发送包给玩家(player, new 队伍数据同步包())
        );
    }

    /**
     * 同步队伍数据给该队伍所有在线成员
     * 用于成员变动后通知所有人刷新
     *
     * @param server 服务器实例
     * @param teamId 队伍ID
     */
    public static void 同步给所有在线成员(MinecraftServer server, UUID teamId) {
        队伍世界数据 worldData = 队伍世界数据.获取(server);

        worldData.获取队伍(teamId).ifPresent(team -> {
            队伍数据同步包 packet = new 队伍数据同步包(team);

            for (UUID memberUUID : team.获取所有成员UUID()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
                if (member != null) {
                    发送包给玩家(member, packet);
                }
            }
        });
    }

    /**
     * 发送"无队伍"同步给指定玩家
     * 用于玩家离开/被踢/队伍解散后
     *
     * @param player 目标玩家
     */
    public static void 同步无队伍(ServerPlayer player) {
        发送包给玩家(player, new 队伍数据同步包());
    }

    /**
     * 发送"无队伍"同步给多个玩家（通过UUID列表）
     * 用于队伍解散时通知所有前成员
     *
     * @param server 服务器实例
     * @param playerUUIDs 玩家UUID列表
     */
    public static void 同步无队伍给多人(MinecraftServer server, List<UUID> playerUUIDs) {
        队伍数据同步包 packet = new 队伍数据同步包();

        for (UUID uuid : playerUUIDs) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                发送包给玩家(player, packet);
            }
        }
    }

    // ==================== 邀请通知 ====================

    /**
     * 发送邀请通知给目标玩家
     * 目标玩家不在线时静默忽略（邀请记录仍在服务端，重连后可通过UI查询）
     *
     * @param target 目标玩家
     * @param inviterUUID 邀请者UUID
     * @param inviterName 邀请者名称
     */
    public static void 发送邀请通知(ServerPlayer target, UUID inviterUUID, String inviterName) {
        发送包给玩家(target, new 队伍邀请通知包(inviterUUID, inviterName));
    }

    /**
     * 同步玩家的所有待处理邀请（登录时调用）
     * 将服务端未过期的邀请逐条推送给客户端
     *
     * @param player 刚登录的玩家
     */
    public static void 同步待处理邀请(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        long currentTime = player.level().getGameTime();
        var invites = 队伍邀请管理器.获取待处理邀请(player.getUUID(), currentTime);

        for (var invite : invites) {
            //尝试获取邀请者名称
            ServerPlayer inviter = server.getPlayerList().getPlayer(invite.邀请者UUID());
            String inviterName = inviter != null
                    ? inviter.getName().getString()
                    : invite.邀请者UUID().toString().substring(0, 8);

            发送包给玩家(player, new 队伍邀请通知包(invite.邀请者UUID(), inviterName));
        }
    }

    // ==================== 事件通知 ====================

    /**
     * 发送成员变动事件通知给队伍所有在线成员
     * 轻量级通知，用于客户端UI提示
     *
     * @param server 服务器实例
     * @param teamId 队伍ID
     * @param event 事件类型
     * @param relatedUUID 相关玩家UUID
     * @param relatedName 相关玩家名称
     * @param excludeUUID 排除的UUID（通常是事件触发者自己），null表示不排除
     */
    public static void 发送事件通知(MinecraftServer server, UUID teamId,队伍成员更新包.事件类型 event,
                                    UUID relatedUUID, String relatedName,
                                    @Nullable UUID excludeUUID) {

        队伍成员更新包 packet = new 队伍成员更新包(event, relatedUUID, relatedName);

        队伍世界数据 worldData = 队伍世界数据.获取(server);
        worldData.获取队伍(teamId).ifPresent(team -> {
            for (UUID memberUUID : team.获取所有成员UUID()) {
                if (memberUUID.equals(excludeUUID)) continue;

                ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
                if (member != null) {
                    发送包给玩家(member, packet);
                }
            }
        });
    }

    // ==================== 消息广播 ====================

    /**
     * 向队伍所有在线成员广播聊天消息
     *
     * @param server 服务器实例
     * @param teamId 队伍ID
     * @param message 消息内容（应使用Component.translatable）
     * @param excludeUUID 排除的玩家UUID（通常是操作者自己），null表示不排除
     */
    public static void 广播消息给队伍(MinecraftServer server, UUID teamId,
                                      Component message, @Nullable UUID excludeUUID) {
        队伍世界数据 worldData = 队伍世界数据.获取(server);

        worldData.获取队伍(teamId).ifPresent(team -> {
            for (UUID memberUUID : team.获取所有成员UUID()) {
                if (memberUUID.equals(excludeUUID)) continue;

                ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
                if (member != null) {
                    member.sendSystemMessage(message);
                }
            }
        });
    }

    /**
     * 向指定UUID列表的在线玩家广播消息
     * 用于队伍解散后通知前成员（此时队伍已不存在）
     *
     * @param server 服务器实例
     * @param playerUUIDs 玩家UUID列表
     * @param message 消息内容
     * @param excludeUUID 排除的UUID，null表示不排除
     */
    public static void 广播消息给玩家列表(MinecraftServer server, List<UUID> playerUUIDs,
                                          Component message, @Nullable UUID excludeUUID) {
        for (UUID uuid : playerUUIDs) {
            if (uuid.equals(excludeUUID)) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }

    // ==================== 内部工具 ====================

    private static void 发送包给玩家(ServerPlayer player, Object packet) {
        ModNetwork.getChannel().send(
                PacketDistributor.PLAYER.with(() -> player),
                packet
        );
    }
}
