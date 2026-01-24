// 文件路径: src/main/java/com/v2t/puellamagi/system/contract/契约能力.java

package com.v2t.puellamagi.system.contract;

import com.v2t.puellamagi.api.contract.I契约;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 契约能力的Capability实现
 * 附加到Player上，管理契约状态
 */
public class 契约能力 implements I契约, ICapabilitySerializable<CompoundTag> {

    private final 契约数据 数据 = new 契约数据();
    private final LazyOptional<I契约> holder = LazyOptional.of(() -> this);

    // ==================== I契约 实现 ====================

    @Override
    public boolean 是否已契约() {
        return 数据.是否已契约();
    }

    @Override
    public long 获取契约时间() {
        return 数据.获取契约时间();
    }

    @Override
    @Nullable
    public ResourceLocation 获取系列ID() {
        return 数据.获取系列ID();
    }

    @Override
    @Nullable
    public ResourceLocation 获取类型ID() {
        return 数据.获取类型ID();
    }

    @Override
    public void 签订契约(ResourceLocation seriesId, ResourceLocation typeId, long gameTime) {
        数据.签订(seriesId, typeId, gameTime);
    }

    @Override
    public void 解除契约() {
        数据.解除();
    }

    @Override
    public CompoundTag 写入NBT() {
        return 数据.写入NBT();
    }

    @Override
    public void 从NBT读取(CompoundTag tag) {
        数据.从NBT读取(tag);
    }

    // ==================== 灵魂宝石相关 ====================

    public boolean 是否已发放灵魂宝石() {
        return 数据.是否已发放灵魂宝石();
    }

    @Nullable
    public UUID 获取灵魂宝石UUID() {
        return 数据.获取灵魂宝石UUID();
    }

    public long 获取灵魂宝石时间戳() {
        return 数据.获取灵魂宝石时间戳();
    }

    public void 记录灵魂宝石发放(UUID gemUUID, long timestamp) {
        数据.记录灵魂宝石发放(gemUUID, timestamp);
    }

    public void 更新灵魂宝石时间戳(long newTimestamp) {
        数据.更新灵魂宝石时间戳(newTimestamp);
    }

    // ==================== 重签冷却相关 ====================

    /**
     * 检查是否在冷却中
     *
     * @param currentGameTime 当前游戏时间
     * @return 是否冷却中
     */
    public boolean 是否冷却中(long currentGameTime) {
        return 数据.是否冷却中(currentGameTime);
    }

    /**
     * 获取剩余冷却显示值
     * 游戏时间模式返回天数，现实时间模式返回分钟数
     *
     * @param currentGameTime 当前游戏时间
     * @return 显示数值
     */
    public int 获取剩余冷却显示值(long currentGameTime) {
        return 数据.获取剩余冷却显示值(currentGameTime);
    }

    /**
     * 设置重签冷却
     *
     * @param currentGameTime 当前游戏时间
     */
    public void 设置重签冷却(long currentGameTime) {
        数据.设置重签冷却(currentGameTime);
    }

    /**
     * 清除冷却
     */
    public void 清除冷却() {
        数据.清除冷却();
    }

    // ==================== 额外数据 ====================

    public CompoundTag 获取额外数据() {
        return 数据.获取额外数据();
    }

    public void 设置额外数据(CompoundTag data) {
        数据.设置额外数据(data);
    }

    public void 复制自(契约能力 other) {
        数据.复制自(other.数据);
    }

    // ==================== ICapabilitySerializable 实现 ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.契约能力.orEmpty(cap, holder);
    }

    @Override
    public CompoundTag serializeNBT() {
        return 写入NBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        从NBT读取(nbt);
    }

    public void 失效() {
        holder.invalidate();
    }
}
