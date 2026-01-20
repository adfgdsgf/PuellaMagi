// 文件路径: src/main/java/com/v2t/puellamagi/core/event/客户端渲染事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.client.timestop.时停灰度效果;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端渲染事件处理
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, value = Dist.CLIENT)
public class 客户端渲染事件 {

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            时停灰度效果.onRenderTick();
        } else if (event.phase == TickEvent.Phase.END) {
            时停灰度效果.renderEffect(event.renderTickTime);
        }
    }
}
