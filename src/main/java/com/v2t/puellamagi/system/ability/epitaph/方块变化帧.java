package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 方块变化帧
 *
 * 记录某一tick中发生的方块变化
 * 用于录制/回放期间追踪方块变化
 *
 * 与方块快照的区别：
 * - 方块快照：某一时刻的静态状态（用于回滚）
 * - 方块变化帧：变化事件（从什么变成什么，用于追踪+复刻）
 *
 * 触发者UUID：
 * - 时间删除用→ 标记这个方块变化是谁造成的
 * - null表示非玩家触发（红石/水流/自然变化等）
 */
public class 方块变化帧 {

    private final BlockPos 位置;
    private final BlockState 旧状态;
    private final BlockState 新状态;
    private final int tick序号;
    @Nullable
    private final UUID 触发者UUID;

    //==================== 构造 ====================

    public 方块变化帧(BlockPos pos, BlockState oldState, BlockState newState, int tickIndex) {
        this(pos, oldState, newState, tickIndex, null);
    }

    public 方块变化帧(BlockPos pos, BlockState oldState, BlockState newState, int tickIndex,@Nullable UUID triggerUUID) {
        this.位置 = pos.immutable();
        this.旧状态 = oldState;
        this.新状态 = newState;
        this.tick序号 = tickIndex;
        this.触发者UUID = triggerUUID;
    }

    // ==================== Getter ====================

    public BlockPos 获取位置() { return 位置; }
    public BlockState 获取旧状态() { return 旧状态; }
    public BlockState 获取新状态() { return 新状态; }
    public int 获取tick序号() { return tick序号; }
    @Nullable
    public UUID 获取触发者UUID() { return 触发者UUID; }
}
