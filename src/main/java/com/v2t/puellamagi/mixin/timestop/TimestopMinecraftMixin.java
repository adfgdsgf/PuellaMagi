// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopMinecraftMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
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
 * 核心：在渲染期间将 partialTick 设为0（针对被冻结的玩家）
 * 对标 Roundabout 的 InputEvents
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
     * 渲染开始前- 如果玩家被时停，将 partialTick 设为 0
     *注入点：FogRenderer.setupNoFog() 调用前（渲染开始）
     */
    @Inject(method = "runTick",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupNoFog()V"))
    private void puellamagi$onRenderStart(boolean renderLevel, CallbackInfo ci) {
        if (puellamagi$shouldFreezeScreen()) {
            // 保存原值
            puellamagi$savedPartialTick = this.timer.partialTick;
            puellamagi$savedTickDelta = this.timer.tickDelta;
            puellamagi$savedPausePartialTick = this.pausePartialTick;

            // 渲染期间设为 0
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
     * getFrameTime - 被冻结时返回 0
     */
    @Inject(method = "getFrameTime", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onGetFrameTime(CallbackInfoReturnable<Float> cir) {
        if (puellamagi$shouldFreezeScreen()) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * getDeltaFrameTime - 被冻结时返回 0
     */
    @Inject(method = "getDeltaFrameTime", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onGetDeltaFrameTime(CallbackInfoReturnable<Float> cir) {
        if (puellamagi$shouldFreezeScreen()) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * 判断是否应该冻结屏幕（玩家自己被时停）
     */
    @Unique
    private boolean puellamagi$shouldFreezeScreen() {
        if (player == null || level == null) {
            return false;
        }

        TimeStop timeStop = (TimeStop) level;
        //玩家被冻结（不是时停者）
        return timeStop.puellamagi$shouldFreezeEntity(player);
    }
}
