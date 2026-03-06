package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 回放期间拦截客户端的方块/实体交互，只放行使用物品
 *
 * use键被注入isDown（动画需要）但客户端不能执行放方块/交互：
 * - useItemOn → 返回PASS（不放方块、不开箱子）→ 无幽灵方块
 * - interact → 返回PASS（不交互实体）
 * - interactAt → 返回PASS（不交互实体）
 * - useItem → 不拦截（客户端本地播动画，C2S包被FULL模式挡住不影响服务端）
 *
 * 实际效果全部由服务端包回放管
 */
@Mixin(MultiPlayerGameMode.class)
public class EpitaphReplayGameModeMixin {

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockInteractAt(CallbackInfoReturnable<InteractionResult> cir) {
        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockInteract(CallbackInfoReturnable<InteractionResult> cir) {
        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockUseItemOn(CallbackInfoReturnable<InteractionResult> cir) {
        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
