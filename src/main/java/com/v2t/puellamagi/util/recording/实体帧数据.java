package com.v2t.puellamagi.util.recording;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import com.v2t.puellamagi.mixin.timestop.WalkAnimationStateAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 实体单帧数据
 *
 * 双层存储策略：
 * - 手动字段：位置/朝向/基础动画等渲染插值关键数据，每tick必须记录
 * - 状态NBT：实体的完整杂项状态（药水效果、苦力怕膨胀、着火等），仅在变化时记录
 *
 * 应用顺序（极其重要）：
 * 1. 如果有状态NBT → entity.load(nbt) → 恢复一切杂项状态
 * 2. 手动设旧值 = 上一帧 → 给渲染插值用
 * 3. 手动设当前值 = 当前帧 → 覆盖NBT中的位置/朝向（NBT的位置不准确）
 *
 * 网络同步：
 * - 服务端每tick通过复刻帧同步包发给客户端
 * - 客户端收到后应用到本地实体（取代connection.teleport）
 * - 客户端和服务端都cancel实体tick，全部由帧数据驱动
 */
public class 实体帧数据 {

    // ==================== 身份====================

    private final UUID 实体UUID;

    // ==================== 位置与朝向（手动字段，每tick） ====================

    private final double x, y, z;
    private final float yRot, xRot;
    private final float yBodyRot;
    private final float yHeadRot;

    // ==================== 速度（手动字段） ====================

    private final double velX, velY, velZ;

    // ==================== 生命值（手动字段） ====================

    private final float 生命值;
    private final float 最大生命值;

    // ==================== 动画状态（手动字段，每tick） ====================

    private final float walkAnimPos;
    private final float walkAnimSpeed;
    private final float attackAnim;

    // 挥手动画
    private final boolean 正在挥手;
    private final int 挥手进度;
    private final boolean 副手挥手;

    // 受击与死亡
    private final int 受击时间;
    private final int 死亡时间;

    //使用物品动画
    private final boolean 正在使用物品;
    private final int 使用物品剩余时间;
    @Nullable
    private final InteractionHand 使用物品的手;

    // 姿态
    private final Pose 姿态;

    // ==================== 装备（全部6槽） ====================

    @Nullable private final ItemStack 主手物品;
    @Nullable private final ItemStack 副手物品;
    @Nullable private final ItemStack 头部装备;
    @Nullable private final ItemStack 胸部装备;
    @Nullable private final ItemStack 腿部装备;
    @Nullable private final ItemStack 脚部装备;

    // ==================== 其他状态（手动字段） ====================

    private final boolean 正在潜行;
    private final boolean 正在冲刺;
    private final boolean 在地面上;

    // ==================== 状态NBT（仅在变化时有值） ====================

