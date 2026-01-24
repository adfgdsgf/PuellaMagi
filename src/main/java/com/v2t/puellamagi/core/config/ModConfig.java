// 文件路径: src/main/java/com/v2t/puellamagi/core/config/ModConfig.java

package com.v2t.puellamagi.core.config;

import com.v2t.puellamagi.PuellaMagi;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/**
 * Mod配置统一注册器
 *
 * 所有配置文件统一在config/puellamagi/ 文件夹下
 * 便于玩家管理和游戏内配置编辑器（如Configured）显示
 */
public class ModConfig {

    /**
     * 注册所有配置
     * 在mod构造函数中调用
     */
    public static void register() {
        ModLoadingContext context = ModLoadingContext.get();

        // 服务端配置（存储在世界存档中，多人游戏由服务器控制）
        context.registerConfig(Type.SERVER, 时停配置.SPEC, "puellamagi/timestop.toml");
        context.registerConfig(Type.SERVER, 契约配置.SPEC, "puellamagi/contract.toml");
        context.registerConfig(Type.SERVER, 灵魂宝石配置.SPEC, "puellamagi/soulgem.toml");
        context.registerConfig(Type.SERVER, 假死环境适应配置.SPEC, "puellamagi/feign_death_adaptation.toml");
        context.registerConfig(Type.SERVER, 搜身配置.SPEC, "puellamagi/search.toml");

        PuellaMagi.LOGGER.info("配置文件注册完成");
    }
}
