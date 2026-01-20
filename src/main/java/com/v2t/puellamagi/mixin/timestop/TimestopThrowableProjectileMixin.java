// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopThrowableProjectileMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停投射物处理;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ThrowableProjectile Mixin
 *
 * 处理投掷物（雪球、末影珍珠等）在时停中的行为
 * 对标 Roundabout 的 TimeStopThrowableProjectile
 */
@Mixin(ThrowableProjectile.class)
public abstract class TimestopThrowableProjectileMixin extends Entity {

    public TimestopThrowableProjectileMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * 时停中创建的投掷物使用自定义 tick
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTick(CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this.level();
        IProjectileAccess access = (IProjectileAccess) this;

        // 在时停范围内 且 是时停中创建的投射物
        if (timeStop.puellamagi$inTimeStopRange(this) && access.puellamagi$isTimeStopCreated()) {
            // 调用 Entity.tick()
            super.tick();
            // 使用自定义惯性处理
            时停投射物处理.tick((ThrowableProjectile) (Object) this);
            this.checkInsideBlocks();
            ci.cancel();
        }
    }
}
