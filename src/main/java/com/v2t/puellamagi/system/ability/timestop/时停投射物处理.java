// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/时停投射物处理.java

package com.v2t.puellamagi.system.ability.timestop;

import com.v2t.puellamagi.api.access.IAbstractArrowAccess;
import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.core.config.时停配置;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 时停投射物处理
 *
 * 处理时停中投射物的惯性运动
 * 完全对标Roundabout 的 TimeMovingProjectile
 */
public final class 时停投射物处理 {

    private 时停投射物处理() {}

    /**
     * 投射物 tick 处理
     *
     * 在时停中替代原版tick，实现惯性衰减
     */
    public static void tick(Projectile projectile) {
        Vec3 delta = projectile.getDeltaMovement();

        // 初始化旋转
        if (projectile.xRotO == 0.0F && projectile.yRotO == 0.0F) {
            double horizontalDist = delta.horizontalDistance();
            projectile.setYRot((float) (Mth.atan2(delta.x, delta.z) * 180.0F / Math.PI));
            projectile.setXRot((float) (Mth.atan2(delta.y, horizontalDist) * 180.0F / Math.PI));projectile.yRotO = projectile.getYRot();
            projectile.xRotO = projectile.getXRot();
        }

        // 检查是否在方块内
        BlockPos blockPos = projectile.blockPosition();
        BlockState blockState = projectile.level().getBlockState(blockPos);
        if (!blockState.isAir()) {
            VoxelShape shape = blockState.getCollisionShape(projectile.level(), blockPos);
            if (!shape.isEmpty()) {
                Vec3 pos = projectile.position();
                for (AABB aabb : shape.toAabbs()) {
                    if (aabb.move(blockPos).contains(pos)) {
                        if (projectile instanceof AbstractArrow) {
                            ((IAbstractArrowAccess) projectile).puellamagi$setInGround(true);
                            ((IProjectileAccess) projectile).puellamagi$setTimeStopCreated(false);
                        }
                        break;
                    }
                }
            }
        }

        // ThrowableProjectile 检查方块内部
        if (projectile instanceof ThrowableProjectile) {
            ((IProjectileAccess) projectile).puellamagi$checkInsideBlocks();
        }

        //箭的震动时间
        if (projectile instanceof AbstractArrow arrow) {
            if (arrow.shakeTime > 0) {
                arrow.shakeTime--;
            }
        }

        // 清除着火状态
        if (projectile.isInWaterOrRain() || blockState.is(Blocks.POWDER_SNOW)) {
            projectile.clearFire();
        }

        // 如果箭已经插在地上，不再处理
        if (projectile instanceof AbstractArrow && ((IAbstractArrowAccess) projectile).puellamagi$isInGround()) {
            return;
        }

        IProjectileAccess access = (IProjectileAccess) projectile;
        float speedMod = access.puellamagi$getSpeedMultiplier();
        Vec3 position = projectile.position();

        // 从配置获取静止阈值
        double stopThreshold = 时停配置.获取静止阈值();

        // 钓鱼钩重力
        if (projectile instanceof FishingHook) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().add(0.0, -0.03 * speedMod, 0.0));
        }

        Vec3 reducedDelta = projectile.getDeltaMovement().multiply(speedMod, speedMod, speedMod);

        // 实体碰撞预检测（接近实体时减速）
        if (speedMod > stopThreshold) {
            Vec3 pos = position;
            Vec3 pos2 = position.add(projectile.getDeltaMovement()).add(projectile.getDeltaMovement()).add(reducedDelta);
            float inflateDist = (float) Math.max(Math.max(reducedDelta.x, reducedDelta.y), reducedDelta.z) * 2;

            HitResult mobHit = ProjectileUtil.getEntityHitResult(
                    projectile.level(), projectile, pos, pos2,
                    projectile.getBoundingBox().expandTowards(projectile.getDeltaMovement()).inflate(1+ inflateDist),
                    access::puellamagi$canHitEntity
            );

            if (mobHit != null) {
                speedMod *= 0.7F;
            }

            reducedDelta = projectile.getDeltaMovement().multiply(speedMod, speedMod, speedMod);
            pos2 = position.add(projectile.getDeltaMovement()).add(reducedDelta);

            HitResult mobHit2 = ProjectileUtil.getEntityHitResult(
                    projectile.level(), projectile, pos, pos2,
                    projectile.getBoundingBox().expandTowards(projectile.getDeltaMovement()).inflate(1),
                    access::puellamagi$canHitEntity
            );

            if (mobHit2 != null) {
                speedMod *= 0.6F;
            }

            if (mobHit2 != null && mobHit != null) {
                if (speedMod <= 0.1) {
                    speedMod = 0.11F;
                }
                access.puellamagi$setSpeedMultiplier(speedMod);
            }
        }

        // 方块碰撞检测
        Vec3 startPos = projectile.position();
        Vec3 endPos = startPos.add(projectile.getDeltaMovement());
        HitResult hitResult = projectile.level().clip(new ClipContext(
                startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile
        ));

        if (hitResult.getType() != HitResult.Type.MISS) {
            endPos = hitResult.getLocation();
        }

        // 实体碰撞检测
        EntityHitResult entityHit = findHitEntity(projectile, startPos, endPos, projectile.getDeltaMovement());
        if (entityHit != null) {
            hitResult = entityHit;
        }

        // 玩家 PvP 检查
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            Entity hitEntity = ((EntityHitResult) hitResult).getEntity();
            Entity owner = projectile.getOwner();
            if (hitEntity instanceof Player && owner instanceof Player) {
                if (!((Player) owner).canHarmPlayer((Player) hitEntity)) {
                    hitResult = null;
                }
            }
        }

        //碰撞处理：停止运动
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            access.puellamagi$setSpeedMultiplier(0);
            access.puellamagi$setTimeStopCreated(false);
        } else if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            access.puellamagi$setSpeedMultiplier(0);
            access.puellamagi$setTimeStopCreated(false);
        }

        // 应用惯性运动
        speedMod = access.puellamagi$getSpeedMultiplier();
        reducedDelta = projectile.getDeltaMovement().multiply(speedMod, speedMod, speedMod);

        if (speedMod > stopThreshold) {
            // 从配置获取惯性衰减系数
            double decayFactor = 时停配置.获取惯性衰减系数();
            access.puellamagi$setSpeedMultiplier((float) (speedMod * decayFactor));
            projectile.setPos(position.x + reducedDelta.x, position.y + reducedDelta.y, position.z + reducedDelta.z);
        } else {
            // 完全静止
            access.puellamagi$setTimeStopCreated(false);
        }
    }

    @Nullable
    private static EntityHitResult findHitEntity(Projectile projectile, Vec3 start, Vec3 end, Vec3 delta) {
        return ProjectileUtil.getEntityHitResult(
                projectile.level(), projectile, start, end,
                projectile.getBoundingBox().expandTowards(delta).inflate(1.0),
                ((IProjectileAccess) projectile)::puellamagi$canHitEntity
        );
    }
}
