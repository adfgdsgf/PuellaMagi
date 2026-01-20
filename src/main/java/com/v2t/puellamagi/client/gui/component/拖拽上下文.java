// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/拖拽上下文.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.util.数学工具;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * 拖拽状态管理器
 *
 * 包含防误触机制：
 * - 鼠标按下时记录位置
 * - 移动超过阈值才进入拖拽状态
 * - 未超过阈值松开视为点击
 */
public class 拖拽上下文<T extends I槽位内容> {

    // 状态
    private boolean 鼠标按下中 = false;
    private boolean 正在拖拽 = false;

    // 按下位置
    private double 按下X, 按下Y;

    // 拖拽内容
    @Nullable
    private T 拖拽内容 = null;

    // 来源槽位索引（-1表示从外部拖入）
    private int 来源索引 = -1;

    // ==================== 状态查询 ====================

    public boolean 是否鼠标按下中() {
        return 鼠标按下中;
    }

    public boolean 是否正在拖拽() {
        return 正在拖拽;
    }

    @Nullable
    public T 获取拖拽内容() {
        return 拖拽内容;
    }

    public int 获取来源索引() {
        return 来源索引;
    }

    // ==================== 拖拽流程 ====================

    /**
     * 开始按下（记录位置，等待判断是否拖拽）
     * @param content 可能被拖拽的内容
     * @param sourceIndex 来源槽位索引
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     */
    public void 开始按下(@Nullable T content, int sourceIndex, double mouseX, double mouseY) {
        this.鼠标按下中 = true;
        this.正在拖拽 = false;
        this.按下X = mouseX;
        this.按下Y = mouseY;
        this.拖拽内容 = content;
        this.来源索引 = sourceIndex;
    }

    /**
     * 鼠标移动时调用，判断是否进入拖拽
     * @return 是否刚进入拖拽状态
     */
    public boolean 更新拖拽(double mouseX, double mouseY) {
        if (!鼠标按下中 || 正在拖拽) {
            return false;
        }

        if (数学工具.超过拖拽阈值(按下X, 按下Y, mouseX, mouseY)) {
            正在拖拽 = true;
            return true;
        }
        return false;
    }

    /**
     * 鼠标松开
     * @return 结果类型
     */
    public 松开结果 结束拖拽() {
        松开结果 result;
        if (正在拖拽) {
            result = 松开结果.拖拽完成;} else if (鼠标按下中) {
            result = 松开结果.点击;
        } else {
            result = 松开结果.无操作;
        }

        // 重置状态
        鼠标按下中 = false;
        正在拖拽 = false;
        拖拽内容 = null;
        来源索引 = -1;

        return result;
    }

    /**
     * 强制取消拖拽
     */
    public void 取消() {
        鼠标按下中 = false;
        正在拖拽 = false;
        拖拽内容 = null;
        来源索引 = -1;
    }

    // ==================== 渲染 ====================

    /**
     * 绘制拖拽中的内容
     */
    public void 绘制拖拽(GuiGraphics graphics, int mouseX, int mouseY, int size) {
        if (!正在拖拽 || 拖拽内容 == null) return;

        int x = mouseX - size / 2;
        int y = mouseY - size / 2;

        // 半透明绘制
        // 注意：具体效果由I槽位内容的绘制方法决定
        拖拽内容.绘制(graphics, x, y, size);
    }

    // ==================== 结果枚举 ====================

    public enum 松开结果 {
        无操作,// 没有按下过
        点击,      // 按下后未移动足够距离
        拖拽完成   // 完成了拖拽
    }
}
