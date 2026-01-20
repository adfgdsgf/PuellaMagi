package com.v2t.puellamagi;

import com.mojang.logging.LogUtils;
import com.v2t.puellamagi.core.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Puella Magi 模组主类
 * 负责模组初始化和各系统注册
 */
@Mod(常量.MOD_ID)
public class PuellaMagi {
    public static final Logger LOGGER = LogUtils.getLogger();

    public PuellaMagi() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册通用初始化事件
        modEventBus.addListener(this::commonSetup);

        // TODO: 后续在此添加DeferredRegister注册
        // ModItems.ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);LOGGER.info("Puella Magi 初始化完成");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 网络包注册
            ModNetwork.register();
            LOGGER.info("Puella Magi 网络注册完成");
        });
    }
}
