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
 * 客户端Level层— 复刻帧驱动
 *
 * 两种模式并行：
 *
 * 1. 怪物/其他玩家→ 帧数据驱动（cancel tick，直接设实体状态）
 *和时停原理相同，够用
 *
 * 2. 本地玩家 → 输入回放（不cancel tick，让MC自己算）
 *    由KeyboardInput/Mouse Mixin 劫持输入
 *MC自己计算 bob、walkDist、手部位置等所有内部状态
 *    解决帧数据驱动无法覆盖MC全部内部状态的问题
 */
@Mixin(Level.class)
public abstract class EpitaphReplayClientLevelMixin {

    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    private void epitaph$clientReplayEntityTick(Consumer<Entity> consumer,
                                                Entity entity,
                                                CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide()) return;

        //========本地玩家：跳过，不cancel tick ========
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

        // ======== 其他实体：帧数据驱动 ========
        if (!客户端复刻管理器.实体是否被控制(entity.getUUID())) return;

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
