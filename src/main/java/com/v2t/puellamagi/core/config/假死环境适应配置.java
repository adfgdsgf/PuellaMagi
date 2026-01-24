// 文件路径: src/main/java/com/v2t/puellamagi/core/config/假死环境适应配置.java

package com.v2t.puellamagi.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 假死状态环境伤害适应配置
 *
 * 功能说明：
 * 灵魂宝石系玩家在假死状态下，若持续受到相同类型的环境伤害（如岩浆、溺水），
 * 会对该伤害类型产生临时免疫，防止被环境反复杀死。
 *
 * 注意：这是专门针对假死状态的保护机制，与未来可能的"适应能力"是不同的系统。
 *
 * 翻译键格式: config.puellamagi.feignDeathAdaptation.xxx
 */
public class 假死环境适应配置 {

    public static final ForgeConfigSpec SPEC;

    // ==================== 配置项 ====================

    public static final ForgeConfigSpec.BooleanValue 启用;
    public static final ForgeConfigSpec.IntValue 基础免疫时长;
    public static final ForgeConfigSpec.IntValue 连续判定窗口;
    public static final ForgeConfigSpec.IntValue 最大免疫时长;
    public static final ForgeConfigSpec.DoubleValue 连续免疫倍率;

    // ==================== 构建配置 ====================

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("feignDeathAdaptation");

        启用 = builder
                .translation("config.puellamagi.feignDeathAdaptation.enabled")
                .comment("是否启用假死状态环境伤害适应。",
                        "启用后，假死时持续受到相同类型环境伤害（岩浆、溺水等），",
                        "会对该伤害类型产生临时免疫，防止被反复杀死。")
                .define("enabled", true);

        基础免疫时长 = builder
                .translation("config.puellamagi.feignDeathAdaptation.baseImmunityDuration")
                .comment("触发适应后的基础免疫时长（tick）。20tick = 1秒。")
                .defineInRange("baseImmunityDuration", 40, 10, 200);

        连续判定窗口 = builder
                .translation("config.puellamagi.feignDeathAdaptation.consecutiveWindow")
                .comment("连续伤害判定窗口（tick）。",
                        "在此时间内再次受到相同类型伤害，视为连续伤害，免疫时间会延长。")
                .defineInRange("consecutiveWindow", 600, 100, 2400);

        最大免疫时长 = builder
                .translation("config.puellamagi.feignDeathAdaptation.maxImmunityDuration")
                .comment("免疫时长上限（tick）。连续触发时不会超过此值。")
                .defineInRange("maxImmunityDuration", 200, 40, 1200);

        连续免疫倍率 = builder
                .translation("config.puellamagi.feignDeathAdaptation.consecutiveMultiplier")
                .comment("连续触发时的时长倍率。",
                        "每次连续触发，免疫时长= 上次时长 × 此倍率（不超过上限）。")
                .defineInRange("consecutiveMultiplier", 1.5, 1.0, 3.0);

        builder.pop();SPEC = builder.build();
    }

    // ==================== 便捷获取方法 ====================

    public static boolean 是否启用() {
        return 启用.get();
    }

    public static int 获取基础免疫时长() {
        return 基础免疫时长.get();
    }

    public static int 获取连续判定窗口() {
        return 连续判定窗口.get();
    }

    public static int 获取最大免疫时长() {
        return 最大免疫时长.get();
    }

    public static double 获取连续免疫倍率() {
        return 连续免疫倍率.get();
    }
}
