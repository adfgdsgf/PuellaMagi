// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopLevelMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.api.timestop.时停实例;
import com.v2t.puellamagi.core.network.ModNetwork;
import com.v2t.puellamagi.core.network.packets.s2c.时停状态同步包;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * Level Mixin - 实现TimeStop 接口
 */
@Mixin(Level.class)
public abstract class TimestopLevelMixin implements TimeStop {

    // ==================== 服务端数据 ====================

    @Unique
    private ImmutableList<LivingEntity> puellamagi$timeStoppers;

    // ==================== 客户端数据 ====================

    @Unique
    private ImmutableList<时停实例> puellamagi$timeStoppersClient;

    // ==================== 延迟初始化 Getter ====================

    @Unique
    private ImmutableList<LivingEntity> puellamagi$getTimeStoppersList() {
        if (puellamagi$timeStoppers == null) {
            puellamagi$timeStoppers = ImmutableList.of();
        }
        return puellamagi$timeStoppers;
    }

    @Unique
    private ImmutableList<时停实例> puellamagi$getTimeStoppersClientList() {
        if (puellamagi$timeStoppersClient == null) {
            puellamagi$timeStoppersClient = ImmutableList.of();
        }
        return puellamagi$timeStoppersClient;
    }

    // ==================== 时停者管理 ====================

    @Override
    public void puellamagi$addTimeStopper(LivingEntity entity) {
        Level self = (Level) (Object) this;
        if (self.isClientSide) return;

        ImmutableList<LivingEntity> current = puellamagi$getTimeStoppersList();
        if (!current.contains(entity)) {
            List<LivingEntity> list = Lists.newArrayList(current);
            list.add(entity);
            puellamagi$timeStoppers = ImmutableList.copyOf(list);

            PuellaMagi.LOGGER.info("[服务端] 添加时停者: {}", entity.getName().getString());

            puellamagi$syncToClients();
        }
    }

    @Override
    public void puellamagi$removeTimeStopper(LivingEntity entity) {
        Level self = (Level) (Object) this;
        if (self.isClientSide) return;

        ImmutableList<LivingEntity> current = puellamagi$getTimeStoppersList();
        if (current.contains(entity)) {
            List<LivingEntity> list = Lists.newArrayList(current);
            list.remove(entity);
            puellamagi$timeStoppers = ImmutableList.copyOf(list);

            PuellaMagi.LOGGER.info("[服务端] 移除时停者: {}", entity.getName().getString());

            puellamagi$syncRemovalToClients(entity);
        }
    }

    @Override
    public List<LivingEntity> puellamagi$getTimeStoppers() {
        return puellamagi$getTimeStoppersList();
    }

