package com.v2t.puellamagi.util.recording;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import com.v2t.puellamagi.mixin.timestop.WalkAnimationStateAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 实体单帧数据（精简版）
 *
 * 三层存储策略：
 * - 插值提示：位置/朝向/速度/地面状态 — NBT中不精确的渲染关键字段
 * - 状态NBT：实体的完整状态 — 每帧都存储，不再差分
 * - 渲染驱动：由MC自己的动画计算器处理（不再手动管理walkDist/bob等）
 *
 * 精简前30+个手动字段 → 精简后12个核心字段
 * 生命值/受击/死亡/装备/使用物品/挥手/姿态/潜行/冲刺等全部从NBT恢复
 * 新增实体状态自动被覆盖，无需手动添加字段
 *
 * 应用顺序（极其重要）：
 * 1. 如果有状态NBT → entity.load(nbt) → 恢复一切状态
 * 2. 手动设旧值 = 上一帧 → 给渲染插值用
 * 3. 手动设当前值 = 当前帧 → 覆盖NBT中的位置/朝向（NBT的位置不准确）
 * 4. 调用MC原生动画计算 → 不再手动管理walkDist/bob
 *
 * 网络同步：
 * - 服务端每tick通过复刻帧同步包发给客户端
 * - 客户端收到后应用到本地实体（取代connection.teleport）
 * - 客户端和服务端都cancel实体tick，全部由帧数据驱动
 */
public class 实体帧数据 {

    // ==================== 身份 ====================

    private final UUID 实体UUID;

    // ==================== 插值提示（每帧必有） ====================

    private final double x, y, z;
    private final float yRot, xRot;
    private final float yBodyRot;
    private final float yHeadRot;
    private final double velX, velY, velZ;
    private final boolean 在地面上;

    // ==================== 状态NBT（每帧都有） ====================

    /**
     * 实体的完整状态
     * 排除了Pos/Rotation/Motion（由手动字段管理）
     * 不再差分 — 每帧都存储完整NBT
     *
     * 包含：生命值、受击时间、死亡时间、装备、使用物品、
     *       挥手、攻击动画、姿态、潜行、冲刺、药水效果等
     */
    @Nullable
    private final CompoundTag 状态NBT;

    // ==================== 构造器 ====================

    private 实体帧数据(Builder builder) {
        this.实体UUID = builder.实体UUID;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.yRot = builder.yRot;
        this.xRot = builder.xRot;
        this.yBodyRot = builder.yBodyRot;
        this.yHeadRot = builder.yHeadRot;
        this.velX = builder.velX;
        this.velY = builder.velY;
        this.velZ = builder.velZ;
        this.在地面上 = builder.在地面上;
        this.状态NBT = builder.状态NBT;
    }

    // ==================== NBT工具方法 ====================

    /**
     * 从实体采集状态NBT（排除位置/朝向/速度相关tag）
     *
     * 排除的tag：
     * - Pos, Rotation, Motion → 由手动字段管理
     * - UUID → 不需要变化检测
     *
     * @return 干净的状态NBT，用于变化比较和存储
     */
    public static CompoundTag 采集状态NBT(Entity entity) {
        CompoundTag full = new CompoundTag();
        entity.saveWithoutId(full);

        // 移除由手动字段管理的tag
        full.remove("Pos");
        full.remove("Rotation");
        full.remove("Motion");
        full.remove("UUID");

        return full;
    }

