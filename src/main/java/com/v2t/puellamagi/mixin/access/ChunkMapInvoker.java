package com.v2t.puellamagi.mixin.access;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * ChunkMap 方法访问器
 */
@Mixin(ChunkMap.class)
public interface ChunkMapInvoker {

    @Invoker("updatePlayerStatus")
    void puellamagi$updatePlayerStatus(ServerPlayer player, boolean added);
}
