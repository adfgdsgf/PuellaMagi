//文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI确认按钮.java

package com.v2t.puellamagi.client.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 二次确认按钮
 *
 * 第一次点击进入确认状态（文字变为"确认?"），
 * 超时或再次点击才执行实际操作。
 *
 * 用法：
 *   1. 创建实例，传入普通文字和确认文字
 *   2. 每帧调用 绘制()
 *   3. 点击时调用 点击() — 返回true表示确认执行
 *   4. 每帧调用 检查超时() 或在render中统一调用
 *
 * 复用场景：离开队伍、解散队伍、踢人、删除预设等危险操作
 */
public class GUI确认按钮 {

    private final Component 普通文字;
    private final Component 确认文字;
    private final boolean 危险样式;

    private boolean 等待确认 = false;
    private long 确认开始时间 = 0;

    /** 确认超时时间（毫秒） */
    private long 超时毫秒 = 3000;

    // ==================== 构造 ====================

    /**
     * @param normalText 正常状态显示的文字
     * @param confirmText 确认状态显示的文字
     * @param danger 是否使用危险样式（红色系）
     */
    public GUI确认按钮(Component normalText, Component confirmText, boolean danger) {
        this.普通文字 = normalText;
        this.确认文字 = confirmText;
        this.危险样式 = danger;
    }

    /**
     * 设置确认超时时间
     *
     * @param millis 毫秒
     * @return this（链式调用）
     */
    public GUI确认按钮 超时(long millis) {
        this.超时毫秒 = millis;
        return this;
    }

    // ==================== 绘制 ====================

    /**
     * 绘制按钮
     *
     * @return鼠标是否悬停
     */
    public boolean 绘制(GuiGraphics graphics, Font font,int x, int y, int w, int h,
                        int mouseX, int mouseY) {
        return 绘制(graphics, font, x, y, w, h, mouseX, mouseY, true);
    }

    /**
     * 绘制按钮（可控制启用状态）
     *
     * @return 鼠标是否悬停
     */
    public boolean 绘制(GuiGraphics graphics, Font font,
                        int x, int y, int w, int h,
                        int mouseX, int mouseY, boolean enabled) {
        Component text = 等待确认 ? 确认文字 : 普通文字;
        return GUI按钮.绘制(graphics, font, x, y, w, h, text,
                mouseX, mouseY,危险样式, enabled);
    }

    // ==================== 交互 ====================

    /**
     * 处理点击
     *
     * @return true = 二次确认完成，应执行实际操作
     *         false = 进入确认等待状态，还不执行
     */
    public boolean 点击() {
        if (等待确认) {
            等待确认 = false;
            return true;
        } else {
            等待确认 = true;
            确认开始时间 = System.currentTimeMillis();
            return false;
        }
    }

    /**
     * 检查是否超时，超时则自动取消确认状态
     * 建议在render()或tick()中每帧调用
     */
    public void 检查超时() {
        if (等待确认 && System.currentTimeMillis() - 确认开始时间 > 超时毫秒) {
            等待确认 = false;
        }
    }

    /**
     * 重置确认状态
     * 切换选中目标、关闭界面时调用
     */
    public void 重置() {
        等待确认 = false;
    }

    /**
     * 是否正在等待确认
     */
    public boolean 是否等待确认() {
        return 等待确认;
    }
}
