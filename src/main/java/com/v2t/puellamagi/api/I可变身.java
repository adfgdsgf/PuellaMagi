package com.v2t.puellamagi.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 可变身对象的接口
 * 定义变身相关的基本操作
 */
public interface I可变身 {

    /**
     * 是否已变身
     */
    boolean 是否已变身();

    /**
     * 设置变身状态
     */
    void 设置变身状态(boolean transformed);

    /**
     * 获取当前魔法少女类型ID
     * 未变身时返回null
     */
    ResourceLocation 获取少女类型();

    /**
     * 设置魔法少女类型
     */
    void 设置少女类型(ResourceLocation typeId);

    /**
     * 获取当前模型ID
     */
    ResourceLocation 获取模型();

    /**
     * 设置模型ID
     */
    void 设置模型(ResourceLocation modelId);

    /**
     * 序列化到NBT
     */
    CompoundTag 写入NBT();

    /**
     * 从NBT反序列化
     */
    void 从NBT读取(CompoundTag tag);
}
