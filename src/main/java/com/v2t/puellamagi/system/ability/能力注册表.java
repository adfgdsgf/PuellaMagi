package com.v2t.puellamagi.system.ability;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I能力;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

/**
 * 能力注册表
 * 管理所有魔法少女固有能力的注册和获取
 *
 * 使用方式：
 * 1. 模组初始化时调用 注册() 添加能力
 * 2. 需要时调用 创建实例() 获取能力实例
 */
public final class 能力注册表 {
    private 能力注册表() {}

    // 存储能力工厂，每次获取创建新实例
    private static final Map<ResourceLocation, Supplier<? extends I能力>> 注册表 = new HashMap<>();

    //缓存能力ID列表，避免频繁创建
    private static List<ResourceLocation> 能力ID缓存 = null;

    /**
     * 注册一个能力
     * @param id 能力唯一标识
     * @param factory 能力工厂，每次调用创建新实例
     */
    public static void 注册(ResourceLocation id, Supplier<? extends I能力> factory) {
        if (注册表.containsKey(id)) {
            PuellaMagi.LOGGER.warn("能力 {} 已存在，将被覆盖", id);
        }
        注册表.put(id, factory);能力ID缓存 = null; // 清除缓存
        PuellaMagi.LOGGER.debug("注册能力: {}", id);
    }

    /**
     * 创建能力实例
     * @param id 能力ID
     * @return 能力实例，不存在则返回empty
     */
    public static Optional<I能力> 创建实例(ResourceLocation id) {
        Supplier<? extends I能力> factory = 注册表.get(id);
        if (factory == null) {
            PuellaMagi.LOGGER.warn("尝试创建不存在的能力: {}", id);
            return Optional.empty();
        }
        try {
            return Optional.of(factory.get());
        } catch (Exception e) {
            PuellaMagi.LOGGER.error("创建能力实例失败: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 检查能力是否已注册
     */
    public static boolean 是否已注册(ResourceLocation id) {
        return 注册表.containsKey(id);
    }

    /**
     * 获取所有已注册能力的ID
     */
    public static List<ResourceLocation> 获取所有能力ID() {
        if (能力ID缓存 == null) {
            能力ID缓存 = List.copyOf(注册表.keySet());
        }
        return 能力ID缓存;
    }

    /**
     * 获取已注册能力数量
     */
    public static int 获取能力数量() {
        return 注册表.size();
    }

    /**
     * 清空注册表（仅用于测试）
     */
    public static void 清空() {
        注册表.clear();
        能力ID缓存 = null;
    }
}
