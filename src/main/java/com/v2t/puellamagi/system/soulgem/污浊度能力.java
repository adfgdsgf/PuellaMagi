// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/污浊度能力.java

package com.v2t.puellamagi.system.soulgem;

import com.v2t.puellamagi.api.soulgem.I污浊度;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 污浊度能力的Capability实现
 * 附加到Player上，管理污浊度状态
 */
public class 污浊度能力 implements I污浊度, ICapabilitySerializable<CompoundTag> {

    private final 污浊度数据 数据 = new 污浊度数据();
    private final LazyOptional<I污浊度> holder = LazyOptional.of(() -> this);

    // ==================== I污浊度 实现 ====================

    @Override
    public float 获取当前值() {
        return 数据.获取当前值();
    }

    @Override
    public float 获取最大值() {
        return 数据.获取最大值();
    }

    @Override
    public float 获取百分比() {
        return 数据.获取百分比();
    }

    @Override
    public void 增加污浊度(float amount) {
        数据.增加(amount);
    }

    @Override
    public void 减少污浊度(float amount) {
        数据.减少(amount);
    }

    @Override
    public void 设置当前值(float value) {
        数据.设置当前值(value);
    }

    @Override
    public boolean 是否已满() {
        return 数据.是否已满();
    }

    @Override
    public boolean 是否为空() {
        return 数据.是否为空();
    }

    @Override
    public void 重置() {
        数据.重置();
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

    /**
     * 设置最大值（用于特殊情况，如装备加成）
     */
    public void 设置最大值(float value) {
        数据.设置最大值(value);
    }

    /**
     * 复制另一个能力的数据
     */
    public void 复制自(污浊度能力 other) {
        if (other != null) {
            数据.复制自(other.数据);
        }
    }

    // ==================== ICapabilitySerializable 实现 ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.污浊度能力.orEmpty(cap, holder);
    }

    @Override
    public CompoundTag serializeNBT() {
        return 写入NBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        从NBT读取(nbt);
    }

    /**
     * 使LazyOptional失效（玩家移除时调用）
     */
    public void 失效() {
        holder.invalidate();
    }
}
