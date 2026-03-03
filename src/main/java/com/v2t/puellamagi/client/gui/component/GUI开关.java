// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI开关.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.util.渲染工具;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 开关切换组件
 *
 * 显示开/关状态，点击切换
 *
 * 复用场景：队伍个人配置、设置界面、任何布尔选项
 */
public final class GUI开关 {

    private GUI开关() {}

    // ==================== 默认颜色 ====================

    public static final int 开启背景色 = 0xFF226622;
    public static final int 开启悬停色 = 0xFF338833;
    public static final int 开启文字色 = 0x55FF55;

    public static final int 关闭背景色 = 0xFF333333;
    public static final int 关闭悬停色 = 0xFF444444;
    public static final int 关闭文字色 = 0x777777;

    public static final int 边框色 = 0xFF555555;
    public static final int 悬停边框色 = 0xFF666666;

    // ==================== 默认文字 ====================

    private static final Component 默认开启文字 = Component.translatable("gui.puellamagi.team.toggle.on");
    private static final Component 默认关闭文字 = Component.translatable("gui.puellamagi.team.toggle.off");

    // ==================== 绘制方法 ====================

    /**
     * 绘制开关（使用默认文字"开/关"）
     *
     * @param graphics 绘图上下文
     * @param font 字体
     * @param x 位置X
     * @param y 位置Y
     * @param w 宽度
     * @param h 高度
     * @param value 当前值
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @return 鼠标是否悬停
     */
    public static boolean 绘制(GuiGraphics graphics, Font font,
                               int x, int y, int w, int h,
                               boolean value, int mouseX, int mouseY) {
        return 绘制(graphics, font, x, y, w, h, value, mouseX, mouseY,
                默认开启文字, 默认关闭文字);
    }

    /**
     * 绘制开关（自定义文字）
     *
     * @param graphics 绘图上下文
     * @param font 字体
     * @param x 位置X
     * @param y 位置Y
     * @param w 宽度
     * @param h 高度
     * @param value 当前值
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param onText 开启状态文字
     * @param offText 关闭状态文字
     * @return 鼠标是否悬停
     */
    public static boolean 绘制(GuiGraphics graphics, Font font,
                               int x, int y, int w, int h,
                               boolean value, int mouseX, int mouseY,
                               Component onText, Component offText) {
        boolean hover = 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, h);

        int bg;
        int textColor;
        Component text;

        if (value) {
            bg = hover ? 开启悬停色 : 开启背景色;
            textColor = 开启文字色;
            text = onText;
        } else {
            bg = hover ? 关闭悬停色 : 关闭背景色;
            textColor = 关闭文字色;
            text = offText;
        }

        graphics.fill(x, y, x + w, y + h, bg);
        渲染工具.绘制边框矩形(graphics, x, y, w, h, hover ? 悬停边框色 : 边框色);
        渲染工具.绘制居中文本(graphics, font, text, x + w / 2, y + 3, textColor);

        return hover;
    }

    /**
     * 命中检测
     */
    public static boolean 命中检测(int mouseX, int mouseY, int x, int y, int w, int h) {
        return 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, h);
    }
}
