// 文件路径: src/main/java/com/v2t/puellamagi/core/config/灵魂宝石配置.java

package com.v2t.puellamagi.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 灵魂宝石系统配置
 *
 * 包含：距离效果、假死、空血、自动回血、悲叹之种、死亡相关
 *翻译键格式: config.puellamagi.soulgem.xxx
 */
public class 灵魂宝石配置 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/Config");

    public static final ForgeConfigSpec SPEC;

    //==================== 距离设置 ====================

    public static final ForgeConfigSpec.ConfigValue<String> 距离阈值;
    public static final ForgeConfigSpec.ConfigValue<String> 移速倍率;
    public static final ForgeConfigSpec.DoubleValue 远距离伤害倍率;

    // ==================== 假死设置 ====================

    public static final ForgeConfigSpec.IntValue 假死超时分钟;
    public static final ForgeConfigSpec.IntValue 位置未知重生成延迟分钟;

    // ==================== 空血设置 ====================

    public static final ForgeConfigSpec.DoubleValue 退出假死血量阈值;
    public static final ForgeConfigSpec.IntValue 空血污浊度增加;

    // ==================== 自动回血设置 ====================

    public static final ForgeConfigSpec.IntValue 回血间隔Tick;
    public static final ForgeConfigSpec.DoubleValue 回血量;
    public static final ForgeConfigSpec.DoubleValue 空血回血倍率;

    // ==================== 悲叹之种设置 ====================

    public static final ForgeConfigSpec.IntValue 悲叹之种污浊度减少;
    public static final ForgeConfigSpec.BooleanValue 悲叹之种修复龟裂;

    // ==================== 死亡设置 ====================

    public static final ForgeConfigSpec.BooleanValue 死亡后允许重签;
    public static final ForgeConfigSpec.BooleanValue 生成遗物;

    // ==================== 缓存（避免每次解析字符串）====================

    private static int[] 距离阈值缓存 = null;
    private static double[] 移速倍率缓存 = null;

    // ==================== 构建配置 ====================

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("soulgem");

        // ========== 距离设置 ==========
        builder.push("distance");

        距离阈值 = builder
                .translation("config.puellamagi.soulgem.distance.thresholds")
                .comment("距离阈值（方块），格式：正常,中距离,远距离,超出范围",
                        "例如 '5,20,50' 表示：",
                        "  0-5格：正常",
                        "  5-20格：中距离（轻微debuff）",
                        "  20-50格：远距离（严重debuff）",
                        "  50格以上：超出范围（假死）")
                .define("thresholds", "5,20,50");

        移速倍率 = builder
                .translation("config.puellamagi.soulgem.distance.speedMultipliers")
                .comment("各阶段移速倍率，格式：正常,中距离,远距离",
                        "例如 '1.0,0.8,0.5' 表示：",
                        "  正常：100%移速",
                        "  中距离：80%移速",
                        "  远距离：50%移速")
                .define("speedMultipliers", "1.0,0.8,0.5");

        远距离伤害倍率 = builder
                .translation("config.puellamagi.soulgem.distance.farDamageMultiplier")
                .comment("远距离伤害倍率。0.5 = 攻击力减半。")
                .defineInRange("farDamageMultiplier", 0.5, 0.1, 1.0);

        builder.pop();

        // ========== 假死设置 ==========
        builder.push("feigndeath");

        假死超时分钟 = builder
                .translation("config.puellamagi.soulgem.feigndeath.timeout")
                .comment("假死超时时间（分钟）。假死超过此时间判定死亡。")
                .defineInRange("timeout", 30, 1, 1440);

        位置未知重生成延迟分钟 = builder
                .translation("config.puellamagi.soulgem.feigndeath.regenerateDelay")
                .comment("灵魂宝石位置未知时的重生成延迟（分钟）。",
                        "持有者离线且无法追踪位置时，等待此时间后在玩家身边重新生成。")
                .defineInRange("regenerateDelay", 5, 1, 60);

        builder.pop();

        // ========== 空血设置 ==========
        builder.push("emptyhealth");

        退出假死血量阈值 = builder
                .translation("config.puellamagi.soulgem.emptyhealth.recoveryThreshold")
                .comment("退出空血假死的血量阈值。血量恢复至此以上时退出假死状态。")
                .defineInRange("recoveryThreshold", 5.0, 1.0, 20.0);

        空血污浊度增加 = builder
                .translation("config.puellamagi.soulgem.emptyhealth.corruptionIncrease")
                .comment("进入空血假死时增加的污浊度。")
                .defineInRange("corruptionIncrease", 2, 0, 100);

        builder.pop();

        // ========== 自动回血设置 ==========
        builder.push("autoheal");

        回血间隔Tick = builder
                .translation("config.puellamagi.soulgem.autoheal.interval")
                .comment("自动回血间隔（tick）。20tick = 1秒。")
                .defineInRange("interval", 40, 1, 200);

        回血量 = builder
                .translation("config.puellamagi.soulgem.autoheal.amount")
                .comment("每次回血量（半心 = 1.0）。")
                .defineInRange("amount", 1.0, 0.5, 10.0);

        空血回血倍率 = builder
                .translation("config.puellamagi.soulgem.autoheal.emptyHealthMultiplier")
                .comment("空血假死状态下的回血倍率（惩罚机制）。",
                        "0.1 = 10%速度（原速度的1/10）",
                        "0.33 = 33%速度（原速度的1/3）",
                        "值越小恢复越慢。")
                .defineInRange("emptyHealthMultiplier", 0.1, 0.01, 1.0);

        builder.pop();

        // ========== 悲叹之种设置 ==========
        builder.push("griefseed");

        悲叹之种污浊度减少 = builder
                .translation("config.puellamagi.soulgem.griefseed.corruptionReduction")
                .comment("使用悲叹之种减少的污浊度。")
                .defineInRange("corruptionReduction", 50, 1, 100);

        悲叹之种修复龟裂 = builder
                .translation("config.puellamagi.soulgem.griefseed.repairCracked")
                .comment("悲叹之种是否能修复龟裂状态的灵魂宝石。")
                .define("repairCracked", true);

        builder.pop();

        // ========== 死亡设置 ==========
        builder.push("death");

        死亡后允许重签 = builder
                .translation("config.puellamagi.soulgem.death.allowRecontract")
                .comment("灵魂宝石销毁死亡后是否允许重新签订契约。")
                .define("allowRecontract", true);

        生成遗物 = builder
                .translation("config.puellamagi.soulgem.death.generateRelic")
                .comment("灵魂宝石销毁后是否生成遗物物品。")
                .define("generateRelic", true);

        builder.pop();

        builder.pop();SPEC = builder.build();
    }

    // ==================== 距离阈值解析 ====================

    /**
     * 获取距离阈值数组
     * 索引：0=正常上限, 1=中距离上限, 2=远距离上限(=超出范围下限)
     */
    public static int[] 获取距离阈值数组() {
        if (距离阈值缓存 == null) {
            距离阈值缓存 = 解析整数数组(距离阈值.get(), new int[]{5, 20, 50});
        }
        return 距离阈值缓存;
    }

    /**
     * 获取移速倍率数组
     * 索引：0=正常, 1=中距离, 2=远距离
     */
    public static double[] 获取移速倍率数组() {
        if (移速倍率缓存 == null) {
            移速倍率缓存 = 解析浮点数组(移速倍率.get(), new double[]{1.0, 0.8, 0.5});
        }
        return 移速倍率缓存;
    }

    /**
     *刷新缓存（配置重载时调用）
     */
    public static void 刷新缓存() {
        距离阈值缓存 = null;
        移速倍率缓存 = null;LOGGER.debug("灵魂宝石配置缓存已刷新");
    }

    private static int[] 解析整数数组(String input, int[] defaultValues) {
        try {
            String[] parts = input.split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            LOGGER.warn("解析距离阈值失败: {}，使用默认值", input);
            return defaultValues;
        }
    }

    private static double[] 解析浮点数组(String input, double[] defaultValues) {
        try {
            String[] parts = input.split(",");
            double[] result = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Double.parseDouble(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            LOGGER.warn("解析移速倍率失败: {}，使用默认值", input);
            return defaultValues;
        }
    }

    // ==================== 便捷获取方法 - 距离 ====================

    public static int 获取正常范围() {
        int[] thresholds = 获取距离阈值数组();
        return thresholds.length > 0 ? thresholds[0] : 5;
    }

    public static int 获取中距离范围() {
        int[] thresholds = 获取距离阈值数组();
        return thresholds.length > 1 ? thresholds[1] : 20;
    }

    public static int 获取远距离范围() {
        int[] thresholds = 获取距离阈值数组();
        return thresholds.length > 2 ? thresholds[2] : 50;
    }

    public static double 获取正常移速倍率() {
        double[] multipliers = 获取移速倍率数组();
        return multipliers.length > 0 ? multipliers[0] : 1.0;
    }

    public static double 获取中距离移速倍率() {
        double[] multipliers = 获取移速倍率数组();
        return multipliers.length > 1 ? multipliers[1] : 0.8;
    }

    public static double 获取远距离移速倍率() {
        double[] multipliers = 获取移速倍率数组();
        return multipliers.length > 2 ? multipliers[2] : 0.5;
    }

    public static double 获取远距离伤害倍率() {
        return 远距离伤害倍率.get();
    }

    // ==================== 便捷获取方法 - 假死 ====================

    public static int 获取假死超时分钟() {
        return 假死超时分钟.get();
    }

    public static long 获取假死超时Tick() {
        return 假死超时分钟.get() * 60L * 20L;
    }

    public static int 获取位置未知重生成延迟分钟() {
        return 位置未知重生成延迟分钟.get();
    }

    public static long 获取位置未知重生成延迟Tick() {
        return 位置未知重生成延迟分钟.get() * 60L * 20L;
    }

    // ==================== 便捷获取方法 - 空血 ====================

    public static double 获取退出假死血量阈值() {
        return 退出假死血量阈值.get();
    }

    public static int 获取空血污浊度增加() {
        return 空血污浊度增加.get();
    }

    // ==================== 便捷获取方法 - 自动回血 ====================

    public static int 获取回血间隔Tick() {
        return 回血间隔Tick.get();
    }

    public static double 获取回血量() {
        return 回血量.get();
    }

    public static double 获取空血回血倍率() {
        return 空血回血倍率.get();
    }

    // ==================== 便捷获取方法 - 悲叹之种 ====================

    public static int 获取悲叹之种污浊度减少() {
        return 悲叹之种污浊度减少.get();
    }

    public static boolean 悲叹之种能修复龟裂() {
        return 悲叹之种修复龟裂.get();
    }

    // ==================== 便捷获取方法 - 死亡 ====================

    public static boolean 死亡后允许重签() {
        return 死亡后允许重签.get();
    }

    public static boolean 是否生成遗物() {
        return 生成遗物.get();
    }
}
