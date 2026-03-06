package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块变化帧
 *
 * 记录某一tick中发生的方块变化
 * 用于录制/回放期间追踪方块变化
 *
 * 与方块快照的区别：
 * - 方块快照：某一时刻的静态状态（用于回滚）
 * - 方块变化帧：变化事件（从什么变成什么，用于追踪+复刻）
 */
public class 方块变化帧 {

    private final BlockPos 位置;
    private final BlockState 旧状态;
    private final BlockState 新状态;
    private final int tick序号;

    // ==================== 构造 ====================

    public 方块变化帧(BlockPos pos, BlockState oldState, BlockState newState, int tickIndex) {
        this.位置 = pos.immutable();
        this.旧状态 = oldState;
        this.新状态 = newState;
        this.tick序号 = tickIndex;
    }

    // ==================== Getter ====================

    public BlockPos 获取位置() { return 位置; }
    public BlockState 获取旧状态() { return 旧状态; }
    public BlockState 获取新状态() { return 新状态; }
    public int 获取tick序号() { return tick序号; }
}
