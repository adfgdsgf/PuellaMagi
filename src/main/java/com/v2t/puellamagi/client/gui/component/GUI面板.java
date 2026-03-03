// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI面板.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.util.渲染工具;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 通用面板组件
 *
 * 面板结构：
 * ┌──────────────────────────┐
 * │ 标题文字（可选）          │← 标题区
 * ├──────────────────────┬───┤
 * │                │▲ │
 * │    内容区域           │ █ │←滚动条区（可选）
 * │                      │ ▼ │
 * └──────────────────────┴───┘
 *
 * 内容区域自动排除：边框 + 标题 + 内边距 + 滚动条
 * 调用方只需在 获取内容区域() 的范围内绘制，无需手动减滚动条宽度
 */
public class GUI面板 {

    //==================== 默认颜色 ====================

    public static final int 默认背景色 = 0xFF1E1E1E;
    public static final int 默认边框色 = 0xFF3A3A3A;
    public static final int 默认标题色 = 0xAAAAAA;

    private static final int 标题区高度 = 14;
    private static final int 边框宽度 = 1;

    // ==================== 面板数据 ====================

    private int x, y, w, h;
    private int 背景色;
    private int 边框色;
    @Nullable
    private Component 标题;
    private int 标题色;
    private int 内边距;
    private int 滚动条预留宽度;

    // ==================== 构造 ====================

    public GUI面板(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.背景色 = 默认背景色;
        this.边框色 = 默认边框色;
        this.标题 = null;
        this.标题色 = 默认标题色;
        this.内边距 = 2;
        this.滚动条预留宽度 = 0;
    }

    // ==================== 链式配置 ====================

    public GUI面板 背景(int color) {
        this.背景色 = color;
        return this;
    }

    public GUI面板 边框(int color) {
        this.边框色 = color;
        return this;
    }

    public GUI面板 标题(Component title) {
        this.标题 = title;
        return this;
    }

    public GUI面板 标题(Component title, int color) {
        this.标题 = title;
        this.标题色 = color;
        return this;
    }

    public GUI面板 内边距(int padding) {
        this.内边距 = padding;
        return this;
    }

    /**
     * 预留滚动条宽度（右侧）
     *设置后获取内容区域() 的宽度会自动减去此值
     *滚动条在内容区域右侧、面板边框内侧绘制
     *
     * @param width 滚动条宽度（含间距），0表示不预留
     */
    public GUI面板 预留滚动条(int width) {
        this.滚动条预留宽度 = width;
        return this;
    }

    // ==================== 位置更新 ====================

    public void 位置(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void 位置(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    // ==================== 绘制 ====================

    /**
     * 绘制面板（背景 + 边框 + 标题）
     */
    public void 绘制(GuiGraphics graphics, Font font) {
        graphics.fill(x, y, x + w, y + h, 背景色);
        渲染工具.绘制边框矩形(graphics, x, y, w, h, 边框色);

        if (标题 != null) {
            graphics.drawString(font, 标题, x + 边框宽度 + 内边距 + 2, y + 3, 标题色);
        }
    }

    // ==================== 区域查询 ====================

    /**
     * 获取内容区域的起始坐标和尺寸
     * 格式：[x, y, w, h]
     *
     * 自动排除：边框 + 标题 + 内边距 + 滚动条预留
     * 调用方在此范围内绘制行即可，行宽 = 返回的w
     */
    public int[] 获取内容区域() {
        int contentX = x + 边框宽度 + 内边距;
        int contentW = w - (边框宽度 + 内边距) * 2 - 滚动条预留宽度;

        int contentY;
        int contentH;
        if (标题 != null) {
            contentY = y + 标题区高度;
            contentH = h - 标题区高度 - 边框宽度 - 内边距;
        } else {
            contentY = y + 边框宽度 + 内边距;
            contentH = h - (边框宽度 + 内边距) * 2;
        }

        return new int[]{contentX, contentY, contentW, contentH};
    }

    /**
     * 获取滚动条推荐位置
     * 格式：[x, y, h]
     *
     * 滚动条在内容区域右侧
     */
    public int[] 获取滚动条区域() {
        int[] content = 获取内容区域();
        int scrollX = content[0] + content[2] + 2;
        int scrollY = content[1];
        int scrollH = content[3];
        return new int[]{scrollX, scrollY, scrollH};
    }

    /**
     * 获取面板外部边界
     */
    public int[] 获取边界() {
        return new int[]{x, y, w, h};
    }

    public int 获取X() { return x; }
    public int 获取Y() { return y; }
    public int 获取宽度() { return w; }
    public int 获取高度() { return h; }

    /**
     * 判断坐标是否在面板内
     */
    public boolean 包含坐标(double mouseX, double mouseY) {
        return 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, h);
    }
}
