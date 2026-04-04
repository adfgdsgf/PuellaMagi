package com.v2t.puellamagi.util.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * 方块快照
 *
 * 保存一个位置的方块状态和方块实体NBT
 * 用于回滚方块变化
 *
 * 复用场景：世界回滚、爆炸撤销、时间倒流
 */
public class 方块快照 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/BlockSnapshot");

    private final BlockPos 位置;
    private final BlockState 方块状态;
    @Nullable
    private final CompoundTag 方块实体NBT;

    // ==================== 构造 ====================

    public 方块快照(BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityNbt) {
        this.位置 = pos.immutable();
        this.方块状态 = state;
        this.方块实体NBT = blockEntityNbt;
    }

    // ==================== 从世界采集 ====================

    /**
     * 从世界中采集方块快照
     */
    public static 方块快照 从世界采集(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        CompoundTag beNbt = null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            beNbt = be.saveWithoutMetadata();
        }

        return new 方块快照(pos, state, beNbt);
    }

    // ==================== 恢复 ====================

    /**
     * 将快照恢复到世界中
     *
     * @param level 目标世界
     * @return 是否成功
     */
    public boolean 恢复到(Level level) {
        try {
            //恢复方块状态（flag3= 通知客户端 + 触发更新）
            level.setBlockAndUpdate(位置, 方块状态);

            // 恢复方块实体数据
            if (方块实体NBT != null) {
                BlockEntity be = level.getBlockEntity(位置);
                if (be != null) {
                    be.load(方块实体NBT);
                    be.setChanged();
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("恢复方块快照失败: pos={}", 位置, e);
            return false;
        }
    }

    // ==================== Getter ====================

    public BlockPos 获取位置() { return 位置; }
    public BlockState 获取方块状态() { return 方块状态; }
    @Nullable
    public CompoundTag 获取方块实体NBT() { return 方块实体NBT; }
}
