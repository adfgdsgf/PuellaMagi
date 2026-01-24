// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopMinecraftMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停豁免系统;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 客户端主类Mixin
 *
 * 核心：在渲染期间将 partialTick 设为0（仅针对画面冻结的玩家）
 */
@Mixin(Minecraft.class)
public abstract class TimestopMinecraftMixin {

    @Shadow
    @Final
    private Timer timer;

    @Shadow
    private float pausePartialTick;

    @Shadow
    public LocalPlayer player;

    @Shadow
    public ClientLevel level;

    @Unique
    private float puellamagi$savedPartialTick = 0;

    @Unique
    private float puellamagi$savedTickDelta = 0;

    @Unique
    private float puellamagi$savedPausePartialTick = 0;

    @Unique
    private boolean puellamagi$wasModified = false;

    /**
     * 渲染开始前- 如果玩家画面被冻结，将 partialTick 设为0
     */
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupNoFog()V"))
    private void puellamagi$onRenderStart(boolean renderLevel, CallbackInfo ci) {
        if (puellamagi$shouldFreezeScreen()) {
            puellamagi$savedPartialTick = this.timer.partialTick;
            puellamagi$savedTickDelta = this.timer.tickDelta;
            puellamagi$savedPausePartialTick = this.pausePartialTick;

            this.timer.partialTick = 0;
            this.timer.tickDelta = 0;
            this.pausePartialTick = 0;

            puellamagi$wasModified = true;
        }
    }

    /**
     * 渲染结束后 - 恢复原值
     */
    @Inject(method = "runTick", at = @At("TAIL"))
    private void puellamagi$onRenderEnd(boolean renderLevel, CallbackInfo ci) {
        if (puellamagi$wasModified) {
            this.timer.partialTick = puellamagi$savedPartialTick;
            this.timer.tickDelta = puellamagi$savedTickDelta;
            this.pausePartialTick = puellamagi$savedPausePartialTick;

            puellamagi$savedPartialTick = 0;
            puellamagi$savedTickDelta = 0;
            puellamagi$savedPausePartialTick = 0;
            puellamagi$wasModified = false;
        }
    }

    /**
     * getFrameTime - 画面冻结时返回 0
     */
    @Inject(method = "getFrameTime", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onGetFrameTime(CallbackInfoReturnable<Float> cir) {
        if (puellamagi$shouldFreezeScreen()) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * getDeltaFrameTime - 画面冻结时返回 0
     */
    @Inject(method = "getDeltaFrameTime", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onGetDeltaFrameTime(CallbackInfoReturnable<Float> cir) {
        if (puellamagi$shouldFreezeScreen()) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * 判断是否应该冻结画面
     *使用豁免系统判断，视觉豁免玩家不冻结画面
     */
    @Unique
    private boolean puellamagi$shouldFreezeScreen() {
        if (player == null || level == null) {
            return false;
        }

        TimeStop timeStop = (TimeStop) level;
        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return false;
        }

        // 使用豁免系统：只有"需要冻结画面"才返回true
        return 时停豁免系统.应该冻结画面(player);
    }
}
