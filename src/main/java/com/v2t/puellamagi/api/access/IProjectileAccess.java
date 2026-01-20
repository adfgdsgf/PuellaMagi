// 文件路径: src/main/java/com/v2t/puellamagi/api/access/IProjectileAccess.java

package com.v2t.puellamagi.api.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/**
 * 投射物访问接口
 *
 * 用于时停中的投射物惯性处理
 * 对标Roundabout 的IProjectileAccess
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
     * 是否是时停中创建的投射物
     */
    boolean puellamagi$isTimeStopCreated();

    /**
     * 设置时停创建标记
     */
    void puellamagi$setTimeStopCreated(boolean created);

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
