// 文件路径: src/main/java/com/v2t/puellamagi/client/timestop/时停灰度效果.java

package com.v2t.puellamagi.client.timestop;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;

/**
 * 时停灰度效果管理器
 *
 * 当玩家处于时停中（作为时停者或有时停能力的被冻结者）时，
 * 应用灰度后处理效果
 */
@OnlyIn(Dist.CLIENT)
public class 时停灰度效果 {

    private static final ResourceLocation GRAYSCALE_SHADER = 资源工具.本mod("shaders/post/grayscale.json");

    private static PostChain grayscaleEffect = null;
    private static boolean effectActive = false;

    /**
     * 每帧调用，检查是否需要应用灰度效果
     */
    public static void onRenderTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            disableEffect();
            return;
        }

        TimeStop timeStop = (TimeStop) mc.level;

        // 判断是否应该显示灰度
        boolean shouldGray = false;

        if (timeStop.puellamagi$hasActiveTimeStop()) {
            // 方案1：玩家是时停者
            if (timeStop.puellamagi$isTimeStopper(mc.player)) {
                shouldGray = true;
            }// 方案2：玩家有时停能力但被冻结（后续实现能力检查）
            // else if (玩家有时停能力) { shouldGray = true; }
        }

        if (shouldGray) {
            enableEffect();
        } else {
            disableEffect();
        }
    }

    /**
     * 渲染灰度效果
     */
    public static void renderEffect(float partialTicks) {
        if (!effectActive || grayscaleEffect == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget framebuffer = mc.getMainRenderTarget();

        grayscaleEffect.resize(framebuffer.width, framebuffer.height);
        grayscaleEffect.process(partialTicks);framebuffer.bindWrite(false);}

    private static void enableEffect() {
        if (effectActive) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        try {
            if (grayscaleEffect == null) {
                grayscaleEffect = new PostChain(mc.getTextureManager(),
                        mc.getResourceManager(),
                        mc.getMainRenderTarget(),
                        GRAYSCALE_SHADER
                );
                grayscaleEffect.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            }
            effectActive = true;
            PuellaMagi.LOGGER.debug("时停灰度效果已启用");
        } catch (IOException e) {
            PuellaMagi.LOGGER.error("无法加载时停灰度shader", e);
            grayscaleEffect = null;
        }
    }

    private static void disableEffect() {
        if (!effectActive) {
            return;
        }

        effectActive = false;
        PuellaMagi.LOGGER.debug("时停灰度效果已禁用");
    }

    /**
     * 资源重载时调用
     */
    public static void onResourceReload() {
        if (grayscaleEffect != null) {
            grayscaleEffect.close();
            grayscaleEffect = null;
        }
        effectActive = false;
    }

    /**
     * 窗口大小改变时调用
     */
    public static void onResize(int width, int height) {
        if (grayscaleEffect != null) {
            grayscaleEffect.resize(width, height);
        }
    }
}
