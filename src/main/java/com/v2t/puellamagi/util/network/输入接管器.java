package com.v2t.puellamagi.util.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 输入接管器
 *
 * 服务端工具：控制哪些玩家的C2S输入包被丢弃
 *
 * 两种模式：
 * FULL -拦截所有C2S包（移动+动作），由服务端完全控制
 *用于：幻术、催眠、傀儡操控
 *
 * REPLAY - 不拦截任何C2S包，纯输入回放
 *          客户端注入按键 → MC自己发C2S → 服务端正常处理
 *          所有动画/音效/背包消耗都由MC正常流程产生
 *          用于：预知复刻（命运锁定）
 *
 * 被接管状态本身仍然有意义（是否被接管 = 是否在被某系统控制中）
 * 但是否拦截包取决于模式
 */
public final class 输入接管器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/InputTakeover");

    /**
     * 接管模式
     */
    public enum 接管模式 {
        /**拦截所有C2S包 */
        FULL,
        /** 不拦截任何C2S包（纯输入回放） */
        REPLAY
    }

    /**
     * 接管信息
     */
    private record 接管信息(String 来源, 接管模式 模式) {}

    /**
     * 被接管的玩家集合
     * key =玩家UUID
     * value = 接管信息（来源 + 模式）
     */
    private static final Map<UUID, 接管信息> 被接管玩家 = new ConcurrentHashMap<>();

    private 输入接管器() {}

    //==================== 操作 ====================

    /**
     * 开始接管玩家输入（默认FULL模式）
     *
     * @param playerUUID 目标玩家UUID
     * @param sourceId 接管来源标识（如"epitaph"、"illusion"）
     * @return 是否成功（如果已被其他来源接管则失败）
     */
    public static boolean 接管(UUID playerUUID, String sourceId) {
        return 接管(playerUUID, sourceId, 接管模式.FULL);
    }

    /**
     * 开始接管玩家输入（指定模式）
     *
     * @param playerUUID 目标玩家UUID
     * @param sourceId   接管来源标识
     * @param mode       接管模式（FULL或REPLAY）
     * @return 是否成功
     */
    public static boolean 接管(UUID playerUUID, String sourceId, 接管模式 mode) {
        接管信息 existing = 被接管玩家.get(playerUUID);
        if (existing != null && !existing.来源().equals(sourceId)) {
            LOGGER.warn("玩家 {} 已被 {} 接管，{} 接管失败",
                    playerUUID, existing.来源(), sourceId);
            return false;
        }

        被接管玩家.put(playerUUID, new 接管信息(sourceId, mode));
        LOGGER.debug("玩家 {} 输入被{} 接管（模式: {}）", playerUUID, sourceId, mode);
        return true;
    }

    /**
     * 释放玩家输入
     *
     * @param playerUUID 目标玩家UUID
     * @param sourceId   接管来源标识（只有同来源才能释放）
     */
    public static void 释放(UUID playerUUID, String sourceId) {
        接管信息 existing = 被接管玩家.get(playerUUID);
        if (existing != null && existing.来源().equals(sourceId)) {
            被接管玩家.remove(playerUUID);
            LOGGER.debug("玩家 {} 输入被 {} 释放", playerUUID, sourceId);
        }
    }

    /**
     * 强制释放（不检查来源，用于异常清理）
     */
    public static void 强制释放(UUID playerUUID) {
        接管信息 removed = 被接管玩家.remove(playerUUID);
        if (removed != null) {
            LOGGER.debug("玩家 {} 输入被强制释放（原来源: {}）", playerUUID, removed.来源());
        }
    }

    /**
     * 正在重放包的玩家集合
     * 重放期间限制系统应该放行
     */
    private static final Set<UUID> 正在重放 = ConcurrentHashMap.newKeySet();

    public static void 标记重放中(UUID playerUUID) {
        正在重放.add(playerUUID);
    }

    public static void 取消重放标记(UUID playerUUID) {
        正在重放.remove(playerUUID);
    }

    public static boolean 是否重放中(UUID playerUUID) {
        return 正在重放.contains(playerUUID);
    }

    // ==================== 查询 ====================

    /**
     * 检查是否应该拦截该玩家的C2S包
     *
     * FULL模式：拦截（返回true）
     * REPLAY模式：不拦截（返回false）
     * 未被接管：不拦截（返回false）
     *
     * Mixin中调用此方法决定是否cancel包处理
     */
    public static boolean 是否拦截(UUID playerUUID) {
        接管信息 info = 被接管玩家.get(playerUUID);
        if (info == null) return false;
        return info.模式() == 接管模式.FULL;
    }

    /**
     * 检查玩家是否被接管（不管模式）
     *
     * 用于判断"这个玩家是否在被某系统控制中"
     * 不用于决定是否拦截包
     */
    public static boolean 是否被接管(UUID playerUUID) {
        return 被接管玩家.containsKey(playerUUID);
    }

    /**
     * 获取接管来源
     */
    @Nullable
    public static String 获取接管来源(UUID playerUUID) {
        接管信息 info = 被接管玩家.get(playerUUID);
        return info != null ? info.来源() : null;
    }

    /**
     * 获取所有被接管的玩家UUID
     */
    public static Set<UUID> 获取所有被接管玩家() {
        return 被接管玩家.keySet();
    }

    // ==================== 生命周期 ====================

    /**
     * 玩家下线时清理
     */
    public static void 玩家下线(UUID playerUUID) {
        强制释放(playerUUID);}

    /**
     * 服务器关闭时清理
     */
    public static void 清除全部() {
        被接管玩家.clear();
        正在重放.clear();
        LOGGER.debug("输入接管器已清空");
    }
}