    /**
     * 比较两个状态NBT是否相同
     */
    public static boolean NBT相同(@Nullable CompoundTag a, @Nullable CompoundTag b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ==================== 从实体采集 ====================

    /**
     * 从活体实体采集一帧数据（插值提示 + 完整NBT）
     *
     * 不再需要手动采集20+个动画字段
     * NBT包含一切状态，新增状态自动被覆盖
     */
    public static 实体帧数据 从实体采集(LivingEntity entity) {
        return new Builder(entity.getUUID())
                .位置(entity.getX(), entity.getY(), entity.getZ())
                .朝向(entity.getYRot(), entity.getXRot())
                .身体朝向(entity.yBodyRot, entity.yHeadRot)
                .速度(entity.getDeltaMovement())
                .在地面(entity.onGround())
                .状态NBT(采集状态NBT(entity))
                .构建();
    }

    /**
     * 从非活体实体采集一帧数据（投射物、掉落物等）
     * 只记录位置/朝向/速度 + NBT
     */
    public static 实体帧数据 从普通实体采集(Entity entity) {
        return new Builder(entity.getUUID())
                .位置(entity.getX(), entity.getY(), entity.getZ())
                .朝向(entity.getYRot(), entity.getXRot())
                .身体朝向(entity.getYRot(), entity.getYRot())
                .速度(entity.getDeltaMovement())
                .在地面(entity.onGround())
                .状态NBT(采集状态NBT(entity))
                .构建();
    }

    // ==================== Getter ====================

    public UUID 获取UUID() { return 实体UUID; }

    // 位置
    public double 获取X() { return x; }
    public double 获取Y() { return y; }
    public double 获取Z() { return z; }
    public Vec3 获取位置() { return new Vec3(x, y, z); }

    // 朝向
    public float 获取YRot() { return yRot; }
    public float 获取XRot() { return xRot; }
    public float 获取身体YRot() { return yBodyRot; }
    public float 获取头部YRot() { return yHeadRot; }

    // 速度
    public Vec3 获取速度() { return new Vec3(velX, velY, velZ); }

    // 其他
    public boolean 是否在地面() { return 在地面上; }

    // NBT
    public boolean 有状态NBT() { return 状态NBT != null; }
    @Nullable
    public CompoundTag 获取状态NBT() { return 状态NBT; }

    // ==================== 网络序列化 ====================

    /**
     * 写入网络包
     * 精简后只序列化12个字段 + NBT
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(实体UUID);

        // 位置朝向
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yRot);
        buf.writeFloat(xRot);
        buf.writeFloat(yBodyRot);
        buf.writeFloat(yHeadRot);

        // 速度
        buf.writeDouble(velX);
        buf.writeDouble(velY);
        buf.writeDouble(velZ);

        // 状态
        buf.writeBoolean(在地面上);

        // 状态NBT（每帧都有）
        buf.writeBoolean(状态NBT != null);
        if (状态NBT != null) {
            buf.writeNbt(状态NBT);
        }
    }

    /**
     * 从网络包读取
     */
    public static 实体帧数据 decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();

        Builder builder = new Builder(uuid)
                .位置(buf.readDouble(), buf.readDouble(), buf.readDouble())
                .朝向(buf.readFloat(), buf.readFloat())
                .身体朝向(buf.readFloat(), buf.readFloat())
                .速度(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()))
                .在地面(buf.readBoolean());

        // 状态NBT
        boolean hasNBT = buf.readBoolean();
        if (hasNBT) {
            builder.状态NBT(buf.readNbt());
        }