    /**
     * 实体的完整杂项状态
     * 排除了Pos/Rotation/Motion（由手动字段管理）
     * null = 本帧相比上一帧没有变化，不需要load
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
        this.生命值 = builder.生命值;
        this.最大生命值 = builder.最大生命值;
        this.walkAnimPos = builder.walkAnimPos;
        this.walkAnimSpeed = builder.walkAnimSpeed;
        this.attackAnim = builder.attackAnim;
        this.正在挥手 = builder.正在挥手;
        this.挥手进度 = builder.挥手进度;
        this.副手挥手 = builder.副手挥手;
        this.受击时间 = builder.受击时间;
        this.死亡时间 = builder.死亡时间;
        this.正在使用物品 = builder.正在使用物品;
        this.使用物品剩余时间 = builder.使用物品剩余时间;
        this.使用物品的手 = builder.使用物品的手;
        this.姿态 = builder.姿态;
        this.主手物品 = builder.主手物品;
        this.副手物品 = builder.副手物品;
        this.头部装备 = builder.头部装备;
        this.胸部装备 = builder.胸部装备;
        this.腿部装备 = builder.腿部装备;
        this.脚部装备 = builder.脚部装备;
        this.正在潜行 = builder.正在潜行;
        this.正在冲刺 = builder.正在冲刺;
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
     * 从活体实体采集一帧手动字段数据
     *
     * 注意：不含状态NBT。NBT的采集和变化检测由录制管理器负责，
     * 通过Builder.状态NBT() 传入。
     */
    public static 实体帧数据 从实体采集(LivingEntity entity) {
        return new Builder(entity.getUUID())
                .位置(entity.getX(), entity.getY(), entity.getZ())
                .朝向(entity.getYRot(), entity.getXRot())
                .身体朝向(entity.yBodyRot, entity.yHeadRot)
                .速度(entity.getDeltaMovement())
                .生命(entity.getHealth(), entity.getMaxHealth())
                .行走动画(entity.walkAnimation.position(), entity.walkAnimation.speed())
                .攻击动画(entity.attackAnim)
                .挥手(entity.swinging, entity.swingTime,
                        entity.swingingArm == InteractionHand.OFF_HAND)
                .受击(entity.hurtTime)
                .死亡(entity.deathTime)
                .使用物品(
                        entity.isUsingItem(),
                        entity.getUseItemRemainingTicks(),
                        entity.isUsingItem() ? entity.getUsedItemHand() : null
                )
                .姿态(entity.getPose())
                .全部装备(
                        entity.getMainHandItem().copy(),
                        entity.getOffhandItem().copy(),
                        entity.getItemBySlot(EquipmentSlot.HEAD).copy(),
                        entity.getItemBySlot(EquipmentSlot.CHEST).copy(),
                        entity.getItemBySlot(EquipmentSlot.LEGS).copy(),
                        entity.getItemBySlot(EquipmentSlot.FEET).copy()
                )
                .潜行(entity.isShiftKeyDown())
                .冲刺(entity.isSprinting())
                .在地面(entity.onGround())
                .构建();
    }

    /**
     * 从非活体实体采集一帧数据（投射物、掉落物等）
     * 只记录位置/朝向/速度
     */
    public static 实体帧数据 从普通实体采集(Entity entity) {
        return new Builder(entity.getUUID())
                .位置(entity.getX(), entity.getY(), entity.getZ())
                .朝向(entity.getYRot(), entity.getXRot())
                .身体朝向(entity.getYRot(), entity.getYRot())
                .速度(entity.getDeltaMovement())
                .生命(0, 0)
                .行走动画(0, 0)
                .攻击动画(0)
                .挥手(false, 0, false)
                .受击(0)
                .死亡(0)
                .使用物品(false, 0, null)
                .姿态(entity.getPose())
                .在地面(entity.onGround())
                .构建();
    }

    // ==================== Getter ====================

    public UUID 获取UUID() { return 实体UUID; }

    //位置
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

    // 生命
    public float 获取生命值() { return 生命值; }
    public float 获取最大生命值() { return 最大生命值; }

    // 动画
    public float 获取WalkAnimPos() { return walkAnimPos; }
    public float 获取WalkAnimSpeed() { return walkAnimSpeed; }
    public float 获取AttackAnim() { return attackAnim; }
    public boolean 是否挥手() { return 正在挥手; }
    public int 获取挥手进度() { return 挥手进度; }
    public boolean 是否副手挥手() { return 副手挥手; }
    public int 获取受击时间() { return 受击时间; }
    public int 获取死亡时间() { return 死亡时间; }
    public boolean 是否使用物品() { return 正在使用物品; }
    public int 获取使用物品剩余时间() { return 使用物品剩余时间; }
    @Nullable public InteractionHand 获取使用物品的手() { return 使用物品的手; }
    public Pose 获取姿态() { return 姿态; }

    // 装备
    @Nullable public ItemStack 获取主手物品() { return 主手物品; }
    @Nullable public ItemStack 获取副手物品() { return 副手物品; }
    @Nullable public ItemStack 获取头部装备() { return 头部装备; }
    @Nullable public ItemStack 获取胸部装备() { return 胸部装备; }
    @Nullable public ItemStack 获取腿部装备() { return 腿部装备; }
    @Nullable public ItemStack 获取脚部装备() { return 脚部装备; }

    // 其他
    public boolean 是否潜行() { return 正在潜行; }
    public boolean 是否冲刺() { return 正在冲刺; }
    public boolean 是否在地面() { return 在地面上; }

    // NBT
    public boolean 有状态NBT() { return 状态NBT != null; }
    @Nullable public CompoundTag 获取状态NBT() { return 状态NBT; }

    // ==================== 网络序列化 ====================

