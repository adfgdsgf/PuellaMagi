//文件路径: src/main/java/com/v2t/puellamagi/core/config/时停配置.java

package com.v2t.puellamagi.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 时停系统配置
 *
 * 翻译键格式: config.puellamagi.timestop.xxx
 * 用于支持游戏内配置编辑器（如Configured mod）
 */
public class 时停配置 {

    public static final ForgeConfigSpec SPEC;

    //==================== 范围设置 ====================

    public static final ForgeConfigSpec.IntValue 时停范围;
    public static final ForgeConfigSpec.BooleanValue 仅当前维度;

    // ==================== 投射物设置 ====================

    public static final ForgeConfigSpec.DoubleValue 惯性衰减系数;
    public static final ForgeConfigSpec.DoubleValue 静止阈值;

    // ==================== 技能设置 ====================

    public static final ForgeConfigSpec.IntValue 蓄力时间;
    public static final ForgeConfigSpec.BooleanValue 受击打断蓄力;
    public static final ForgeConfigSpec.IntValue 语音冷却;

    // ==================== 构建配置 ====================

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("timestop");

        // 范围设置
        builder.push("range");

        时停范围 = builder
                .translation("config.puellamagi.timestop.range.range")
                .comment("时停范围（方块）。-1或0表示无限范围。")
                .defineInRange("range", -1, -1, 1000);

        仅当前维度 = builder
                .translation("config.puellamagi.timestop.range.currentDimensionOnly")
                .comment("是否只影响当前维度。")
                .define("currentDimensionOnly", true);

        builder.pop();

        // 投射物设置
        builder.push("projectile");

        惯性衰减系数 = builder
                .translation("config.puellamagi.timestop.projectile.inertiaDecay")
                .comment("投射物惯性衰减系数。每tick速度乘以此值，0.87表示每tick减速13%。")
                .defineInRange("inertiaDecay", 0.87, 0.1, 1.0);

        静止阈值 = builder
                .translation("config.puellamagi.timestop.projectile.stopThreshold")
                .comment("投射物静止阈值。速度低于此值时完全静止。")
                .defineInRange("stopThreshold", 0.01, 0.001, 0.1);

        builder.pop();

        // 技能设置
        builder.push("skill");

        蓄力时间 = builder
                .translation("config.puellamagi.timestop.skill.chargeTime")
                .comment("蓄力时间（tick）。20tick = 1秒。")
                .defineInRange("chargeTime", 50, 0, 200);

        受击打断蓄力 = builder
                .translation("config.puellamagi.timestop.skill.interruptOnDamage")
                .comment("蓄力期间受击是否打断。")
                .define("interruptOnDamage", false);

        语音冷却 = builder
                .translation("config.puellamagi.timestop.skill.voiceCooldown")
                .comment("语音播放保护冷却（毫秒）。防止快速按键导致语音鬼畜。")
                .defineInRange("voiceCooldown", 500, 0, 5000);

        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    // ==================== 便捷获取方法 ====================

    public static int 获取时停范围() {
        return 时停范围.get();
    }

    public static boolean 是无限范围() {
        int range = 时停范围.get();
        return range <= 0;
    }

    public static boolean 是仅当前维度() {
        return 仅当前维度.get();
    }

    public static double 获取惯性衰减系数() {
        return 惯性衰减系数.get();
    }

    public static double 获取静止阈值() {
        return 静止阈值.get();
    }

    public static int 获取蓄力时间() {
        return 蓄力时间.get();
    }

    public static boolean 是受击打断蓄力() {
        return 受击打断蓄力.get();
    }

    public static int 获取语音冷却() {
        return 语音冷却.get();
    }
}
