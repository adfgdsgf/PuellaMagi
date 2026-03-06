package com.v2t.puellamagi.util.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存在屏蔽器
 *
 * 服务端工具：控制某个实体对哪些玩家不可见
 * 被屏蔽的实体：服务端不向指定玩家发送该实体的S2C包
 *（位置、动作、装备、状态等）
 *
 * 与隐身药水的区别：
 * - 隐身药水：客户端收到实体数据，只是不渲染/半透明
 * - 存在屏蔽：客户端完全收不到实体数据，实体不存在
 *
 * 复用场景：时间删除（使用者消失）、真隐身、相位转移
 */
public final class 存在屏蔽器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/ExistenceBlocker");

    /**
     *屏蔽规则表
     * key = 被屏蔽实体的UUID
     * value = 屏蔽条目（来源ID + 对哪些玩家屏蔽）
     */
    private static final Map<UUID, 屏蔽条目> 屏蔽表 = new ConcurrentHashMap<>();

    private 存在屏蔽器() {}

    // ==================== 数据结构 ====================

    /**
     * 屏蔽条目
     */
    public static class 屏蔽条目 {
        private final String 来源ID;
        private final boolean 对所有人屏蔽;
        private final Set<UUID> 屏蔽目标;

        /**
         * 对所有人屏蔽
         */
        public 屏蔽条目(String sourceId) {
            this.来源ID = sourceId;
            this.对所有人屏蔽 = true;
            this.屏蔽目标 = Collections.emptySet();
        }

        /**
         * 对指定玩家屏蔽
         */
        public 屏蔽条目(String sourceId, Set<UUID> targets) {
            this.来源ID = sourceId;
            this.对所有人屏蔽 = false;
            this.屏蔽目标 = new HashSet<>(targets);
        }

        public String 获取来源ID() { return 来源ID; }

        /**
         * 检查是否对指定玩家屏蔽
         */
        public boolean 对该玩家屏蔽(UUID viewerUUID) {
            return 对所有人屏蔽|| 屏蔽目标.contains(viewerUUID);
        }
    }

    // ==================== 操作 ====================

    /**
     * 对所有人屏蔽实体
     *
     * @param entityUUID 被屏蔽的实体UUID
     * @param sourceId   屏蔽来源标识
     */
    public static void 屏蔽全部(UUID entityUUID, String sourceId) {
        屏蔽表.put(entityUUID, new 屏蔽条目(sourceId));
        LOGGER.debug("实体 {} 被 {} 对所有人屏蔽", entityUUID, sourceId);
    }

    /**
     * 对指定玩家屏蔽实体
     *
     * @param entityUUID 被屏蔽的实体UUID
     * @param sourceId   屏蔽来源标识
     * @param viewers    对这些玩家屏蔽
     */
    public static void 屏蔽指定(UUID entityUUID, String sourceId, Set<UUID> viewers) {
        屏蔽表.put(entityUUID, new 屏蔽条目(sourceId, viewers));
        LOGGER.debug("实体 {} 被 {} 对 {} 个玩家屏蔽",
                entityUUID, sourceId, viewers.size());
    }

    /**
     * 对除了指定玩家以外的所有人屏蔽
     * 常用场景：使用者自己能看到自己，其他人看不到
     *
     * @param entityUUID 被屏蔽的实体UUID
     * @param sourceId   屏蔽来源标识
     * @param exceptUUID 排除的玩家（不对此玩家屏蔽）
     */
    public static void 屏蔽除外(UUID entityUUID, String sourceId, UUID exceptUUID) {
        // 使用特殊标记实现"除了某人对所有人屏蔽"屏蔽表.put(entityUUID, new 排除屏蔽条目(sourceId, exceptUUID));
        LOGGER.debug("实体 {} 被 {} 屏蔽（排除 {}）",
                entityUUID, sourceId, exceptUUID);
    }

    /**
     * 解除屏蔽
     *
     * @param entityUUID 被屏蔽的实体UUID
     * @param sourceId   屏蔽来源标识（只有同来源才能解除）
     */
    public static void 解除屏蔽(UUID entityUUID, String sourceId) {
        屏蔽条目 entry = 屏蔽表.get(entityUUID);
        if (entry != null && entry.获取来源ID().equals(sourceId)) {
            屏蔽表.remove(entityUUID);
            LOGGER.debug("实体 {} 的屏蔽被{} 解除", entityUUID, sourceId);
        }
    }

    /**
     * 强制解除屏蔽（不检查来源）
     */
    public static void 强制解除(UUID entityUUID) {
        屏蔽条目 removed = 屏蔽表.remove(entityUUID);
        if (removed != null) {
            LOGGER.debug("实体 {} 的屏蔽被强制解除（原来源: {}）",
                    entityUUID, removed.获取来源ID());
        }
    }

    // ==================== 查询 ====================

    /**
     * 检查实体是否对指定观察者屏蔽
     * Mixin中发送S2C包前调用此方法
     *
     * @param entityUUID 实体UUID
     * @param viewerUUID 观察者（接收S2C包的玩家）UUID
     * @return true = 应该屏蔽（不发包给此玩家）
     */
    public static boolean 是否屏蔽(UUID entityUUID, UUID viewerUUID) {
        屏蔽条目 entry = 屏蔽表.get(entityUUID);
        if (entry == null) return false;
        return entry.对该玩家屏蔽(viewerUUID);
    }

    /**
     * 检查实体是否被任何人屏蔽
     */
    public static boolean 是否有屏蔽(UUID entityUUID) {
        return 屏蔽表.containsKey(entityUUID);
    }

    // ==================== 生命周期 ====================

    /**
     * 实体移除时清理
     */
    public static void 实体移除(UUID entityUUID) {
        强制解除(entityUUID);
    }

    /**
     * 玩家下线时清理（作为被屏蔽实体）
     */
    public static void 玩家下线(UUID playerUUID) {
        强制解除(playerUUID);
    }

    /**
     * 服务器关闭时清理
     */
    public static void 清除全部() {
        屏蔽表.clear();
        LOGGER.debug("存在屏蔽器已清空");
    }

    // ==================== 排除屏蔽条目 ====================

    /**
     * 特殊条目：对除了指定玩家以外的所有人屏蔽
     */
    private static class 排除屏蔽条目 extends 屏蔽条目 {
        private final UUID 排除UUID;

        public 排除屏蔽条目(String sourceId, UUID exceptUUID) {
            super(sourceId);
            this.排除UUID = exceptUUID;
        }

        @Override
        public boolean 对该玩家屏蔽(UUID viewerUUID) {
            // 排除的玩家不屏蔽，其他人全屏蔽
            return !排除UUID.equals(viewerUUID);
        }
    }
}
