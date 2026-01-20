package com.v2t.puellamagi.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * NBT读写简化工具
 * 提供安全的读取方法，避免空指针和重复代码
 */
public final class NBT工具 {
    private NBT工具() {}

    /**
     * 安全获取Int，不存在则返回默认值
     */
    public static int 获取Int(CompoundTag tag, String key, int defaultValue) {
        return tag.contains(key) ? tag.getInt(key) : defaultValue;
    }

    /**
     * 安全获取Boolean，不存在则返回默认值
     */
    public static boolean 获取Boolean(CompoundTag tag, String key, boolean defaultValue) {
        return tag.contains(key) ? tag.getBoolean(key) : defaultValue;
    }

    /**
     * 安全获取String，不存在则返回默认值
     */
    public static String 获取String(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key) ? tag.getString(key) : defaultValue;
    }

    /**
     * 安全获取ResourceLocation，不存在则返回null
     */
    @Nullable
    public static ResourceLocation 获取资源路径(CompoundTag tag, String key) {
        if (!tag.contains(key)) return null;
        String value = tag.getString(key);
        return value.isEmpty() ? null : new ResourceLocation(value);
    }

    /**
     * 写入ResourceLocation
     */
    public static void 写入资源路径(CompoundTag tag, String key, @Nullable ResourceLocation value) {
        if (value != null) {
            tag.putString(key, value.toString());
        }
    }
}
