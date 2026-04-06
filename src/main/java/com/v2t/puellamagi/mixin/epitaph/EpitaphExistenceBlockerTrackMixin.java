package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.util.network.存在屏蔽器;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * 存在屏蔽器的tracker层面实现
 *
 * 注入 ChunkMap$TrackedEntity.updatePlayer(ServerPlayer)
 * 在MC决定是否track实体之前，检查存在屏蔽器的规则
 *
 * 为什么在这里而不是在 sendChanges：
 * - sendChanges被cancel → ServerEntity内部lastSentXxx不更新
 * - 屏蔽解除后MC算出巨大的delta → 观察者看到瞬移
 * - 在tracker层面拦截：sendChanges正常执行，内部状态保持同步
 * - 被屏蔽的viewer不在seenBy中 → MC自然不给他发包
 * - 屏蔽解除后viewer重新加入seenBy → MC发完整的spawn包（和正常进入范围一样）
 *
 * 效果：
 * - 被屏蔽实体对指定viewer完全不可见（和走出追踪范围一样）
 * - sendChanges正常执行 → 内部状态不脱节
 * - 屏蔽解除后正常重新追踪 → 无瞬移
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class EpitaphExistenceBlockerTrackMixin {

    @Shadow
    @Final
    Entity entity;

    @Shadow
    @Final
    ServerEntity serverEntity;

    @Shadow
    @Final
    Set<net.minecraft.server.network.ServerPlayerConnection> seenBy;

    /**
     * 在 updatePlayer 的HEAD拦截
     *
     * updatePlayer(ServerPlayer) 是MC每tick检查"是否应该track该玩家"的方法
     * 如果存在屏蔽器指示该viewer不应该看到该实体 → 直接return不执行后续逻辑
     * 同时如果该viewer已经在seenBy中 → 移除并发removePairing（让实体从viewer客户端消失）
     */
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockTrackingForShieldedViewers(ServerPlayer viewer, CallbackInfo ci) {
        if (this.entity == null) return;

        // 检查该实体是否对此viewer被屏蔽
        if (!存在屏蔽器.是否屏蔽(this.entity.getUUID(), viewer.getUUID())) {
            return;
        }

        // 被屏蔽 → 如果viewer已经在seenBy中，发removePairing让客户端移除实体
        if (this.seenBy.remove(viewer.connection)) {
            this.serverEntity.removePairing(viewer);
        }

        // cancel整个updatePlayer → MC不会重新把viewer加回seenBy
        ci.cancel();
    }
}
