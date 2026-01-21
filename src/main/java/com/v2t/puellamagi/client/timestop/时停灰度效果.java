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
 */
@OnlyIn(Dist.CLIENT)
public class 时停灰度效果 {

    private static final ResourceLocation GRAYSCALE_SHADER = 资源工具.本mod("shaders/post/grayscale.json");

    private static PostChain grayscaleEffect = null;
    private static boolean effectActive = false;

    public static void onRenderTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            disableEffect();
            return;
        }

        // 如果处于冻结状态，不需要灰度（冻结帧已经是静态画面）
        if (时停冻结帧.isFrozen() && 时停冻结帧.hasFrozenFrame()) {
            disableEffect();
            return;
        }

        TimeStop timeStop = (TimeStop) mc.level;

        boolean shouldGray = false;

        if (timeStop.puellamagi$hasActiveTimeStop()) {
            if (timeStop.puellamagi$isTimeStopper(mc.player)) {
                shouldGray = true;
            } else if (timeStop.puellamagi$inTimeStopRange(mc.player)) {
                shouldGray = true;
            }
        }

        if (shouldGray) {
            enableEffect();
        } else {
            disableEffect();
        }
    }

    public static void renderEffect(float partialTicks) {
        if (!effectActive || grayscaleEffect == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget framebuffer = mc.getMainRenderTarget();

        grayscaleEffect.resize(framebuffer.width, framebuffer.height);
        grayscaleEffect.process(partialTicks);
        framebuffer.bindWrite(false);}

    private static void enableEffect() {
        if (effectActive) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        try {
            if (grayscaleEffect == null) {
                grayscaleEffect = new PostChain(
                        mc.getTextureManager(),
                        mc.getResourceManager(),
                        mc.getMainRenderTarget(),
                        GRAYSCALE_SHADER
                );
                grayscaleEffect.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            }
            effectActive = true;
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
    }

    public static void onResourceReload() {
        if (grayscaleEffect != null) {
            grayscaleEffect.close();
            grayscaleEffect = null;
        }
        effectActive = false;
    }

    public static void onResize(int width, int height) {
        if (grayscaleEffect != null) {
            grayscaleEffect.resize(width, height);
        }
    }
}
