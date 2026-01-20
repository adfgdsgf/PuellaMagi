// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopAbstractArrowMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.IAbstractArrowAccess;
import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停投射物处理;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AbstractArrow Mixin
 *
 * 处理箭类投射物在时停中的行为
 * 对标 Roundabout 的 TimeStopAbstractArrow
 */
@Mixin(AbstractArrow.class)
public abstract class TimestopAbstractArrowMixin extends Entity implements IAbstractArrowAccess {

    public TimestopAbstractArrowMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ==================== IAbstractArrowAccess 实现 ====================

    @Unique
    @Override
    public boolean puellamagi$isInGround() {
        return this.inGround;
    }

    @Unique
    @Override
    public void puellamagi$setInGround(boolean inGround) {
        this.inGround = inGround;
    }

    // ==================== Tick拦截 ====================

    /**
     * 时停中创建的箭使用自定义 tick
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
            时停投射物处理.tick((AbstractArrow) (Object) this);
            this.checkInsideBlocks();
            ci.cancel();
        }
    }

    // ==================== 拾取拦截 ====================

    /**
     * 时停范围内的箭可以被拾取（即使在空中）
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerTouch(Player player, CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this.level();

        if (timeStop.puellamagi$inTimeStopRange(this)) {
            if (!this.level().isClientSide && (this.inGround || this.isNoPhysics())) {
                if (this.tryPickup(player)) {
                    player.take(this, 1);
                    this.discard();
                }
            }ci.cancel();
        }
    }

    // ==================== Shadow ====================

    @Shadow
    protected boolean inGround;

    @Shadow
    public abstract boolean isNoPhysics();

    @Shadow
    protected abstract boolean tryPickup(Player player);
}
