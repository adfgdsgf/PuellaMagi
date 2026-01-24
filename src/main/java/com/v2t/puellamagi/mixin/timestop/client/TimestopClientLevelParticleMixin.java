// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopClientLevelParticleMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端世界粒子生成拦截
 *
 * 在时停范围内阻止新粒子生成（环境粒子如火焰、岩浆等）
 */
@Mixin(ClientLevel.class)
public class TimestopClientLevelParticleMixin {

    /**
     * 拦截 addParticle - 阻止时停范围内的环境粒子生成
     */
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAddParticle(ParticleOptions options, double x, double y, double z,double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this;

        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return;
        }

        Vec3i pos = new Vec3i((int) x, (int) y, (int) z);
        if (timeStop.puellamagi$inTimeStopRange(pos)) {
            // 在时停范围内，阻止新粒子生成
            ci.cancel();
        }
    }

    /**
     * 拦截 addParticle 重载版本
     */
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAddParticleWithForce(ParticleOptions options, boolean force,double x, double y, double z,
                                                   double xSpeed, double ySpeed, double zSpeed,
                                                   CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this;

        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return;
        }

        Vec3i pos = new Vec3i((int) x, (int) y, (int) z);
        if (timeStop.puellamagi$inTimeStopRange(pos)) {
            ci.cancel();
        }
    }

    /**
     * 拦截 addAlwaysVisibleParticle
     */
    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAddAlwaysVisibleParticle(ParticleOptions options,double x, double y, double z,
                                                       double xSpeed, double ySpeed, double zSpeed,
                                                       CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this;

        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return;
        }

        Vec3i pos = new Vec3i((int) x, (int) y, (int) z);
        if (timeStop.puellamagi$inTimeStopRange(pos)) {
            ci.cancel();
        }
    }

    /**
     * 拦截 addAlwaysVisibleParticle 重载版本
     */
    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAddAlwaysVisibleParticleWithForce(ParticleOptions options, boolean force,
                                                                double x, double y, double z,
                                                                double xSpeed, double ySpeed, double zSpeed,
                                                                CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this;

        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return;
        }

        Vec3i pos = new Vec3i((int) x, (int) y, (int) z);
        if (timeStop.puellamagi$inTimeStopRange(pos)) {
            ci.cancel();
        }
    }
}
