// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthDeathMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡拦截 Mixin
 *
 * 职责：拦截 die() 防止真正死亡
 * 例外：致命伤害（/kill、虚空）时允许死亡
 */
@Mixin(LivingEntity.class)
public abstract class EmptyHealthDeathMixin {

    @Inject(
            method = "die",
            at = @At("HEAD"),
            cancellable = true
    )
    private void puellamagi$onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        if (!能力工具.是灵魂宝石系(player)) return;

        if (能力工具.应该跳过限制(player)) return;

        // 致命伤害：允许死亡，清除标记
        if (能力工具.是致命伤害(source) || 假死状态处理器.是致命伤害中(player.getUUID())) {
            假死状态处理器.清除致命伤害标记(player.getUUID());
            return;  // 不拦截，让玩家正常死亡
        }

        // 取消死亡（进入假死）
        ci.cancel();
    }
}
