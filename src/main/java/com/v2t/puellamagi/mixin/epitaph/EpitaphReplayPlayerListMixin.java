package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 回放期间让音效等"跳过自己"的广播也包含自己
 *
 * MC的音效系统：
 * Level.playSound(player, pos, sound)
 * → PlayerList.broadcast(player, x, y, z, range, dimension, packet)
 * → 跳过传入的player（因为客户端自己会播）
 *
 * 包回放时客户端不知道在操作 → 不会自己播→ 需要收到服务端的包
 *
 * 和ServerChunkCache.broadcast是完全不同的方法
 * 那个是实体相关的广播（挥手/受伤等）
 * 这个是位置相关的广播（音效/粒子等）
 */
@Mixin(PlayerList.class)
public class EpitaphReplayPlayerListMixin {

    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Inject(method = "broadcast", at = @At("RETURN"))
    private void epitaph$alsoSendToLockedPlayer(
            @Nullable Player excludedPlayer,
            double x, double y, double z, double range,
            ResourceKey<Level> dimension,Packet<?> packet,
            CallbackInfo ci) {

        // 没有排除的玩家 → 所有人都收到了→ 不需要补发
        if (excludedPlayer == null) return;

        // 不是服务端玩家
        if (!(excludedPlayer instanceof ServerPlayer sp)) return;

        // 不是被锁定的回放玩家
        if (!复刻引擎.玩家是否被锁定(sp.getUUID())) return;

        // 补发给被跳过的自己
        sp.connection.send(packet);
    }
}
