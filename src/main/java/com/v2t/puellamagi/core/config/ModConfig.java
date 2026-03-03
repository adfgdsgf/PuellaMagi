// 文件路径: src/main/java/com/v2t/puellamagi/core/config/ModConfig.java

package com.v2t.puellamagi.core.config;

import com.v2t.puellamagi.PuellaMagi;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**
 * Mod配置统一注册器
 *
 * 配置类型说明：
 * - COMMON: 全局配置，存储在 config/ 目录，所有存档共用
 * - SERVER: 存档配置，存储在 saves/存档/serverconfig/，每个存档独立
 * - CLIENT: 客户端配置，存储在 config/，仅客户端
 */
public class ModConfig {

    public static void register() {
        ModLoadingContext context = ModLoadingContext.get();

        // 使用 COMMON 类型，配置文件在 config/puellamagi/ 目录
        context.registerConfig(Type.COMMON, 时停配置.SPEC, "puellamagi/时停配置.toml");
        context.registerConfig(Type.COMMON, 契约配置.SPEC, "puellamagi/契约配置.toml");
        context.registerConfig(Type.COMMON, 灵魂宝石配置.SPEC, "puellamagi/灵魂宝石配置.toml");
        context.registerConfig(Type.COMMON, 假死环境适应配置.SPEC, "puellamagi/假死环境适应配置.toml");
        context.registerConfig(Type.COMMON,搜身配置.SPEC, "puellamagi/搜身配置.toml");

        PuellaMagi.LOGGER.info("配置文件注册完成");
    }
}