        return builder.构建();
    }

    // ==================== 应用到实体 ====================

    /**
     * 将帧数据应用到活体实体（服务端 + 客户端通用）
     *
     * 应用顺序：
     * 1. load状态NBT（恢复一切状态：血量/受击/死亡/装备/使用物品/挥手/药水等）
     * 2. 设旧值 = 上一帧 → 给渲染插值用
     * 3. 设当前值 = 当前帧 → 覆盖NBT中的位置/朝向
     * 4. 驱动动画系统 → 调用MC原生方法
     *
     * 不再手动设20+个动画字段 — 全部由NBT恢复
     * 新增的动画状态自动被覆盖
     *
     * @param entity 目标实体
     * @param 上一帧 上一tick的帧数据（用于旧值），首帧传null
     */
    public void 应用到活体(LivingEntity entity, @Nullable 实体帧数据 上一帧) {

        // ======== 第1步：load状态NBT ========
        // 恢复一切状态：生命值、受击时间、死亡时间、装备、使用物品、挥手、
        // 攻击动画、姿态、潜行、冲刺、药水效果、着火等
        if (状态NBT != null) {
            entity.load(状态NBT);
        }

        // ======== 第2步：旧值（插值起点） ========
        if (上一帧 != null) {
            // 位置旧值（两组）
            entity.xo = 上一帧.x;
            entity.yo = 上一帧.y;
            entity.zo = 上一帧.z;
            entity.xOld = 上一帧.x;
            entity.yOld = 上一帧.y;
            entity.zOld = 上一帧.z;

            // 朝向旧值
            entity.yRotO = 上一帧.yRot;
            entity.xRotO = 上一帧.xRot;
            entity.yBodyRotO = 上一帧.yBodyRot;
            entity.yHeadRotO = 上一帧.yHeadRot;

            // 攻击动画旧值（从NBT恢复后entity.attackAnim是当前值）
            // 旧值需要设为上一帧NBT恢复后的值
            // 但上一帧的attackAnim在NBT恢复时已经被覆盖了
            // 所以这里不设oAttackAnim — MC自己在tick中管理
        } else {
            // 首帧：旧值 = 当前值（无插值，静止一帧）
            entity.xo = x;
            entity.yo = y;
            entity.zo = z;
            entity.xOld = x;
            entity.yOld = y;
            entity.zOld = z;

            entity.yRotO = yRot;
            entity.xRotO = xRot;
            entity.yBodyRotO = yBodyRot;
            entity.yHeadRotO = yHeadRot;
        }

        // ======== 第3步：当前值（覆盖NBT） ========

        // 位置与朝向（NBT中的值不精确，用手动字段覆盖）
        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yBodyRot = yBodyRot;
        entity.yHeadRot = yHeadRot;
        entity.setYHeadRot(yHeadRot);

        // 速度
        entity.setDeltaMovement(velX, velY, velZ);

        // 地面状态
        entity.setOnGround(在地面上);

        // ======== 第4步：驱动动画系统 ========

        // 计算本tick移动量（用于walkDist和bob）
        double prevX = (上一帧 != null) ? 上一帧.x : x;
        double prevZ = (上一帧 != null) ? 上一帧.z : z;
        float dx = (float) (x - prevX);
        float dz = (float) (z - prevZ);
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        // walkDist：手部左右摇摆幅度（旧版兼容字段）
        entity.walkDistO = entity.walkDist;
        entity.walkDist += Math.min(horizontalDist * 0.6f, 1.0f);

        // WalkAnimationState：MC1.20.1中真正驱动走路动画的系统
        // cancel tick后MC不会调用WalkAnimationState.update()
        // 需要手动更新speed和position，否则腿不会动
        if (entity instanceof ILivingEntityAccess access) {
            WalkAnimationState walkAnim = access.puellamagi$getWalkAnimation();
            WalkAnimationStateAccessor walkAccessor = (WalkAnimationStateAccessor) (Object) walkAnim;
            // speedOld = 上一帧的speed（用于插值）
            walkAccessor.setSpeedOld(walkAccessor.getSpeed());
            // speed：根据水平移动距离计算，和MC原生LivingEntity.calculateEntityAnimation一致
            float speedTarget = Math.min(horizontalDist * 4.0f, 1.0f);
            walkAccessor.setSpeed(walkAccessor.getSpeed() + (speedTarget - walkAccessor.getSpeed()) * 0.4f);
            // position：累加speed，驱动腿部摆动周期
            walkAccessor.setPosition(walkAccessor.getPosition() + walkAccessor.getSpeed());
        }

        // bob：第一人称镜头/手部上下晃动
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            player.oBob = player.bob;
            float bobTarget = (在地面上 && entity.deathTime == 0)
                    ? Math.min(0.1f, horizontalDist) : 0.0f;
            player.bob += (bobTarget - player.bob) * 0.4f;
        }
    }

    /**
     * 将帧数据应用到普通实体（投射物、掉落物等）
     */
    public void 应用到普通实体(Entity entity, @Nullable 实体帧数据 上一帧) {
        // NBT
        if (状态NBT != null) {
            entity.load(状态NBT);
        }

        // 旧值 — 两组都要设
        if (上一帧 != null) {
            entity.xo = 上一帧.x;
            entity.yo = 上一帧.y;
            entity.zo = 上一帧.z;

            entity.xOld = 上一帧.x;
            entity.yOld = 上一帧.y;
            entity.zOld = 上一帧.z;

            entity.yRotO = 上一帧.yRot;
            entity.xRotO = 上一帧.xRot;
        } else {
            entity.xo = x;
            entity.yo = y;
            entity.zo = z;

            entity.xOld = x;
            entity.yOld = y;
            entity.zOld = z;

            entity.yRotO = yRot;
            entity.xRotO = xRot;
        }

        // 当前值
        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.setDeltaMovement(velX, velY, velZ);
        entity.setOnGround(在地面上);
    }

    // ==================== Builder ====================

    public static class Builder {
        private final UUID 实体UUID;

        // 插值提示
        private double x, y, z;
        private float yRot, xRot;
        private float yBodyRot, yHeadRot;
        private double velX, velY, velZ;
        private boolean 在地面上;

        // NBT
        @Nullable
        private CompoundTag 状态NBT;

        public Builder(UUID uuid) {
            this.实体UUID = uuid;
        }

        public Builder 位置(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public Builder 朝向(float yRot, float xRot) {
            this.yRot = yRot;
            this.xRot = xRot;
            return this;
        }

        public Builder 身体朝向(float yBodyRot, float yHeadRot) {
            this.yBodyRot = yBodyRot;
            this.yHeadRot = yHeadRot;
            return this;
        }

        public Builder 速度(Vec3 vel) {
            this.velX = vel.x;
            this.velY = vel.y;
            this.velZ = vel.z;
            return this;
        }

        public Builder 在地面(boolean onGround) {
            this.在地面上 = onGround;
            return this;
        }

        /**
         * 设置状态NBT
         * 精简后每帧都传入完整NBT，不再差分
         */
        public Builder 状态NBT(@Nullable CompoundTag nbt) {
            this.状态NBT = nbt;
            return this;
        }

        public 实体帧数据 构建() {
            return new 实体帧数据(this);
        }
    }
}
