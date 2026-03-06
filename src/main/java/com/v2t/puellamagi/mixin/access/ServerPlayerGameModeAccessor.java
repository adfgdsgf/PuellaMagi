package com.v2t.puellamagi.mixin.access;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ServerPlayerGameMode的字段访问器
 *
 * 用于录制开始时检查玩家是否正在挖方块
 */
@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {

    @Accessor("isDestroyingBlock")
    boolean puellamagi$isDestroyingBlock();

    @Accessor("destroyPos")
    BlockPos puellamagi$getDestroyPos();
}
