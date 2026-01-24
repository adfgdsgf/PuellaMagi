// 文件路径: src/main/java/com/v2t/puellamagi/core/config/搜身配置.java

package com.v2t.puellamagi.core.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 搜身系统配置
 *
 * 翻译键格式: config.puellamagi.search.xxx
 */
public class 搜身配置 {

    public static final ForgeConfigSpec SPEC;

    // ==================== 搜身设置 ====================

    public static final ForgeConfigSpec.BooleanValue 启用搜身系统;
    public static final ForgeConfigSpec.BooleanValue 假死时可被搜身;
    public static final ForgeConfigSpec.BooleanValue 时停时可被搜身;
    public static final ForgeConfigSpec.BooleanValue 通知被搜身者;
    public static final ForgeConfigSpec.BooleanValue 允许拿取物品;
    public static final ForgeConfigSpec.BooleanValue 允许放入物品;

    // ==================== 构建配置 ====================

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("search");

        启用搜身系统 = builder
                .translation("config.puellamagi.search.enabled")
                .comment("是否启用搜身系统。",
                        "启用后，可以对处于特定状态的玩家进行搜身。")
                .define("enabled", true);

        假死时可被搜身 = builder
                .translation("config.puellamagi.search.feignDeathSearchable")
                .comment("假死状态的玩家是否可被搜身。")
                .define("feignDeathSearchable", true);

        时停时可被搜身 = builder
                .translation("config.puellamagi.search.timeStopSearchable")
                .comment("被时停冻结的玩家是否可被搜身。",
                        "只有时停者可以搜身被冻结的玩家。")
                .define("timeStopSearchable", true);

        通知被搜身者 = builder
                .translation("config.puellamagi.search.notifyTarget")
                .comment("是否通知被搜身的玩家。",
                        "启用后，被搜身时会收到消息提示。")
                .define("notifyTarget", true);

        允许拿取物品 = builder
                .translation("config.puellamagi.search.allowTake")
                .comment("是否允许从目标背包拿取物品。")
                .define("allowTake", true);

        允许放入物品 = builder
                .translation("config.puellamagi.search.allowPut")
                .comment("是否允许向目标背包放入物品。")
                .define("allowPut", true);

        builder.pop();

        SPEC = builder.build();
    }

    // ==================== 便捷获取方法 ====================

    public static boolean 是否启用() {
        return 启用搜身系统.get();
    }

    public static boolean 假死时可被搜身() {
        return 假死时可被搜身.get();
    }

    public static boolean 时停时可被搜身() {
        return 时停时可被搜身.get();
    }

    public static boolean 是否通知被搜身者() {
        return 通知被搜身者.get();
    }

    public static boolean 允许拿取物品() {
        return 允许拿取物品.get();
    }

    public static boolean 允许放入物品() {
        return 允许放入物品.get();
    }
}
