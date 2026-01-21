// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/hud/可编辑HUD注册表.java

package com.v2t.puellamagi.client.gui.hud;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 可编辑HUD注册表
 *
 * 管理所有实现了I可编辑HUD的HUD组件
 */
public final class 可编辑HUD注册表 {
    private 可编辑HUD注册表() {}

    private static final List<I可编辑HUD> 注册列表 = new ArrayList<>();

    /**
     * 注册一个可编辑HUD
     */
    public static void 注册(I可编辑HUD hud) {
        if (!注册列表.contains(hud)) {
            注册列表.add(hud);
        }
    }

    /**
     * 获取所有注册的HUD
     */
    public static List<I可编辑HUD> 获取所有() {
        return List.copyOf(注册列表);
    }

    /**
     * 获取当前玩家应该显示的HUD列表
     */
    public static List<I可编辑HUD> 获取玩家可见HUD(Player player) {
        List<I可编辑HUD> visible = new ArrayList<>();
        for (I可编辑HUD hud : 注册列表) {
            if (hud.当前是否显示(player)) {
                visible.add(hud);
            }
        }
        return visible;
    }

    /**
     * 让指定HUD列表进入编辑模式
     */
    public static void 进入编辑模式(List<I可编辑HUD> hudList) {
        for (I可编辑HUD hud : hudList) {
            hud.进入编辑模式();
        }
    }

    /**
     * 让指定HUD列表退出编辑模式
     */
    public static void 退出编辑模式(List<I可编辑HUD> hudList, boolean save) {
        for (I可编辑HUD hud : hudList) {
            hud.退出编辑模式(save);
        }
    }

    /**
     * 重置所有HUD为默认设置
     */
    public static void 全部重置(List<I可编辑HUD> hudList) {
        for (I可编辑HUD hud : hudList) {
            hud.重置为默认();
        }
    }
}
