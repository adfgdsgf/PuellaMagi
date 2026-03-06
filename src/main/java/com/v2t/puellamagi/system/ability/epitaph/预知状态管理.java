package com.v2t.puellamagi.system.ability.epitaph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预知状态管理
 *
 * 管理预知/时间删除能力的4阶段状态机
 * 每个使用者独立维护一份状态
 *
 * Phase 0: 待机
 * Phase 1: 录制中（悄悄录制，无提示）
 * Phase 2: 回溯+复刻（命运锁定，使用者看影子）
 * Phase 3: 时间删除（使用者自由行动，不可见）
 */
public final class 预知状态管理 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/EpitaphState");

    // ==================== 阶段枚举 ====================

    public enum 阶段 {
        待机(0),
        录制中(1),
        复刻中(2),
        时间删除(3);

        private final int 序号;

        阶段(int index) {
            this.序号 = index;
        }

        public int 获取序号() { return 序号; }
    }

    // ==================== 玩家状态 ====================

    /**
     * 单个玩家的预知状态
     */
    public static class 玩家预知状态 {
        private 阶段 当前阶段 = 阶段.待机;
        private long 阶段开始时间 = 0;

        // Phase 1: 录制相关
        private int 已录制帧数 = 0;

        // Phase 2: 复刻相关
        private int 当前复刻帧 = 0;
        private int 总复刻帧数 = 0;

        public 阶段 获取阶段() { return 当前阶段; }
        public long 获取阶段开始时间() { return 阶段开始时间; }
        public int 获取已录制帧数() { return 已录制帧数; }
        public int 获取当前复刻帧() { return 当前复刻帧; }
        public int 获取总复刻帧数() { return 总复刻帧数; }

        public void 增加录制帧() { 已录制帧数++; }
        public void 推进复刻帧() { 当前复刻帧++; }
        public boolean 复刻已结束() { return 当前复刻帧 >= 总复刻帧数; }
    }

    // ==================== 状态表 ====================

    private static final Map<UUID, 玩家预知状态> 状态表 = new ConcurrentHashMap<>();

    private 预知状态管理() {}

    // ==================== 阶段转换 ====================

    /**
     * 进入录制阶段（Phase 0→ Phase 1）
     */
    public static boolean 开始录制(UUID playerUUID, long gameTime) {
        阶段 current = 获取阶段(playerUUID);
        if (current != 阶段.待机) {
            LOGGER.warn("玩家 {} 不在待机阶段，无法开始录制（当前: {}）",
                    playerUUID, current);
            return false;
        }玩家预知状态 state = new 玩家预知状态();
        state.当前阶段 = 阶段.录制中;
        state.阶段开始时间 = gameTime;
        state.已录制帧数 = 0;
        状态表.put(playerUUID, state);

        LOGGER.debug("玩家 {} 开始录制", playerUUID);
        return true;
    }

    /**
     * 结束录制，进入复刻阶段（Phase 1 → Phase 2）
     *
     * @param totalFrames 录制的总帧数（复刻要播放多少帧）
     */
    public static boolean 开始复刻(UUID playerUUID, long gameTime, int totalFrames) {
        阶段 current = 获取阶段(playerUUID);
        if (current != 阶段.录制中) {
            LOGGER.warn("玩家 {} 不在录制阶段，无法开始复刻（当前: {}）",
                    playerUUID, current);
            return false;
        }

        玩家预知状态 state = 状态表.get(playerUUID);
        state.当前阶段 = 阶段.复刻中;
        state.阶段开始时间 = gameTime;
        state.当前复刻帧 = 0;
        state.总复刻帧数 = totalFrames;

        LOGGER.debug("玩家 {} 开始复刻（总帧数: {}）", playerUUID, totalFrames);
        return true;
    }

    /**
     * 进入时间删除阶段（Phase 2 → Phase 3）
     */
    public static boolean 开始时间删除(UUID playerUUID, long gameTime) {
        阶段 current = 获取阶段(playerUUID);
        if (current != 阶段.复刻中) {
            LOGGER.warn("玩家 {} 不在复刻阶段，无法进入时间删除（当前: {}）",
                    playerUUID, current);
            return false;
        }

        玩家预知状态 state = 状态表.get(playerUUID);
        state.当前阶段 = 阶段.时间删除;
        state.阶段开始时间 = gameTime;

        LOGGER.debug("玩家 {} 进入时间删除", playerUUID);
        return true;
    }

    /**
     * 结束能力，回到待机（任何阶段 → Phase 0）
     */
    public static void 结束(UUID playerUUID) {
        阶段 old = 获取阶段(playerUUID);
        状态表.remove(playerUUID);
        LOGGER.debug("玩家 {} 结束预知能力（从 {} 回到待机）", playerUUID, old);
    }

    /**
     * 取消能力（异常终止，如死亡/下线）
     */
    public static void 取消(UUID playerUUID) {
        if (状态表.containsKey(playerUUID)) {
            阶段 old = 获取阶段(playerUUID);
            状态表.remove(playerUUID);
            LOGGER.debug("玩家 {} 预知能力被取消（从 {}）", playerUUID, old);
        }
    }

    // ==================== 查询 ====================

    /**
     * 获取玩家当前阶段
     */
    public static 阶段 获取阶段(UUID playerUUID) {
        玩家预知状态 state = 状态表.get(playerUUID);
        return state != null ? state.获取阶段() : 阶段.待机;
    }

    /**
     * 获取玩家完整状态
     */
    @javax.annotation.Nullable
    public static 玩家预知状态 获取状态(UUID playerUUID) {
        return 状态表.get(playerUUID);
    }

    /**
     * 玩家是否在使用预知能力（非待机）
     */
    public static boolean 是否活跃(UUID playerUUID) {
        return 获取阶段(playerUUID) != 阶段.待机;
    }

    /**
     * 任何玩家是否在使用预知能力
     * 用于互斥检查等
     */
    public static boolean 有任何活跃() {
        return !状态表.isEmpty();
    }

    /**
     * 玩家是否处于命运锁定中（Phase 2被影响者）
     * 注意：这里判断的是使用者自己是否在Phase 2
     * 被影响者的锁定由输入接管器管理
     */
    public static boolean 是否复刻中(UUID playerUUID) {
        return 获取阶段(playerUUID) == 阶段.复刻中;
    }

    /**
     * 玩家是否处于时间删除中*/
    public static boolean 是否时间删除中(UUID playerUUID) {
        return 获取阶段(playerUUID) == 阶段.时间删除;
    }

    // ==================== 生命周期 ====================

    /**
     * 玩家下线时清理
     */
    public static void 玩家下线(UUID playerUUID) {
        取消(playerUUID);
    }

    /**
     * 服务器关闭时清理
     */
    public static void 清除全部() {
        状态表.clear();
        LOGGER.debug("预知状态管理已清空");
    }
}
