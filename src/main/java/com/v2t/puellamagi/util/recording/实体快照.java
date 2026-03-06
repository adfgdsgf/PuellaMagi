package com.v2t.puellamagi.util.recording;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 实体完整快照
 *
 * 使用NBT保存实体的完整状态，用于回滚恢复
 * 与实体帧数据的区别：帧数据只保存渲染/物理信息，快照保存一切
 *
 * 复用场景：世界回滚、时间倒流、存档点
 */
public class 实体快照 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/EntitySnapshot");

    private final UUID 实体UUID;
    private final CompoundTag 完整NBT;
    private final double x, y, z;
    private final float yRot, xRot;

    // ==================== 构造 ====================

    public 实体快照(UUID uuid, CompoundTag nbt, double x, double y, double z,float yRot, float xRot) {
        this.实体UUID = uuid;
        this.完整NBT = nbt;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
    }

    // ==================== 从实体采集 ====================

    /**
     * 从实体创建完整快照
     */
    public static 实体快照 从实体创建(Entity entity) {
        CompoundTag nbt = new CompoundTag();
        entity.saveWithoutId(nbt);

        return new 实体快照(
                entity.getUUID(),
                nbt,
                entity.getX(), entity.getY(), entity.getZ(),
                entity.getYRot(), entity.getXRot()
        );
    }

    // ==================== 恢复 ====================

    /**
     * 将快照恢复到目标实体
     * 实体必须是同一个UUID
     */
    public boolean 恢复到(Entity entity) {
        if (!entity.getUUID().equals(实体UUID)) {LOGGER.warn("实体UUID不匹配：快照={}, 目标={}", 实体UUID, entity.getUUID());
            return false;
        }

        try {
            //玩家使用teleportTo（会同步给客户端，防止位置被C2S包覆盖）
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(x, y, z);
                player.setYRot(yRot);
                player.setXRot(xRot);
                player.yRotO = yRot;
                player.xRotO = xRot;
                // 从NBT恢复血量
                if (完整NBT.contains("Health")) {
                    player.setHealth(完整NBT.getFloat("Health"));
                }
            } else {
                // 非玩家实体完整恢复
                entity.load(完整NBT);
                entity.setPos(x, y, z);
                entity.setYRot(yRot);
                entity.setXRot(xRot);
                entity.yRotO = yRot;
                entity.xRotO = xRot;
                entity.xo = x;
                entity.yo = y;
                entity.zo = z;
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("恢复实体快照失败: UUID={}", 实体UUID, e);
            return false;
        }
    }

    // ==================== Getter ====================

    public UUID 获取UUID() { return 实体UUID; }
    public CompoundTag 获取NBT() { return 完整NBT.copy(); }
    public double 获取X() { return x; }
    public double 获取Y() { return y; }
    public double 获取Z() { return z; }
    public float 获取YRot() { return yRot; }
    public float 获取XRot() { return xRot; }
}
