// 文件路径: src/main/java/com/v2t/puellamagi/system/series/系列注册表.java

package com.v2t.puellamagi.system.series;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.series.I系列;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * 系列注册表
 * 管理所有魔法少女系列的注册、查询和生命周期分发
 */
public final class 系列注册表 {
    private 系列注册表() {}

    // 系列ID -> 系列实例
    private static final Map<ResourceLocation, I系列>注册表 = new HashMap<>();

    //缓存
    private static List<ResourceLocation> 系列ID缓存 = null;

    // ==================== 注册与查询 ====================

    /**
     * 注册一个系列
     */
    public static void 注册(I系列 series) {
        if (series == null) return;

        ResourceLocation id = series.获取ID();
        if (注册表.containsKey(id)) {
            PuellaMagi.LOGGER.warn("系列 {} 已存在，将被覆盖", id);
        }

        注册表.put(id, series);
        系列ID缓存 = null;
        PuellaMagi.LOGGER.info("注册魔法少女系列: {}", id);
    }

    /**
     * 获取系列实例
     */
    public static Optional<I系列> 获取(ResourceLocation id) {
        return Optional.ofNullable(注册表.get(id));
    }

    /**
     * 检查系列是否已注册
     */
    public static boolean 是否已注册(ResourceLocation id) {
        return 注册表.containsKey(id);
    }

    /**
     * 获取所有已注册系列的ID
     */
    public static List<ResourceLocation> 获取所有系列ID() {
        if (系列ID缓存 == null) {
            系列ID缓存 = List.copyOf(注册表.keySet());
        }
        return 系列ID缓存;
    }

    /**
     * 获取所有系列（只读）
     */
    public static Collection<I系列> 获取所有系列() {
        return Collections.unmodifiableCollection(注册表.values());
    }

    /**
     * 获取已注册系列数量
     */
    public static int 获取系列数量() {
        return 注册表.size();
    }

    // ==================== 生命周期分发 ====================

    /**
     * 分发玩家Tick到对应的系列
     */
    public static void tickPlayer(ServerPlayer player) {
        获取玩家系列(player).ifPresent(series -> {
            series.tick(player);
        });
    }

    /**
     * 分发玩家登录事件
     */
    public static void onPlayerLogin(ServerPlayer player) {
        获取玩家系列(player).ifPresent(series -> {
            series.玩家登录时(player);
        });
    }

    /**
     * 分发玩家登出事件
     */
    public static void onPlayerLogout(ServerPlayer player) {
        获取玩家系列(player).ifPresent(series -> {
            series.玩家登出时(player);
        });
    }

    /**
     * 分发玩家重生事件
     */
    public static void onPlayerRespawn(ServerPlayer player) {
        获取玩家系列(player).ifPresent(series -> {
            series.玩家重生时(player);
        });
    }

    /**
     * 分发维度切换事件
     */
    public static void onDimensionChange(ServerPlayer player) {
        获取玩家系列(player).ifPresent(series -> {
            series.维度切换时(player);
        });
    }

    // ==================== 内部方法 ====================

    /**
     * 获取玩家所属系列
     */
    private static Optional<I系列> 获取玩家系列(ServerPlayer player) {
        return 能力工具.获取契约能力(player).filter(contract -> contract.是否已契约())
                .map(contract -> contract.获取系列ID())
                .flatMap(系列注册表::获取);
    }

    // ==================== 测试支持 ====================

    /**
     * 清空注册表（仅用于测试）
     */
    public static void 清空() {
        注册表.clear();
        系列ID缓存 = null;
    }
}
