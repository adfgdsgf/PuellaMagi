// 文件路径: src/main/java/com/v2t/puellamagi/core/config/契约配置.java

package com.v2t.puellamagi.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 契约系统配置
 *
 * 翻译键格式: config.puellamagi.contract.xxx
 * 用于支持游戏内配置编辑器（如Configured mod）
 */
public class 契约配置 {

    public static final ForgeConfigSpec SPEC;

    // ==================== 重签冷却设置 ====================

    public static final ForgeConfigSpec.BooleanValue 启用重签冷却;
    public static final ForgeConfigSpec.EnumValue<时间模式> 冷却时间模式;
    public static final ForgeConfigSpec.IntValue 游戏时间冷却天数;
    public static final ForgeConfigSpec.IntValue 现实时间冷却分钟;
    public static final ForgeConfigSpec.BooleanValue 创造模式绕过冷却;

    // ==================== 时间模式枚举 ====================

    public enum 时间模式 {
        /**
         * 游戏时间（按游戏天数计算）
         * 1游戏天 = 24000 tick≈ 20分钟现实时间
         * 优点：符合游戏沉浸感
         * 缺点：玩家可以通过睡觉跳过
         */
        GAME_TIME("game_time"),

        /**
         * 现实时间（按现实分钟计算）
         * 优点：无法跳过
         * 缺点：服务器关闭时不计时
         */
        REAL_TIME("real_time");

        private final String id;

        时间模式(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    // ==================== 构建配置 ====================

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("contract");

        // 重签冷却设置
        builder.push("cooldown");

        启用重签冷却 = builder
                .translation("config.puellamagi.contract.cooldown.enabled")
                .comment("是否启用重签契约冷却。解除契约后需要等待一段时间才能重新签约。")
                .define("enabled", true);

        冷却时间模式 = builder
                .translation("config.puellamagi.contract.cooldown.timeMode")
                .comment("冷却时间计算模式。",
                        "GAME_TIME: 游戏时间（按游戏天数，可被睡觉跳过）",
                        "REAL_TIME: 现实时间（按分钟，无法跳过）")
                .defineEnum("timeMode", 时间模式.GAME_TIME);

        游戏时间冷却天数 = builder
                .translation("config.puellamagi.contract.cooldown.gameDays")
                .comment("游戏时间模式下的冷却天数。",
                        "1游戏天 = 24000tick ≈ 20分钟现实时间。",
                        "仅在timeMode = GAME_TIME 时生效。")
                .defineInRange("gameDays", 3, 0, 30);

        现实时间冷却分钟 = builder
                .translation("config.puellamagi.contract.cooldown.realMinutes")
                .comment("现实时间模式下的冷却分钟数。",
                        "仅在 timeMode = REAL_TIME 时生效。")
                .defineInRange("realMinutes", 60, 0, 10080);  // 最多7天

        创造模式绕过冷却 = builder
                .translation("config.puellamagi.contract.cooldown.creativeBypass")
                .comment("创造模式是否绕过重签冷却。")
                .define("creativeBypass", true);

        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    // ==================== 便捷获取方法 ====================

    /**
     * 是否启用重签冷却
     */
    public static boolean 是否启用重签冷却() {
        return 启用重签冷却.get();
    }

    /**
     * 获取冷却时间模式
     */
    public static 时间模式 获取时间模式() {
        return 冷却时间模式.get();
    }

    /**
     * 是否使用游戏时间模式
     */
    public static boolean 是游戏时间模式() {
        return 冷却时间模式.get() == 时间模式.GAME_TIME;
    }

    /**
     * 获取游戏时间冷却（tick）
     */
    public static long 获取游戏时间冷却Tick() {
        return 游戏时间冷却天数.get() * 24000L;
    }

    /**
     * 获取游戏时间冷却天数
     */
    public static int 获取游戏时间冷却天数() {
        return 游戏时间冷却天数.get();
    }

    /**
     * 获取现实时间冷却（毫秒）
     */
    public static long 获取现实时间冷却毫秒() {
        return 现实时间冷却分钟.get() * 60 * 1000L;
    }

    /**
     * 获取现实时间冷却分钟数
     */
    public static int 获取现实时间冷却分钟() {
        return 现实时间冷却分钟.get();
    }

    /**
     * 创造模式是否绕过冷却
     */
    public static boolean 创造模式绕过冷却() {
        return 创造模式绕过冷却.get();
    }
}
