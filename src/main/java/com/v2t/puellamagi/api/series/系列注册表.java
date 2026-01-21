// 文件路径: src/main/java/com/v2t/puellamagi/system/series/系列注册表.java

package com.v2t.puellamagi.system.series;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.series.I系列;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 系列注册表
 * 管理所有魔法少女系列的注册和查询
 */
public final class 系列注册表 {
    private 系列注册表() {}

    //系列ID -> 系列实例
    private static final Map<ResourceLocation, I系列> 注册表 = new HashMap<>();

    //缓存
    private static List<ResourceLocation> 系列ID缓存 = null;

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

    /**
     * 清空注册表（仅用于测试）
     */
    public static void 清空() {
        注册表.clear();
        系列ID缓存 = null;
    }
}
