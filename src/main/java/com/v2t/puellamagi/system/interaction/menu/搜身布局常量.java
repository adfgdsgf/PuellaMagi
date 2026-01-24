// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/menu/搜身布局常量.java

package com.v2t.puellamagi.system.interaction.menu;

/**
 * 搜身界面布局常量
 *
 * 统一管理搜身系统的所有UI布局参数
 * 供菜单、区域管理器、界面类共同引用
 */
public final class 搜身布局常量 {

    private 搜身布局常量() {}

    // ==================== 通用UI常量 ====================

    /**槽位尺寸（像素） */
    public static final int 槽位尺寸 = 18;

    /** 标签高度（像素） */
    public static final int 标签高度 = 12;

    /** 分组之间的间距（像素） */
    public static final int 区域间距 = 4;

    // ==================== 主面板布局 ====================

    /** 主面板左边距 */
    public static final int 主区域左边距 = 8;

    /** 主面板右边距 */
    public static final int 主区域右边距 = 8;

    /** 主区域宽度（9列槽位） */
    public static final int 主区域宽度 = 9 * 槽位尺寸;  // 162px

    /** 主面板总宽度 */
    public static final int 主面板宽度 = 主区域左边距 + 主区域宽度 + 主区域右边距;  // 178px

    /** 顶部边距 */
    public static final int 顶部边距 = 18;

    /** 最大目标区域高度（6行） */
    public static final int 最大目标区域高度 = 6 * 槽位尺寸;  // 108px

    /** 分隔线区域高度 */
    public static final int 分隔线高度 = 8;

    /** 搜身者背包区域高度 */
    public static final int 搜身者背包高度 = 标签高度 + 4* 槽位尺寸 + 11;

    // ==================== 侧边面板布局 ====================

    /** 侧边面板与主面板的间隔 */
    public static final int 侧边面板间隔 = 6;

    /** 侧边面板内边距 */
    public static final int 侧边面板内边距 = 7;

    /** 侧边面板顶部偏移（相对于GUI顶部） */
    public static final int 侧边面板顶部偏移 = 18;

    // ==================== 计算方法 ====================

    /**
     * 计算侧边区域槽位的起始X坐标
     */
    public static int 计算侧边槽位起始X() {
        return 主面板宽度 + 侧边面板间隔 + 侧边面板内边距;
    }

    /**
     * 计算侧边区域内容的起始Y坐标
     */
    public static int 计算侧边内容起始Y() {
        return 侧边面板顶部偏移 + 侧边面板内边距;
    }
}
