// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopProjectileRendererMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 箭渲染器 Mixin
 *
 * 将被冻结箭的 partialTick 设为 0，防止尾巴抖动
 */
@Mixin(ArrowRenderer.class)
public abstract class TimestopProjectileRendererMixin<T extends AbstractArrow> extends EntityRenderer<T> {

    protected TimestopProjectileRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 修改 render 方法的 partialTick 参数
     */
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/projectile/AbstractArrow;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private float puellamagi$fixPartialTick(float partialTick, T entity, float entityYaw, float partialTickParam, PoseStack poseStack, MultiBufferSource buffer, int light) {
        TimeStop timeStop = (TimeStop) entity.level();

        // 被冻结或在时停范围内的箭使用固定 partialTick
        if (timeStop.puellamagi$inTimeStopRange(entity)) {
            return 0.0f;
        }

        return partialTick;
    }
}
