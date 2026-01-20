package com.v2t.puellamagi.system.transformation;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 魔法少女类型注册表
 * 管理所有魔法少女类型的注册和查询
 */
public final class 魔法少女类型注册表 {
    private 魔法少女类型注册表() {}

    // 类型ID -> 类型定义
    private static final Map<ResourceLocation, 魔法少女类型> 注册表 = new HashMap<>();

    // 缓存
    private static List<ResourceLocation> 类型ID缓存 = null;

    /**
     * 注册一种魔法少女类型
     */
    public static void 注册(魔法少女类型 type) {
        if (type == null) return;

        ResourceLocation id = type.获取ID();
        if (注册表.containsKey(id)) {
            PuellaMagi.LOGGER.warn("魔法少女类型 {} 已存在，将被覆盖", id);
        }

        注册表.put(id, type);
        类型ID缓存 = null; // 清除缓存
        PuellaMagi.LOGGER.debug("注册魔法少女类型: {}", id);
    }

    /**
     * 获取类型定义
     */
    public static Optional<魔法少女类型> 获取(ResourceLocation id) {
        return Optional.ofNullable(注册表.get(id));
    }

    /**
     * 检查类型是否已注册
     */
    public static boolean 是否已注册(ResourceLocation id) {
        return 注册表.containsKey(id);
    }

    /**
     * 获取所有已注册类型的ID
     */
    public static List<ResourceLocation> 获取所有类型ID() {
        if (类型ID缓存 == null) {
            类型ID缓存 = List.copyOf(注册表.keySet());
        }
        return 类型ID缓存;
    }

    /**
     * 获取指定系列下的所有类型
     */
    public static List<魔法少女类型> 获取系列下所有类型(ResourceLocation seriesId) {
        return 注册表.values().stream()
                .filter(type -> type.获取所属系列().equals(seriesId))
                .collect(Collectors.toList());
    }

    /**
     * 获取已注册类型数量
     */
    public static int 获取类型数量() {
        return 注册表.size();
    }

    /**
     * 获取所有类型（只读）
     */
    public static Collection<魔法少女类型> 获取所有类型() {
        return Collections.unmodifiableCollection(注册表.values());
    }

    /**
     * 清空注册表（仅用于测试）
     */
    public static void 清空() {
        注册表.clear();
        类型ID缓存 = null;
    }
}
