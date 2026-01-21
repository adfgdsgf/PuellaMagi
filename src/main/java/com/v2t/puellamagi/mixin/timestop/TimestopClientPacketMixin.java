// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopClientPacketMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停豁免系统;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
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
 *拦截时停中被冻结实体的动画/伤害包
 * 防止被冻结的实体姿势变化
 */
@Mixin(ClientPacketListener.class)
public class TimestopClientPacketMixin {

    /**
     * 拦截受伤动画包
     */
    @Inject(method = "handleHurtAnimation", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onHurtAnimation(ClientboundHurtAnimationPacket packet, CallbackInfo ci) {
        if (puellamagi$shouldBlockAnimationPacket(packet.id())) {
            ci.cancel();
        }
    }

    /**
     * 拦截实体动画包（挥手等）
     */
    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onAnimate(ClientboundAnimatePacket packet, CallbackInfo ci) {
        if (puellamagi$shouldBlockAnimationPacket(packet.getId())) {
            ci.cancel();
        }
    }

    /**
     * 拦截伤害事件包（1.19.4+新增，影响实体姿势）
     */
    @Inject(method = "handleDamageEvent", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        if (puellamagi$shouldBlockAnimationPacket(packet.entityId())) {
            ci.cancel();
        }
    }

    /**
     * 判断是否应该拦截动画包
     *
     * 拦截条件：
     * 1. 存在时停
     * 2. 实体是LivingEntity
     * 3. 实体的tick被冻结（shouldFreezeEntity返回true）
     */
    @Unique
    private boolean puellamagi$shouldBlockAnimationPacket(int entityId) {
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

        // 使用豁免系统判断：需要冻结(tick)的实体才拦截动画
        return 时停豁免系统.应该冻结(entity);
    }
}
