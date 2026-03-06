package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回放期间让破坏进度包不跳过操作者自己
 *
 * destroyBlockProgress()不走broadcast()
 * 它自己直接遍历玩家列表并跳过entityId==自己的
 * 所以EpitaphReplayBroadcastMixin覆盖不到这里
 * 需要单独处理
 */
@Mixin(ServerLevel.class)
public class EpitaphReplayServerLevelMixin {

    @Inject(method = "destroyBlockProgress", at = @At("RETURN"))
    private void epitaph$sendDestructionToLockedPlayer(int entityId, BlockPos pos,int progress, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;

        for (ServerPlayer player : self.players()) {
            if (player.getId() != entityId) continue;

            if (!复刻引擎.玩家是否被锁定(player.getUUID())) continue;

            player.connection.send(
                    new ClientboundBlockDestructionPacket(entityId, pos, progress)
            );break;
        }
    }
}
