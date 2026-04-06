package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ServerEntity.sendChanges 拦截
 *
 * 双重职责：
 *
 * 1. 帧驱动的实体（怪物等）：cancel sendChanges，位置由帧数据驱动
 *
 * 2. 时删使用者：sendChanges位置欺骗
 *    - 发包前：暂存真实位置 → 设为命运位置
 *    - MC正常执行sendChanges → 发命运位置给所有viewer（包括B）
 *    - 发包后：恢复真实位置
 *    → B看到A在命运位置走路
 *    → A客户端由拦截Mixin忽略命运位置包，保持真实位置
 *    → sendChanges内部lastSentXxx正常更新为命运位置
 *
 * 被锁定的玩家不拦截 → 传令兵正常同步使用物品/装备/姿态
 */
@Mixin(ServerEntity.class)
public abstract class EpitaphReplayServerEntityMixin {

    @Shadow
    @Final
    private Entity entity;

    // ==================== 位置欺骗暂存 ====================

    /**
     * 暂存的真实位置（sendChanges期间临时使用）
     * 仅在当前线程当前方法内有效，不存在并发问题
     */
    @Unique
    private double puellamagi$realX, puellamagi$realY, puellamagi$realZ;
    @Unique
    private float puellamagi$realYRot, puellamagi$realXRot;
    @Unique
    private float puellamagi$realYBodyRot, puellamagi$realYHeadRot;
    @Unique
    private boolean puellamagi$spoofing = false;

    @Inject(method = "sendChanges", at = @At("HEAD"), cancellable = true)
    private void epitaph$sendChangesHead(CallbackInfo ci) {
        // 帧驱动的实体（怪物等）→ 拦截
        if (复刻引擎.实体是否被复刻控制(this.entity)) {
            ci.cancel();
            return;
        }

        // 时删使用者 → 位置欺骗
        复刻引擎.命运位置 destiny = 复刻引擎.获取命运位置(this.entity.getUUID());
        if (destiny != null) {
            // 暂存真实位置
            puellamagi$realX = this.entity.getX();
            puellamagi$realY = this.entity.getY();
            puellamagi$realZ = this.entity.getZ();
            puellamagi$realYRot = this.entity.getYRot();
            puellamagi$realXRot = this.entity.getXRot();
            if (this.entity instanceof LivingEntity living) {
                puellamagi$realYBodyRot = living.yBodyRot;
                puellamagi$realYHeadRot = living.yHeadRot;
            }

            // 设为命运位置
            this.entity.setPos(destiny.x, destiny.y, destiny.z);
            this.entity.setYRot(destiny.yRot);
            this.entity.setXRot(destiny.xRot);
            if (this.entity instanceof LivingEntity living) {
                living.yBodyRot = destiny.yBodyRot;
                living.yHeadRot = destiny.yHeadRot;
                living.setYHeadRot(destiny.yHeadRot);
            }

            puellamagi$spoofing = true;
            // 不cancel → 让MC正常执行sendChanges → 发命运位置给所有viewer
        }
    }

    @Inject(method = "sendChanges", at = @At("RETURN"))
    private void epitaph$sendChangesReturn(CallbackInfo ci) {
        // 位置欺骗结束：恢复真实位置
        if (puellamagi$spoofing) {
            this.entity.setPos(puellamagi$realX, puellamagi$realY, puellamagi$realZ);
            this.entity.setYRot(puellamagi$realYRot);
            this.entity.setXRot(puellamagi$realXRot);
            if (this.entity instanceof LivingEntity living) {
                living.yBodyRot = puellamagi$realYBodyRot;
                living.yHeadRot = puellamagi$realYHeadRot;
                living.setYHeadRot(puellamagi$realYHeadRot);
            }

            puellamagi$spoofing = false;
        }
    }
}
