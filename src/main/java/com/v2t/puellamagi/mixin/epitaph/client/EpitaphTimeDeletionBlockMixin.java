package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.时间删除客户端处理器;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时删期间A的客户端方块更新拦截
 *
 * 时删期间帧方块修正会在服务端执行A的操作（如挖方块），
 * MC原版会将这些方块变化通过方块更新包发给所有玩家。
 *
 * 此Mixin在A的客户端上拦截方块更新包：
 * - 检查方块位置是否在"忽略列表"中
 * - 在列表中 → cancel（A看不到这个方块变化 → 方块对A来说还在）
 * - 不在列表中 → 放行（正常方块变化）
 *
 * 不做业务逻辑，只做拦截和转发
 * 实际的忽略列表管理在 时间删除客户端处理器 中
 */
@Mixin(ClientPacketListener.class)
public class EpitaphTimeDeletionBlockMixin {

    /**
     * 拦截单方块更新包
     *
     * MC用 ClientboundBlockUpdatePacket 通知客户端某个方块变化了
     * 如果该位置在忽略列表中 → cancel → A不会看到方块被破坏/放置
     */
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockSingleBlockUpdate(ClientboundBlockUpdatePacket packet,
                                                 CallbackInfo ci) {
        if (!时间删除客户端处理器.有忽略方块()) return;

        BlockPos pos = packet.getPos();
        if (时间删除客户端处理器.是否忽略方块(pos)) {
            ci.cancel();
        }
    }

    /**
     * 拦截批量方块更新包（区段级别）
     *
     * MC用 ClientboundSectionBlocksUpdatePacket 通知一个区段内的多个方块变化
     * 如果其中任何方块在忽略列表中 → 需要特殊处理
     *
     * 策略：检查整个区段是否有任何忽略方块
     * - 没有 → 直接放行（性能最优）
     * - 有 → cancel整个包，然后手动处理：忽略列表中的方块跳过，其余正常应用
     */
    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockSectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet,
                                                   CallbackInfo ci) {
        if (!时间删除客户端处理器.有忽略方块()) return;

        // 遍历更新列表检查是否有需要忽略的
        boolean 有忽略 = 时间删除客户端处理器.区段包含忽略方块(packet);

        if (有忽略) {
            // cancel原包，手动应用非忽略的方块变化
            ci.cancel();
            时间删除客户端处理器.过滤并应用区段方块更新(packet);
        }
    }
}
