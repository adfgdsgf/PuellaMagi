package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import com.v2t.puellamagi.util.实体工具;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

/**
 * ServerEntity.sendChanges 拦截
 *
 * 三重职责：
 *
 * 1. 帧驱动的实体（怪物等）：cancel sendChanges，位置由帧数据驱动
 *
 * 2. 时删使用者：sendChanges位置欺骗（对普通观察者发命运位置）
 *    - 发包前：暂存真实位置 → 设为命运位置
 *    - MC正常执行sendChanges → 发命运位置给所有viewer
 *    - 发包后：恢复真实位置
 *    → 普通viewer看到A在命运位置走路
 *    → A客户端由拦截Mixin忽略命运位置包，保持真实位置
 *    → sendChanges内部lastSentXxx正常更新为命运位置
 *
 * 3. 多时删互相可见性修正：
 *    - sendChanges后向同会话中其他时删使用者发送真实位置teleport包
 *    - 覆盖sendChanges发出的命运位置 → 时删使用者之间看到彼此的真实位置
 *    - teleport包在同一server tick内发出，客户端同帧处理，无闪烁
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

            // 多时删互相可见性修正：给同会话中的其他时删使用者发真实位置
            puellamagi$给时删观察者发送真实位置();

            puellamagi$spoofing = false;
        }
    }

    // ==================== 多时删互相可见性修正 ====================

    /**
     * 向同会话中其他时删使用者发送真实位置的teleport包
     *
     * 为什么不会交替闪烁（命运位置/真实位置）：
     *
     * 同一server tick中sendChanges发出的move包和此处发出的teleport包，
     * 通过同一TCP连接按顺序到达客户端。客户端在同一个tick()中批量处理
     * 所有pending的网络包。两个包都调用entity.lerpTo()设置lerp目标，
     * 后到的teleport包完全覆盖前面move包的lerp目标。
     *
     * 最终lerp目标 = 真实位置，不存在"上一帧命运位置、下一帧真实位置"的交替。
     *
     * 时序：
     * 1. sendChanges HEAD：实体位置 → 命运位置
     * 2. MC执行sendChanges → 发命运位置的move包给所有viewer
     * 3. sendChanges RETURN：实体位置 → 真实位置（已恢复）
     * 4. 此方法：给其他时删使用者单独发真实位置teleport包
     * 5. 客户端同一tick处理 → move包的lerpTo被teleport包的lerpTo覆盖
     *
     * 效果：
     * - 普通观察者：只有move包生效 → 看到命运位置（正确）
     * - 时删观察者B：teleport包覆盖move包 → 看到A的真实位置（正确）
     * - A自己：客户端由EpitaphTimeDeletionPacketMixin拦截自己的位置包（不受影响）
     */
    @Unique
    private void puellamagi$给时删观察者发送真实位置() {
        UUID 实体UUID = this.entity.getUUID();

        // 获取同会话中所有时删玩家
        Set<UUID> 时删玩家集合 = 复刻引擎.获取同会话时删玩家(实体UUID);
        if (时删玩家集合.size() <= 1) {
            // 只有自己或没有 → 无需修正
            return;
        }

        if (!(this.entity.level() instanceof ServerLevel 服务端维度)) {
            return;
        }

        // 构建真实位置的teleport包（实体位置已恢复为真实位置）
        ClientboundTeleportEntityPacket 真实位置包 =
                new ClientboundTeleportEntityPacket(this.entity);

        // 给同会话中的其他时删使用者发送
        for (UUID 观察者UUID : 时删玩家集合) {
            // 跳过自己（自己的客户端已有拦截Mixin处理）
            if (观察者UUID.equals(实体UUID)) continue;

            Entity 观察者实体 = 实体工具.按UUID查找实体(服务端维度, 观察者UUID);
            if (观察者实体 instanceof ServerPlayer 观察者玩家) {
                观察者玩家.connection.send(真实位置包);
            }
        }
    }
}
