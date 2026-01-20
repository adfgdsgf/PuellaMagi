// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopLevelMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.api.timestop.时停实例;
import com.v2t.puellamagi.core.network.ModNetwork;
import com.v2t.puellamagi.core.network.packets.s2c.时停状态同步包;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.UUID;

/**
 * Level Mixin - 实现 TimeStop 接口
 *
 * 存储时停者列表，提供基础判断方法
 * 参考Roundabout 的TimeStopWorld
 */
@Mixin(Level.class)
public abstract class TimestopLevelMixin implements TimeStop {

    //==================== 服务端数据 ====================

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

            // 立即同步给所有客户端
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

            // 发送移除同步包
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
        for (LivingEntity stopper : puellamagi$getTimeStoppersList()) {
            if (stopper.getUUID().equals(uuid)) {
                return true;
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

        //========== 新增：时停者发射的投射物不被冻结 ==========
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            com.v2t.puellamagi.api.access.IProjectileAccess access =
                    (com.v2t.puellamagi.api.access.IProjectileAccess) entity;
            if (access.puellamagi$isTimeStopCreated()) {
                return false;  // 有惯性标记的投射物不冻结，让它自己处理
            }
        }
        // =====================================================

        return puellamagi$inTimeStopRange(entity);
    }

    @Override
    public boolean puellamagi$inTimeStopRange(Vec3i pos) {
        Level self = (Level) (Object) this;

        if (!self.isClientSide) {
            ImmutableList<LivingEntity> stoppers = puellamagi$getTimeStoppersList();
            if (!stoppers.isEmpty()) {
                // 目前使用无限范围，后续可配置
                return true;
            }
        } else {
            for (时停实例 instance : puellamagi$getTimeStoppersClientList()) {
                if (instance.在范围内(pos.getX(), pos.getY(), pos.getZ())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean puellamagi$inTimeStopRange(Entity entity) {
        return puellamagi$inTimeStopRange(new Vec3i((int) entity.getX(),
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

    // ==================== 客户端同步（核心修复！）====================

    @Override
    public void puellamagi$addTimeStopperClient(int entityId, double x, double y, double z, double range) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide) return;

        List<时停实例> list = Lists.newArrayList(puellamagi$getTimeStoppersClientList());
        // 移除旧的同ID实例（如果存在）
        list.removeIf(instance -> instance.实体ID == entityId);
        // 添加新实例
        list.add(new 时停实例(entityId, x, y, z, range));
        puellamagi$timeStoppersClient = ImmutableList.copyOf(list);

        PuellaMagi.LOGGER.info("[客户端] 添加时停者: ID={}", entityId);
    }

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

        // 向所有玩家发送所有时停者的状态
        for (ServerPlayer player : serverLevel.players()) {
            for (LivingEntity stopper : stoppers) {
                时停状态同步包 packet = 时停状态同步包.开始(
                        stopper.getId(),
                        stopper.getX(),
                        stopper.getY(),
                        stopper.getZ(),
                        -1  // -1 表示无限范围
                );
                ModNetwork.getChannel().send(
                        PacketDistributor.PLAYER.with(() -> player),
                        packet
                );
            }
        }
    }

    /**
     * 同步移除时停者给所有客户端
     */
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
