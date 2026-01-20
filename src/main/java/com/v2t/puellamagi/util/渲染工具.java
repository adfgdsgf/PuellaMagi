//文件路径: src/main/java/com/v2t/puellamagi/util/渲染工具.java

package com.v2t.puellamagi.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * GUI渲染工具类
 * 提供通用的绘制方法，避免重复代码
 */
public final class 渲染工具 {
    private 渲染工具() {}

    // ==================== 锚点系统 ====================

    /**
     * 屏幕锚点（九宫格）
     */
    public enum 锚点 {
        左上, 上中, 右上,
        左中, 正中, 右中,
        左下, 下中, 右下
    }

    /**
     * 计算锚点的基准坐标
     * @param anchor 锚点
     * @param screenW 屏幕宽度
     * @param screenH 屏幕高度
     * @return [x, y] 基准坐标
     */
    public static int[] 计算锚点坐标(锚点 anchor, int screenW, int screenH) {
        int x = switch (anchor) {
            case 左上, 左中, 左下 -> 0;
            case 上中, 正中, 下中 -> screenW / 2;
            case 右上, 右中, 右下 -> screenW;};
        int y = switch (anchor) {
            case 左上, 上中, 右上 -> 0;
            case 左中, 正中, 右中 -> screenH / 2;
            case 左下, 下中, 右下 -> screenH;
        };
        return new int[]{x, y};
    }

    /**
     * 计算元素的实际绘制坐标
     * 根据锚点和偏移自动调整位置
     *
     * @param anchor 锚点
     * @param offsetX X偏移
     * @param offsetY Y偏移
     * @param elementW 元素宽度
     * @param elementH 元素高度
     * @param screenW 屏幕宽度
     * @param screenH 屏幕高度
     * @return [x, y] 实际绘制坐标
     */
    public static int[] 计算实际坐标(锚点 anchor, int offsetX, int offsetY,int elementW, int elementH, int screenW, int screenH) {
        int[] base = 计算锚点坐标(anchor, screenW, screenH);
        int x = base[0] + offsetX;
        int y = base[1] + offsetY;

        // 右侧锚点：向左偏移元素宽度
        if (anchor == 锚点.右上|| anchor == 锚点.右中 || anchor == 锚点.右下) {
            x -= elementW;
        }
        // 中间锚点：向左偏移半个元素宽度
        if (anchor == 锚点.上中 || anchor == 锚点.正中 || anchor == 锚点.下中) {
            x -= elementW / 2;
        }
        // 下方锚点：向上偏移元素高度
        if (anchor == 锚点.左下 || anchor == 锚点.下中 || anchor == 锚点.右下) {
            y -= elementH;
        }
        // 中间锚点：向上偏移半个元素高度
        if (anchor == 锚点.左中 || anchor == 锚点.正中 || anchor == 锚点.右中) {
            y -= elementH / 2;
        }

        return new int[]{x, y};
    }

    // ==================== 区域判断 ====================

    /**
     * 判断鼠标是否在指定区域内
     */
    public static boolean 鼠标在区域内(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // ==================== 矩形绘制 ====================

    /**
     * 绘制纯色矩形
     */
    public static void 绘制矩形(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x, y, x + w, y + h, color);
    }

    /**
     * 绘制带边框的矩形
     * @param borderColor 边框颜色
     * @param fillColor 填充颜色（0表示透明）
     * @param borderWidth 边框宽度
     */
    public static void 绘制边框矩形(GuiGraphics gui, int x, int y, int w, int h,int borderColor, int fillColor, int borderWidth) {
        // 填充
        if ((fillColor & 0xFF000000) != 0) {
            gui.fill(x + borderWidth, y + borderWidth,
                    x + w - borderWidth, y + h - borderWidth, fillColor);
        }
        // 边框 - 上
        gui.fill(x, y, x + w, y + borderWidth, borderColor);
        // 边框 - 下
        gui.fill(x, y + h - borderWidth, x + w, y + h, borderColor);
        // 边框 - 左
        gui.fill(x, y + borderWidth, x + borderWidth, y + h - borderWidth, borderColor);
        // 边框 - 右
        gui.fill(x + w - borderWidth, y + borderWidth, x + w, y + h - borderWidth, borderColor);
    }

    /**
     * 绘制简单边框矩形（1像素边框）
     */
    public static void 绘制边框矩形(GuiGraphics gui, int x, int y, int w, int h, int borderColor) {
        绘制边框矩形(gui, x, y, w, h, borderColor, 0, 1);
    }

