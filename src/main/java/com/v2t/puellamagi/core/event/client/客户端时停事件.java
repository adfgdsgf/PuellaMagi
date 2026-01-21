// 文件路径: src/main/java/com/v2t/puellamagi/core/event/客户端时停事件.java

package com.v2t.puellamagi.core.event.client;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.client.timestop.时停冻结帧;
import com.v2t.puellamagi.client.timestop.时停灰度效果;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端时停相关事件处理
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, value = Dist.CLIENT)
public class 客户端时停事件 {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && event.getEntity().getId() == mc.player.getId()) {
                时停冻结帧.reset();
                时停灰度效果.onResourceReload();
            }
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide) {
            时停冻结帧.reset();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        时停冻结帧.cleanup();
        时停灰度效果.onResourceReload();
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        时停冻结帧.reset();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide) {
            时停冻结帧.reset();
        }
    }
}
