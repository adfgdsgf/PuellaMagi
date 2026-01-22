// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthAggroMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 空血假死仇恨Mixin
 *
 * 职责：
 * - 阻止怪物将空血/假死的玩家设为目标
 * - 清除已有仇恨
 *
 * 注意：不只检查空血假死标记，还检查血量<=0，
 *       因为标记可能还没设置
 */
@Mixin(Mob.class)
public abstract class EmptyHealthAggroMixin {

    @Shadow
    public abstract LivingEntity getTarget();

    /**
     * 拦截设置目标
     */
    @Inject(
            method = "setTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    private void puellamagi$onSetTarget(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;

        if (puellamagi$不应被攻击(target)) {
            ci.cancel();
        }
    }

    /**
     * 每tick检查当前目标
     */
    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void puellamagi$onTick(CallbackInfo ci) {Mob self = (Mob) (Object) this;
        LivingEntity target = this.getTarget();

        if (target != null && puellamagi$不应被攻击(target)) {
            self.setTarget(null);
        }
    }

    /**
     * 判断目标是否不应被攻击
     *
     * 条件：灵魂宝石系玩家且（血量<=0 或 假死中）
     */
    @Unique
    private boolean puellamagi$不应被攻击(LivingEntity target) {
        if (!(target instanceof Player player)) return false;

        if (!能力工具.是灵魂宝石系(player)) return false;

        // 血量<=0 或 假死中，都不应被攻击
        return player.getHealth() <= 0 || 能力工具.是否假死中(player);
    }
}
