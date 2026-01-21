// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/污浊度数据.java

package com.v2t.puellamagi.system.soulgem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * 污浊度数据
 *纯POJO，存储污浊度的当前值和最大值
 *
 * 污浊度范围：0 ~ 最大值（默认100）
 * 0% =纯净，100% = 完全污浊（魔女化边缘）
 */
public class 污浊度数据 {

    private static final String NBT_当前值 = "Current";
    private static final String NBT_最大值 = "Max";

    private float 当前值 = 0f;
    private float 最大值 = 100f;

    // ==================== 基础操作 ====================

    /**
     * 增加污浊度
     * @param amount 增加量（正数）
     */
    public void 增加(float amount) {
        if (amount <= 0) return;
        当前值 = Math.min(当前值 + amount, 最大值);
    }

    /**
     * 减少污浊度
     * @param amount 减少量（正数）
     */
    public void 减少(float amount) {
        if (amount <= 0) return;
        当前值 = Math.max(当前值 - amount, 0f);
    }

    /**
     * 设置当前值
     */
    public void 设置当前值(float value) {
        当前值 = Mth.clamp(value, 0f, 最大值);
    }

    /**
     * 设置最大值
     */
    public void 设置最大值(float value) {
        最大值 = Math.max(value, 1f);
        // 确保当前值不超过新的最大值
        if (当前值 > 最大值) {
            当前值 = 最大值;
        }
    }

    /**
     * 重置为初始状态
     */
    public void 重置() {
        当前值 = 0f;
        最大值 = 100f;
    }

    // ==================== 查询方法 ====================

    public float 获取当前值() {
        return 当前值;
    }

    public float 获取最大值() {
        return 最大值;
    }

    /**
     * 获取百分比（0.0 ~ 1.0）
     *用于HUD渲染
     */
    public float 获取百分比() {
        if (最大值 <= 0) return 0f;
        return 当前值 / 最大值;
    }

    /**
     * 是否已满（100%污浊）
     */
    public boolean 是否已满() {
        return 当前值 >= 最大值;
    }

    /**
     * 是否为空（完全纯净）
     */
    public boolean 是否为空() {
        return 当前值 <= 0f;
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(NBT_当前值, 当前值);
        tag.putFloat(NBT_最大值, 最大值);
        return tag;
    }

    public void 从NBT读取(CompoundTag tag) {
        if (tag == null) return;
        最大值 = tag.getFloat(NBT_最大值);
        if (最大值 <= 0) 最大值 = 100f;
        当前值 = Mth.clamp(tag.getFloat(NBT_当前值), 0f, 最大值);
    }

    // ==================== 复制 ====================

    public void 复制自(污浊度数据 other) {
        if (other == null) return;
        this.当前值 = other.当前值;
        this.最大值 = other.最大值;
    }
}
