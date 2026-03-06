package com.v2t.puellamagi.api.access;

import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * LivingEntity 访问接口
 *
 * 提供访问 protected/private 字段的方法
 */
public interface ILivingEntityAccess {

    // ==================== Lerp 插值 ====================

    double puellamagi$getLerpX();
    double puellamagi$getLerpY();
    double puellamagi$getLerpZ();

    int puellamagi$getLerpSteps();
    void puellamagi$setLerpSteps(int steps);

    void puellamagi$setLerp(Vec3 pos);

    // ==================== 动画 ====================

    float puellamagi$getAnimStep();
    void puellamagi$setAnimStep(float value);

    float puellamagi$getAnimStepO();
    void puellamagi$setAnimStepO(float value);

    // ==================== WalkAnimationState ====================

    WalkAnimationState puellamagi$getWalkAnimation();

    // ==================== 碰撞 ====================

    void puellamagi$pushEntities();

    // ==================== 跳跃 ====================

    boolean puellamagi$isJumping();
    void puellamagi$setJumping(boolean jumping);

    // ==================== 使用物品（复刻用） ====================

    /**
     * 获取正在使用的物品
     * LivingEntity.useItem 是 protected 字段
     */
    ItemStack puellamagi$getUseItem();

    /**
     * 设置正在使用的物品
     */
    void puellamagi$setUseItem(ItemStack item);

    /**
     * 获取使用物品剩余tick
     * LivingEntity.useItemRemaining 是 protected 字段
     */
    int puellamagi$getUseItemRemaining();

    /**
     * 设置使用物品剩余tick
     *用于复刻时还原拉弓/吃东西等动画进度
     */
    void puellamagi$setUseItemRemaining(int ticks);

    //==================== EntityData 标志位（复刻用） ====================

    /**
     * 设置LivingEntity 的 DATA_LIVING_ENTITY_FLAGS
     *
     * bit 0x1 = 是否正在使用物品
     * bit 0x2 = 使用物品的手（0=主手, 1=副手）
     * bit 0x4 = 是否正在旋转（鞘翅）
     *
     * 必须通过这个方法设置，否则客户端看不到使用物品动画
     * 直接设useItemRemaining只改内部字段，不触发EntityData同步
     */
    void puellamagi$setLivingEntityFlag(int flag, boolean value);
}
