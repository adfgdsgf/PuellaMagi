// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/hud/I可编辑HUD.java

package com.v2t.puellamagi.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * 可编辑HUD接口
 *
 * 所有可在HUD编辑界面中调整的HUD组件都应实现此接口
 * 支持：位置、方向、缩放的统一编辑
 */
public interface I可编辑HUD {

    /**
     * 获取HUD唯一标识（用于存储配置）
     */
    String 获取HUD标识();

    /**
     * 获取显示名称（编辑界面中显示）
     */
    Component 获取显示名称();

    /**
     * 检测当前玩家是否应该显示此HUD
     */
    boolean 当前是否显示(Player player);

    //==================== 位置相关 ====================

    /**
     * 获取当前位置
     * @return [x, y]
     */
    int[] 获取当前位置();

    /**
     * 设置位置
     */
    void 设置位置(int x, int y);

    /**
     * 获取默认位置
     * @return [x, y]
     */
    int[] 获取默认位置();

    /**
     * 获取当前尺寸（考虑缩放后的实际尺寸）
     * @return [宽度, 高度]
     */
    int[] 获取尺寸();

    // ==================== 方向相关 ====================

    /**
     * 是否支持方向切换
     */
    default boolean 支持方向切换() {
        return false;
    }

    /**
     * 获取当前方向
     */
    default HUD方向 获取方向() {
        return HUD方向.横向;
    }

    /**
     * 设置方向
     */
    default void 设置方向(HUD方向 direction) {
        // 默认不支持
    }

    // ==================== 缩放相关 ====================

    /**
     * 是否支持缩放
     */
    default boolean 支持缩放() {
        return false;
    }

    /**
     * 获取当前缩放比例
     */
    default float 获取缩放() {
        return 1.0f;
    }

    /**
     * 设置缩放比例
     */
    default void 设置缩放(float scale) {
        // 默认不支持
    }

    /**
     * 获取最小缩放
     */
    default float 获取最小缩放() {
        return 0.5f;
    }

    /**
     * 获取最大缩放
     */
    default float 获取最大缩放() {
        return 2.0f;
    }

    // ==================== 编辑模式 ====================

    /**
     * 进入编辑模式
     */
    void 进入编辑模式();

    /**
     * 退出编辑模式
     * @param save 是否保存更改
     */
    void 退出编辑模式(boolean save);

    /**
     * 是否处于编辑模式
     */
    boolean 是否编辑模式();

    /**
     * 重置为默认设置
     */
    default void 重置为默认() {
        int[] defaultPos = 获取默认位置();
        设置位置(defaultPos[0], defaultPos[1]);
        if (支持方向切换()) {
            设置方向(HUD方向.横向);
        }
        if (支持缩放()) {
            设置缩放(1.0f);
        }
    }

    // ==================== 坐标检测 ====================

    /**
     * 检查坐标是否在HUD区域内
     */
    default boolean 坐标在HUD上(double mouseX, double mouseY) {
        int[] pos = 获取当前位置();
        int[] size = 获取尺寸();
        return mouseX >= pos[0] && mouseX <= pos[0] + size[0]
                && mouseY >= pos[1] && mouseY <= pos[1] + size[1];
    }

    /**
     *绘制编辑模式边框
     */
    void 绘制编辑边框(GuiGraphics graphics, boolean selected);

    // ==================== 方向枚举 ====================

    enum HUD方向 {
        横向,纵向;

        public HUD方向 切换() {
            return this == 横向 ? 纵向 : 横向;
        }
    }
}
