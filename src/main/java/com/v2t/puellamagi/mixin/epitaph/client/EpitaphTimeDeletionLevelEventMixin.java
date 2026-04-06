package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.时间删除客户端处理器;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时删期间A的客户端方块破坏效果拦截
 *
 * levelEvent(2001) = 方块破坏声音 + 粒子
 * 时删期间帧方块修正会在服务端执行destroyBlock → 广播levelEvent给所有玩家
 *
 * B的客户端需要收到（B看到的是"命运中的世界"，方块正常被破坏）
 * A的客户端需要拦截（A看到的是"真实世界"，方块不应该被破坏）
 *
 * 与 EpitaphTimeDeletionBlockMixin 配合：
 * - BlockMixin 拦截方块状态更新包（A看不到方块消失）
 * - 此Mixin 拦截破坏效果包（A听不到声音、看不到粒子）
 *
 * 不做业务逻辑，只做拦截和转发
 * 实际的忽略列表管理在 时间删除客户端处理器 中
 */
@Mixin(ClientPacketListener.class)
public class EpitaphTimeDeletionLevelEventMixin {

    /**
     * 拦截方块破坏效果
     *
     * levelEvent type=2001 = 方块破坏（声音+碎裂粒子）
     * 如果事件位置在忽略列表中 → cancel → A不会听到声音、看不到粒子
     */
    @Inject(method = "handleLevelEvent", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockDestroyEffect(ClientboundLevelEventPacket packet,
                                             CallbackInfo ci) {
        if (!时间删除客户端处理器.有忽略方块()) return;

        // 只拦截方块破坏效果（type=2001）
        // 其他levelEvent（如打开门=1005、烟花=2000等）不拦截
        if (packet.getType() != 2001) return;

        BlockPos pos = packet.getPos();
        if (时间删除客户端处理器.是否忽略方块(pos)) {
            ci.cancel();
        }
    }
}
