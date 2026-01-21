// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopItemEntityMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.IItemEntityAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停掉落物处理;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class TimestopItemEntityMixin extends Entity implements IItemEntityAccess {

    //==================== EntityData 定义（自动同步）====================

    @Unique
    private static final EntityDataAccessor<Boolean> PUELLAMAGI_TIME_STOP_CREATED =SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private static final EntityDataAccessor<Float> PUELLAMAGI_SPEED_MULTIPLIER =
            SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.FLOAT);

    public TimestopItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ==================== 注册 EntityData ====================

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void puellamagi$defineSynchedData(CallbackInfo ci) {
        this.entityData.define(PUELLAMAGI_TIME_STOP_CREATED, false);
        this.entityData.define(PUELLAMAGI_SPEED_MULTIPLIER, 0.75f);
    }

    // ==================== IItemEntityAccess 实现 ====================

    @Override
    public float puellamagi$getSpeedMultiplier() {
        return this.entityData.get(PUELLAMAGI_SPEED_MULTIPLIER);
    }

    @Override
    public void puellamagi$setSpeedMultiplier(float multiplier) {
        this.entityData.set(PUELLAMAGI_SPEED_MULTIPLIER, multiplier);
    }

    @Override
    public boolean puellamagi$isTimeStopCreated() {
        return this.entityData.get(PUELLAMAGI_TIME_STOP_CREATED);
    }

    @Override
    public void puellamagi$setTimeStopCreated(boolean created) {
        this.entityData.set(PUELLAMAGI_TIME_STOP_CREATED, created);
    }

    // ==================== 时停标记注入 ====================

    @Inject(method = "setThrower", at = @At("TAIL"))
    private void puellamagi$onSetThrower(UUID throwerUUID, CallbackInfo ci) {
        if (throwerUUID != null && !this.level().isClientSide) {
            TimeStop timeStop = (TimeStop) this.level();

            if (timeStop.puellamagi$inTimeStopRange(this) &&
                    timeStop.puellamagi$isTimeStopper(throwerUUID)) {
                this.entityData.set(PUELLAMAGI_TIME_STOP_CREATED, true);
                this.entityData.set(PUELLAMAGI_SPEED_MULTIPLIER, 0.75f);
            }
        }
    }

    // ==================== Tick拦截 ====================

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTick(CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this.level();

        if (timeStop.puellamagi$inTimeStopRange(this) &&
                this.entityData.get(PUELLAMAGI_TIME_STOP_CREATED)) {
            super.tick();
            时停掉落物处理.tick((ItemEntity) (Object) this);
            ci.cancel();
        }
    }

//==================== 拾取拦截 ====================

    /**
     * 时停范围内禁止自动拾取，需要右键拾取
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerTouch(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        TimeStop timeStop = (TimeStop) self.level();

        // 不在时停范围内，正常处理
        if (!timeStop.puellamagi$inTimeStopRange(self)) {
            return;
        }

        // 时停范围内，禁止所有自动拾取（包括时停者）
        // 需要右键拾取
        if (timeStop.puellamagi$hasActiveTimeStop()) {
            ci.cancel();
        }
    }
}
