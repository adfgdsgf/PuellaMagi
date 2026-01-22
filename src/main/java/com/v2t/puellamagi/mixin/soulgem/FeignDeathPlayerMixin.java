// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/FeignDeathPlayerMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 假死状态-玩家行为拦截
 *
 * 拦截所有可能的行动：
 * - 移动、跳跃
 * - 攻击实体
 * - 交互实体
 * - 丢弃物品
 */
@Mixin(Player.class)
public abstract class FeignDeathPlayerMixin extends LivingEntity {

    protected FeignDeathPlayerMixin() {
        super(null, null);
    }

    /**
     * 拦截移动
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 travelVector, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (假死状态处理器.应该限制行动(player)) {
            this.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }

    /**
     * 拦截跳跃
     */
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (假死状态处理器.应该限制行动(player)) {
            ci.cancel();
        }
    }

    /**
     * 拦截攻击实体
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (假死状态处理器.应该限制行动(player)) {
            ci.cancel();
        }
    }

    /**
     * 拦截右键交互实体
     */
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractOn(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        if (假死状态处理器.应该限制行动(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    /**
     * 拦截丢弃物品
     */
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void onDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership,CallbackInfoReturnable<ItemEntity> cir) {
        Player player = (Player) (Object) this;
        if (假死状态处理器.应该限制行动(player)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * 拦截mayBuild检查（影响放置方块等）
     */
    @Inject(method = "mayBuild", at = @At("HEAD"), cancellable = true)
    private void onMayBuild(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (假死状态处理器.应该限制行动(player)) {
            cir.setReturnValue(false);
        }
    }
}
