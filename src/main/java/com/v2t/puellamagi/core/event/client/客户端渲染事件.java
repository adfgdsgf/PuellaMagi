package com.v2t.puellamagi.core.event.client;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.client.timestop.时停灰度效果;
import com.v2t.puellamagi.util.投影工具;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

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

    /**
     * 世界渲染阶段缓存投影矩阵
     *
     * 在AFTER_SKY阶段获取——此时PoseStack最干净
     * 只缓存投影矩阵（包含正确的动态FOV）
     * view变换由投影工具实时从Camera方向向量计算
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        投影工具.更新投影矩阵(new Matrix4f(event.getProjectionMatrix()));
    }
}
