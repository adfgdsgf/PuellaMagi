package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时删期间客户端位置包拦截
 *
 * 时删期间sendChanges位置欺骗会把A的命运位置发给所有viewer
 * A自己也会收到 → MC的lerp系统会把A拉到命运位置 → A看到自己被拉回去
 *
 * 此Mixin在A的客户端上拦截这些位置包：
 * - 时删自由状态下，忽略针对本地玩家的移动/传送包
 * - A保持在真实位置自由行动
 *
 * 不影响其他实体的位置包（B看到的怪物等正常接收）
 */
@Mixin(ClientPacketListener.class)
public class EpitaphTimeDeletionPacketMixin {

    /**
     * 拦截相对移动包
     *
     * MC用这种包发送小范围移动（delta < 8格）
     * 时删自由状态下本地玩家的移动包 = 位置欺骗发出的命运位置
     * → 必须忽略，否则A会被拉到命运位置
     */
    @Inject(method = "handleMoveEntity", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockMoveEntityForLocalPlayer(ClientboundMoveEntityPacket packet,
                                                       CallbackInfo ci) {
        if (!客户端复刻管理器.是否时间删除自由()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 获取包的目标实体
        Entity target = packet.getEntity(mc.level);
        if (target == null) return;

        // 只拦截本地玩家的位置包
        if (target.getId() == mc.player.getId()) {
            ci.cancel();
        }
    }

    /**
     * 拦截绝对传送包
     *
     * MC用这种包发送大范围移动（delta >= 8格）
     * 时删自由状态下同理需要忽略
     */
    @Inject(method = "handleTeleportEntity", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockTeleportForLocalPlayer(ClientboundTeleportEntityPacket packet,
                                                     CallbackInfo ci) {
        if (!客户端复刻管理器.是否时间删除自由()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 只拦截本地玩家的传送包
        if (packet.getId() == mc.player.getId()) {
            ci.cancel();
        }
    }
}
