// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍管理器.java

package com.v2t.puellamagi.system.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 队伍管理器
 *
 * 职责：队伍操作的统一API入口
 * 委托具体逻辑给队伍世界数据
 *
 * 所有操作通过此类调用，不直接操作世界数据
 */
public final class 队伍管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/TeamManager");

    private 队伍管理器() {}

    // ==================== 队伍操作 ====================

    public static 操作结果 创建队伍(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return 操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        if (worldData.玩家有队伍(player.getUUID())) {
            return 操作结果.失败("message.puellamagi.team.error.already_in_team");
        }

        long gameTime = player.level().getGameTime();
        队伍数据 team = worldData.创建队伍(player.getUUID(), gameTime);

        if (team == null) {
            return 操作结果.失败("message.puellamagi.team.error.create_failed");
        }

        return 操作结果.成功("message.puellamagi.team.created");
    }

    public static 操作结果 离开队伍(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return 操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        if (!worldData.玩家有队伍(player.getUUID())) {
            return 操作结果.失败("message.puellamagi.team.error.no_team");
        }

        boolean success = worldData.离开队伍(player.getUUID());

        if (!success) {
            return 操作结果.失败("message.puellamagi.team.error.leave_failed");
        }

        return 操作结果.成功("message.puellamagi.team.left");
    }

    /**
     * 踢出成员
     * 通过权限系统判断操作者是否有踢出权限，不硬编码队长检查
     */
    public static 操作结果 踢出成员(ServerPlayer operator, UUID targetUUID) {
        MinecraftServer server = operator.getServer();
        if (server == null) return 操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        // 获取操作者队伍
        Optional<队伍数据> teamOpt = worldData.获取玩家队伍(operator.getUUID());
        if (teamOpt.isEmpty()) {
            return 操作结果.失败("message.puellamagi.team.error.no_team");
        }

        队伍数据 team = teamOpt.get();

        // 权限检查
        if (!team.成员有权限(operator.getUUID(), 队伍权限.踢出成员)) {
            return 操作结果.失败("message.puellamagi.team.error.no_permission");
        }

        // 不能踢自己
        if (operator.getUUID().equals(targetUUID)) {
            return 操作结果.失败("message.puellamagi.team.error.kick_self");
        }

        // 不能踢不在队伍中的人
        if (!team.是成员(targetUUID)) {
            return 操作结果.失败("message.puellamagi.team.error.not_in_team");
        }

        // 执行踢出
        team.移除成员(targetUUID);
        worldData.移除玩家队伍映射(targetUUID);
        worldData.标记已修改();

        return 操作结果.成功("message.puellamagi.team.kick_success");
    }

    /**
     * 解散队伍
     * 通过权限系统判断操作者是否有解散权限
     */
    public static 操作结果 解散队伍(ServerPlayer operator) {
        MinecraftServer server = operator.getServer();
        if (server == null) return 操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        Optional<队伍数据> teamOpt = worldData.获取玩家队伍(operator.getUUID());
        if (teamOpt.isEmpty()) {
            return 操作结果.失败("message.puellamagi.team.error.no_team");
        }

        队伍数据 team = teamOpt.get();

        // 权限检查
        if (!team.成员有权限(operator.getUUID(), 队伍权限.解散队伍)) {
            return 操作结果.失败("message.puellamagi.team.error.no_permission");
        }

        // 清理所有成员的反向索引
        List<UUID> members = team.获取所有成员UUID();
        for (UUID memberUUID : members) {
            worldData.移除玩家队伍映射(memberUUID);
        }

        // 移除队伍
        worldData.移除队伍(team.获取队伍ID());
        worldData.标记已修改();

        LOGGER.info("队伍 {} 被 {} 解散，共 {} 名成员",team.获取队伍ID(), operator.getUUID(), members.size());

        return 操作结果.成功("message.puellamagi.team.disbanded");
    }

    /**
     * 转移队长
     * 通过权限系统判断操作者是否有转移队长权限
     */
    public static 操作结果 转移队长(ServerPlayer operator, UUID targetUUID) {
        MinecraftServer server = operator.getServer();
        if (server == null) return 操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        Optional<队伍数据> teamOpt = worldData.获取玩家队伍(operator.getUUID());
        if (teamOpt.isEmpty()) {
            return 操作结果.失败("message.puellamagi.team.error.no_team");
        }

        队伍数据 team = teamOpt.get();

        // 权限检查
        if (!team.成员有权限(operator.getUUID(), 队伍权限.转移队长)) {
            return 操作结果.失败("message.puellamagi.team.error.no_permission");
        }

        boolean success = team.转移队长(operator.getUUID(), targetUUID);

        if (!success) {
            return 操作结果.失败("message.puellamagi.team.error.transfer_failed");
        }

        worldData.标记已修改();
        return 操作结果.成功("message.puellamagi.team.transfer_success");
    }

    public static 操作结果 强制拉人(ServerPlayer operator, ServerPlayer target) {
        MinecraftServer server = operator.getServer();
        if (server == null) return 操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        UUID teamId = worldData.获取玩家队伍ID(operator.getUUID());
        if (teamId == null) {
            return 操作结果.失败("message.puellamagi.team.error.no_team");
        }

        if (worldData.玩家有队伍(target.getUUID())) {
            return 操作结果.失败("message.puellamagi.team.error.target_has_team");
        }

        long gameTime = operator.level().getGameTime();
        boolean success = worldData.加入队伍(target.getUUID(), teamId, gameTime);

        if (!success) {
            return 操作结果.失败("message.puellamagi.team.error.add_failed");
        }

        return 操作结果.成功("message.puellamagi.team.add_success");
    }

    // ==================== 查询 ====================

    /**
     * 判断两个玩家是否同队
     */
    public static boolean 是否同队(MinecraftServer server, UUID playerA, UUID playerB) {
        if (server == null) return false;
        return 队伍世界数据.获取(server).是否同队(playerA, playerB);
    }

    /**
     * 获取玩家所在队伍
     */
    public static Optional<队伍数据> 获取玩家队伍(MinecraftServer server, UUID playerUUID) {
        if (server == null) return Optional.empty();
        return 队伍世界数据.获取(server).获取玩家队伍(playerUUID);
    }

    /**
     * 玩家是否有队伍
     */
    public static boolean 玩家有队伍(MinecraftServer server, UUID playerUUID) {
        if (server == null) return false;
        return 队伍世界数据.获取(server).玩家有队伍(playerUUID);
    }

    /**
     * 获取成员个人配置（跨系统查询用）
     *
     * 示例：时停觉醒系统查询目标是否开启觉醒
     * 队伍管理器.获取个人配置(server, playerUUID, "timestopAwakening")
     */
    public static boolean 获取个人配置(MinecraftServer server, UUID playerUUID, String key) {
        if (server == null) return false;

        return 队伍世界数据.获取(server).获取玩家队伍(playerUUID)
                .flatMap(team -> team.获取成员(playerUUID))
                .map(member -> member.获取个人配置().获取配置(key))
                .orElse(false);
    }

    // ==================== 操作结果 ====================

    /**
     * 操作结果封装
     * 消息为翻译键，由命令层通过Component.translatable()渲染
     */
    public record 操作结果(boolean 成功, String 消息) {

        public static 操作结果 成功(String translationKey) {
            return new 操作结果(true, translationKey);
        }

        public static 操作结果 失败(String translationKey) {
            return new 操作结果(false, translationKey);
        }
    }
}
