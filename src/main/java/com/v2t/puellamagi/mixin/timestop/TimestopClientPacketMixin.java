// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopClientPacketMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端网络包拦截
 *
 * 阻止时停范围内实体的状态更新包生效
 * 解决多人游戏中被冻结实体动作变化的问题
 */
@Mixin(ClientPacketListener.class)
public class TimestopClientPacketMixin {

    /**
     * 拦截受伤动画包
     */
    @Inject(method = "handleHurtAnimation", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onHurtAnimation(ClientboundHurtAnimationPacket packet, CallbackInfo ci) {
        if (shouldBlockEntityPacket(packet.id())) {
            ci.cancel();
        }
    }

    /**
     * 拦截实体动画包（挥手等）
     */
    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAnimate(ClientboundAnimatePacket packet, CallbackInfo ci) {
        if (shouldBlockEntityPacket(packet.getId())) {
            ci.cancel();
        }
    }

    /**
     * 拦截实体速度包
     */
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (shouldBlockEntityPacket(packet.getId())) {
            ci.cancel();
        }
    }

    /**
     * 拦截实体数据包（部分数据）
     *
     * 注意：这个比较复杂，可能需要细分哪些数据应该拦截
     */
    @Inject(method = "handleSetEntityData", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetEntityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        if (shouldBlockEntityPacket(packet.id())) {
            // 只在时停中拦截，允许时停结束后的同步
            ci.cancel();
        }
    }

    /**
     * 判断是否应该阻止该实体的状态更新
     */
    private boolean shouldBlockEntityPacket(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }

        TimeStop timeStop = (TimeStop) mc.level;
        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return false;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        // 时停者的更新不拦截
        if (timeStop.puellamagi$isTimeStopper(entity)) {
            return false;
        }

        // 被冻结的实体，拦截其状态更新
        return timeStop.puellamagi$shouldFreezeEntity(entity);
    }
}