    /**
     * 写入网络包
     * 服务端每tick发给客户端，客户端用于直接设实体状态
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

        // 生命
        buf.writeFloat(生命值);
        buf.writeFloat(最大生命值);

        // 动画
        buf.writeFloat(walkAnimPos);
        buf.writeFloat(walkAnimSpeed);
        buf.writeFloat(attackAnim);
        buf.writeBoolean(正在挥手);
        buf.writeVarInt(挥手进度);
        buf.writeBoolean(副手挥手);
        buf.writeVarInt(受击时间);
        buf.writeVarInt(死亡时间);

        // 使用物品
        buf.writeBoolean(正在使用物品);
        buf.writeVarInt(使用物品剩余时间);
        buf.writeBoolean(使用物品的手!= null);
        if (使用物品的手 != null) {
            buf.writeEnum(使用物品的手);}

        // 姿态
        buf.writeEnum(姿态);

        // 装备（6槽）
        buf.writeItem(主手物品 != null ? 主手物品 : ItemStack.EMPTY);
        buf.writeItem(副手物品 != null ? 副手物品 : ItemStack.EMPTY);
        buf.writeItem(头部装备 != null ? 头部装备 : ItemStack.EMPTY);
        buf.writeItem(胸部装备 != null ? 胸部装备 : ItemStack.EMPTY);
        buf.writeItem(腿部装备 != null ? 腿部装备 : ItemStack.EMPTY);
        buf.writeItem(脚部装备 != null ? 脚部装备 : ItemStack.EMPTY);

        // 状态
        buf.writeBoolean(正在潜行);
        buf.writeBoolean(正在冲刺);
        buf.writeBoolean(在地面上);

        // 状态NBT（可选）
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
                .生命(buf.readFloat(), buf.readFloat())
                .行走动画(buf.readFloat(), buf.readFloat())
                .攻击动画(buf.readFloat())
                .挥手(buf.readBoolean(), buf.readVarInt(), buf.readBoolean())
                .受击(buf.readVarInt())
                .死亡(buf.readVarInt());

        // 使用物品
        boolean using = buf.readBoolean();
        int remaining = buf.readVarInt();
        boolean hasHand = buf.readBoolean();
        InteractionHand hand = hasHand ? buf.readEnum(InteractionHand.class) : null;
        builder.使用物品(using, remaining, hand);

        // 姿态
        builder.姿态(buf.readEnum(Pose.class));

        // 装备
        builder.全部装备(
                buf.readItem(), buf.readItem(),
                buf.readItem(), buf.readItem(),
                buf.readItem(), buf.readItem()
        );

        // 状态
        builder.潜行(buf.readBoolean());
        builder.冲刺(buf.readBoolean());
        builder.在地面(buf.readBoolean());

        // 状态NBT
        boolean hasNBT = buf.readBoolean();
        if (hasNBT) {
            builder.状态NBT(buf.readNbt());
        }

        return builder.构建();
    }

    // ==================== 应用到实体 ====================

    /**
     * 将帧数据应用到活体实体（服务端+ 客户端通用）
     *
     *应用顺序：
     * 1. load状态NBT（恢复杂项状态：药水/着火/苦力怕膨胀等）
     * 2. 设旧值 = 上一帧 → 给渲染插值用
     * 3. 设当前值 = 当前帧 → 覆盖NBT中的位置/朝向
     * 4.驱动动画系统 → 调用MC自己的计算器而非直接塞录制值
     *
     * 第4步的关键：
     * tick被cancel后MC的动画计算器全部停止
     * 必须手动调用它们，用位置差作为输入
     * 这样腿部摆动、手部摇摆、镜头晃动全部自然运行
     *
     * @param entity 目标实体
     * @param上一帧 上一tick的帧数据（用于旧值），首帧传null
     */
    public void 应用到活体(LivingEntity entity, @Nullable 实体帧数据 上一帧) {

        // ======== 第1步：load状态NBT ========
        if (状态NBT != null) {
            entity.load(状态NBT);
        }

        // ======== 第2步：旧值（插值起点） ========
        // MC用lerp(partialTick, 旧值, 当前值) 做帧间平滑
        //旧值必须 = 上一帧的值，这样插值结果平滑连续
        //
        // 位置有两组旧值：
        //   xo/yo/zo — Camera和部分渲染计算用
        //   xOld/yOld/zOld — LevelRenderer世界坐标定位用
        if (上一帧 != null) {
            // 位置旧值
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

            // 攻击动画旧值（手部挥砍渲染用）
            entity.oAttackAnim = 上一帧.attackAnim;
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

            entity.oAttackAnim = attackAnim;
        }

        // ======== 第3步：当前值（覆盖NBT） ========

        // 位置与朝向
        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yBodyRot = yBodyRot;
        entity.yHeadRot = yHeadRot;
        entity.setYHeadRot(yHeadRot);

        // 速度
        entity.setDeltaMovement(velX, velY, velZ);

        // 生命值
        entity.setHealth(生命值);

        // 攻击/挥手
        entity.attackAnim = attackAnim;
        entity.swinging = 正在挥手;
        entity.swingTime = 挥手进度;

        // 受击与死亡
        entity.hurtTime = 受击时间;
        entity.deathTime = 死亡时间;

        // 使用物品
        if (entity instanceof ILivingEntityAccess access) {
            if (正在使用物品 && 使用物品的手!= null) {
                ItemStack useStack = (使用物品的手 == InteractionHand.MAIN_HAND)
                        ? entity.getMainHandItem() : entity.getOffhandItem();
                access.puellamagi$setUseItem(useStack.copy());
                access.puellamagi$setUseItemRemaining(使用物品剩余时间);

                access.puellamagi$setLivingEntityFlag(1, true);
                access.puellamagi$setLivingEntityFlag(2,
                        使用物品的手 == InteractionHand.OFF_HAND);
            } else {
                access.puellamagi$setUseItem(ItemStack.EMPTY);
                access.puellamagi$setUseItemRemaining(0);

                access.puellamagi$setLivingEntityFlag(1, false);
                access.puellamagi$setLivingEntityFlag(2, false);
            }
        }

        //姿态
        entity.setPose(姿态);

        // 状态标志
        entity.setShiftKeyDown(正在潜行);
        entity.setSprinting(正在冲刺);
        entity.setOnGround(在地面上);

        // 装备
        if (主手物品 != null) entity.setItemSlot(EquipmentSlot.MAINHAND, 主手物品.copy());
        if (副手物品 != null) entity.setItemSlot(EquipmentSlot.OFFHAND, 副手物品.copy());
        if (头部装备 != null) entity.setItemSlot(EquipmentSlot.HEAD, 头部装备.copy());
        if (胸部装备 != null) entity.setItemSlot(EquipmentSlot.CHEST, 胸部装备.copy());
        if (腿部装备 != null) entity.setItemSlot(EquipmentSlot.LEGS, 腿部装备.copy());
        if (脚部装备 != null) entity.setItemSlot(EquipmentSlot.FEET, 脚部装备.copy());

        // ======== 第4步：驱动动画系统 ========
        //
        // tick被cancel后以下MC内部计算器全部停止：
        //   walkAnimation —腿部摆动（第三人称走路动画）
        //   walkDist — 手部摇摆幅度/步行音效
        //   bob — 镜头/手部上下晃动（第一人称走路手晃）
        //
        // 用上一帧和当前帧的位置差作为输入，手动调用它们
        // 这和MC在tick()中做的完全一样，只是输入来自录制数据

        // 计算本tick移动量
        double prevX = (上一帧 != null) ? 上一帧.x : x;
        double prevY = (上一帧 != null) ? 上一帧.y : y;
        double prevZ = (上一帧 != null) ? 上一帧.z : z;

        float dx = (float) (x - prevX);
        float dy = (float) (y - prevY);
        float dz = (float) (z - prevZ);
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);

