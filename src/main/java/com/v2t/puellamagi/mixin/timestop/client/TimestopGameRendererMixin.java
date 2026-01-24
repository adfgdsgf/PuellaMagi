// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopGameRendererMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v2t.puellamagi.api.access.IEntityAndData;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停豁免系统;
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
 * -阻止受伤晃动
 * - 阻止行走晃动
 * - 固定手持物品渲染
 *
 * 对"画面冻结"和"视觉豁免"（tick被冻结）的玩家都生效
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

    @Unique
    private boolean puellamagi$cleared = false;

    /**
     * 时停中不应该有受伤晃动
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onBobHurt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (puellamagi$cleared) return;

        if (puellamagi$shouldFreezePlayerRendering()) {
            if (minecraft.getCameraEntity() instanceof LivingEntity living) {
                float frozenTick = ((IEntityAndData) living).puellamagi$getPreTSTick();

                puellamagi$cleared = true;
                this.bobHurt(poseStack, frozenTick);
                puellamagi$cleared = false;
            }
            ci.cancel();
        }
    }

    /**
     * 时停中不应该有行走晃动
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onBobView(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (puellamagi$cleared) return;

        if (puellamagi$shouldFreezePlayerRendering()) {
            if (minecraft.getCameraEntity() instanceof Player player) {
                float frozenTick = ((IEntityAndData) player).puellamagi$getPreTSTick();

                puellamagi$cleared = true;
                this.bobView(poseStack, frozenTick);
                puellamagi$cleared = false;
            }
            ci.cancel();
        }
    }

    /**
     * 时停中手持物品渲染使用固定 partialTick
     */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onRenderItemInHand(PoseStack poseStack, Camera camera, float partialTick, CallbackInfo ci) {
        if (puellamagi$cleared) return;

        if (puellamagi$shouldFreezePlayerRendering()) {
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

    /**
     * 判断是否应该冻结玩家自身的渲染（手、视角晃动等）
     *
     * 条件：玩家的tick被冻结（包括画面冻结和视觉豁免）
     */
    @Unique
    private boolean puellamagi$shouldFreezePlayerRendering() {
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        TimeStop timeStop = (TimeStop) minecraft.player.level();
        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return false;
        }

        // 使用豁免系统：只要tick被冻结（包括视觉豁免），就冻结手的渲染
        return 时停豁免系统.应该冻结(minecraft.player);
    }
}
