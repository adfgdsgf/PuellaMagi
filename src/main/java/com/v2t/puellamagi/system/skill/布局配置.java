package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.常量;
import net.minecraft.nbt.CompoundTag;

/**
 * 技能栏布局配置
 * 控制技能栏在屏幕上的显示位置和样式
 */
public class 布局配置 {

    private static final String KEY_ORIENTATION = "Orientation";
    private static final String KEY_ANCHOR = "Anchor";
    private static final String KEY_SCALE = "Scale";
    private static final String KEY_OFFSET_X = "OffsetX";
    private static final String KEY_OFFSET_Y = "OffsetY";

    /**
     * 排列方向
     */
    public enum 方向 {
        横向,
        纵向
    }

    /**
     * 屏幕锚点（九宫格）
     */
    public enum 锚点 {
        左上, 上中, 右上,
        左中, 正中, 右中,
        左下, 下中, 右下
    }

    private 方向 排列方向;
    private 锚点 屏幕锚点;
    private float 缩放比例;
    private int 偏移X;
    private int 偏移Y;

    /**
     * 默认构造器（屏幕下方居中，横向排列）
     */
    public 布局配置() {
        this.排列方向 = 方向.横向;
        this.屏幕锚点 = 锚点.下中;
        this.缩放比例 = 1.0f;
        this.偏移X = 0;
        this.偏移Y = -40; // 略微上移，避免和原版快捷栏重叠
    }

    /**
     * 完整构造器
     */
    public 布局配置(方向 排列方向, 锚点 屏幕锚点, float 缩放比例, int 偏移X, int 偏移Y) {
        this.排列方向 = 排列方向;
        this.屏幕锚点 = 屏幕锚点;
        this.缩放比例 = Math.max(常量.最小缩放, Math.min(常量.最大缩放, 缩放比例));
        this.偏移X = 偏移X;
        this.偏移Y = 偏移Y;
    }

    // ==================== Getter ====================

    public 方向 获取排列方向() {
        return 排列方向;
    }

    public 锚点 获取屏幕锚点() {
        return 屏幕锚点;
    }

    public float 获取缩放比例() {
        return 缩放比例;
    }

    public int 获取偏移X() {
        return 偏移X;
    }

    public int 获取偏移Y() {
        return 偏移Y;
    }

    // ==================== Setter ====================

    public void 设置排列方向(方向 dir) {
        this.排列方向 = dir;
    }

    public void 设置屏幕锚点(锚点 anchor) {
        this.屏幕锚点 = anchor;
    }

    public void 设置缩放比例(float scale) {
        this.缩放比例 = Math.max(常量.最小缩放, Math.min(常量.最大缩放, scale));
    }

    public void 设置偏移(int x, int y) {
        this.偏移X = x;
        this.偏移Y = y;
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_ORIENTATION, 排列方向.ordinal());
        tag.putInt(KEY_ANCHOR, 屏幕锚点.ordinal());
        tag.putFloat(KEY_SCALE, 缩放比例);
        tag.putInt(KEY_OFFSET_X, 偏移X);
        tag.putInt(KEY_OFFSET_Y, 偏移Y);
        return tag;
    }

    public static 布局配置 从NBT读取(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return new 布局配置();
        }

        方向[]方向值 = 方向.values();锚点[] 锚点值 = 锚点.values();

        int orientationIdx = tag.getInt(KEY_ORIENTATION);
        int anchorIdx = tag.getInt(KEY_ANCHOR);

        方向 dir = (orientationIdx >= 0 && orientationIdx < 方向值.length)
                ? 方向值[orientationIdx] : 方向.横向;
        锚点 anchor = (anchorIdx >= 0 && anchorIdx < 锚点值.length)
                ? 锚点值[anchorIdx] : 锚点.下中;

        float scale = tag.getFloat(KEY_SCALE);
        if (scale <= 0) scale = 1.0f;

        int offsetX = tag.getInt(KEY_OFFSET_X);
        int offsetY = tag.getInt(KEY_OFFSET_Y);

        return new 布局配置(dir, anchor, scale, offsetX, offsetY);
    }

    // ==================== 复制 ====================

    public 布局配置 复制() {
        return new 布局配置(排列方向, 屏幕锚点, 缩放比例, 偏移X, 偏移Y);
    }

    // ==================== 坐标计算 ====================

    /**
     * 根据锚点计算基准坐标
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @return [x, y] 基准坐标
     */
    public int[] 计算锚点坐标(int screenWidth, int screenHeight) {
        int x = switch (屏幕锚点) {
            case 左上, 左中, 左下 -> 0;
            case 上中, 正中, 下中 -> screenWidth / 2;
            case 右上, 右中, 右下 -> screenWidth;};
        int y = switch (屏幕锚点) {
            case 左上, 上中, 右上 -> 0;
            case 左中, 正中, 右中 -> screenHeight / 2;
            case 左下, 下中, 右下 -> screenHeight;
        };
        return new int[]{x +偏移X, y + 偏移Y};
    }
}
