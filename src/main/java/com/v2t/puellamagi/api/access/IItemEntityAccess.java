// 文件路径: src/main/java/com/v2t/puellamagi/api/access/IItemEntityAccess.java

package com.v2t.puellamagi.api.access;

/**
 * ItemEntity 访问接口
 *
 * 完全对标 IProjectileAccess
 */
public interface IItemEntityAccess {

    /**
     * 获取速度倍率（惯性衰减用）
     */
    float puellamagi$getSpeedMultiplier();

    /**
     * 设置速度倍率
     */
    void puellamagi$setSpeedMultiplier(float multiplier);

    /**
     * 是否是时停中创建/扔出的掉落物
     */
    boolean puellamagi$isTimeStopCreated();

    /**
     * 设置时停创建标记
     */
    void puellamagi$setTimeStopCreated(boolean created);
}
