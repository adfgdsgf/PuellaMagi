// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopClientPacketMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端网络包拦截
 *
 * 只拦截被冻结的 LivingEntity 的动画包
 * 解决多人游戏中被冻结生物动作变化的问题
 */
@Mixin(ClientPacketListener.class)
public class TimestopClientPacketMixin {

    /**
     * 拦截受伤动画包
     */
    @Inject(method = "handleHurtAnimation", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onHurtAnimation(ClientboundHurtAnimationPacket packet, CallbackInfo ci) {
        if (puellamagi$shouldBlockLivingEntityPacket(packet.id())) {
            ci.cancel();
        }
    }

    /**
     * 拦截实体动画包（挥手等）
     */
    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAnimate(ClientboundAnimatePacket packet, CallbackInfo ci) {
        if (puellamagi$shouldBlockLivingEntityPacket(packet.getId())) {
            ci.cancel();
        }
    }

    /**
     * 只拦截被冻结的LivingEntity
     */
    @Unique
    private boolean puellamagi$shouldBlockLivingEntityPacket(int entityId) {
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

        // 只处理 LivingEntity
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        // 时停者不拦截
        if (timeStop.puellamagi$isTimeStopper(entity)) {
            return false;
        }

        return timeStop.puellamagi$shouldFreezeEntity(entity);
    }
}
