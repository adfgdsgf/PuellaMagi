// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍邀请管理器.java

package com.v2t.puellamagi.system.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队伍邀请管理器
 *
 * 职责：管理邀请/申请的临时状态
 *邀请不持久化，服务器重启后清空
 *
 * 邀请流程：
 * 1. A邀请B →创建邀请记录
 * 2. B收到通知 → 查看待处理邀请
 * 3. B接受/拒绝 → 处理邀请
 * 4. 超时自动过期
 */
public final class 队伍邀请管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/TeamInvite");

    /** 邀请超时时间（tick），60秒 */
    private static final long 邀请超时TICK = 20* 60;

    /** 被邀请者UUID → 邀请列表 */
    private static final Map<UUID, List<邀请数据>> 待处理邀请 = new ConcurrentHashMap<>();

    private 队伍邀请管理器() {}

    //==================== 邀请数据 ====================

    /**
     * 单条邀请记录
     */
    public record 邀请数据(
            UUID 邀请者UUID,
            UUID 队伍ID,
            long 创建时间
    ) {
        /**
         * 是否已过期
         */
        public boolean 已过期(long currentTime) {
            return currentTime - 创建时间 > 邀请超时TICK;
        }
    }

    // ==================== 邀请操作 ====================

    /**
     * 发送邀请
     *
     * @param inviter 邀请者
     * @param target 被邀请者
     * @return 操作结果
     */
    public static 队伍管理器.操作结果 发送邀请(ServerPlayer inviter, ServerPlayer target) {
        MinecraftServer server = inviter.getServer();
        if (server == null) return 队伍管理器.操作结果.失败("message.puellamagi.team.error.server");

        队伍世界数据 worldData = 队伍世界数据.获取(server);

        // 邀请者必须有队伍
        UUID teamId = worldData.获取玩家队伍ID(inviter.getUUID());
        if (teamId == null) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.no_team");
        }

        // 目标不能已有队伍
        if (worldData.玩家有队伍(target.getUUID())) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.target_has_team");
        }

        // 不能邀请自己
        if (inviter.getUUID().equals(target.getUUID())) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.invite_self");
        }

        // 检查是否已有相同邀请
        List<邀请数据> targetInvites = 待处理邀请.computeIfAbsent(
                target.getUUID(), k -> new ArrayList<>()
        );

        long currentTime = inviter.level().getGameTime();

        // 清理过期邀请
        targetInvites.removeIf(invite -> invite.已过期(currentTime));

        // 检查重复邀请
        boolean 已存在 = targetInvites.stream()
                .anyMatch(invite -> invite.队伍ID().equals(teamId));
        if (已存在) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.already_invited");
        }

        // 添加邀请
        targetInvites.add(new 邀请数据(inviter.getUUID(), teamId, currentTime));
        LOGGER.info("玩家 {} 邀请 {} 加入队伍 {}",
                inviter.getName().getString(),
                target.getName().getString(),
                teamId.toString().substring(0, 8));

        return 队伍管理器.操作结果.成功("message.puellamagi.team.invite_sent");
    }

    /**
     * 接受邀请
     *
     * @param player 接受者
     * @param inviterUUID 邀请者UUID（用于确定接受哪个邀请）
     * @return 操作结果
     */
    public static 队伍管理器.操作结果 接受邀请(ServerPlayer player, UUID inviterUUID) {
        MinecraftServer server = player.getServer();
        if (server == null) return 队伍管理器.操作结果.失败("message.puellamagi.team.error.server");

        List<邀请数据> invites = 待处理邀请.get(player.getUUID());
        if (invites == null || invites.isEmpty()) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.no_invite");
        }

        long currentTime = player.level().getGameTime();

        // 清理过期邀请
        invites.removeIf(invite -> invite.已过期(currentTime));

        // 找到对应邀请
        邀请数据 targetInvite = null;
        for (邀请数据 invite : invites) {
            if (invite.邀请者UUID().equals(inviterUUID)) {
                targetInvite = invite;
                break;
            }
        }

        if (targetInvite == null) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.invite_expired");
        }

        // 检查玩家是否已有队伍
        队伍世界数据 worldData = 队伍世界数据.获取(server);
        if (worldData.玩家有队伍(player.getUUID())) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.already_in_team");
        }

        // 加入队伍
        boolean success = worldData.加入队伍(
                player.getUUID(), targetInvite.队伍ID(), currentTime
        );

        if (!success) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.join_failed");
        }

        // 移除所有邀请（已加入队伍）
        待处理邀请.remove(player.getUUID());

        LOGGER.info("玩家 {} 接受邀请加入队伍 {}",
                player.getName().getString(),
                targetInvite.队伍ID().toString().substring(0, 8));

        return 队伍管理器.操作结果.成功("message.puellamagi.team.invite_accepted");
    }

    /**
     * 拒绝邀请
     *
     * @param player 拒绝者
     * @param inviterUUID 邀请者UUID
     * @return 操作结果
     */
    public static 队伍管理器.操作结果 拒绝邀请(ServerPlayer player, UUID inviterUUID) {
        List<邀请数据> invites = 待处理邀请.get(player.getUUID());
        if (invites == null || invites.isEmpty()) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.no_invite");
        }

        boolean removed = invites.removeIf(
                invite -> invite.邀请者UUID().equals(inviterUUID)
        );

        if (!removed) {
            return 队伍管理器.操作结果.失败("message.puellamagi.team.error.invite_expired");
        }

        // 清理空列表
        if (invites.isEmpty()) {
            待处理邀请.remove(player.getUUID());
        }

        LOGGER.debug("玩家 {} 拒绝了来自 {} 的邀请",
                player.getName().getString(), inviterUUID);

        return 队伍管理器.操作结果.成功("message.puellamagi.team.invite_declined");
    }

    // ==================== 查询 ====================

    /**
     * 获取玩家的待处理邀请列表
     */
    public static List<邀请数据> 获取待处理邀请(UUID playerUUID, long currentTime) {
        List<邀请数据> invites = 待处理邀请.get(playerUUID);
        if (invites == null) return Collections.emptyList();

        // 清理过期的
        invites.removeIf(invite -> invite.已过期(currentTime));

        if (invites.isEmpty()) {
            待处理邀请.remove(playerUUID);
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(invites);
    }

    /**
     * 获取待处理邀请数量
     */
    public static int 获取待处理邀请数量(UUID playerUUID, long currentTime) {
        return 获取待处理邀请(playerUUID, currentTime).size();
    }

    /**
     * 是否有待处理邀请
     */
    public static boolean 有待处理邀请(UUID playerUUID, long currentTime) {
        return !获取待处理邀请(playerUUID, currentTime).isEmpty();
    }

    // ==================== 生命周期 ====================

    /**
     * 玩家登出时清理
     */
    public static void onPlayerLogout(UUID playerUUID) {
        // 作为被邀请者：保留邀请（重连后仍可接受）
        // 作为邀请者：不需要特殊处理（邀请会超时）
    }

    /**
     * 清空所有邀请
     */
    public static void clearAll() {
        待处理邀请.clear();
    }
}
