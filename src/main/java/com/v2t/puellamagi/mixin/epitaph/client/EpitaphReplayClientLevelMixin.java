package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import com.v2t.puellamagi.client.客户端复刻管理器;
import com.v2t.puellamagi.util.recording.实体帧数据;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 客户端Level层 — 复刻帧驱动
 *
 * 三种模式：
 *
 * 1. 被控制实体（怪物/投射物等）→ cancel tick + 帧数据完整驱动
 *    cancel tick后，由帧数据设置位置/朝向/NBT状态/WalkAnimationState
 *
 * 2. 本地玩家 → 不cancel tick，输入回放（KeyboardInput/Mouse Mixin劫持输入）
 *    MC自己计算bob/walkDist/手部位置等所有内部状态
 *
 * 3. 被锁定玩家（非本地）→ 完全不干预，让MC原生系统处理
 *    服务端对被锁定玩家正常tick（由输入帧驱动），位置通过sendChanges()发给客户端
 *    客户端通过原生lerp系统平滑移动 + calculateEntityAnimation()计算走路动画
 *    → 走路/挥手/使用物品等所有动画由MC自己处理，最稳妥
 */
@Mixin(Level.class)
public abstract class EpitaphReplayClientLevelMixin {

    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    private void epitaph$clientReplayEntityTick(Consumer<Entity> consumer,
                                                Entity entity,
                                                CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide()) return;

        // ======== 本地玩家：跳过，不cancel tick ========
        // 本地玩家的输入由KeyboardInput/Mouse Mixin 替换
        // MC自己tick → 所有动画状态自动正确
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null &&
                entity.getUUID().equals(localPlayer.getUUID()) &&
                (客户端复刻管理器.本地玩家是否输入回放中() || 客户端复刻管理器.是否时间删除自由())) {
            if (localPlayer instanceof ILivingEntityAccess access) {
                access.puellamagi$setLerpSteps(0);
            }
            return;
        }

        // 不在帧数据中的实体 → 不干预
        if (!客户端复刻管理器.实体是否被控制(entity.getUUID())) return;

        // ======== 被锁定玩家：完全不干预 ========
        // 服务端对被锁定玩家正常tick（由输入帧驱动），不cancel tick
        // 位置/状态通过MC原生的sendChanges()发给客户端
        // 客户端通过lerp平滑移动 + calculateEntityAnimation()正常计算走路动画
        // 不需要帧数据驱动 — MC自己的同步系统处理一切
        if (客户端复刻管理器.实体是否被锁定玩家(entity.getUUID())) {
            return;
        }

        // ======== 被控制实体（怪物/投射物等）：cancel tick + 帧数据驱动 ========
        实体帧数据 当前帧 = 客户端复刻管理器.获取当前帧(entity.getUUID());
        实体帧数据 上一帧 = 客户端复刻管理器.获取上一帧(entity.getUUID());

        if (当前帧 != null) {
            if (entity instanceof LivingEntity living) {
                当前帧.应用到活体(living, 上一帧);

                // 清除lerp残留
                if (living instanceof ILivingEntityAccess access) {
                    access.puellamagi$setLerpSteps(0);
                }
            } else {
                当前帧.应用到普通实体(entity, 上一帧);
            }
        }

        ci.cancel();
    }
}
