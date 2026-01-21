// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/时停掉落物处理.java

package com.v2t.puellamagi.system.ability.timestop;

import com.v2t.puellamagi.api.access.IItemEntityAccess;
import com.v2t.puellamagi.core.config.时停配置;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 时停掉落物处理
 *
 * 完全照抄 时停投射物处理
 */
public final class 时停掉落物处理 {

    private 时停掉落物处理() {}

    public static void tick(ItemEntity item) {
        Vec3 delta = item.getDeltaMovement();

        // 检查是否在方块内
        BlockPos blockPos = item.blockPosition();
        BlockState blockState = item.level().getBlockState(blockPos);
        if (!blockState.isAir()) {
            VoxelShape shape = blockState.getCollisionShape(item.level(), blockPos);
            if (!shape.isEmpty()) {
                Vec3 pos = item.position();
                for (AABB aabb : shape.toAabbs()) {
                    if (aabb.move(blockPos).contains(pos)) {
                        // 在方块内，停止
                        IItemEntityAccess access = (IItemEntityAccess) item;
                        access.puellamagi$setTimeStopCreated(false);
                        return;
                    }
                }
            }
        }

        // 清除着火状态
        if (item.isInWaterOrRain() || blockState.is(Blocks.POWDER_SNOW)) {
            item.clearFire();
        }

        IItemEntityAccess access = (IItemEntityAccess) item;
        float speedMod = access.puellamagi$getSpeedMultiplier();
        Vec3 position = item.position();

        double stopThreshold = 时停配置.获取静止阈值();

        Vec3 reducedDelta = item.getDeltaMovement().multiply(speedMod, speedMod, speedMod);

        // 方块碰撞检测
        Vec3 startPos = item.position();
        Vec3 endPos = startPos.add(item.getDeltaMovement());
        HitResult hitResult = item.level().clip(new ClipContext(
                startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, item
        ));

        // 碰撞处理：停止运动
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            access.puellamagi$setSpeedMultiplier(0);
            access.puellamagi$setTimeStopCreated(false);
        }

        // 应用惯性运动
        if (speedMod > stopThreshold) {
            double decayFactor = 时停配置.获取惯性衰减系数();
            access.puellamagi$setSpeedMultiplier((float) (speedMod * decayFactor));

            double newX = position.x + reducedDelta.x;
            double newY = position.y + reducedDelta.y;
            double newZ = position.z + reducedDelta.z;

            item.setPos(newX, newY, newZ);

            // 同步旧位置 = 新位置，让渲染位置和实际位置一致
            item.xo = newX;
            item.yo = newY;
            item.zo = newZ;
        } else {
            access.puellamagi$setTimeStopCreated(false);
        }
    }
}
