// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI按钮.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.util.渲染工具;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 通用按钮组件
 *
 * 支持：
 * - 普通/危险样式
 * - 悬停高亮
 * - 禁用状态
 * - 自定义颜色
 *
 * 复用场景：所有GUI界面的按钮绘制与命中检测
 */
public final class GUI按钮 {

    private GUI按钮() {}

    //==================== 默认颜色 ====================

    public static final int 默认背景色 = 0xFF333333;
    public static final int 默认悬停色 = 0xFF484848;
    public static final int 默认边框色 = 0xFF555555;
    public static final int 默认悬停边框色 = 0xFF777777;
    public static final int 默认文字色 = 0xFFFFFF;

    public static final int 危险背景色 = 0xFF4A2020;
    public static final int 危险悬停色 = 0xFF6A3030;

    public static final int 禁用背景色 = 0xFF2A2A2A;
    public static final int 禁用文字色 = 0x666666;

    // ====================绘制方法 ====================

    /**
     * 绘制标准按钮
     *
     * @param graphics 绘图上下文
     * @param font 字体
     * @param x 位置X
     * @param y 位置Y
     * @param w 宽度
     * @param h 高度
     * @param text 按钮文字
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @return鼠标是否悬停在按钮上
     */
    public static boolean 绘制(GuiGraphics graphics, Font font,int x, int y, int w, int h,
                               Component text, int mouseX, int mouseY) {
        return 绘制(graphics, font, x, y, w, h, text, mouseX, mouseY,
                false, true);
    }

    /**
     * 绘制按钮（完整参数）
     *
     * @param graphics 绘图上下文
     * @param font 字体
     * @param x 位置X
     * @param y 位置Y
     * @param w 宽度
     * @param h 高度
     * @param text 按钮文字
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param danger 是否危险样式（红色系）
     * @param enabled 是否可用
     * @return 鼠标是否悬停在按钮上（禁用时始终返回false）
     */
    public static boolean 绘制(GuiGraphics graphics, Font font,
                               int x, int y, int w, int h,
                               Component text, int mouseX, int mouseY,
                               boolean danger, boolean enabled) {
        return 绘制自定义(graphics, font, x, y, w, h, text, mouseX, mouseY,
                danger ? 危险背景色 : 默认背景色,
                danger ? 危险悬停色 : 默认悬停色,
                默认边框色, 默认悬停边框色,
                默认文字色, enabled);
    }

    /**
     * 绘制完全自定义颜色的按钮
     *
     * @param graphics 绘图上下文
     * @param font 字体
     * @param x 位置X
     * @param y 位置Y
     * @param w 宽度
     * @param h 高度
     * @param text 按钮文字
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param bgColor 背景色
     * @param hoverBgColor 悬停背景色
     * @param borderColor 边框色
     * @param hoverBorderColor 悬停边框色
     * @param textColor 文字色
     * @param enabled 是否可用
     * @return 鼠标是否悬停在按钮上（禁用时始终返回false）
     */
    public static boolean 绘制自定义(GuiGraphics graphics, Font font,
                                     int x, int y, int w, int h,
                                     Component text, int mouseX, int mouseY,
                                     int bgColor, int hoverBgColor,
                                     int borderColor, int hoverBorderColor,
                                     int textColor, boolean enabled) {
        if (!enabled) {
            graphics.fill(x, y, x + w, y + h, 禁用背景色);
            渲染工具.绘制边框矩形(graphics, x, y, w, h, borderColor);
            渲染工具.绘制居中文本(graphics, font, text,
                    x + w / 2, y + (h - 8) / 2, 禁用文字色);
            return false;
        }

        boolean hover = 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, h);
        int bg = hover ? hoverBgColor : bgColor;
        int border = hover ? hoverBorderColor : borderColor;

        graphics.fill(x, y, x + w, y + h, bg);
        渲染工具.绘制边框矩形(graphics, x, y, w, h, border);
        渲染工具.绘制居中文本(graphics, font, text,
                x + w / 2, y + (h - 8) / 2, textColor);

        return hover;
    }

    /**
     * 绘制小型文字按钮（行内使用，无边框变化）
     *
     * @param graphics 绘图上下文
     * @param font 字体
     * @param x 位置X
     * @param y 位置Y
     * @param w 宽度
     * @param h 高度
     * @param text 按钮文字
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param textColor 文字颜色
     * @param danger 是否危险样式
     * @return 鼠标是否悬停
     */
    public static boolean 绘制小型(GuiGraphics graphics, Font font,
                                   int x, int y, int w, int h,
                                   Component text, int mouseX, int mouseY,
                                   int textColor, boolean danger) {
        boolean hover = 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, h);
        int bg = danger
                ? (hover ? 危险悬停色 : 危险背景色)
                : (hover ? 默认悬停色 : 默认背景色);

        graphics.fill(x, y, x + w, y + h, bg);
        渲染工具.绘制居中文本(graphics, font, text,
                x + w / 2, y + (h - 8) / 2, textColor);

        return hover;
    }

    // ==================== 命中检测 ====================

    /**
     * 检测按钮是否被点击
     * 与绘制方法配合使用，传入相同的坐标和尺寸
     *
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param x 按钮X
     * @param y 按钮Y
     * @param w 按钮宽度
     * @param h 按钮高度
     * @return 是否命中
     */
    public static boolean 命中检测(int mouseX, int mouseY, int x, int y, int w, int h) {
        return 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, h);
    }

    // ==================== 自适应布局 ====================

    /**
     * 计算自适应按钮宽度
     * 当按钮总宽度超过可用空间时，等比缩小每个按钮
     *
     * @param 原始宽度 每个按钮的原始宽度数组
     * @param 可用总宽度 容器可用的总宽度
     * @param 间距 按钮之间的间距
     * @return 调整后的宽度数组
     */
    public static int[] 计算自适应宽度(int[] 原始宽度, int 可用总宽度, int 间距) {
        if (原始宽度.length == 0) return new int[0];

        int totalOriginal = 0;
        for (int w : 原始宽度) {
            totalOriginal += w;
        }
        int totalGaps = (原始宽度.length - 1) * 间距;
        int totalNeeded = totalOriginal + totalGaps;

        int[] result = new int[原始宽度.length];

        if (totalNeeded <= 可用总宽度) {
            // 不需要缩放
            System.arraycopy(原始宽度, 0, result, 0, 原始宽度.length);
        } else {
            // 等比缩放按钮宽度（间距不缩）
            int availableForButtons = 可用总宽度 - totalGaps;
            for (int i = 0; i < 原始宽度.length; i++) {
                result[i] = Math.max(16, 原始宽度[i] * availableForButtons / totalOriginal);
            }
        }
        return result;
    }

}
