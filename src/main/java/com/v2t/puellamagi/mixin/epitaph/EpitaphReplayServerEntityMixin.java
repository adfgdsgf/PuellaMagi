package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 只拦截帧驱动的实体（怪物等），不拦截被锁定玩家
 *
 * 帧驱动的实体：位置由帧数据驱动，传令兵的位置包会冲突 → 拦截
 *
 * 被锁定的玩家：状态由服务端正常处理（包回放）
 *传令兵需要同步：使用物品状态、装备变化、姿态变化等
 *   位置包虽然也会传，但客户端已有清除插值机制，不会拉扯
 */
@Mixin(ServerEntity.class)
public abstract class EpitaphReplayServerEntityMixin {

    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "sendChanges", at = @At("HEAD"), cancellable = true)
    private void epitaph$skipTrackingForControlled(CallbackInfo ci) {
        // 只拦截帧驱动的实体（怪物等）
        if (复刻引擎.实体是否被复刻控制(this.entity)) {
            ci.cancel();
        }

        // 被锁定玩家不拦截 → 传令兵正常工作
        // 使用物品/装备变化/姿态等状态自动同步给客户端
    }
}
