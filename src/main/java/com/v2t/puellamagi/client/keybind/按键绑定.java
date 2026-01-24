// 文件路径: src/main/java/com/v2t/puellamagi/client/keybind/按键绑定.java

package com.v2t.puellamagi.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.v2t.puellamagi.常量;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 按键绑定注册
 *
 * 所有快捷键在此定义，通过ClientSetup注册到Forge
 */
public final class 按键绑定 {
    private 按键绑定() {}

    //按键类别
    private static final String 类别 = "key." + 常量.MOD_ID + ".category";

    //==================== 变身相关 ====================

    /**
     * 变身/解除变身切换键
     * 默认：R
     */
    public static final KeyMapping 变身键 = new KeyMapping(
            "key." + 常量.MOD_ID + ".transform",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            类别
    );

    // ==================== 技能栏相关 ====================

    /**
     * 技能1-6快捷键
     * 默认：Z/X/C/V/B/N
     */
    public static final KeyMapping[] 技能键 = new KeyMapping[] {
            创建技能键(1, GLFW.GLFW_KEY_Z),
            创建技能键(2, GLFW.GLFW_KEY_X),
            创建技能键(3, GLFW.GLFW_KEY_C),
            创建技能键(4, GLFW.GLFW_KEY_V),
            创建技能键(5, GLFW.GLFW_KEY_B),
            创建技能键(6, GLFW.GLFW_KEY_N)
    };

    private static KeyMapping 创建技能键(int index, int defaultKey) {
        return new KeyMapping(
                "key." + 常量.MOD_ID + ".skill_" + index,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                defaultKey,
                类别
        );
    }

    /**
     * 打开技能栏编辑界面
     * 默认：K
     */
    public static final KeyMapping 技能栏编辑键 = new KeyMapping(
            "key." + 常量.MOD_ID + ".skill_bar_edit",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            类别
    );

    /**
     * 切换到下一个预设
     * 默认：]
     */
    public static final KeyMapping 下一预设键 = new KeyMapping(
            "key." + 常量.MOD_ID + ".preset_next",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            类别
    );

    /**
     * 切换到上一个预设
     * 默认：[
     */
    public static final KeyMapping 上一预设键 = new KeyMapping(
            "key." + 常量.MOD_ID + ".preset_prev",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,
            类别
    );

    /**
     * 折叠/展开技能栏
     * 默认：未绑定
     */
    public static final KeyMapping 技能栏折叠键 = new KeyMapping(
            "key." + 常量.MOD_ID + ".skill_bar_toggle",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            类别
    );

    // ==================== 交互相关 ====================

    /**
     * 搜身修饰键
     * 按住此键 + 右键点击玩家 = 搜身
     * 默认：左Ctrl
     */
    public static final KeyMapping 搜身修饰键 = new KeyMapping(
            "key." + 常量.MOD_ID + ".search_modifier",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            类别
    );

    // ==================== 获取所有按键 ====================

    public static KeyMapping[] 获取所有按键() {
        KeyMapping[] all = new KeyMapping[技能键.length + 6];

        all[0] = 变身键;
        all[1] = 技能栏编辑键;
        all[2] = 下一预设键;
        all[3] = 上一预设键;
        all[4] = 技能栏折叠键;
        all[5] = 搜身修饰键;

        System.arraycopy(技能键, 0, all, 6, 技能键.length);

        return all;
    }
}
