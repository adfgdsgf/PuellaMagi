// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopLivingEntityMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生物实体 Mixin
 *
 * 职责：
 * 1. 拦截伤害处理（时停伤害累计）
 * 2. 头部/身体旋转设置拦截
 * 3. 时停投射物无视无敌帧（使用计时器判断）
 */
@Mixin(LivingEntity.class)
public abstract class TimestopLivingEntityMixin {

    // ==================== 伤害拦截 ====================

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 客户端不处理伤害逻辑
        if (self.level().isClientSide) {
            return;
        }

        // ===== 时停投射物无视无敌帧（使用计时器判断）=====
        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile) {
            IProjectileAccess access = (IProjectileAccess) directEntity;
            // 计时器大于0时清除无敌帧
            if (access.puellamagi$getInvincibilityBypassTicks() > 0) {
                self.invulnerableTime = 0;}
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
                cir.setReturnValue(false);}
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
