// 文件路径: src/main/java/com/v2t/puellamagi/api/contract/I契约.java

package com.v2t.puellamagi.api.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 契约接口
 *
 * 玩家与契约兽签订契约后成为魔法少女
 * 契约决定了所属系列和魔法少女类型
 */
public interface I契约 {

    //==================== 契约状态 ====================

    /**
     * 是否已签订契约
     */
    boolean 是否已契约();

    /**
     * 获取契约时间（游戏tick）
     * 用于统计/成就等
     */
    long 获取契约时间();

    // ==================== 系列与类型 ====================

    /**
     * 获取所属系列ID
     * 如: puellamagi:soul_gem
     */
    @Nullable
    ResourceLocation 获取系列ID();

    /**
     * 获取魔法少女类型ID
     * 如: puellamagi:time_manipulator
     */
    @Nullable
    ResourceLocation 获取类型ID();

    // ==================== 契约操作 ====================

    /**
     * 签订契约
     * @param seriesId 系列ID
     * @param typeId 魔法少女类型ID
     * @param gameTime 当前游戏时间
     */
    void 签订契约(ResourceLocation seriesId, ResourceLocation typeId, long gameTime);

    /**
     * 解除契约（如果允许的话）
     */
    void 解除契约();

    // ==================== 序列化 ====================

    CompoundTag 写入NBT();

    void 从NBT读取(CompoundTag tag);
}
