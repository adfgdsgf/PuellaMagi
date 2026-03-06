package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.交互包帧;
import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import com.v2t.puellamagi.util.network.输入接管器;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截C2S包— 录制和回放双重职责
 *
 * 录制中（玩家正常操作）：
 *   不拦截包（让MC正常处理）
 *   但把包的关键数据存一份到录制管理器
 *
 * 回放中（FULL模式接管）：
 *   拦截所有真实的C2S包（玩家无法操作）
 *   交互包由复刻引擎按tick重放（精确坐标）
 *   移动包由帧驱动处理
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class EpitaphInputInterceptMixin {

    @Shadow
    public ServerPlayer player;

    //==================== 移动类 ====================

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    // ==================== 交互类（录制 +拦截） ====================

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        //录制中：存一份，不拦截
        if (puellamagi$是否录制中()) {
            BlockHitResult hit = packet.getHitResult();
            录制管理器.接收交互包(player.getUUID(), new 交互包帧.右键方块包(
                    hit.getBlockPos(),
                    hit.getDirection(),
                    packet.getHand(),
                    hit.getLocation(),
                    packet.getSequence(),
                    hit.isInside()
            ));
            // 不cancel，让MC正常处理
            return;
        }

        // 回放中：拦截真实操作
        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (puellamagi$是否录制中()) {
            录制管理器.接收交互包(player.getUUID(), new 交互包帧.玩家动作包(
                    packet.getPos(),
                    packet.getDirection(),
                    packet.getAction().ordinal(),
                    packet.getSequence()
            ));
            return;
        }

        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (puellamagi$是否录制中()) {
            录制管理器.接收交互包(player.getUUID(), new 交互包帧.使用物品包(
                    packet.getHand(),
                    packet.getSequence()
            ));
            return;
        }

        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (puellamagi$是否录制中()) {
            // InteractPacket比较特殊，没有直接的getter
            // 但我们可以通过packet的dispatch来获取信息
            // 简化处理：记录实体ID和手
            // 注意：ServerboundInteractPacket的内部结构较复杂
            // 先记录基础信息，后续如果需要再扩展
            return;
        }

        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (puellamagi$是否录制中()) {
            录制管理器.接收交互包(player.getUUID(), new 交互包帧.挥手包(
                    packet.getHand()
            ));
            return;
        }

        if (输入接管器.是否拦截(player.getUUID())) {
            ci.cancel();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 当前玩家是否在录制中
     */
    @Unique
    private boolean puellamagi$是否录制中() {
        return 录制管理器.玩家是否在录制中(player.getUUID());
    }
}
