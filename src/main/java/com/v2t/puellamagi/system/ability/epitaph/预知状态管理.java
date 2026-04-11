package com.v2t.puellamagi.system.ability.epitaph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预知状态管理
 *
 * 管理预知/时间删除能力的5阶段状态机
 * 每个使用者独立维护一份状态
 *
 * Phase 0: 待机
 * Phase 1: 录制中（悄悄录制，无提示）
 * Phase 1.5: 等待回放（自己录完了，等录制组中其他人也录完）
 * Phase 2: 回溯+复刻（命运锁定，使用者看影子）
 * Phase 3: 时间删除（使用者自由行动，不可见）
 *
 * 个人状态机 + 全局录制组协调 = 正确分层：
 * - 本类管个人进度（这个玩家处于预知流程的哪个阶段）
 * - 录制组管理器管全局协调（谁在录制、何时合并、何时开始回放）
 */
public final class 预知状态管理 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/EpitaphState");

    // ==================== 阶段枚举 ====================

    public enum 阶段 {
        待机(0),
        录制中(1),
        等待回放(2),
        复刻中(3),
        时间删除(4);

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

        // 录制组关联
        private UUID 录制组ID = null;

        // Phase 2/3: 复刻相关
        private int 当前复刻帧 = 0;
        private int 总复刻帧数 = 0;

        // Phase 4: 时间删除相关
        private int 时删激活帧 = 0;

        public 阶段 获取阶段() { return 当前阶段; }
        public long 获取阶段开始时间() { return 阶段开始时间; }
        public int 获取已录制帧数() { return 已录制帧数; }
        public int 获取当前复刻帧() { return 当前复刻帧; }
        public int 获取总复刻帧数() { return 总复刻帧数; }

        public int 获取时删激活帧() { return 时删激活帧; }

        @javax.annotation.Nullable
        public UUID 获取录制组ID() { return 录制组ID; }

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
     * 进入等待回放阶段（Phase 1 → Phase 1.5）
     *
     * 自己录完了，但录制组中还有其他人在录制
     * 等录制组关闭后由录制组管理器触发开始复刻
     *
     * @param groupID 所在录制组ID
     */
    public static boolean 进入等待回放(UUID playerUUID, long gameTime, UUID groupID) {
        阶段 current = 获取阶段(playerUUID);
        if (current != 阶段.录制中) {
            LOGGER.warn("玩家 {} 不在录制阶段，无法进入等待回放（当前: {}）",
                    playerUUID, current);
            return false;
        }

        玩家预知状态 state = 状态表.get(playerUUID);
        state.当前阶段 = 阶段.等待回放;
        state.阶段开始时间 = gameTime;
        state.录制组ID = groupID;

        LOGGER.debug("玩家 {} 进入等待回放（录制组: {}）", playerUUID, groupID);
        return true;
    }

    /**
     * 结束录制/等待，进入复刻阶段（Phase 1/1.5 → Phase 2）
     *
     * 可以从录制中直接进入（单人场景或最后一个录完的人）
     * 也可以从等待回放进入（录制组关闭触发）
     *
     * @param totalFrames 合并后的总帧数（复刻要播放多少帧）
     * @param groupID     录制组ID
     */
    public static boolean 开始复刻(UUID playerUUID, long gameTime, int totalFrames, UUID groupID) {
        阶段 current = 获取阶段(playerUUID);
        if (current != 阶段.录制中 && current != 阶段.等待回放) {
            LOGGER.warn("玩家 {} 不在录制/等待阶段，无法开始复刻（当前: {}）",
                    playerUUID, current);
            return false;
        }

        玩家预知状态 state = 状态表.get(playerUUID);
        state.当前阶段 = 阶段.复刻中;
        state.阶段开始时间 = gameTime;
        state.当前复刻帧 = 0;
        state.总复刻帧数 = totalFrames;
        state.录制组ID = groupID;

        LOGGER.debug("玩家 {} 开始复刻（总帧数: {}, 录制组: {}）", playerUUID, totalFrames, groupID);
        return true;
    }

    /**
     * 兼容旧接口：无录制组ID的开始复刻（单人场景退化）
     */
    public static boolean 开始复刻(UUID playerUUID, long gameTime, int totalFrames) {
        return 开始复刻(playerUUID, gameTime, totalFrames, null);
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
        state.时删激活帧 = state.当前复刻帧;

        LOGGER.debug("玩家 {} 进入时间删除（激活帧={}）", playerUUID, state.时删激活帧);
        return true;
    }

    // ==================== 查询辅助 ====================

    /**
     * 玩家是否处于等待回放中
     */
    public static boolean 是否等待回放中(UUID playerUUID) {
        return 获取阶段(playerUUID) == 阶段.等待回放;
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
