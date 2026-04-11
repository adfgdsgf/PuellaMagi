package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时删期间客户端包拦截
 *
 * 时删期间A在服务端按命运走，但A的客户端已自由。
 * 服务端会通过各种包同步A的命运状态给A的客户端，需要拦截：
 *
 * 1. 位置包（MoveEntity/TeleportEntity）→ 防止A被拉到命运位置
 * 2. 实体数据包（SetEntityData）→ 防止命运中的状态（onGround/fallDistance等）
 *    触发A客户端本地的落地音效/粒子等副作用
 *
 * 只拦截针对本地玩家的包，不影响其他实体
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

        Entity target = packet.getEntity(mc.level);
        if (target == null) return;

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

        if (packet.getId() == mc.player.getId()) {
            ci.cancel();
        }
    }

    /**
     * 拦截实体数据包
     *
     * MC用这种包同步实体的DataAccessor值（onGround、pose、SharedFlags等）
     * 时删期间命运中A的状态变化（如跳跃后落地onGround=true）会通过此包发来
     * → A的客户端收到后触发本地的落地音效和粒子 → 不应该有
     *
     * 拦截后A的客户端不会收到命运中自己的状态变化
     * A自己客户端的LocalPlayer状态由本地tick自己管理
     */
    @Inject(method = "handleSetEntityData", at = @At("HEAD"), cancellable = true)
    private void epitaph$blockEntityDataForLocalPlayer(ClientboundSetEntityDataPacket packet,
                                                       CallbackInfo ci) {
        if (!客户端复刻管理器.是否时间删除自由()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (packet.id() == mc.player.getId()) {
            ci.cancel();
        }
    }
}