    @Override
    public boolean puellamagi$isTimeStopper(Entity entity) {
        if (entity == null) return false;

        Level self = (Level) (Object) this;
        if (!self.isClientSide) {
            for (LivingEntity stopper : puellamagi$getTimeStoppersList()) {
                if (stopper.getId() == entity.getId()) {
                    return true;
                }
            }
        } else {
            for (时停实例 instance : puellamagi$getTimeStoppersClientList()) {
                if (instance.实体ID == entity.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean puellamagi$isTimeStopper(UUID uuid) {
        Level self = (Level) (Object) this;

        if (!self.isClientSide) {
            // 服务端：直接检查列表
            for (LivingEntity stopper : puellamagi$getTimeStoppersList()) {
                if (stopper.getUUID().equals(uuid)) {
                    return true;
                }
            }
        } else {
            // 客户端：通过 entityId 获取实体再检查 UUID
            for (时停实例 instance : puellamagi$getTimeStoppersClientList()) {
                Entity entity = self.getEntity(instance.实体ID);
                if (entity != null && entity.getUUID().equals(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 冻结判断 ====================


    @Override
    public boolean puellamagi$shouldFreezeEntity(Entity entity) {
        if (!puellamagi$hasActiveTimeStop()) {
            return false;
        }

        if (puellamagi$isTimeStopper(entity)) {
            return false;
        }

        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }

        if (entity.isSpectator()) {
            return false;
        }

        // 投射物豁免
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            com.v2t.puellamagi.api.access.IProjectileAccess access =
                    (com.v2t.puellamagi.api.access.IProjectileAccess) entity;
            if (access.puellamagi$isTimeStopCreated()) {
                return false;
            }
        }

        //===== 掉落物豁免 =====
        if (entity instanceof ItemEntity) {
            com.v2t.puellamagi.api.access.IItemEntityAccess access =
                    (com.v2t.puellamagi.api.access.IItemEntityAccess) entity;
            if (access.puellamagi$isTimeStopCreated()) {
                return false;
            }
        }

        return puellamagi$inTimeStopRange(entity);
    }

    @Override
    public boolean puellamagi$inTimeStopRange(Vec3i pos) {
        Level self = (Level) (Object) this;

        // 从配置获取范围
        int configRange = com.v2t.puellamagi.core.config.时停配置.获取时停范围();

        // 无限范围（配置为-1 或 0）
        if (configRange <= 0) {
            if (!self.isClientSide) {
                return !puellamagi$getTimeStoppersList().isEmpty();
            } else {
                return !puellamagi$getTimeStoppersClientList().isEmpty();
            }
        }

        // 有限范围 - 检查是否在任意时停者范围内
        double rangeSquared = (double) configRange * configRange;

        if (!self.isClientSide) {
            // 服务端：直接获取时停者实时位置
            for (LivingEntity stopper : puellamagi$getTimeStoppersList()) {
                double dx = pos.getX() - stopper.getX();
                double dy = pos.getY() - stopper.getY();
                double dz = pos.getZ() - stopper.getZ();
                if (dx * dx + dy * dy + dz * dz <= rangeSquared) {
                    return true;
                }
            }
        } else {
            // 客户端：尝试获取实体实时位置，否则用存储的坐标
            for (时停实例 instance : puellamagi$getTimeStoppersClientList()) {
                Entity stopper = self.getEntity(instance.实体ID);

                double x, y, z;
                if (stopper != null) {
                    // 实体在客户端存在，用实时位置
                    x = stopper.getX();
                    y = stopper.getY();
                    z = stopper.getZ();
                } else {
                    // 实体不在视野内，用同步包携带的坐标
                    x = instance.x;
                    y = instance.y;
                    z = instance.z;
                }

                double dx = pos.getX() - x;
                double dy = pos.getY() - y;
                double dz = pos.getZ() - z;
                if (dx * dx + dy * dy + dz * dz <= rangeSquared) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean puellamagi$inTimeStopRange(Entity entity) {
        return puellamagi$inTimeStopRange(new Vec3i(
                (int) entity.getX(),
                (int) entity.getY(),
                (int) entity.getZ()
        ));
    }

    // ==================== 状态查询 ====================

    @Override
    public boolean puellamagi$hasActiveTimeStop() {
        Level self = (Level) (Object) this;

        if (!self.isClientSide) {
            return !puellamagi$getTimeStoppersList().isEmpty();
        } else {
            return !puellamagi$getTimeStoppersClientList().isEmpty();
        }
    }

    // ==================== 方块实体冻结 ====================

    @Inject(method = "shouldTickBlocksAt(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onShouldTickBlocksAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (puellamagi$inTimeStopRange(pos)) {
            Level self = (Level) (Object) this;
            if (self.getBlockState(pos).is(Blocks.MOVING_PISTON)) {
                return;
            }
            cir.setReturnValue(false);
        }
    }

    // ==================== Tick ====================

    @Override
    public void puellamagi$tickTimeStop() {
        Level self = (Level) (Object) this;

        if (!self.isClientSide) {
            ImmutableList<LivingEntity> stoppers = puellamagi$getTimeStoppersList();
            if (!stoppers.isEmpty()) {
                List<LivingEntity> toRemove = Lists.newArrayList();
                for (LivingEntity stopper : stoppers) {
                    if (stopper.isRemoved() || !stopper.isAlive()) {
                        toRemove.add(stopper);
                    }
                }
                for (LivingEntity entity : toRemove) {
                    puellamagi$removeTimeStopper(entity);
                }
            }
        }
    }

    // ==================== 客户端同步 ====================

    @Override
    public void puellamagi$addTimeStopperClient(int entityId, double x, double y, double z, double range) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide) return;

        List<时停实例> list = Lists.newArrayList(puellamagi$getTimeStoppersClientList());
        list.removeIf(instance -> instance.实体ID == entityId);
        list.add(new 时停实例(entityId, x, y, z, range));
        puellamagi$timeStoppersClient = ImmutableList.copyOf(list);

        PuellaMagi.LOGGER.info("[客户端] 添加时停者: ID={}", entityId);}

    @Override
    public void puellamagi$removeTimeStopperClient(int entityId) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide) return;

        List<时停实例> list = Lists.newArrayList(puellamagi$getTimeStoppersClientList());
        list.removeIf(instance -> instance.实体ID == entityId);
        puellamagi$timeStoppersClient = ImmutableList.copyOf(list);

        PuellaMagi.LOGGER.info("[客户端] 移除时停者: ID={}", entityId);
    }

    @Override
    public void puellamagi$syncToClients() {
        Level self = (Level) (Object) this;
        if (self.isClientSide) return;

        if (!(self instanceof ServerLevel serverLevel)) return;

        ImmutableList<LivingEntity> stoppers = puellamagi$getTimeStoppersList();
        if (stoppers.isEmpty()) return;

        for (ServerPlayer player : serverLevel.players()) {
            for (LivingEntity stopper : stoppers) {
                时停状态同步包 packet = 时停状态同步包.开始(
                        stopper.getId(),
                        stopper.getX(),
                        stopper.getY(),
                        stopper.getZ(),
                        -1
                );
                ModNetwork.getChannel().send(
                        PacketDistributor.PLAYER.with(() -> player),
                        packet
                );
            }
        }
    }

    @Unique
    private void puellamagi$syncRemovalToClients(LivingEntity entity) {
        Level self = (Level) (Object) this;
        if (self.isClientSide) return;

        if (!(self instanceof ServerLevel serverLevel)) return;

        时停状态同步包 packet = 时停状态同步包.结束(entity.getId());

        for (ServerPlayer player : serverLevel.players()) {
            ModNetwork.getChannel().send(
                    PacketDistributor.PLAYER.with(() -> player),
                    packet
            );
        }
    }
}
