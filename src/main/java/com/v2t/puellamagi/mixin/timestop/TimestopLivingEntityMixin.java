// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopLivingEntityMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生物实体 Mixin
 *
 * 职责：拦截伤害处理、头部/身体旋转设置
 * tick/aiStep 拦截已移至 Level 层
 */
@Mixin(LivingEntity.class)
public abstract class TimestopLivingEntityMixin {

    // ==================== 伤害拦截 ====================

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;


        if (self.level().isClientSide) {
            return;
        }


        // 客户端不处理伤害逻辑
        if (self.level().isClientSide) {
            return;
        }

        TimeStop timeStop = (TimeStop) self.level();

        // 没有时停则正常处理
        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return;
        }

        Entity attacker = source.getEntity();

        // 被冻结实体受到时停者攻击 -> 累计伤害
        if (timeStop.puellamagi$shouldFreezeEntity(self)) {
            if (attacker instanceof Player player && timeStop.puellamagi$isTimeStopper(player)) {
                时停管理器.存储伤害(self, source, amount);
                cir.setReturnValue(false);
                return;
            }
        }

        // 时停者受到被冻结实体攻击 -> 免疫
        if (self instanceof Player player && timeStop.puellamagi$isTimeStopper(player)) {
            if (attacker != null && timeStop.puellamagi$shouldFreezeEntity(attacker)) {
                cir.setReturnValue(false);
            }
        }
    }

    // ==================== 朝向拦截 ====================

    @Inject(method = "setYHeadRot", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetYHeadRot(float rotation, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        TimeStop timeStop = (TimeStop) self.level();

        if (timeStop.puellamagi$shouldFreezeEntity(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "setYBodyRot", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetYBodyRot(float rotation, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        TimeStop timeStop = (TimeStop) self.level();

        if (timeStop.puellamagi$shouldFreezeEntity(self)) {
            ci.cancel();
        }
    }
}
