// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthDeathAnimMixin.java

package com.v2t.puellamagi.mixin.soulgem.client;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截死亡动画，阻止假死玩家倒地
 */
@Mixin(LivingEntity.class)
public abstract class EmptyHealthDeathAnimMixin {

    @Shadow
    public int deathTime;

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void onTickDeath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide && self instanceof Player player) {
            if (假死状态处理器.客户端应该显示假死效果(player)) {
                this.deathTime = 0;ci.cancel();
            }
        }
    }
}
