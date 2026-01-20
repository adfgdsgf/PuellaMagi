// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopGameRendererMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v2t.puellamagi.api.access.IEntityAndData;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameRenderer Mixin
 *
 * 处理时停中的画面效果：
 * - 阻止受伤晃动
 * - 阻止行走晃动
 * - 固定手持物品渲染
 *
 * 参考 Roundabout 的 TimeStopGameRenderer
 */
@Mixin(GameRenderer.class)
public abstract class TimestopGameRendererMixin {

    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    private void bobHurt(PoseStack poseStack, float partialTick) {}

    @Shadow
    private void bobView(PoseStack poseStack, float partialTick) {}

    @Shadow
    public void renderItemInHand(PoseStack poseStack, Camera camera, float partialTick) {}

    /**
     * 防止递归调用
     */
    @Unique
    private boolean puellamagi$cleared = false;

    /**
     * 时停中不应该有受伤晃动
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onBobHurt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (puellamagi$cleared) return;

        if (minecraft.player != null) {
            TimeStop timeStop = (TimeStop) minecraft.player.level();

            if (timeStop.puellamagi$shouldFreezeEntity(minecraft.player)) {
                if (minecraft.getCameraEntity() instanceof LivingEntity living) {
                    // 使用时停前的 partialTick
                    float frozenTick = ((IEntityAndData) living).puellamagi$getPreTSTick();

                    puellamagi$cleared = true;
                    this.bobHurt(poseStack, frozenTick);
                    puellamagi$cleared = false;
                }
                ci.cancel();
            }
        }
    }

    /**
     * 时停中不应该有行走晃动
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onBobView(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (puellamagi$cleared) return;

        if (minecraft.player != null) {
            TimeStop timeStop = (TimeStop) minecraft.player.level();

            if (timeStop.puellamagi$shouldFreezeEntity(minecraft.player)) {
                if (minecraft.getCameraEntity() instanceof Player player) {
                    float frozenTick = ((IEntityAndData) player).puellamagi$getPreTSTick();

                    puellamagi$cleared = true;
                    this.bobView(poseStack, frozenTick);
                    puellamagi$cleared = false;
                }
                ci.cancel();
            }
        }
    }

    /**
     * 时停中手持物品渲染使用固定 partialTick
     */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onRenderItemInHand(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) {
        if (puellamagi$cleared) return;

        if (minecraft.player != null) {
            TimeStop timeStop = (TimeStop) minecraft.player.level();

            if (timeStop.puellamagi$shouldFreezeEntity(minecraft.player)) {
                if (minecraft.getCameraEntity() != null) {
                    Entity entity = minecraft.getCameraEntity();
                    float frozenTick = ((IEntityAndData) entity).puellamagi$getPreTSTick();

                    puellamagi$cleared = true;
                    this.renderItemInHand(poseStack, camera, frozenTick);
                    puellamagi$cleared = false;
                }
                ci.cancel();
            }
        }
    }
}
