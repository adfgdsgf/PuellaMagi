package com.v2t.puellamagi.util;

import com.v2t.puellamagi.常量;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 本地化快捷工具
 * 禁止硬编码文本，所有显示文本必须通过此工具
 */
public final class 本地化工具 {
    private 本地化工具() {}

    /**
     * 通用翻译键
     */
    public static MutableComponent 翻译(String fullKey, Object... args) {
        return Component.translatable(fullKey, args);
    }

    /**
     * 技能名称 (skill.puellamagi.xxx)
     */
    public static MutableComponent 技能名(String skillId) {
        return Component.translatable("skill." + 常量.MOD_ID + "." + skillId);
    }

    /**
     * 能力名称 (ability.puellamagi.xxx)
     */
    public static MutableComponent 能力名(String abilityId) {
        return Component.translatable("ability." + 常量.MOD_ID + "." + abilityId);
    }

    /**
     * GUI文本 (gui.puellamagi.xxx)
     */
    public static MutableComponent GUI(String key) {
        return Component.translatable("gui." + 常量.MOD_ID + "." + key);
    }

    /**
     * 消息文本 (message.puellamagi.xxx)
     */
    public static MutableComponent 消息(String key, Object... args) {
        return Component.translatable("message." + 常量.MOD_ID + "." + key, args);
    }

    /**
     * 提示文本 (tooltip.puellamagi.xxx)
     */
    public static MutableComponent 提示(String key, Object... args) {
        return Component.translatable("tooltip." + 常量.MOD_ID + "." + key, args);
    }

    /**
     * 按键名称 (key.puellamagi.xxx)
     */
    public static MutableComponent 按键(String key) {
        return Component.translatable("key." + 常量.MOD_ID + "." + key);
    }
}
