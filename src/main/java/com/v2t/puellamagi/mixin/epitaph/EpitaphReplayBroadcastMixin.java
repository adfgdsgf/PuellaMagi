package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回放期间让"跳过自己"的广播也包含自己
 *
 * MC正常逻辑：
 * broadcast(entity, packet) → 发给追踪者，跳过entity自己
 * 因为正常游戏客户端自己已经知道了
 *
 *包回放时：
 * 客户端不知道在做什么操作（按键没注入）
 * 需要收到服务端的S2C包才能显示效果
 *
 * 修复：broadcast()执行后，检查entity是否是被锁定的回放玩家
 * 如果是 → 补发一份给自己
 *
 * 一个Mixin覆盖所有"跳过自己"的场景：
 * 挥手动画、破坏粒子、攻击音效、使用物品效果...
 * 不需要逐个查找逐个补发
 */
@Mixin(ServerChunkCache.class)
public class EpitaphReplayBroadcastMixin {

    @Shadow
    @Final
    private ChunkMap chunkMap;

    @Inject(method = "broadcast", at = @At("RETURN"))
    private void epitaph$alsoSendToLockedPlayer(Entity entity, Packet<?> packet,CallbackInfo ci) {
        // 只处理玩家
        if (!(entity instanceof ServerPlayer player)) return;

        // 只处理被锁定的回放玩家
        if (!复刻引擎.玩家是否被锁定(player.getUUID())) return;

        // 补发给自己（原方法跳过了自己）
        player.connection.send(packet);
    }
}
