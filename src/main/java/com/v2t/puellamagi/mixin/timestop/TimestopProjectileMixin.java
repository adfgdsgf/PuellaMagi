// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopProjectileMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 投射物 Mixin
 *
 * 实现 IProjectileAccess，处理时停中的投射物标记
 * 对标 Roundabout 的 TimeStopProjectile
 */
@Mixin(Projectile.class)
public abstract class TimestopProjectileMixin extends Entity implements IProjectileAccess {

    public TimestopProjectileMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    //==================== 常量 ====================

    /**
     * 无敌帧绕过持续时间（tick）
     * 60 ticks = 3秒，足够箭矢飞行和命中
     */
    @Unique
    private static final int INVINCIBILITY_BYPASS_DURATION = 60;

    // ==================== 时停投射物字段 ====================

    @Unique
    private boolean puellamagi$isTimeStopCreated = false;

    @Unique
    private boolean puellamagi$isDeflected = false;

    @Unique
    private float puellamagi$speedMultiplier = 0.75F;

    /**
     * 无敌帧绕过计时器
     * 大于0时命中可清除目标无敌帧
     * 时停期间冻结，时停结束后开始倒计时
     */
    @Unique
    private int puellamagi$invincibilityBypassTicks = 0;

    // ==================== IProjectileAccess 实现 ====================

    @Override
    public float puellamagi$getSpeedMultiplier() {
        return puellamagi$speedMultiplier;
    }

    @Override
    public void puellamagi$setSpeedMultiplier(float multiplier) {
        puellamagi$speedMultiplier = multiplier;
    }

    @Override
    public boolean puellamagi$isTimeStopCreated() {
        return puellamagi$isTimeStopCreated;
    }

    @Override
    public void puellamagi$setTimeStopCreated(boolean created) {
        puellamagi$isTimeStopCreated = created;
    }

    @Override
    public int puellamagi$getInvincibilityBypassTicks() {
        return puellamagi$invincibilityBypassTicks;
    }

    @Override
    public void puellamagi$setInvincibilityBypassTicks(int ticks) {
        puellamagi$invincibilityBypassTicks = ticks;
    }

    @Override
    public boolean puellamagi$isDeflected() {
        return puellamagi$isDeflected;
    }

    @Override
    public void puellamagi$setDeflected(boolean deflected) {
        puellamagi$isDeflected = deflected;
    }

    @Override
    public void puellamagi$onHit(HitResult hitResult) {
        this.onHit(hitResult);
    }

    @Override
    public boolean puellamagi$canHitEntity(Entity entity) {
        return this.canHitEntity(entity);
    }

    @Override
    public void puellamagi$checkInsideBlocks() {
        this.checkInsideBlocks();
    }

    // ==================== 时停标记与计时器初始化 ====================

    /**
     * 设置 Owner 时处理时停标记和计时器
     *
     * 关键逻辑：
     * 1. 无论如何先重置计时器（处理捡起重射场景）
     * 2. 如果是时停者发射，则设置标记和计时器
     */
    @Inject(method = "setOwner", at = @At("HEAD"))
    private void puellamagi$onSetOwner(@Nullable Entity owner, CallbackInfo ci) {
        // 关键：先重置计时器，处理捡起重射的情况
        puellamagi$invincibilityBypassTicks = 0;
        puellamagi$isTimeStopCreated = false;

        if (owner instanceof LivingEntity) {
            TimeStop timeStop = (TimeStop) owner.level();

            // 在时停范围内 且 发射者是时停者（不会被冻结）
            if (timeStop.puellamagi$inTimeStopRange(owner) &&!timeStop.puellamagi$shouldFreezeEntity(owner)) {
                puellamagi$isTimeStopCreated = true;
                puellamagi$invincibilityBypassTicks = INVINCIBILITY_BYPASS_DURATION;puellamagi$speedMultiplier = 0.75F;
            }
        }
    }

    // ==================== 计时器倒计时 ====================

    /**
     * 在投射物 tick 中处理计时器倒计时
     *
     * 只在非时停状态下递减，实现"时停期间冻结"效果
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void puellamagi$onTick(CallbackInfo ci) {
        // 只在服务端处理
        if (this.level().isClientSide) {
            return;
        }

        // 有计时器才需要处理
        if (puellamagi$invincibilityBypassTicks <= 0) {
            return;
        }

        TimeStop timeStop = (TimeStop) this.level();

        // 关键：只在没有时停激活时才倒计时
        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            puellamagi$invincibilityBypassTicks--;
        }
    }

    // ==================== Shadow ====================

    @Shadow
    protected abstract void onHit(HitResult hitResult);

    @Shadow
    protected abstract boolean canHitEntity(Entity entity);
}
