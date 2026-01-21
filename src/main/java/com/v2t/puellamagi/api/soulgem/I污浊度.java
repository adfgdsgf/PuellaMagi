// 文件路径: src/main/java/com/v2t/puellamagi/api/soulgem/I污浊度.java

package com.v2t.puellamagi.api.soulgem;

import net.minecraft.nbt.CompoundTag;

/**
 * 污浊度能力接口
 * 定义污浊度的基本操作
 */
public interface I污浊度 {

    /**
     * 获取当前污浊度值
     */
    float 获取当前值();

    /**
     * 获取最大污浊度值
     */
    float 获取最大值();

    /**
     * 获取污浊度百分比（0.0 ~ 1.0）
     */
    float 获取百分比();

    /**
     * 增加污浊度
     * @param amount 增加量（正数）
     */
    void 增加污浊度(float amount);

    /**
     * 减少污浊度
     * @param amount 减少量（正数）
     */
    void 减少污浊度(float amount);

    /**
     * 设置当前污浊度
     */
    void 设置当前值(float value);

    /**
     * 是否已满（魔女化边缘）
     */
    boolean 是否已满();

    /**
     * 是否为空（完全纯净）
     */
    boolean 是否为空();

    /**
     * 重置污浊度
     */
    void 重置();

    // ==================== 序列化 ====================

    CompoundTag 写入NBT();

    void 从NBT读取(CompoundTag tag);
}
