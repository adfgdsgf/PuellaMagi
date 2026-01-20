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

    //==================== 时停投射物字段 ====================

    @Unique
    private boolean puellamagi$isTimeStopCreated = false;

    @Unique
    private boolean puellamagi$isDeflected = false;

    @Unique
    private float puellamagi$speedMultiplier = 0.75F;

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

    // ==================== 时停标记注入 ====================

    /**
     * 设置 Owner 时检查是否在时停中
     *
     * 关键修复：时停者发射的投射物才有惯性！
     * 条件：在时停范围内 且 发射者不会被冻结（即时停者）
     */
    @Inject(method = "setOwner", at = @At("HEAD"))
    private void puellamagi$onSetOwner(@Nullable Entity owner, CallbackInfo ci) {
        if (owner instanceof LivingEntity) {
            TimeStop timeStop = (TimeStop) owner.level();

            // 关键：在时停范围内 且 发射者是时停者（不会被冻结）
            if (timeStop.puellamagi$inTimeStopRange(owner) &&!timeStop.puellamagi$shouldFreezeEntity(owner)) {
                puellamagi$isTimeStopCreated = true;// 重置速度倍率
                puellamagi$speedMultiplier = 0.75F;
            }
        }
    }

    // ==================== Shadow ====================

    @Shadow
    protected abstract void onHit(HitResult hitResult);

    @Shadow
    protected abstract boolean canHitEntity(Entity entity);
}
