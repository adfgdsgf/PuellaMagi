// 文件路径: src/main/java/com/v2t/puellamagi/api/access/ILivingEntityAccess.java

package com.v2t.puellamagi.api.access;

import net.minecraft.world.entity.WalkAnimationState;
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

    // ==================== WalkAnimationState（新增！）====================

    /**
     * 获取行走动画状态
     *用于访问 speedOld 和 position
     */
    WalkAnimationState puellamagi$getWalkAnimation();

    // ==================== 碰撞 ====================

    void puellamagi$pushEntities();
}
