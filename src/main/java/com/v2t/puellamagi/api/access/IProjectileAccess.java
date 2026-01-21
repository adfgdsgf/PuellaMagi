// 文件路径: src/main/java/com/v2t/puellamagi/api/access/IProjectileAccess.java

package com.v2t.puellamagi.api.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/**
 * 投射物访问接口
 *
 * 用于时停中的投射物惯性处理
 * 对标 Roundabout 的 IProjectileAccess
 */
public interface IProjectileAccess {

    /**
     * 获取速度倍率（惯性衰减用）
     */
    float puellamagi$getSpeedMultiplier();

    /**
     * 设置速度倍率
     */
    void puellamagi$setSpeedMultiplier(float multiplier);

    /**
     * 是否是时停中创建的投射物（惯性阶段使用，时停结束后会被清除）
     */
    boolean puellamagi$isTimeStopCreated();

    /**
     * 设置时停创建标记
     */
    void puellamagi$setTimeStopCreated(boolean created);

    /**
     * 获取无敌帧绕过剩余时间（tick）
     * 大于0时命中可清除目标无敌帧
     */
    int puellamagi$getInvincibilityBypassTicks();

    /**
     * 设置无敌帧绕过剩余时间
     */
    void puellamagi$setInvincibilityBypassTicks(int ticks);

    /**
     * 是否被偏转过
     */
    boolean puellamagi$isDeflected();

    /**
     * 设置偏转标记
     */
    void puellamagi$setDeflected(boolean deflected);

    /**
     * 调用 onHit
     */
    void puellamagi$onHit(HitResult hitResult);

    /**
     * 检查能否命中实体
     */
    boolean puellamagi$canHitEntity(Entity entity);

    /**
     * 检查是否在方块内
     */
    void puellamagi$checkInsideBlocks();
}
