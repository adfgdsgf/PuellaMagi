// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopWeatherMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 天气渲染 Mixin - 时停中冻结雨雪效果
 *
 * 原理：
 * 1. 固定 ticks 值
 * 2. 固定 partialTick 为 0（解决抖动）
 */
@Mixin(LevelRenderer.class)
public class TimestopWeatherMixin {

    @Shadow
    private int ticks;

    @Unique
    private static int puellamagi$frozenTicks = -1;

    @Unique
    private int puellamagi$realTicks;

    /**
     * 渲染雨雪前- 替换 ticks
     */
    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private void puellamagi$beforeRenderWeather(LightTexture lightTexture, float partialTick, double x, double y, double z, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        TimeStop timeStop = (TimeStop) mc.level;

        if (timeStop.puellamagi$inTimeStopRange(mc.player)) {
            if (puellamagi$frozenTicks < 0) {
                puellamagi$frozenTicks = this.ticks;
            }
            puellamagi$realTicks = this.ticks;
            this.ticks = puellamagi$frozenTicks;
        } else {
            puellamagi$frozenTicks = -1;}
    }

    /**
     * 渲染雨雪后 - 恢复 ticks
     */
    @Inject(method = "renderSnowAndRain", at = @At("RETURN"))
    private void puellamagi$afterRenderWeather(LightTexture lightTexture, float partialTick, double x, double y, double z, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        TimeStop timeStop = (TimeStop) mc.level;

        if (timeStop.puellamagi$inTimeStopRange(mc.player) && puellamagi$frozenTicks >= 0) {
            this.ticks = puellamagi$realTicks;
        }
    }

    /**
     * 固定 partialTick 参数为 0（解决抖动）
     */
    @ModifyVariable(method = "renderSnowAndRain",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private float puellamagi$freezePartialTick(float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && mc.player != null) {
            TimeStop timeStop = (TimeStop) mc.level;
            if (timeStop.puellamagi$inTimeStopRange(mc.player) && puellamagi$frozenTicks >= 0) {
                return 0.0f;
            }
        }

        return partialTick;
    }
}
