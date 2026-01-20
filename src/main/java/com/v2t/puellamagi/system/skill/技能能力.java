package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.core.registry.ModCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能能力- Capability实现
 * 存储玩家的技能预设和冷却数据
 */
public class 技能能力 implements ICapabilitySerializable<CompoundTag> {

    private static final String KEY_PRESETS = "Presets";
    private static final String KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String KEY_COOLDOWNS = "Cooldowns";

    // 预设列表（无上限）
    private List<技能预设> 预设列表 = new ArrayList<>();

    // 当前激活的预设索引
    private int 当前预设索引 = 0;

    // 技能冷却表：技能ID -> 剩余冷却tick
    private Map<ResourceLocation, Integer> 冷却表 = new HashMap<>();

    private final LazyOptional<技能能力> holder = LazyOptional.of(() -> this);

    public 技能能力() {
        // 创建默认预设
        预设列表.add(new 技能预设("预设1"));}

    // ==================== 预设管理 ====================

    /**
     * 获取当前预设
     */
    public 技能预设 获取当前预设() {
        if (预设列表.isEmpty()) {
            预设列表.add(new 技能预设("预设1"));
        }
        if (当前预设索引 < 0|| 当前预设索引>= 预设列表.size()) {
            当前预设索引 = 0;
        }
        return 预设列表.get(当前预设索引);
    }

    /**
     * 获取所有预设
     */
    public List<技能预设> 获取所有预设() {
        return 预设列表;
    }

    /**
     * 获取当前预设索引
     */
    public int 获取当前预设索引() {
        return 当前预设索引;
    }

    /**
     * 获取预设数量
     */
    public int 获取预设数量() {
        return 预设列表.size();
    }

    /**
     * 切换到下一个预设（循环）
     */
    public void 下一个预设() {
        if (预设列表.isEmpty()) return;
        当前预设索引 = (当前预设索引 + 1) % 预设列表.size();
    }

    /**
     * 切换到上一个预设（循环）
     */
    public void 上一个预设() {
        if (预设列表.isEmpty()) return;
        当前预设索引 = (当前预设索引 - 1 + 预设列表.size()) % 预设列表.size();
    }

    /**
     * 直接切换到指定索引
     */
    public void 切换预设(int index) {
        if (index >= 0 && index < 预设列表.size()) {
            当前预设索引 = index;
        }
    }

    /**
     * 添加新预设
     */
    public void 添加预设(技能预设 preset) {
        if (preset != null) {
            预设列表.add(preset);
        }
    }

    /**
     * 添加新的默认预设
     */
    public void 添加默认预设() {
        String name = "预设" + (预设列表.size() + 1);
        预设列表.add(new 技能预设(name));
    }

    /**
     * 删除预设
     */
    public boolean 删除预设(int index) {
        // 至少保留一个预设
        if (预设列表.size() <= 1) return false;
        if (index < 0 || index >= 预设列表.size()) return false;

        预设列表.remove(index);

        // 调整当前索引
        if (当前预设索引 >=预设列表.size()) {
            当前预设索引 = 预设列表.size() - 1;
        }
        return true;
    }

    // ==================== 冷却管理 ====================

    /**
     * 设置技能冷却
     * @param skillId 技能ID
     * @param ticks 冷却tick数
     */
    public void 设置冷却(ResourceLocation skillId, int ticks) {
        if (skillId != null && ticks > 0) {
            冷却表.put(skillId, ticks);
        }
    }

    /**
     * 获取技能剩余冷却
     * @return剩余tick数，0表示可用
     */
    public int 获取剩余冷却(ResourceLocation skillId) {
        return 冷却表.getOrDefault(skillId, 0);
    }

    /**
     * 检查技能是否在冷却中
     */
    public boolean 是否冷却中(ResourceLocation skillId) {
        return 获取剩余冷却(skillId) > 0;
    }

    /**
     * 每tick更新冷却
     */
    public void tick() {
        if (冷却表.isEmpty()) return;

        // 减少所有冷却并移除已结束的
        冷却表.entrySet().removeIf(entry -> {
        int remaining = entry.getValue() - 1;
        if (remaining <= 0) {
            return true; // 移除
        } else {
            entry.setValue(remaining);
            return false;
        }
    });
}

    /**
     * 清除所有冷却
     */
    public void 清除所有冷却() {
        冷却表.clear();
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();

        // 保存预设列表
        ListTag presetsTag = new ListTag();
        for (技能预设 preset : 预设列表) {
            presetsTag.add(preset.写入NBT());
        }
        tag.put(KEY_PRESETS, presetsTag);

        // 保存当前索引
        tag.putInt(KEY_CURRENT_INDEX, 当前预设索引);

        // 保存冷却表
        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : 冷却表.entrySet()) {
            cooldownsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put(KEY_COOLDOWNS, cooldownsTag);

        return tag;
    }

    public void 从NBT读取(CompoundTag tag) {
        // 读取预设列表
        预设列表.clear();
        ListTag presetsTag = tag.getList(KEY_PRESETS, Tag.TAG_COMPOUND);
        for (int i = 0; i < presetsTag.size(); i++) {
            预设列表.add(技能预设.从NBT读取(presetsTag.getCompound(i)));
        }

        // 确保至少有一个预设
        if (预设列表.isEmpty()) {
            预设列表.add(new 技能预设("预设1"));
        }

        // 读取当前索引
        当前预设索引 = tag.getInt(KEY_CURRENT_INDEX);
        if (当前预设索引 < 0 || 当前预设索引 >= 预设列表.size()) {
            当前预设索引 = 0;
        }

        // 读取冷却表
        冷却表.clear();
        CompoundTag cooldownsTag = tag.getCompound(KEY_COOLDOWNS);
        for (String key : cooldownsTag.getAllKeys()) {
            冷却表.put(new ResourceLocation(key), cooldownsTag.getInt(key));
        }
    }

    // ==================== 复制 ====================

    public void 复制自(技能能力 other) {
        this.从NBT读取(other.写入NBT());
    }

    // ==================== ICapabilitySerializable ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.技能能力.orEmpty(cap, holder.cast());
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
