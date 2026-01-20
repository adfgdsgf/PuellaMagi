// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopServerLevelMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * 服务端世界 Mixin - 完全对标 Roundabout 的 WorldTickServer
 *
 * 核心职责：
 * 1. 冻结实体的tick 拦截（服务端逻辑）
 * 2. 流体和方块 tick 拦截
 * 3. 时停伤害释放检查（每tick检查每个实体）
 * 4. 游戏时间冻结（太阳/月亮）
 */
@Mixin(ServerLevel.class)
public abstract class TimestopServerLevelMixin {

    /**
     * 世界 tick 开始 - 对标 roundabout$tickTimeStopList
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void puellamagi$onTickHead(BooleanSupplier supplier, CallbackInfo ci) {
        ((TimeStop) this).puellamagi$tickTimeStop();
    }

    /**
     * 游戏时间（太阳/月亮）冻结 - 对标 roundabout$TickEntity3
     */
    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickTime(CallbackInfo ci) {
        if (((TimeStop) this).puellamagi$hasActiveTimeStop()) {
            ci.cancel();
        }
    }

    /**
     * 非乘客实体 tick 拦截 - 对标 roundabout$TickEntity2
     *
     * 关键改动：每个实体都检查是否需要释放累计伤害
     */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickNonPassenger(Entity entity, CallbackInfo ci) {
        if (!entity.isRemoved()) {
            //===== 关键：每个实体都检查伤害释放 =====
            puellamagi$tickTSDamage(entity);

            if (((TimeStop) this).puellamagi$shouldFreezeEntity(entity)) {
                if (entity instanceof LivingEntity living) {
                    living.hurtTime = 0;
                    entity.invulnerableTime = 0;
                    ((ILivingEntityAccess) living).puellamagi$pushEntities();
                } else if (entity instanceof ItemEntity) {
                    // 掉落物特殊处理
                } else if (entity instanceof FishingHook) {
                    // 钓鱼钩特殊处理
                } else if (entity instanceof Boat) {
                    entity.lerpTo(entity.getX(), entity.getY(), entity.getZ(),
                            entity.getYRot(), entity.getXRot(), 3, false);
                    entity.walkDistO = entity.walkDist;
                    entity.xRotO = entity.getXRot();
                    entity.yRotO = entity.getYRot();
                }

                // 处理乘客
                for (Entity passenger : entity.getPassengers()) {
                    puellamagi$tickPassengerTS(entity, passenger);
                }

                ci.cancel();
            }
        }
    }

    /**
     * 乘客实体 tick 拦截 - 对标 roundabout$TickEntity5
     */
    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickPassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (passenger.isRemoved() || passenger.getVehicle() != vehicle) {
            passenger.stopRiding();
        } else {
            // ===== 乘客也检查伤害释放 =====
            puellamagi$tickTSDamage(passenger);

            if (((TimeStop) this).puellamagi$shouldFreezeEntity(passenger)) {
                if (passenger instanceof LivingEntity living) {
                    passenger.invulnerableTime = 0;
                    living.hurtTime = 0;
                    ((ILivingEntityAccess) living).puellamagi$pushEntities();
                } else if (passenger instanceof ItemEntity) {
                    // 掉落物特殊处理
                } else if (passenger instanceof FishingHook) {
                    // 钓鱼钩特殊处理
                }

                for (Entity subPassenger : passenger.getPassengers()) {
                    puellamagi$tickPassengerTS(passenger, subPassenger);
                }

                ci.cancel();
            }
        }
    }

    /**
     * 流体 tick 拦截 - 对标 roundabout$FluidTick
     */
    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickFluid(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (((TimeStop) this).puellamagi$inTimeStopRange(pos)) {
            // 重新安排 tick，让流体保持等待状态
            ServerLevel self = (ServerLevel) (Object) this;
            self.scheduleTick(pos, fluid, fluid.getTickDelay(self));
            ci.cancel();
        }
    }

    /**
     * 方块 tick 拦截 - 对标 roundabout$BlockTick
     */
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (((TimeStop) this).puellamagi$inTimeStopRange(pos)) {
            // 重新安排 tick
            ServerLevel self = (ServerLevel) (Object) this;
            self.scheduleTick(pos, block, 1);
            ci.cancel();
        }
    }

    /**
     * 递归处理乘客
     */
    @Unique
    private void puellamagi$tickPassengerTS(Entity vehicle, Entity passenger) {
        // 乘客也检查伤害释放
        puellamagi$tickTSDamage(passenger);

        if (((TimeStop) this).puellamagi$shouldFreezeEntity(passenger)) {
            if (passenger instanceof LivingEntity living) {
                passenger.invulnerableTime = 0;
                living.hurtTime = 0;
                ((ILivingEntityAccess) living).puellamagi$pushEntities();
            }

            for (Entity subPassenger : passenger.getPassengers()) {
                puellamagi$tickPassengerTS(passenger, subPassenger);
            }
        }
    }

    // ==================== 伤害释放检查 ====================

    /**
     * 检查并释放累计伤害 - 对标 roundabout$TickTSDamage
     *
     * 核心逻辑：
     * - 实体不再被冻结（时停结束或不在范围内）
     * - 且有累计伤害
     * → 释放伤害
     */
    @Unique
    private void puellamagi$tickTSDamage(Entity entity) {
        if (entity instanceof LivingEntity living) {
            // 关键条件：不被冻结 + 有累计伤害
            if (!((TimeStop) this).puellamagi$shouldFreezeEntity(entity)) {
                时停管理器.尝试释放实体伤害(living);
            }
        }
    }
}
