package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 假死状态 - 客户端输入拦截
 *
 * 直接禁用攻击和使用键，消除视觉残留
 */
@Mixin(Minecraft.class)
public class FeignDeathInputMixin {

    @Shadow
    @Final
    public Options options;

    /**
     * 在处理按键绑定时，如果假死则禁用攻击/使用键
     */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onHandleKeybinds(CallbackInfo ci) {
        if (假死状态处理器.客户端是否假死()) {
            this.options.keyAttack.setDown(false);
            this.options.keyUse.setDown(false);
        }
    }

    /**
     * 拦截startUseItem -阻止使用物品的开始
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (假死状态处理器.客户端是否假死()) {
            ci.cancel();
        }
    }

    /**
     * 拦截continueAttack - 阻止持续攻击
     */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (假死状态处理器.客户端是否假死()) {
            ci.cancel();
        }
    }

    /**
     * 拦截startAttack - 阻止开始攻击
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (假死状态处理器.客户端是否假死()) {
            cir.setReturnValue(false);
        }
    }
}
