package com.v2t.puellamagi;

/**
 * 全局常量定义
 * 所有魔数、配置默认值、阈值等统一在此管理
 */
public final class 常量 {
    private 常量() {}

    //==================== 基础信息 ====================
    public static final String MOD_ID = "puellamagi";

    // ==================== GUI交互 ====================
    public static final int 拖拽触发阈值 = 5;        // 像素，移动超过此值才视为拖拽
    public static final int 双击间隔 = 250;          // 毫秒

    // ==================== GUI组件 ====================

    // 槽位默认尺寸
    public static final int GUI_槽位尺寸 = 22;
    public static final int GUI_槽位间距 = 2;
    public static final int GUI_槽位边框宽度 = 1;

    // 高亮颜色
    public static final int GUI_颜色_悬停高亮 = 0xFFFFD700;  // 金色
    public static final int GUI_颜色_拖拽高亮 = 0xFF00FF00;  // 绿色
    public static final int GUI_颜色_选中高亮 = 0xFFFF69B4;  // 粉色
    public static final int GUI_颜色_禁用 = 0xFF666666;      // 灰色

    // ==================== 技能栏 ====================
    public static final int 默认技能槽位数 = 6;      // 每个预设默认6个槽位
    public static final int 默认槽位大小 = 20;       // 像素
    public static final float 最小缩放 = 0.5f;
    public static final float 最大缩放 = 2.0f;
    // 注意：预设数量不设上限，由List动态管理

    // ==================== 网络 ====================
    public static final String 网络协议版本 = "1";

    // ==================== 游戏机制 ====================
    public static final int 变身冷却 = 100;          // tick (5秒)
}