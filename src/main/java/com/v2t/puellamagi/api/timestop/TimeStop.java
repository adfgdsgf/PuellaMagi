// 文件路径: src/main/java/com/v2t/puellamagi/api/timestop/TimeStop.java

package com.v2t.puellamagi.api.timestop;

import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * 时停接口- 由Level 实现
 *
 * 参考Roundabout mod 的设计，将时停状态绑定到 Level 而非外部管理器
 */
public interface TimeStop {

    //==================== 时停者管理 ====================

    /**
     * 添加时停者
     */
    void puellamagi$addTimeStopper(LivingEntity entity);

    /**
     * 移除时停者
     */
    void puellamagi$removeTimeStopper(LivingEntity entity);

    /**
     * 获取所有时停者
     */
    List<LivingEntity> puellamagi$getTimeStoppers();

    /**
     * 判断是否是时停者
     */
    boolean puellamagi$isTimeStopper(Entity entity);

    /**
     * 判断是否是时停者（通过UUID）
     */
    boolean puellamagi$isTimeStopper(UUID uuid);

    // ==================== 冻结判断 ====================

    /**
     * 判断实体是否应该被冻结
     *
     * 核心判断方法，考虑：
     * - 是否存在时停
     * - 是否是时停者本身
     * - 是否在时停范围内
     * - 特殊实体豁免（创造模式等）
     */
    boolean puellamagi$shouldFreezeEntity(Entity entity);

    /**
     * 判断位置是否在时停范围内
     */
    boolean puellamagi$inTimeStopRange(Vec3i pos);

    /**
     * 判断实体是否在时停范围内
     */
    boolean puellamagi$inTimeStopRange(Entity entity);

    // ==================== 状态查询 ====================

    /**
     * 是否存在时停
     */
    boolean puellamagi$hasActiveTimeStop();

    // ==================== Tick ====================

    /**
     * 每tick 调用，处理时停逻辑
     */
    void puellamagi$tickTimeStop();

    // ==================== 客户端同步 ====================

    /**
     * 添加时停者（客户端）
     */
    void puellamagi$addTimeStopperClient(int entityId, double x, double y, double z, double range);

    /**
     * 移除时停者（客户端）
     */
    void puellamagi$removeTimeStopperClient(int entityId);

    /**
     * 同步时停状态到所有客户端
     */
    void puellamagi$syncToClients();
}
