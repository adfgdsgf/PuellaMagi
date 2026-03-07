/*
package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import com.v2t.puellamagi.util.network.输入接管器;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

*/
/**
 * 拦截C2S包 — 通用录制 + 回放拦截
 *
 * 录制中：不拦截（让MC正常处理），但存一份原始包对象到录制管理器
 * 回放中（FULL模式）：拦截所有真实C2S包（玩家无法操作）
 *
 * 包分两类处理：
 * - 移动包：回放中拦截（输入回放管移动），录制时不存（帧数据管位置）
 * - 其他所有包：录制时存原始对象，回放时拦截
 *
 * 这样自动兼容所有mod的C2S包：
 * - 原版交互包 ✅
 * - TACZ射击包 ✅（如果走标准C2S流程）
 * - 其他mod技能包 ✅
 *//*

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class EpitaphInputInterceptMixin {

    @Shadow
    public ServerPlayer player;

    //==================== 移动类（只拦截，不录制） ====================

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

    // ==================== 交互类（录制 + 拦截） ====================

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    //==================== 容器类 ====================

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onContainerClose(ServerboundContainerClosePacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onContainerButton(ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onCreativeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlaceRecipe(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    //==================== Forge自定义包（特殊处理） ====================

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (puellamagi$录制中存自定义包(packet)) return;
        if (输入接管器.是否拦截(player.getUUID())) ci.cancel();
    }

    */
/**
     * 自定义包特殊处理：复制字节再存
     *
     * CustomPayload包内部有一次性字节缓冲区
     * MC处理完会销毁 → 回放时再读就崩了
     * 所以录制时复制一份字节 → 回放时用备份造新包
     *//*

    @Unique
    private boolean puellamagi$录制中存自定义包(ServerboundCustomPayloadPacket packet) {
        if (!录制管理器.玩家是否在录制中(player.getUUID())) {
            return false;
        }

        // 复制字节数据（在MC读取之前备份）
        net.minecraft.network.FriendlyByteBuf originalData = packet.getData();
        byte[] bytes = new byte[originalData.readableBytes()];
        originalData.getBytes(originalData.readerIndex(), bytes);

        // 存复制后的包
        录制管理器.接收自定义包(player.getUUID(),
                packet.getIdentifier(),
                bytes
        );

        return true;
    }

    // ==================== 工具方法 ====================

    */
/**
     * 如果正在录制，存一份包到录制管理器
     *
     * @return true = 正在录制（调用方不要拦截，让MC正常处理）
     *         false = 没在录制
     *//*

    @Unique
    private boolean puellamagi$录制中存包(net.minecraft.network.protocol.Packet<?> packet) {
        if (!录制管理器.玩家是否在录制中(player.getUUID())) {
            return false;
        }
        录制管理器.接收C2S包(player.getUUID(), packet);
        return true;
    }
}
*/
