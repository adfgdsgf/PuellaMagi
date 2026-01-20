// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/I槽位内容.java

package com.v2t.puellamagi.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 槽位内容接口
 *抽象技能、物品等可放入槽位的东西
 */
public interface I槽位内容 {

    /**
     * 获取内容的唯一标识
     */
    ResourceLocation 获取ID();

    /**
     * 获取显示名称
     */
    Component 获取名称();

    /**
     * 获取图标资源路径（可为null，则显示首字母）
     */
    @Nullable
    ResourceLocation 获取图标();

    /**
     * 绘制内容到槽位中
     * @param graphics 绘图上下文
     * @param x 槽位左上角X
     * @param y 槽位左上角Y
     * @param size 槽位尺寸
     */
    default void 绘制(GuiGraphics graphics, int x, int y, int size) {
        // 默认实现：绘制首字母
        String initial = 获取ID().getPath().substring(0, 1).toUpperCase();
        graphics.drawCenteredString(
                net.minecraft.client.Minecraft.getInstance().font,
                initial,
                x + size / 2,
                y + (size - 8) / 2,
                0xFFFFFF
        );
    }

    /**
     * 获取详情信息（用于悬浮显示）
     */
    default List<Component> 获取详情() {
        return List.of(获取名称());
    }
}
