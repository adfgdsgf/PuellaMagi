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

    // ==================== 额外方法 ====================

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
