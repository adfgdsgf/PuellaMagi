// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI翻页.java

package com.v2t.puellamagi.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;

/**
 * 通用翻页组件
 *
 * 风格与GUI滚动条保持一致：
 * - 链式配置
 * - 统一的鼠标交互接口
 * - 自动计算页数
 */
public class GUI翻页 {

    // ==================== 布局常量 ====================

    private static final int 按钮宽度 = 16;
    private static final int 按钮高度 = 12;
    private static final int 间距 = 4;

    // ==================== 配置 ====================

    private int x, y;
    private int 总数量 = 0;
    private int 每页容量 = 1;
    private int 当前页 = 0;

    // 颜色
    private int 按钮颜色 = 0xAA666666;
    private int 按钮悬停颜色 = 0xAA888888;
    private int 按钮禁用颜色 = 0x44666666;
    private int 文字颜色 = 0xFFFFFFFF;
    private int 文字禁用颜色 = 0xFF888888;

    // ==================== 状态 ====================

    private boolean 左按钮悬停 = false;
    private boolean 右按钮悬停 = false;

    // 回调
    private IntConsumer 翻页回调 = null;

    // ==================== 构造器 ====================

    public GUI翻页(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // ==================== 链式配置 ====================

    public GUI翻页 位置(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public GUI翻页 总数量(int total) {
        this.总数量 = Math.max(0, total);
        // 确保当前页有效
        int maxPage = 获取最大页();
        if (当前页 > maxPage) {
            当前页 = maxPage;
        }
        return this;
    }

    public GUI翻页 每页容量(int size) {
        this.每页容量 = Math.max(1, size);
        return this;
    }

    public GUI翻页 颜色(int 按钮颜色, int 按钮悬停颜色, int 文字颜色) {
        this.按钮颜色 = 按钮颜色;
        this.按钮悬停颜色 = 按钮悬停颜色;
        this.文字颜色 = 文字颜色;
        return this;
    }

    public GUI翻页 翻页回调(IntConsumer callback) {
        this.翻页回调 = callback;
        return this;
    }

    // ==================== 状态查询 ====================

    public int 获取总页数() {
        if (总数量 <= 0) return 1;
        return (int) Math.ceil((double) 总数量 / 每页容量);
    }

    public int 获取最大页() {
        return Math.max(0, 获取总页数() - 1);
    }

    public int 获取当前页() {
        return 当前页;
    }

    public int 获取起始索引() {
        return 当前页 * 每页容量;
    }

    public int 获取结束索引() {
        return Math.min((当前页 + 1) * 每页容量, 总数量);
    }

    public int 获取当前页数量() {
        return 获取结束索引() - 获取起始索引();
    }

    public boolean 需要显示() {
        return 获取总页数() > 1;
    }

    public boolean 可以上一页() {
        return 当前页 > 0;
    }

    public boolean 可以下一页() {
        return 当前页 < 获取最大页();
    }

    // ==================== 操作 ====================

    public void 上一页() {
        if (可以上一页()) {
            当前页--;
            触发回调();
        }
    }

    public void 下一页() {
        if (可以下一页()) {
            当前页++;
            触发回调();
        }
    }

    public void 设置当前页(int page) {
        int target = Math.max(0, Math.min(page, 获取最大页()));
        当前页 = target;  // 直接设置，不触发回调（用于初始化同步）
    }

    public void 重置() {
        当前页 = 0;
    }

    private void 触发回调() {
        if (翻页回调 != null) {
            翻页回调.accept(当前页);
        }
    }

    // ====================坐标计算 ====================

    private int 获取组件宽度() {
        Font font = Minecraft.getInstance().font;
        String pageText = (当前页 + 1) + "/" + 获取总页数();
        int textWidth = font.width(pageText);
        return 按钮宽度 +间距 + textWidth + 间距 + 按钮宽度;
    }

    private int 获取左按钮X() {
        return x;
    }

    private int 获取右按钮X() {
        return x + 获取组件宽度() - 按钮宽度;
    }

    private int 获取文字X() {
        return x + 按钮宽度 + 间距;
    }

    // ==================== 坐标检测 ====================

    private boolean 坐标在左按钮上(double mouseX, double mouseY) {
        int btnX = 获取左按钮X();
        return mouseX >= btnX && mouseX< btnX + 按钮宽度
                && mouseY >= y && mouseY < y + 按钮高度;
    }

    private boolean 坐标在右按钮上(double mouseX, double mouseY) {
        int btnX = 获取右按钮X();
        return mouseX >= btnX && mouseX < btnX + 按钮宽度
                && mouseY >= y && mouseY < y + 按钮高度;
    }

    // ==================== 渲染 ====================

    public void 更新悬停(int mouseX, int mouseY) {
        左按钮悬停 = 坐标在左按钮上(mouseX, mouseY);
        右按钮悬停 = 坐标在右按钮上(mouseX, mouseY);
    }

    public void 绘制(GuiGraphics gui, int mouseX, int mouseY) {
        if (!需要显示()) return;

        更新悬停(mouseX, mouseY);

        Font font = Minecraft.getInstance().font;

        // 左按钮 <
        int leftBtnX = 获取左按钮X();
        int leftColor = !可以上一页() ? 按钮禁用颜色 : (左按钮悬停 ? 按钮悬停颜色 : 按钮颜色);
        gui.fill(leftBtnX, y, leftBtnX + 按钮宽度, y + 按钮高度, leftColor);
        int leftTextColor = 可以上一页() ? 文字颜色 : 文字禁用颜色;
        gui.drawCenteredString(font, "<", leftBtnX + 按钮宽度 / 2, y + 2, leftTextColor);

        // 页码文字
        String pageText = (当前页 + 1) + "/" + 获取总页数();
        int textX = 获取文字X();
        int textWidth = font.width(pageText);
        gui.drawString(font, pageText, textX, y + 2, 文字颜色, false);

        // 右按钮 >
        int rightBtnX = 获取右按钮X();
        int rightColor = !可以下一页() ? 按钮禁用颜色 : (右按钮悬停 ? 按钮悬停颜色 : 按钮颜色);
        gui.fill(rightBtnX, y, rightBtnX + 按钮宽度, y + 按钮高度, rightColor);
        int rightTextColor = 可以下一页() ? 文字颜色 : 文字禁用颜色;
        gui.drawCenteredString(font, ">", rightBtnX + 按钮宽度 / 2, y + 2, rightTextColor);
    }

    // ==================== 鼠标交互 ====================

    /**
     * 鼠标点击
     * @return 是否处理了点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !需要显示()) return false;

        if (坐标在左按钮上(mouseX, mouseY) &&可以上一页()) {
            上一页();
            return true;
        }

        if (坐标在右按钮上(mouseX, mouseY) && 可以下一页()) {
            下一页();
            return true;
        }

        return false;
    }

    /**
     * 鼠标滚轮（可选，在翻页区域滚动时切换页）
     * @return 是否处理了滚动
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!需要显示()) return false;

        // 检查是否在组件区域内
        if (mouseX >= x && mouseX < x + 获取组件宽度()
                && mouseY >= y && mouseY < y + 按钮高度) {
            if (delta > 0) {
                上一页();
            } else if (delta < 0) {
                下一页();
            }
            return true;
        }

        return false;
    }
}
