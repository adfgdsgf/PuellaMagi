// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopItemEntityRendererMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 掉落物渲染器 Mixin
 *
 * 对标TimestopProjectileRendererMixin
 */
@Mixin(ItemEntityRenderer.class)
public abstract class TimestopItemEntityRendererMixin extends EntityRenderer<ItemEntity> {

    protected TimestopItemEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            ordinal = 1,  // ← 改成1，partialTick是第二个float参数
            argsOnly = true
    )
    private float puellamagi$fixPartialTick(float partialTick, ItemEntity entity, float entityYaw, float partialTickParam, PoseStack poseStack, MultiBufferSource buffer, int light) {
        TimeStop timeStop = (TimeStop) entity.level();

        if (timeStop.puellamagi$inTimeStopRange(entity)) {
            return 0.0f;
        }

        return partialTick;
    }
}
