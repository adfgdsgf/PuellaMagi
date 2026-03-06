package com.v2t.puellamagi.mixin.access;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * LivingEntity 访问Mixin
 *
 * 通过继承方式访问 protected字段
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

    // ==================== WalkAnimationState ====================

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

    // ==================== 跳跃 ====================

    @Shadow
    protected boolean jumping;

    @Unique
    @Override
    public boolean puellamagi$isJumping() {
        return this.jumping;
    }

    @Unique
    @Override
    public void puellamagi$setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    // ==================== 使用物品（复刻用） ====================

    @Shadow
    protected ItemStack useItem;

    @Shadow
    protected int useItemRemaining;

    @Unique
    @Override
    public ItemStack puellamagi$getUseItem() {
        return this.useItem;
    }

    @Unique
    @Override
    public void puellamagi$setUseItem(ItemStack item) {
        this.useItem = item;
    }

    @Unique
    @Override
    public int puellamagi$getUseItemRemaining() {
        return this.useItemRemaining;
    }

    @Unique
    @Override
    public void puellamagi$setUseItemRemaining(int ticks) {
        this.useItemRemaining = ticks;
    }

    // ==================== EntityData 标志位（复刻用） ====================

    @Shadow
    protected void setLivingEntityFlag(int flag, boolean value) {}

    @Unique
    @Override
    public void puellamagi$setLivingEntityFlag(int flag, boolean value) {
        this.setLivingEntityFlag(flag, value);
    }
}
