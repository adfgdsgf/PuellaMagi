// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopLivingEntityRendererMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * LivingEntityRenderer Mixin
 *
 * 核心：将被冻结实体的渲染 partialTick 固定为 0
 * 这样所有动画插值计算都会使用固定值，彻底消除抖动
 */
@Mixin(LivingEntityRenderer.class)
public abstract class TimestopLivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    /**
     * 修改 render 方法的 partialTick 参数
     *
     * 方法签名: render(LivingEntity, float entityYaw, float partialTick, PoseStack, MultiBufferSource, int light)
     * partialTick 是第二个 float 参数 (ordinal = 1)
     */
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private float puellamagi$fixPartialTick(float partialTick, T entity, float entityYaw, float partialTickParam, PoseStack poseStack, MultiBufferSource buffer, int light) {
        TimeStop timeStop = (TimeStop) entity.level();

        if (timeStop.puellamagi$shouldFreezeEntity(entity)) {
            // 被冻结的实体使用固定的 partialTick = 0
            return 0.0f;
        }

        return partialTick;
    }
}
