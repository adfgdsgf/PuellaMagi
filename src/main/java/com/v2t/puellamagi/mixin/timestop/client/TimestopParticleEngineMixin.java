// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopParticleEngineMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.v2t.puellamagi.api.access.IParticleAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.mixin.access.AccessParticleMixin;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 粒子引擎 Mixin - 时停中冻结粒子
 * 对标Roundabout 的 TimeStopParticleEngine
 */
@Mixin(ParticleEngine.class)
public class TimestopParticleEngineMixin {

    @Shadow
    protected ClientLevel level;

    @Shadow
    @Final
    private static List<ParticleRenderType> RENDER_ORDER;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Shadow
    @Final
    private Map<ParticleRenderType, Queue<Particle>> particles;

    //==================== 粒子添加时标记 ====================

    @ModifyVariable(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), argsOnly = true)
    private Particle puellamagi$markParticleTS(Particle particle) {
        if (particle != null && level != null) {
            AccessParticleMixin access = (AccessParticleMixin) particle;
            Vec3i pos = new Vec3i(
                    (int) access.puellamagi$getX(),
                    (int) access.puellamagi$getY(),
                    (int) access.puellamagi$getZ()
            );

            if (((TimeStop) level).puellamagi$inTimeStopRange(pos)) {
                ((IParticleAccess) particle).puellamagi$setTimeStopCreated(true);}
        }
        return particle;
    }

    // ==================== 粒子 Tick拦截 ====================

    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickParticle(Particle particle, CallbackInfo ci) {
        if (particle instanceof ItemPickupParticle) {
            return;
        }

        AccessParticleMixin access = (AccessParticleMixin) particle;
        IParticleAccess particleAccess = (IParticleAccess) particle;

        if (access == null) {
            return;
        }

        // 时停中创建的粒子正常tick
        if (particleAccess.puellamagi$isTimeStopCreated()) {
            return;
        }

        Vec3i pos = new Vec3i(
                (int) access.puellamagi$getX(),
                (int) access.puellamagi$getY(),
                (int) access.puellamagi$getZ()
        );

        if (((TimeStop) level).puellamagi$inTimeStopRange(pos)) {
            // 同步旧位置，防止渲染抖动
            access.puellamagi$setXO(access.puellamagi$getX());
            access.puellamagi$setYO(access.puellamagi$getY());
            access.puellamagi$setZO(access.puellamagi$getZ());
            ci.cancel();
        }
    }

    // ==================== 粒子渲染 - 固定 partialTick ====================

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onRender(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {

        if (level == null) {
            return;
        }

        TimeStop timeStop = (TimeStop) level;
        if (timeStop.puellamagi$getTimeStoppers().isEmpty()) {
            return; // 没有时停，正常渲染
        }

        // 有时停时，使用自定义渲染逻辑
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        for (ParticleRenderType renderType : RENDER_ORDER) {
            Iterable<Particle> particleQueue = this.particles.get(renderType);
            if (particleQueue != null) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferBuilder = tesselator.getBuilder();
                renderType.begin(bufferBuilder, this.textureManager);

                for (Particle particle : particleQueue) {
                    try {
                        AccessParticleMixin access = (AccessParticleMixin) particle;
                        IParticleAccess particleAccess = (IParticleAccess) particle;

                        float tickToUse = partialTick;

                        // 非时停创建的粒子 + 非拾取粒子
                        if (access != null && !particleAccess.puellamagi$isTimeStopCreated()
                                && !(particle instanceof ItemPickupParticle)) {

                            Vec3i pos = new Vec3i(
                                    (int) access.puellamagi$getX(),
                                    (int) access.puellamagi$getY(),
                                    (int) access.puellamagi$getZ()
                            );

                            if (timeStop.puellamagi$inTimeStopRange(pos)) {
                                // 在时停范围内，使用存储的 partialTick
                                tickToUse = particleAccess.puellamagi$getPreTSTick();
                            } else {
                                // 不在范围内，更新存储的 partialTick
                                particleAccess.puellamagi$setPreTSTick();
                            }
                        }

                        particle.render(bufferBuilder, camera, tickToUse);

                    } catch (Throwable throwable) {
                        CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering Particle");
                        CrashReportCategory category = crashReport.addCategory("Particle being rendered");
                        category.setDetail("Particle", particle::toString);
                        category.setDetail("Particle Type", renderType::toString);
                        throw new ReportedException(crashReport);
                    }
                }

                renderType.end(tesselator);
            }
        }

        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();

        ci.cancel();
    }
}
