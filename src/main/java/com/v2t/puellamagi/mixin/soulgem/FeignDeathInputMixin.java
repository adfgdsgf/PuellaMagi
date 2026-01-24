// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/FeignDeathInputMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.api.restriction.限制类型;
import com.v2t.puellamagi.system.restriction.行动限制管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 行动限制 - 客户端输入拦截
 *
 * 直接禁用攻击和使用键，消除视觉残留
 * 注意：客户端需要能查询到限制状态（通过同步包）
 */
@Mixin(Minecraft.class)
public class FeignDeathInputMixin {

    @Shadow
    @Final
    public Options options;

    @Shadow
    public LocalPlayer player;

    /**
     * 在处理按键绑定时，检查限制状态
     */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onHandleKeybinds(CallbackInfo ci) {
        if (this.player == null) return;

        // 检查攻击限制
        if (行动限制管理器.是否被限制(this.player, 限制类型.攻击)) {
            this.options.keyAttack.setDown(false);
        }

        // 检查使用物品限制（右键）
        if (行动限制管理器.是否被限制(this.player, 限制类型.使用物品)&& 行动限制管理器.是否被限制(this.player, 限制类型.交互方块)) {
            this.options.keyUse.setDown(false);
        }
    }

    /**
     * 拦截startUseItem -阻止使用物品的开始
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (this.player == null) return;

        if (行动限制管理器.是否被限制(this.player, 限制类型.使用物品)) {
            ci.cancel();
        }
    }

    /**
     * 拦截continueAttack - 阻止持续攻击
     */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (this.player == null) return;

        if (行动限制管理器.是否被限制(this.player, 限制类型.攻击)) {
            ci.cancel();
        }
    }

    /**
     * 拦截startAttack - 阻止开始攻击
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null) return;

        if (行动限制管理器.是否被限制(this.player, 限制类型.攻击)) {
            cir.setReturnValue(false);
        }
    }
}
