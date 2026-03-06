package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Level层拦截被复刻控制的实体tick
 *
 * 与时停架构相同的原理：
 * - 时停：guardEntityTick中cancel → 实体不tick → 保持冻结
 * - 复刻：guardEntityTick中cancel → 实体不tick → 由帧数据驱动状态
 *
 * 两个Mixin可共存：都注入guardEntityTick的HEAD，任一cancel即生效
 * 优先级：时停先检查也没关系（被时停冻结的实体不应被复刻驱动）
 */
@Mixin(Level.class)
public abstract class EpitaphReplayLevelMixin {

    @Shadow
    public abstract boolean isClientSide();

    /**
     * 拦截被复刻控制的实体的tick执行
     *
     * guardEntityTick 是所有实体tick的统一入口
     * cancel后实体的AI、物理、自然逻辑全部不执行
     * 状态完全由复刻引擎.tick() 中的帧数据应用决定
     */
    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    private void epitaph$cancelControlledEntityTick(Consumer<Entity> consumer,Entity entity,
                                                    CallbackInfo ci) {
        // 只在服务端拦截
        // 客户端实体的状态由服务端通过网络包同步
        if (!this.isClientSide() && 复刻引擎.实体是否被复刻控制(entity)) {
            ci.cancel();
        }
    }
}
