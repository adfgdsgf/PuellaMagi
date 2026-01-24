// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/FeignDeathPlayerMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.api.restriction.限制类型;
import com.v2t.puellamagi.system.restriction.行动限制管理器;
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
 * 行动限制 -玩家行为拦截
 *
 * 统一调用行动限制管理器，不直接依赖具体状态
 * 支持：假死、灵魂视角、未来的定身/沉默等
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
        if (行动限制管理器.是否被限制(player, 限制类型.移动)) {
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
        if (行动限制管理器.是否被限制(player, 限制类型.跳跃)) {
            ci.cancel();
        }
    }

    /**
     * 拦截攻击实体
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (行动限制管理器.是否被限制(player, 限制类型.攻击)) {
            ci.cancel();
        }
    }

    /**
     * 拦截右键交互实体
     */
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void onInteractOn(Entity entity, InteractionHand hand,
                              CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        if (行动限制管理器.是否被限制(player, 限制类型.交互实体)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    /**
     * 拦截丢弃物品
     */
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",at = @At("HEAD"), cancellable = true)
    private void onDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership,CallbackInfoReturnable<ItemEntity> cir) {
        Player player = (Player) (Object) this;
        if (行动限制管理器.是否被限制(player, 限制类型.丢弃物品)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * 拦截mayBuild检查（影响放置方块等）
     */
    @Inject(method = "mayBuild", at = @At("HEAD"), cancellable = true)
    private void onMayBuild(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        // mayBuild影响方块交互，使用交互方块限制
        if (行动限制管理器.是否被限制(player, 限制类型.交互方块)) {
            cir.setReturnValue(false);
        }
    }
}
