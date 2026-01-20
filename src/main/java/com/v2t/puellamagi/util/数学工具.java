package com.v2t.puellamagi.util;

import com.v2t.puellamagi.常量;

/**
 * 数学计算工具
 * 提供距离计算、范围限制、插值等通用数学方法
 */
public final class 数学工具 {
    private 数学工具() {}

    /**
     * 计算两点间距离
     */
    public static double 计算距离(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * 判断是否超过拖拽阈值
     */
    public static boolean 超过拖拽阈值(double startX, double startY, double currentX, double currentY) {
        return 计算距离(startX, startY, currentX, currentY) >= 常量.拖拽触发阈值;
    }

    /**
     * 限制int值在范围内
     */
    public static int 限制范围(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制float值在范围内
     */
    public static float 限制范围(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制double值在范围内
     */
    public static double 限制范围(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 线性插值
     */
    public static float 插值(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    /**
     * 线性插值（double版本）
     */
    public static double 插值(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}
