// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopClientLevelMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.v2t.puellamagi.api.access.IEntityAndData;
import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.mixin.timestop.WalkAnimationStateAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * 客户端世界 Mixin - 完全对标Roundabout 的 WorldTickClient
 */
@Mixin(ClientLevel.class)
public abstract class TimestopClientLevelMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void puellamagi$onTickHead(BooleanSupplier supplier, CallbackInfo ci) {
        ((TimeStop) this).puellamagi$tickTimeStop();
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickNonPassenger(Entity entity, CallbackInfo ci) {
        if (!entity.isRemoved()) {
            // 每帧存储旧位置
            puellamagi$storeOldPositionsForTS(entity);

            if (((TimeStop) this).puellamagi$shouldFreezeEntity(entity)) {
                puellamagi$tickEntityTS(entity);

                for (Entity passenger : entity.getPassengers()) {
                    puellamagi$tickPassengerTS(entity, passenger);
                }

                ci.cancel();
            }
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        puellamagi$storeOldPositionsForTS(passenger);

        if (passenger.isRemoved() || passenger.getVehicle() != vehicle) {
            passenger.stopRiding();
        } else {
            if (((TimeStop) this).puellamagi$shouldFreezeEntity(passenger)) {
                passenger.setDeltaMovement(Vec3.ZERO);
                puellamagi$tickEntityTS(passenger);

                if (passenger.isPassenger()) {
                    passenger.getVehicle().positionRider(passenger);
                }

                for (Entity subPassenger : passenger.getPassengers()) {
                    puellamagi$tickPassengerTS(passenger, subPassenger);
                }

                ci.cancel();
            }
        }
    }

    @Unique
    private void puellamagi$storeOldPositionsForTS(Entity entity) {
        IEntityAndData data = (IEntityAndData) entity;
        data.puellamagi$setPrevX(entity.getX());
        data.puellamagi$setPrevY(entity.getY());
        data.puellamagi$setPrevZ(entity.getZ());
    }

    /**
     * 冻结实体的 tick 替代 - 对标 roundabout$TSTickEntity
     */
    @Unique
    private void puellamagi$tickEntityTS(Entity entity) {
        if (entity instanceof LivingEntity living) {
            puellamagi$tickLivingEntityTS(living);
            entity.invulnerableTime = 0;
            living.hurtTime = 0;
        } else {
            //========== 非生物实体的旧值同步（防止抖动）==========
            // 位置旧值
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();

            // 渲染位置旧值（关键！防止投射物抖动）
            entity.xo = entity.getX();
            entity.yo = entity.getY();
            entity.zo = entity.getZ();

            // 旋转旧值（关键！防止箭尾巴抖动）
            entity.xRotO = entity.getXRot();
            entity.yRotO = entity.getYRot();

            // 行走动画旧值
            entity.walkDistO = entity.walkDist;

            // 特殊实体处理
            if (entity instanceof FishingHook) {
                //钓鱼钩已在上面处理
            } else if (entity instanceof Boat) {
                entity.lerpTo(entity.getX(), entity.getY(), entity.getZ(),
                        entity.getYRot(), entity.getXRot(), 3, false);
            }
        }
    }

    /**
     * 生物实体的冻结 tick - 关键方法！
     */
    @Unique
    private void puellamagi$tickLivingEntityTS(LivingEntity living) {
        ILivingEntityAccess accessor = (ILivingEntityAccess) living;

        // ========== 1. 同步动画步进 ==========
        accessor.puellamagi$setAnimStepO(accessor.puellamagi$getAnimStep());

        // ========== 2. 同步位置和旋转（调用原版方法）==========
        living.setOldPosAndRot();

        // ========== 3. 同步身体和头部旋转 ==========
        living.yBodyRotO = living.yBodyRot;
        living.yHeadRotO = living.yHeadRot;

        // ========== 4. 同步攻击动画 ==========
        living.oAttackAnim = living.attackAnim;

        // ========== 5. 同步 WalkAnimationState（关键！防止腿部抖动）==========
        WalkAnimationState walkAnim = accessor.puellamagi$getWalkAnimation();
        WalkAnimationStateAccessor walkAccessor = (WalkAnimationStateAccessor) (Object) walkAnim;
        walkAccessor.setSpeedOld(walkAccessor.getSpeed());

        // ========== 6. Lerp 插值处理 ==========
        int lerpSteps = accessor.puellamagi$getLerpSteps();
        if (lerpSteps > 0) {
            double lerpX = accessor.puellamagi$getLerpX();
            double lerpY = accessor.puellamagi$getLerpY();
            double lerpZ = accessor.puellamagi$getLerpZ();

            double newX = living.getX() + (lerpX - living.getX()) / (double) lerpSteps;
            double newY = living.getY() + (lerpY - living.getY()) / (double) lerpSteps;
            double newZ = living.getZ() + (lerpZ - living.getZ()) / (double) lerpSteps;

            accessor.puellamagi$setLerpSteps(lerpSteps - 1);
            living.setPos(newX, newY, newZ);
        }

        // ========== 7. 碰撞推动 ==========
        accessor.puellamagi$pushEntities();
    }

    @Unique
    private void puellamagi$tickPassengerTS(Entity vehicle, Entity passenger) {
        puellamagi$storeOldPositionsForTS(passenger);

        if (((TimeStop) this).puellamagi$shouldFreezeEntity(passenger)) {
            puellamagi$tickEntityTS(passenger);

            if (passenger.isPassenger()) {
                vehicle.positionRider(passenger);
            }

            for (Entity subPassenger : passenger.getPassengers()) {
                puellamagi$tickPassengerTS(passenger, subPassenger);
            }
        }
    }
}
