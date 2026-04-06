// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopLivingEntityMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.时停;
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

        时停 时停 = (时停) self.level();

        // 没有时停则正常处理
        if (!时停.puellamagi$hasActiveTimeStop()) {
            return;
        }

        Entity attacker = source.getEntity();

        // 被冻结实体受到时停者（或其投射物/召唤物）攻击 -> 累计伤害
        if (时停.puellamagi$shouldFreezeEntity(self)) {
            // 检查攻击来源是否为时停者（直接攻击或通过投射物/召唤物间接攻击）
            if (attacker != null && 是否时停者攻击(attacker, 时停)) {
                时停管理器.存储伤害(self, source, amount);
                cir.setReturnValue(false);
                return;
            }
        }

        // 时停者受到被冻结实体攻击 -> 免疫
        if (self instanceof Player player && 时停.puellamagi$isTimeStopper(player)) {
            if (attacker != null && 时停.puellamagi$shouldFreezeEntity(attacker)) {
                cir.setReturnValue(false);}
        }
    }

    // ==================== 朝向拦截 ====================

    @Inject(method = "setYHeadRot", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetYHeadRot(float rotation, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        时停 时停 = (时停) self.level();

        if (时停.puellamagi$shouldFreezeEntity(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "setYBodyRot", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetYBodyRot(float rotation, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        时停 时停 = (时停) self.level();

        if (时停.puellamagi$shouldFreezeEntity(self)) {
            ci.cancel();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断攻击者是否为时停者或时停者的投射物/召唤物
     *
     * 检查链：
     * 1. 攻击者本身是时停者Player → true
     * 2. 攻击者是投射物 → 检查owner是否为时停者
     * 3. 其他情况 → false
     */
    @org.spongepowered.asm.mixin.Unique
    private static boolean 是否时停者攻击(Entity attacker, 时停 时停) {
        // 直接是时停者Player
        if (attacker instanceof Player player && 时停.puellamagi$isTimeStopper(player)) {
            return true;
        }

        // 投射物：检查其owner
        if (attacker instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof Player player && 时停.puellamagi$isTimeStopper(player)) {
                return true;
            }
        }

        return false;
    }
}
