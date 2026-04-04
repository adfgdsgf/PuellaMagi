// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI覆盖层.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.util.渲染工具;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 覆盖层组件
 *
 * 提供：
 * - 半透明遮罩（覆盖底层内容）
 * - z层级管理（确保遮罩盖住底层文字）
 * - 弹窗容器定位
 *
 * 使用方式：
 *   GUI覆盖层 overlay = new GUI覆盖层(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
 *   overlay.开始渲染(graphics);
 *   // pushPose +遮罩
 *   // ... 绘制弹窗内容 ...
 *   overlay.结束渲染(graphics);  // popPose
 *
 * 复用场景：邀请面板、确认对话框、任何模态弹窗
 */
public class GUI覆盖层 {

    // ==================== 默认配置 ====================

    public static final int 默认遮罩色 = 0xE8000000;

    // ==================== 数据 ====================

    private int 遮罩X, 遮罩Y, 遮罩W, 遮罩H;
    private int 遮罩色;
    private int z层级;

    // ==================== 构造 ====================

    /**
     * @param x 遮罩区域X
     * @param y 遮罩区域Y
     * @param w 遮罩区域宽度
     * @param h 遮罩区域高度
     */
    public GUI覆盖层(int x, int y, int w, int h) {
        this.遮罩X = x;
        this.遮罩Y = y;
        this.遮罩W = w;
        this.遮罩H = h;
        this.遮罩色 = 默认遮罩色;
        this.z层级 = 渲染工具.Z_层级_弹窗;
    }

    // ==================== 链式配置 ====================

    public GUI覆盖层 遮罩颜色(int color) {
        this.遮罩色 = color;
        return this;
    }

    public GUI覆盖层 层级(int zLevel) {
        this.z层级 = zLevel;
        return this;
    }

    // ==================== 位置更新 ====================

    /**
     * 更新遮罩区域（界面resize时调用）
     */
    public void 更新区域(int x, int y, int w, int h) {
        this.遮罩X = x;
        this.遮罩Y = y;
        this.遮罩W = w;
        this.遮罩H = h;
    }

    // ==================== 渲染控制 ====================

    /**
     * 开始覆盖层渲染
     * 提升z层级 +绘制遮罩
     * 之后的所有绘制都在此z层级上
     *
     * 必须与 结束渲染() 配对调用
     */
    public void 开始渲染(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, z层级);

        //绘制遮罩
        graphics.fill(遮罩X, 遮罩Y, 遮罩X + 遮罩W, 遮罩Y + 遮罩H, 遮罩色);
    }

    /**
     * 结束覆盖层渲染
     * 恢复z层级
     *
     * 必须与 开始渲染() 配对调用
     */
    public void 结束渲染(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    /**
     * 使用Runnable执行覆盖层渲染（自动配对）
     * 等价于手动调用 开始渲染 / 结束渲染，但不会遗漏popPose
     *
     * @param graphics 绘图上下文
     * @param 渲染动作 在覆盖层z层级上执行的渲染逻辑
     */
    public void 渲染(GuiGraphics graphics, Runnable 渲染动作) {
        开始渲染(graphics);
        try {
            渲染动作.run();
        } finally {
            结束渲染(graphics);
        }
    }

    // ==================== 弹窗定位辅助 ====================

    /**
     * 计算居中弹窗的坐标
     *
     * @param popupW 弹窗宽度
     * @param popupH 弹窗高度
     * @return [x, y] 弹窗左上角坐标
     */
    public int[] 计算居中坐标(int popupW, int popupH) {
        int px = 遮罩X + (遮罩W - popupW) / 2;
        int py = 遮罩Y + (遮罩H - popupH) / 2;
        return new int[]{px, py};
    }

    /**
     * 判断坐标是否在遮罩区域内
     */
    public boolean 在遮罩内(double mouseX, double mouseY) {
        return 渲染工具.鼠标在区域内(mouseX, mouseY, 遮罩X, 遮罩Y, 遮罩W, 遮罩H);
    }
}
