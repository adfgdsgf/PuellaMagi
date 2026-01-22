// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthDeathMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡拦截Mixin
 *
 * 职责：拦截die()防止真正死亡
 * 假死触发已移至 EmptyHealthImmunityMixin 的 hurt() 中
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

        // 不可拦截的伤害
        if (puellamagi$应该允许死亡(source)) {
            return;
        }

        // 取消死亡
        ci.cancel();
    }

    @Unique
    private boolean puellamagi$应该允许死亡(DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return true;
        }
        if (source.is(DamageTypes.GENERIC_KILL)) {
            return true;
        }
        return false;
    }
}
