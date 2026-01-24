//文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthRenderMixin.java

package com.v2t.puellamagi.mixin.soulgem.client;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复空血假死玩家的红色受伤效果
 */
@Mixin(LivingEntityRenderer.class)
public abstract class EmptyHealthRenderMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "getWhiteOverlayProgress", at = @At("HEAD"), cancellable = true)
    private void onGetWhiteOverlayProgress(T entity, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof Player player) {
            if (假死状态处理器.客户端应该显示假死效果(player)) {
                cir.setReturnValue(0.0F);
            }
        }
    }
}
