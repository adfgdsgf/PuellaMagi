// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopFrozenFrameMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v2t.puellamagi.client.timestop.时停冻结帧;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameRenderer Mixin -冻结帧渲染
 *
 * 流程：
 * 1. HEAD：状态更新，没有白名单界面时直接渲染冻结帧并cancel
 * 2. @Redirect renderLevel：冻结时跳过世界渲染
 * 3. @Redirect Gui.render：冻结时渲染冻结帧+聊天，然后截图
 * 4. Screen正常渲染
 */
@Mixin(GameRenderer.class)
public abstract class TimestopFrozenFrameMixin {

    @Shadow
    @Final
    Minecraft minecraft;

    @Unique
    private GuiGraphics puellamagi$currentGuiGraphics;

    /**
     * render() 开头
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onRenderHead(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        时停冻结帧.onFrameStart();

        if (!时停冻结帧.shouldSkipRender()) {
            return;
        }

        // 有白名单界面时，让render继续执行
        if (时停冻结帧.isWhitelistedScreen(minecraft.screen)) {
            return;
        }

        // 没有界面：直接渲染冻结帧+聊天并cancel
        时停冻结帧.renderFrozenFrame();

        // 渲染聊天（需要创建GuiGraphics）
        GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        时停冻结帧.renderChat(guiGraphics);
        guiGraphics.flush();

        ci.cancel();
    }

    /**
     * 替换 renderLevel 调用
     */
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"
            )
    )
    private void puellamagi$redirectRenderLevel(GameRenderer instance, float partialTicks, long nanoTime, PoseStack poseStack) {
        if (时停冻结帧.shouldSkipRender()) {
            return;
        }
        instance.renderLevel(partialTicks, nanoTime, poseStack);
    }

    /**
     * 替换 Gui.render 调用
     */
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"
            )
    )
    private void puellamagi$redirectGuiRender(Gui gui, GuiGraphics guiGraphics, float partialTick) {
        // 保存GuiGraphics供后续使用
        puellamagi$currentGuiGraphics = guiGraphics;

        if (时停冻结帧.shouldSkipRender()) {
            // 渲染冻结帧
            时停冻结帧.renderFrozenFrame();
            // 渲染聊天（覆盖在冻结帧上）
            时停冻结帧.renderChat(guiGraphics);} else {
            // 正常渲染HUD
            gui.render(guiGraphics, partialTick);
            // 在Gui.render之后截图（此时有世界+HUD，没有Screen）
            时停冻结帧.captureAfterGui();
        }
    }
}