        // --- walkAnimation：腿部摆动 ---
        // 复刻 LivingEntity.calculateEntityAnimation() 的逻辑
        // update()内部会自动管理speedOld和position的平滑过渡
        float totalDist = (float) Math.sqrt(
                dx * dx + (在地面上 ? 0 : dy * dy) + dz * dz);
        float animDelta = Math.min(totalDist * (在地面上 ? 4.0f : 1.0f), 1.0f);
        entity.walkAnimation.update(animDelta,0.4f);

        // --- walkDist：手部左右摇摆幅度 ---
        // 复刻 Entity.move() 中的 walkDist 累加逻辑
        entity.walkDistO = entity.walkDist;
        entity.walkDist += Math.min(horizontalDist * 0.6f, 1.0f);

        // --- bob：第一人称镜头/手部上下晃动 ---
        // bob/oBob 是 Player 专属字段，LivingEntity 没有
        // 不更新这个→ partialTick在两个旧值之间振荡 → 手部高频抖动
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            player.oBob = player.bob;
            float bobTarget = (在地面上 && 死亡时间 == 0)
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

        // 旧值— 两组都要设
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
        entity.setPose(姿态);
    }

    // ==================== Builder ====================

    public static class Builder {
        private final UUID 实体UUID;

        // 位置朝向
        private double x, y, z;
        private float yRot, xRot;
        private float yBodyRot, yHeadRot;

        // 速度
        private double velX, velY, velZ;

        // 生命
        private float 生命值, 最大生命值;

        // 动画
        private float walkAnimPos, walkAnimSpeed;
        private float attackAnim;
        private boolean 正在挥手;
        private int 挥手进度;
        private boolean 副手挥手;
        private int 受击时间;
        private int 死亡时间;

        // 使用物品
        private boolean 正在使用物品;
        private int 使用物品剩余时间;
        @Nullable private InteractionHand 使用物品的手;

        // 姿态
        private Pose 姿态 = Pose.STANDING;

        // 装备
        private ItemStack 主手物品, 副手物品;
        private ItemStack 头部装备, 胸部装备, 腿部装备, 脚部装备;

        // 状态
        private boolean 正在潜行, 正在冲刺, 在地面上;

        // NBT
        @Nullable private CompoundTag 状态NBT;

        public Builder(UUID uuid) {
            this.实体UUID = uuid;
        }

        public Builder 位置(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
            return this;
        }

        public Builder 朝向(float yRot, float xRot) {
            this.yRot = yRot; this.xRot = xRot;
            return this;
        }

        public Builder 身体朝向(float yBodyRot, float yHeadRot) {
            this.yBodyRot = yBodyRot; this.yHeadRot = yHeadRot;
            return this;
        }

        public Builder 速度(Vec3 vel) {
            this.velX = vel.x; this.velY = vel.y; this.velZ = vel.z;
            return this;
        }

        public Builder 生命(float current, float max) {
            this.生命值 = current; this.最大生命值 = max;
            return this;
        }

        public Builder 行走动画(float pos, float speed) {
            this.walkAnimPos = pos; this.walkAnimSpeed = speed;
            return this;
        }

        public Builder 攻击动画(float anim) {
            this.attackAnim = anim;
            return this;
        }

        public Builder 挥手(boolean swinging, int swingTime, boolean offHand) {
            this.正在挥手 = swinging;
            this.挥手进度 = swingTime;
            this.副手挥手 = offHand;
            return this;
        }

        public Builder 受击(int hurtTime) {
            this.受击时间 = hurtTime;
            return this;
        }

        public Builder 死亡(int deathTime) {
            this.死亡时间 = deathTime;
            return this;
        }

        public Builder 使用物品(boolean using, int remaining, @Nullable InteractionHand hand) {
            this.正在使用物品 = using;
            this.使用物品剩余时间 = remaining;
            this.使用物品的手 = hand;
            return this;
        }

        public Builder 姿态(Pose pose) {
            this.姿态 = pose;
            return this;
        }

        public Builder 全部装备(ItemStack main, ItemStack off,
                                ItemStack head, ItemStack chest,
                                ItemStack legs, ItemStack feet) {
            this.主手物品 = main;
            this.副手物品 = off;
            this.头部装备 = head;
            this.胸部装备 = chest;
            this.腿部装备 = legs;
            this.脚部装备 = feet;
            return this;
        }

        public Builder 潜行(boolean sneaking) {
            this.正在潜行 = sneaking;
            return this;
        }

        public Builder 冲刺(boolean sprinting) {
            this.正在冲刺 = sprinting;
            return this;
        }

        public Builder 在地面(boolean onGround) {
            this.在地面上 = onGround;
            return this;
        }

        /**
         * 设置状态NBT（仅在与上一帧不同时传入，相同时传null）
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
