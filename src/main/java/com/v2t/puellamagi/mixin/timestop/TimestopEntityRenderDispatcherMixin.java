// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopEntityRenderDispatcherMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.v2t.puellamagi.api.access.IEntityAndData;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体渲染分发器 Mixin
 *
 * 处理被冻结实体的阴影渲染，使用存储的旧位置
 * 对标 Roundabout 的 ZEntityRenderDispatcher
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class TimestopEntityRenderDispatcherMixin {

    @Shadow
    @Final
    private static RenderType SHADOW_RENDER_TYPE;

    @Shadow
    private static void renderBlockShadow(
            PoseStack.Pose pose, VertexConsumer consumer, ChunkAccess chunk,
            LevelReader level, BlockPos pos, double x, double y, double z,
            float radius, float opacity) {
    }

    /**
     * 阴影渲染 - 被冻结实体使用存储的位置
     * 对标 roundabout$RenderShadow
     */
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void puellamagi$onRenderShadow(
            PoseStack poseStack, MultiBufferSource buffer, Entity entity,
            float opacity, float partialTick, LevelReader level, float radius,
            CallbackInfo ci) {

        TimeStop timeStop = (TimeStop) entity.level();

        // 只处理被冻结的生物实体
        if (timeStop.puellamagi$shouldFreezeEntity(entity) && entity instanceof LivingEntity) {
            IEntityAndData data = (IEntityAndData) entity;

            // 使用固定的 partialTick（当前帧时间，但用存储的位置）
            float fixedPartialTick = Minecraft.getInstance().getFrameTime();

            float adjustedRadius = radius;
            if (entity instanceof Mob mob && mob.isBaby()) {
                adjustedRadius = radius * 0.5F;
            }

            // 使用存储的旧位置进行插值，而非实体当前位置
            double renderX = Mth.lerp(fixedPartialTick, data.puellamagi$getPrevX(), entity.getX());
            double renderY = Mth.lerp(fixedPartialTick, data.puellamagi$getPrevY(), entity.getY());
            double renderZ = Mth.lerp(fixedPartialTick, data.puellamagi$getPrevZ(), entity.getZ());

            float shadowOpacity = Math.min(opacity / 0.5F, adjustedRadius);
            int minX = Mth.floor(renderX - adjustedRadius);
            int maxX = Mth.floor(renderX + adjustedRadius);
            int minY = Mth.floor(renderY - shadowOpacity);
            int maxY = Mth.floor(renderY);
            int minZ = Mth.floor(renderZ - adjustedRadius);
            int maxZ = Mth.floor(renderZ + adjustedRadius);

            PoseStack.Pose pose = poseStack.last();
            VertexConsumer consumer = buffer.getBuffer(SHADOW_RENDER_TYPE);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    mutablePos.set(x, 0, z);
                    ChunkAccess chunk = level.getChunk(mutablePos);

                    for (int y = minY; y <= maxY; y++) {
                        mutablePos.setY(y);
                        float shadowStrength = opacity - (float) (renderY - entity.getY()) * 0.5F;
                        renderBlockShadow(pose, consumer, chunk, level, mutablePos,
                                renderX, renderY, renderZ, adjustedRadius, shadowStrength);
                    }
                }
            }

            ci.cancel();
        }
    }
}
