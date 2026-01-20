package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

/**
 * 技能注册表
 * 管理所有技能的注册和获取
 */
public final class 技能注册表 {
    private 技能注册表() {}

    // 技能ID -> 技能工厂
    private static final Map<ResourceLocation, Supplier<? extends I技能>> 注册表 = new HashMap<>();

    //缓存
    private static List<ResourceLocation> 技能ID缓存 = null;

    /**
     * 注册一个技能
     * @param id 技能唯一标识
     * @param factory 技能工厂，每次调用创建新实例
     */
    public static void 注册(ResourceLocation id, Supplier<? extends I技能> factory) {
        if (注册表.containsKey(id)) {
            PuellaMagi.LOGGER.warn("技能 {} 已存在，将被覆盖", id);
        }
        注册表.put(id, factory);
        技能ID缓存 = null;
        PuellaMagi.LOGGER.debug("注册技能: {}", id);
    }

    /**
     * 创建技能实例
     */
    public static Optional<I技能> 创建实例(ResourceLocation id) {Supplier<? extends I技能> factory = 注册表.get(id);
        if (factory == null) {
            PuellaMagi.LOGGER.warn("尝试创建不存在的技能: {}", id);
            return Optional.empty();
        }
        try {
            return Optional.of(factory.get());
        } catch (Exception e) {
            PuellaMagi.LOGGER.error("创建技能实例失败: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 检查技能是否已注册
     */
    public static boolean 是否已注册(ResourceLocation id) {
        return 注册表.containsKey(id);
    }

    /**
     * 获取所有已注册技能的ID
     */
    public static List<ResourceLocation> 获取所有技能ID() {
        if (技能ID缓存 == null) {
            技能ID缓存 = List.copyOf(注册表.keySet());
        }
        return 技能ID缓存;
    }

    /**
     * 获取已注册技能数量
     */
    public static int 获取技能数量() {
        return 注册表.size();
    }

    /**
     * 清空注册表（仅用于测试）
     */
    public static void 清空() {
        注册表.clear();
        技能ID缓存 = null;
    }
}
