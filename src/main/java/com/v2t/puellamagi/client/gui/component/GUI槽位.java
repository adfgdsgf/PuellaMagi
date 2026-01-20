// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI槽位.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * 可复用的GUI槽位组件
 */
public class GUI槽位 {

    // ==================== 配置 ====================

    private int x, y;
    private int 尺寸;
    private int 索引;

    @Nullable
    private 资源工具.纹理信息 背景纹理;

    @Nullable
    private I槽位内容 内容;

    @Nullable
    private String 快捷键文字;

    // ==================== 状态 ====================

    private boolean 悬停中= false;
    private boolean 选中 = false;
    private boolean 拖拽悬停 = false;
    private boolean 禁用 = false;

    // ==================== 构造器 ====================

    public GUI槽位(int 索引) {
        this.索引 = 索引;
        this.尺寸 = 常量.GUI_槽位尺寸;
    }

    public GUI槽位(int 索引, int x, int y) {
        this(索引);
        this.x = x;
        this.y = y;
    }

    // ==================== 配置方法（链式调用） ====================

    public GUI槽位 位置(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public GUI槽位 尺寸(int size) {
        this.尺寸 = size;
        return this;
    }

    /**
     * 设置背景纹理（使用纹理信息）
     */
    public GUI槽位 背景(资源工具.纹理信息 texture) {
        this.背景纹理 = texture;
        return this;
    }

    public GUI槽位 内容(@Nullable I槽位内容 content) {
        this.内容 = content;
        return this;
    }

    public GUI槽位 快捷键(@Nullable String keyText) {
        this.快捷键文字 = keyText;
        return this;
    }

    public GUI槽位 禁用(boolean disabled) {
        this.禁用 = disabled;
        return this;
    }

    // ==================== Getter ====================

    public int 获取X() {
        return x;
    }

    public int 获取Y() {
        return y;
    }

    public int 获取尺寸() {
        return 尺寸;
    }

    public int 获取索引() {
        return 索引;
    }

    @Nullable
    public I槽位内容 获取内容() {
        return 内容;
    }

    public boolean 是否为空() {
        return 内容== null;
    }

    public boolean 是否悬停() {
        return 悬停中;
    }

    public boolean 是否选中() {
        return 选中;
    }

    public boolean 是否禁用() {
        return 禁用;
    }

    // ==================== 状态更新 ====================

    public void 设置内容(@Nullable I槽位内容 content) {
        this.内容 = content;
    }

    public void 设置选中(boolean selected) {
        this.选中 = selected;
    }

    public void 设置拖拽悬停(boolean dragHover) {
        this.拖拽悬停 = dragHover;
    }

    /**
     * 更新悬停状态
     * @return 是否悬停在此槽位上
     */
    public boolean 更新悬停(int mouseX, int mouseY) {悬停中 = !禁用 && 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, 尺寸,尺寸);
        return 悬停中;
    }

    // ==================== 渲染 ====================

    public void 绘制(GuiGraphics graphics) {
        // 正确的绘制顺序：背景 -> 内容 -> 高亮 -> 快捷键 -> 禁用遮罩

        // 1. 绘制背景
        绘制背景(graphics);

        // 2. 绘制内容绘制内容(graphics);

        // 3. 绘制高亮边框
        绘制高亮(graphics);

        // 4. 绘制快捷键
        绘制快捷键(graphics);

        // 5. 禁用遮罩
        if (禁用) {
            graphics.fill(x, y, x + 尺寸, y + 尺寸, 0x80000000);
        }
    }

    private void 绘制背景(GuiGraphics graphics) {
        if (背景纹理 != null) {
            渲染工具.绘制纹理(graphics, 背景纹理, x, y, 尺寸, 尺寸);
        } else {
            // 无纹理时绘制默认背景
            graphics.fill(x, y, x + 尺寸, y + 尺寸, 渲染工具.颜色_槽位背景);
            渲染工具.绘制边框矩形(graphics, x, y, 尺寸, 尺寸, 渲染工具.颜色_槽位边框);
        }
    }

    private void 绘制高亮(GuiGraphics graphics) {
        int highlightColor = 0;

        if (拖拽悬停) {
            highlightColor = 常量.GUI_颜色_拖拽高亮;
        } else if (选中) {
            highlightColor = 常量.GUI_颜色_选中高亮;
        } else if (悬停中) {
            highlightColor = 常量.GUI_颜色_悬停高亮;
        }

        if (highlightColor != 0) {
            渲染工具.绘制边框矩形(graphics, x, y, 尺寸, 尺寸, highlightColor, 0, 2);
        }
    }

    private void 绘制内容(GuiGraphics graphics) {
        if (内容 != null) {
            int padding = 2;
            内容.绘制(graphics, x + padding, y + padding, 尺寸 - padding * 2);
        }
    }

    private void 绘制快捷键(GuiGraphics graphics) {
        if (快捷键文字 == null || 快捷键文字.isEmpty()) {
            return;
        }

        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textX = x + 尺寸 - font.width(快捷键文字) - 2;
        int textY = y + 尺寸 - font.lineHeight;

        // 绘制阴影
        graphics.drawString(font, 快捷键文字, textX + 1, textY + 1, 0x000000, false);
        graphics.drawString(font, 快捷键文字, textX, textY, 0x888888, false);
    }

    // ==================== 交互判断 ====================

    public boolean 包含坐标(double mouseX, double mouseY) {
        return 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, 尺寸, 尺寸);
    }
}
