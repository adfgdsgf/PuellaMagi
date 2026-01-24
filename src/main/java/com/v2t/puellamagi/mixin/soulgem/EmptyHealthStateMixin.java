// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthStateMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 空血状态 Mixin
 *
 * 核心：让空血的灵魂宝石系玩家，isDeadOrDying() 返回 false
 * 效果：原版认为玩家没死，不触发死亡 UI、死亡动画
 *
 * 例外：致命伤害（/kill、虚空）时不拦截，让玩家正常死亡
 */
@Mixin(LivingEntity.class)
public abstract class EmptyHealthStateMixin {

    @Inject(method = "isDeadOrDying",
            at = @At("HEAD"),
            cancellable = true
    )
    private void puellamagi$onIsDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;

        // 只处理灵魂宝石系
        if (!能力工具.是灵魂宝石系(player)) return;

        // 创造模式不拦截
        if (能力工具.应该跳过限制(player)) return;

        // 致命伤害中：不拦截，让玩家正常死亡
        if (假死状态处理器.是致命伤害中(player.getUUID())) {
            return;  // 返回原版结果（true）
        }

        // 血量 <= 0 时，告诉原版"我没死"
        if (player.getHealth() <= 0) {
            cir.setReturnValue(false);
        }
    }
}
