// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopMobMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 怪物 Mixin
 *
 * 职责：阻止目标切换
 */
@Mixin(Mob.class)
public abstract class TimestopMobMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetTarget(LivingEntity target, CallbackInfo ci) {Mob self = (Mob) (Object) this;
        TimeStop timeStop = (TimeStop) self.level();

        if (timeStop.puellamagi$shouldFreezeEntity(self)) {
            ci.cancel();
        }
    }
}
