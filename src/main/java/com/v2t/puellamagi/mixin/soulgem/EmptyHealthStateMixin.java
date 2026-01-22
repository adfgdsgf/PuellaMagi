// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthStateMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 空血状态Mixin
 *
 * 核心：让空血的灵魂宝石系玩家，isDeadOrDying()返回false
 * 效果：原版认为玩家没死，不触发死亡UI、死亡动画
 *
 * 注意：不依赖空血假死标记，因为该标记在die()中设置，
 *       但isDeadOrDying()在die()之前就被调用
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

        // 血量<=0时，告诉原版"我没死"
        if (player.getHealth() <= 0) {
            cir.setReturnValue(false);
        }
    }
}
