// 文件路径: src/main/java/com/v2t/puellamagi/PuellaMagi.java

package com.v2t.puellamagi;

import com.mojang.logging.LogUtils;
import com.v2t.puellamagi.core.config.ModConfig;
import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.core.registry.ModMenuTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Puella Magi 模组主类
 *
 * 职责：模组入口，注册EventBus和Config
 * 所有初始化逻辑在模组事件.java中处理
 */
@Mod(常量.MOD_ID)
public class PuellaMagi {

    public static final Logger LOGGER = LogUtils.getLogger();

    public PuellaMagi() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置（统一入口）
        ModConfig.register();

        // 注册DeferredRegister
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);LOGGER.info("Puella Magi 模组入口初始化完成");
    }
}
