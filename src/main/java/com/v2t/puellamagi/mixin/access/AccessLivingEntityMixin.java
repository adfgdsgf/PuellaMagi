// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/AccessLivingEntityMixin.java

package com.v2t.puellamagi.mixin.access;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * LivingEntity 访问Mixin
 *
 * 通过继承方式访问 protected 字段
 */
@Mixin(LivingEntity.class)
public abstract class AccessLivingEntityMixin extends Entity implements ILivingEntityAccess {

    public AccessLivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ==================== Lerp 插值 ====================

    @Shadow
    protected double lerpX;

    @Shadow
    protected double lerpY;

    @Shadow
    protected double lerpZ;

    @Shadow
    protected int lerpSteps;

    @Unique
    @Override
    public double puellamagi$getLerpX() {
        return this.lerpX;
    }

    @Unique
    @Override
    public double puellamagi$getLerpY() {
        return this.lerpY;
    }

    @Unique
    @Override
    public double puellamagi$getLerpZ() {
        return this.lerpZ;
    }

    @Unique
    @Override
    public int puellamagi$getLerpSteps() {
        return this.lerpSteps;
    }

    @Unique
    @Override
    public void puellamagi$setLerpSteps(int steps) {
        this.lerpSteps = steps;
    }

    @Unique
    @Override
    public void puellamagi$setLerp(Vec3 pos) {
        this.lerpX = pos.x;
        this.lerpY = pos.y;
        this.lerpZ = pos.z;
    }

    // ==================== 动画 ====================

    @Shadow
    protected float animStep;

    @Shadow
    protected float animStepO;

    @Unique
    @Override
    public float puellamagi$getAnimStep() {
        return this.animStep;
    }

    @Unique
    @Override
    public void puellamagi$setAnimStep(float value) {
        this.animStep = value;
    }

    @Unique
    @Override
    public float puellamagi$getAnimStepO() {
        return this.animStepO;
    }

    @Unique
    @Override
    public void puellamagi$setAnimStepO(float value) {
        this.animStepO = value;
    }

    // ==================== WalkAnimationState（新增！）====================

    @Shadow
    @Final
    public WalkAnimationState walkAnimation;

    @Unique
    @Override
    public WalkAnimationState puellamagi$getWalkAnimation() {
        return this.walkAnimation;
    }

    // ==================== 碰撞 ====================

    @Shadow
    protected abstract void pushEntities();

    @Unique
    @Override
    public void puellamagi$pushEntities() {
        this.pushEntities();
    }
}
