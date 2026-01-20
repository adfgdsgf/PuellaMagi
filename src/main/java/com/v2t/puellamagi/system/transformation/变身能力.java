package com.v2t.puellamagi.system.transformation;

import com.v2t.puellamagi.api.I可变身;
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
 * 变身能力的Capability实现
 * 附加到Player上，管理变身状态
 *
 * 注意：一个玩家只能同时拥有一个魔法少女类型
 */
public class 变身能力 implements I可变身, ICapabilitySerializable<CompoundTag> {

    private final 变身数据 数据 = new 变身数据();
    private final LazyOptional<I可变身> holder = LazyOptional.of(() -> this);

    // ==================== I可变身 实现 ====================

    @Override
    public boolean 是否已变身() {
        return 数据.是否已变身();
    }

    @Override
    public void 设置变身状态(boolean transformed) {
        数据.设置变身状态(transformed);
    }

    @Override
    public ResourceLocation 获取少女类型() {
        return 数据.获取少女类型();
    }

    @Override
    public void 设置少女类型(ResourceLocation typeId) {
        数据.设置少女类型(typeId);
    }

    @Override
    public ResourceLocation 获取模型() {
        return 数据.获取模型();
    }

    @Override
    public void 设置模型(ResourceLocation modelId) {
        数据.设置模型(modelId);
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

    public CompoundTag 获取能力数据() {
        return 数据.获取能力数据();
    }

    public void 设置能力数据(CompoundTag data) {
        数据.设置能力数据(data);
    }

    public ResourceLocation 获取所属系列() {
        return 数据.获取所属系列();
    }

    public void 设置所属系列(ResourceLocation seriesId) {
        数据.设置所属系列(seriesId);
    }

    public int 获取当前阶段索引() {
        return 数据.获取当前阶段索引();
    }

    public void 设置当前阶段索引(int index) {
        数据.设置当前阶段索引(index);
    }

    public void 重置() {
        数据.重置();
    }

    public void 清空() {
        数据.清空();
    }

    public void 复制自(变身能力 other) {
        数据.复制自(other.数据);
    }

    // ==================== ICapabilitySerializable 实现 ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.变身能力.orEmpty(cap, holder);
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