    // ==================== 文本绘制 ====================

    /**
     * 绘制居中文本
     */
    public static void 绘制居中文本(GuiGraphics gui, Font font, Component text, int centerX, int y, int color) {
        int textWidth = font.width(text);
        gui.drawString(font, text, centerX - textWidth / 2, y, color);
    }

    /**
     * 绘制右对齐文本
     */
    public static void 绘制右对齐文本(GuiGraphics gui, Font font, Component text, int rightX, int y, int color) {
        int textWidth = font.width(text);
        gui.drawString(font, text, rightX - textWidth, y, color);
    }

    /**
     * 绘制自动换行文本
     * @param maxWidth 最大宽度
     * @return 实际绘制的行数
     */
    public static int 绘制换行文本(GuiGraphics gui, Font font, Component text,
                                   int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int lineHeight = font.lineHeight + 2;

        for (int i = 0; i < lines.size(); i++) {
            gui.drawString(font, lines.get(i), x, y + i * lineHeight, color);
        }

        return lines.size();
    }

    /**
     * 绘制带阴影的文本
     */
    public static void 绘制阴影文本(GuiGraphics gui, Font font, Component text,
                                    int x, int y, int color, int shadowColor) {
        gui.drawString(font, text, x + 1, y + 1, shadowColor, false);
        gui.drawString(font, text, x, y, color, false);
    }

    // ==================== 图标与纹理 ====================

    /**
     * 绘制纹理（使用纹理信息，自动处理尺寸）
     *
     * @param gui 绘图上下文
     * @param 纹理 纹理信息（包含路径和原始尺寸）
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param drawWidth 绘制宽度
     * @param drawHeight 绘制高度
     */
    public static void 绘制纹理(GuiGraphics gui, 资源工具.纹理信息 纹理,
                                int x, int y, int drawWidth, int drawHeight) {
        绘制纹理(gui, 纹理, x, y, drawWidth, drawHeight, 1.0f);
    }

    /**
     * 绘制纹理（带透明度）
     *
     * @param gui 绘图上下文
     * @param 纹理 纹理信息（包含路径和原始尺寸）
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param drawWidth 绘制宽度
     * @param drawHeight 绘制高度
     * @param alpha 透明度 0.0~1.0
     */
    public static void 绘制纹理(GuiGraphics gui, 资源工具.纹理信息 纹理,
                                int x, int y, int drawWidth, int drawHeight, float alpha) {
        RenderSystem.enableBlend();
        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }

