// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI滚动条.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.util.渲染工具;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 通用滚动条组件
 *
 * 支持：
 * - 鼠标拖动滑块
 * - 点击轨道跳转
 * - 滚轮滚动
 * - 自动计算滑块大小
 */
public class GUI滚动条 {

    // ==================== 配置 ====================

    private int x, y;
    private int 宽度 = 6;
    private int 高度;

    // 滚动范围
    private int 总行数 = 0;
    private int 可见行数 = 1;
    private int 当前行 = 0;

    // 颜色配置
    private int 轨道颜色 = 0x40FFFFFF;
    private int 滑块颜色 = 0xAAFFFFFF;
    private int 滑块悬停颜色 = 0xFFFFFFFF;

    // ==================== 状态 ====================

    private boolean 悬停中 = false;
    private boolean 正在拖动 = false;
    private int 拖动起始Y = 0;
    private int 拖动起始行 = 0;

    //滚动变化回调
    private Runnable 滚动回调 = null;

    // ==================== 构造器 ====================

    public GUI滚动条(int x, int y, int 高度) {
        this.x = x;
        this.y = y;
        this.高度 = 高度;
    }

    // ==================== 配置方法（链式调用） ====================

    public GUI滚动条 位置(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public GUI滚动条 尺寸(int 宽度, int 高度) {
        this.宽度 = 宽度;
        this.高度 = 高度;
        return this;
    }

    public GUI滚动条 宽度(int 宽度) {
        this.宽度 = 宽度;
        return this;
    }

    public GUI滚动条 滚动范围(int 总行数, int 可见行数) {
        this.总行数 = 总行数;
        this.可见行数 = Math.max(1, 可见行数);
        // 确保当前行在有效范围内
        this.当前行 = Math.min(this.当前行, 获取最大滚动行());
        return this;
    }

    public GUI滚动条 颜色(int 轨道颜色, int 滑块颜色, int 滑块悬停颜色) {
        this.轨道颜色 =轨道颜色;
        this.滑块颜色 = 滑块颜色;
        this.滑块悬停颜色 = 滑块悬停颜色;
        return this;
    }

    public GUI滚动条 滚动回调(Runnable callback) {
        this.滚动回调 = callback;
        return this;
    }

    // ==================== Getter ====================

    public int 获取当前行() {
        return 当前行;
    }

    public int 获取最大滚动行() {
        return Math.max(0, 总行数 - 可见行数);
    }

    public boolean 是否可滚动() {
        return 获取最大滚动行() > 0;
    }

    public boolean 是否正在拖动() {
        return 正在拖动;
    }

    // ==================== Setter ====================

    public void 设置当前行(int 行) {
        int 旧值 = this.当前行;
        this.当前行 = Math.max(0, Math.min(行, 获取最大滚动行()));
        if (旧值 != this.当前行 && 滚动回调 != null) {
            滚动回调.run();
        }
    }

    public void 设置总行数(int 总行数) {
        this.总行数 = 总行数;
        this.当前行 = Math.min(this.当前行, 获取最大滚动行());
    }

    // ==================== 滑块计算 ====================

    private int 获取滑块高度() {
        if (!是否可滚动()) return 高度;
        int minHeight = 20;
        int calculatedHeight = 高度 * 可见行数 / 总行数;
        return Math.max(minHeight, calculatedHeight);
    }

    private int 获取滑块Y() {
        if (!是否可滚动()) return y;
        int 滑块高度 = 获取滑块高度();
        int 可移动范围 = 高度 - 滑块高度;
        float 滚动百分比 = (float) 当前行 / 获取最大滚动行();
        return y + (int) (滚动百分比 * 可移动范围);
    }

    // ==================== 坐标检测 ====================

    /**
     * 检查坐标是否在滑块上
     */
    public boolean 坐标在滑块上(double mouseX, double mouseY) {
        if (!是否可滚动()) return false;

        int 滑块Y = 获取滑块Y();
        int 滑块高度 = 获取滑块高度();

        return mouseX >= x && mouseX < x + 宽度
                && mouseY >= 滑块Y && mouseY < 滑块Y + 滑块高度;
    }

    /**
     * 检查坐标是否在轨道上
     */
    public boolean 坐标在轨道上(double mouseX, double mouseY) {
        if (!是否可滚动()) return false;

        return mouseX >= x && mouseX < x + 宽度
                && mouseY >= y && mouseY < y + 高度;
    }

    // ====================渲染 ====================

    /**
     * 更新悬停状态
     */
    public void 更新悬停(int mouseX, int mouseY) {悬停中 = 坐标在滑块上(mouseX, mouseY);
    }

    /**
     * 绘制滚动条
     */
    public void 绘制(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!是否可滚动()) return;

        更新悬停(mouseX, mouseY);

        // 绘制轨道
        graphics.fill(x, y, x + 宽度, y + 高度,轨道颜色);

        // 绘制滑块
        int 滑块Y = 获取滑块Y();
        int 滑块高度 = 获取滑块高度();
        int 颜色 = (悬停中 || 正在拖动) ?滑块悬停颜色 : 滑块颜色;
        graphics.fill(x, 滑块Y, x + 宽度, 滑块Y + 滑块高度, 颜色);
    }

    // ==================== 鼠标交互 ====================

    /**
     * 鼠标点击
     * @return 是否处理了点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !是否可滚动()) return false;

        if (坐标在滑块上(mouseX, mouseY)) {
            // 开始拖动滑块
            正在拖动 = true;
            拖动起始Y = (int) mouseY;
            拖动起始行 = 当前行;
            return true;
        } else if (坐标在轨道上(mouseX, mouseY)) {
            // 点击轨道，跳转到对应位置
            float 点击百分比 = (float) (mouseY - y) / 高度;
            设置当前行(Math.round(点击百分比 * 获取最大滚动行()));
            return true;
        }

        return false;
    }

    /**
     * 鼠标拖动
     * @return 是否处理了拖动
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !正在拖动) return false;

        int 滑块高度 = 获取滑块高度();
        int 可移动范围 = 高度 - 滑块高度;

        if (可移动范围 > 0) {
            int deltaY = (int) mouseY - 拖动起始Y;
            float deltaPercent = (float) deltaY / 可移动范围;
            int deltaRows = Math.round(deltaPercent * 获取最大滚动行());
            设置当前行(拖动起始行 + deltaRows);
        }

        return true;
    }

    /**
     * 鼠标释放
     * @return 是否处理了释放
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && 正在拖动) {
            正在拖动 = false;
            return true;
        }
        return false;
    }

    /**
     * 鼠标滚轮
     * @param delta 滚动量（正=向上，负=向下）
     * @return 是否处理了滚动
     */
    public boolean mouseScrolled(double delta) {
        if (!是否可滚动()) return false;

        if (delta > 0) {
            设置当前行(当前行 - 1);
        } else if (delta < 0) {
            设置当前行(当前行 + 1);
        }

        return true;
    }

    /**
     * 检查坐标是否在滚动区域内（轨道区域）
     *用于判断是否要处理滚轮事件
     */
    public boolean 坐标在滚动区域内(double mouseX, double mouseY, int 区域X, int 区域Y, int 区域宽度, int 区域高度) {
        return mouseX >= 区域X && mouseX < 区域X + 区域宽度
                && mouseY >= 区域Y && mouseY < 区域Y + 区域高度;
    }
}
