package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * 方块实体变化帧
 *
 * 记录某一tick某个方块实体的NBT变化
 * 和方块变化帧并列，用于容器等方块实体的内容修正
 *
 * 回放时：
 * - 到对应tick → 强制设新NBT
 * - 同时对比新旧NBT的物品差异 → 扣/加玩家背包
 * - 保证总量不变（不刷不丢）
 */
public class 方块实体变化帧 {

    private final BlockPos 位置;
    private final CompoundTag 旧NBT;
    private final CompoundTag 新NBT;
    private final int tick序号;

    public 方块实体变化帧(BlockPos pos, CompoundTag oldNbt, CompoundTag newNbt, int tickIndex) {
        this.位置 = pos.immutable();
        this.旧NBT = oldNbt;
        this.新NBT = newNbt;
        this.tick序号 = tickIndex;
    }

    public BlockPos 获取位置() { return 位置; }
    public CompoundTag 获取旧NBT() { return 旧NBT; }
    public CompoundTag 获取新NBT() { return 新NBT; }
    public int 获取tick序号() { return tick序号; }
}