        gui.blit(纹理.路径(), x, y, drawWidth, drawHeight,
                0, 0, 纹理.宽度(), 纹理.高度(), 纹理.宽度(), 纹理.高度());

        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        RenderSystem.disableBlend();
    }

    /**
     * 绘制纹理（原始尺寸）
     *
     * @param gui 绘图上下文
     * @param 纹理 纹理信息
     * @param x 绘制位置X
     * @param y 绘制位置Y
     */
    public static void 绘制纹理原尺寸(GuiGraphics gui, 资源工具.纹理信息 纹理, int x, int y) {
        绘制纹理(gui, 纹理, x, y, 纹理.宽度(), 纹理.高度());
    }

    /**
     * 绘制纹理（原始尺寸，带透明度）
     *
     * @param gui 绘图上下文
     * @param 纹理 纹理信息
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param alpha 透明度 0.0~1.0
     */
    public static void 绘制纹理原尺寸(GuiGraphics gui, 资源工具.纹理信息 纹理, int x, int y, float alpha) {
        绘制纹理(gui, 纹理, x, y, 纹理.宽度(), 纹理.高度(), alpha);
    }

    /**
     * 绘制缩放纹理（兼容旧代码，使用ResourceLocation）
     *
     * @param gui 绘图上下文
     * @param texture 纹理资源
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param drawWidth 绘制宽度
     * @param drawHeight 绘制高度
     * @param textureSize 纹理文件尺寸（假设正方形）
     */
    public static void 绘制缩放纹理(GuiGraphics gui, ResourceLocation texture,
                                    int x, int y, int drawWidth, int drawHeight, int textureSize) {
        绘制缩放纹理(gui, texture, x, y, drawWidth, drawHeight, textureSize,1.0f);
    }

    /**
     * 绘制缩放纹理（带透明度，兼容旧代码）
     *
     * @param gui 绘图上下文
     * @param texture 纹理资源
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param drawWidth 绘制宽度
     * @param drawHeight 绘制高度
     * @param textureSize 纹理文件尺寸（假设正方形）
     * @param alpha 透明度 0.0~1.0
     */
    public static void 绘制缩放纹理(GuiGraphics gui, ResourceLocation texture,
                                    int x, int y, int drawWidth, int drawHeight,
                                    int textureSize, float alpha) {
        RenderSystem.enableBlend();
        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }

        gui.blit(texture, x, y, drawWidth, drawHeight,
                0, 0, textureSize, textureSize, textureSize, textureSize);

        if (alpha < 1.0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        RenderSystem.disableBlend();
    }

    /**
     * 绘制技能图标
     * @param icon 图标资源路径
     * @param x 左上角X
     * @param y 左上角Y
     * @param size 图标尺寸
     */
    public static void 绘制技能图标(GuiGraphics gui, ResourceLocation icon, int x, int y, int size) {
        RenderSystem.enableBlend();
        gui.blit(icon, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();
    }

    /**
     * 绘制技能图标（带冷却遮罩）
     * @param cooldownPercent 冷却百分比0.0~1.0，0=可用，1=完全冷却
     */
    public static void 绘制技能图标带冷却(GuiGraphics gui, ResourceLocation icon,
                                          int x, int y, int size, float cooldownPercent) {
        // 先绘制图标
        绘制技能图标(gui, icon, x, y, size);

        // 绘制冷却遮罩
        if (cooldownPercent > 0) {
            int maskHeight = (int) (size * cooldownPercent);
            int maskY = y + size - maskHeight;
            // 半透明黑色遮罩
            gui.fill(x, maskY, x + size, y + size, 0x80000000);
        }
    }

    /**
     * 绘制空槽位背景
     */
    public static void 绘制空槽位(GuiGraphics gui, int x, int y, int size) {
        // 深色背景
        gui.fill(x, y, x + size, y + size, 0xFF1A1A1A);
        // 边框
        绘制边框矩形(gui, x, y, size, size, 0xFF3A3A3A);}

    /**
     * 绘制选中槽位高亮
     */
    public static void 绘制槽位高亮(GuiGraphics gui, int x, int y, int size, int color) {
        绘制边框矩形(gui, x - 1, y - 1, size + 2, size + 2, color, 0, 2);
    }

    // ==================== 动画辅助 ====================

    /**
     * 计算渐变动画值
     * @param current 当前值
     * @param target 目标值
     * @param speed 速度（0~1之间，越大越快）
     * @return 新的当前值
     */
    public static float 动画插值(float current, float target, float speed) {
        if (Math.abs(target - current) < 0.001f) {
            return target;
        }
        return current + (target - current) * speed;
    }

    // ==================== 颜色工具 ====================

    /**
     * 创建带透明度的颜色
     * @param rgb RGB颜色值（0x000000 ~ 0xFFFFFF）
     * @param alpha 透明度（0~255）
     * @return ARGB颜色值
     */
    public static int 颜色带透明度(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * 调整颜色透明度
     * @param argb 原ARGB颜色
     * @param alphaMultiplier 透明度乘数（0~1）
     */
    public static int 调整透明度(int argb, float alphaMultiplier) {
        int alpha = (argb >> 24) & 0xFF;
        alpha = (int) (alpha * alphaMultiplier);
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    // ==================== 常用颜色常量 ====================

    public static final int 颜色_白色 = 0xFFFFFFFF;
    public static final int 颜色_黑色 = 0xFF000000;
    public static final int 颜色_灰色 = 0xFF808080;
    public static final int 颜色_深灰 = 0xFF404040;
    public static final int 颜色_浅灰 = 0xFFC0C0C0;
    public static final int 颜色_红色 = 0xFFFF0000;
    public static final int 颜色_绿色 = 0xFF00FF00;
    public static final int 颜色_蓝色 = 0xFF0000FF;
    public static final int 颜色_黄色 = 0xFFFFFF00;
    public static final int 颜色_品红 = 0xFFFF00FF;
    public static final int 颜色_金色 = 0xFFFFD700;

    // GUI专用颜色
    public static final int 颜色_槽位背景 = 0xFF1A1A1A;
    public static final int 颜色_槽位边框 = 0xFF3A3A3A;
    public static final int 颜色_槽位选中 = 0xFFFFD700;
    public static final int 颜色_冷却遮罩 = 0x80000000;
    public static final int 颜色_文字阴影 = 0xFF202020;
}
