package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.mixin.access.ServerPlayerGameModeAccessor;
import com.v2t.puellamagi.util.recording.*;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 录制管理器
 *
 * 职责：
 * - 管理预知能力的录制流程（开始/停止/采集）
 * - 存储录制数据（实体帧 + 玩家输入帧 + 鼠标增量 + 方块变化）
 * - 管理世界快照（回滚用）
 * - 管理NBT变化检测（每tick比较，只在变化时存储NBT）
 *
 * 每个使用者独立维护一份录制会话
 * 录制在服务端静默执行，不通知任何玩家
 */
public final class 录制管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/EpitaphRecorder");

    /** 默认录制范围（方块） */
    private static final double 录制范围 = 128.0;

    /**
     * 获取录制范围（方块）
     * 供其他系统读取，避免硬编码重复值
     */
    public static double 获取录制范围() {
        return 录制范围;
    }

    /**
     * 检查玩家是否在任何录制会话中被录制
     */
    public static boolean 玩家是否在录制中(UUID playerUUID) {
        for (录制会话 session : 活跃会话.values()) {
            if (session.被录制实体.contains(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    //==================== 录制会话 ====================

    /**
     * 单个使用者的录制会话
     */
    public static class 录制会话 {
        /** 通用帧数据（实体位置/朝向/动画+ 状态NBT） */
        public final 录制数据 帧数据;

        /** 玩家输入帧（epitaph专用，20Hz移动方向） */
        public final Map<UUID, List<玩家输入帧>> 玩家输入表;

        /** 方块变化记录 */
        public final List<方块变化帧> 方块变化列表;

        /** 按tick编号 + 玩家UUID存储的交互包帧 */
        public final Map<Integer, Map<UUID, 交互包帧>> 交互包表 = new HashMap<>();

        /** 录制起点世界快照（回滚用） */
        public final 世界快照 起点快照;

        /** 录制范围中心（使用者激活时的位置） */
        public final Vec3 录制中心;

        /** 所在维度 */
        public final ServerLevel 维度;

        /** 被录制的实体UUID集合（动态扩展，包含录制中新出现的实体） */
        public final Set<UUID> 被录制实体;

        /**
         * 每个实体上一次存储的状态NBT（用于变化检测）
         * key = 实体UUID, value = 上一次存储时的状态NBT
         * 首次出现的实体没有条目 → 视为"有变化" → 强制存储
         */
        public final Map<UUID, CompoundTag> 上次状态NBT缓存;

        /**
         * 玩家鼠标增量数据（扁平列表，所有样本按时间顺序排列）
         * 不按tick分组，回放时按帧数均匀分配
         */
        public final Map<UUID, List<float[]>> 鼠标样本表 = new HashMap<>();

        /**
         * 玩家最新输入缓冲区（覆盖式）
         * 客户端C2S包到达时更新，采集帧时读取
         * 保证输入帧和实体帧1:1同步
         */
        public final Map<UUID, 玩家输入帧> 最新输入缓冲 = new HashMap<>();

        /** 被录制玩家的快照（录制开始时拍的）*/
        public final Map<UUID, 玩家快照>玩家快照表;

        /** 录制结束时的玩家快照（校验用） */
        public Map<UUID, 玩家快照> 结束快照表;



        public 录制会话(录制数据 data, 世界快照 snapshot, Vec3 center,ServerLevel level, Set<UUID> entities) {
            this.帧数据 = data;
            this.玩家输入表 = new HashMap<>();
            this.方块变化列表 = new ArrayList<>();
            this.起点快照 = snapshot;
            this.录制中心 = center;
            this.维度 = level;
            this.被录制实体 = entities;
            this.上次状态NBT缓存 = new HashMap<>();
            this.玩家快照表 = new HashMap<>();
        }
    }

    /** 活跃的录制会话：使用者UUID → 会话 */
    private static final Map<UUID, 录制会话> 活跃会话 = new ConcurrentHashMap<>();

    private 录制管理器() {}

    // ==================== 录制控制 ====================

    /**
     * 开始录制
     *
     * @param user 使用者
     * @param maxFrames 最大帧数
     * @return 是否成功
     */
    public static boolean 开始录制(ServerPlayer user, int maxFrames) {
        UUID userUUID = user.getUUID();

        if (活跃会话.containsKey(userUUID)) {
            LOGGER.warn("玩家 {} 已有活跃录制会话", user.getName().getString());
            return false;
        }

        ServerLevel level = user.serverLevel();
        Vec3 center = user.position();
        long gameTime = level.getGameTime();

        // 确定录制范围内的实体
        Set<UUID> entities = 收集范围内实体UUID(level, center);

        // 拍摄世界快照
        世界快照 snapshot = new 世界快照(gameTime);
        snapshot.采集范围内实体(level, center, 录制范围);

        // 确保正在被挖的方块被快照记录（即使同一tick被破坏了）
        // 情况：方块在拍快照的同一tick被破坏 → 快照里是空气
        // 回滚时恢复的是空气 → 方块凭空消失
        for (UUID entityUUID : entities) {
            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                ServerPlayerGameModeAccessor gameMode =(ServerPlayerGameModeAccessor) sp.gameMode;

                if (gameMode.puellamagi$isDestroyingBlock()) {
                    BlockPos pos = gameMode.puellamagi$getDestroyPos();
                    if (pos != null && !snapshot.包含方块(pos)) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            snapshot.添加方块(new com.v2t.puellamagi.util.recording
                                    .方块快照(pos, state, null));
                            LOGGER.debug("补录正在挖的方块到快照: pos={}, state={}", pos, state);
                        }
                    }
                }
            }
        }

        // 创建录制数据
        录制数据 data = new 录制数据(gameTime, maxFrames);

        // 创建会话
        录制会话 session = new 录制会话(data, snapshot, center, level, entities);

        // 初始化每个玩家的输入帧列表
        for (UUID entityUUID : entities) {
            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity instanceof ServerPlayer) {
                session.玩家输入表.put(entityUUID, new ArrayList<>());
            }
        }

        // 给每个范围内的玩家拍快照
        for (UUID entityUUID : entities) {
            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                session.玩家快照表.put(entityUUID, 玩家快照.从玩家采集(sp));
            }
        }

        // 补录正在进行中的状态
        // 如果玩家在按下录制键之前就已经在使用物品（吃东西/拉弓等）
        // 录制不会捕获"开始使用"的包（发生在录制之前）
        // 回放时服务端不知道玩家在使用物品 → 使用失败
        // 这里手动补一个"开始使用物品"的包到第0帧
        // 删掉这整段
        // for (UUID entityUUID : entities) {
        //     Entity entity = findEntityByUUID(level, entityUUID);
        //     if (entity instanceof ServerPlayer sp && sp.isUsingItem()) {
        //         Map<UUID, 交互包帧> tickMap = session.交互包表.computeIfAbsent(0,
        //                 k -> new HashMap<>());
        //         交互包帧 firstFrame = tickMap.computeIfAbsent(entityUUID,
        //                 k -> new交互包帧(0));
        //         firstFrame.添加(new 交互包帧.使用物品包(
        //                 sp.getUsedItemHand(),
        //                 0
        //         ));
        //         LOGGER.debug("补录玩家 {} 的使用物品状态: hand={}",
        //                 sp.getName().getString(), sp.getUsedItemHand());
        //     }
        // }

        活跃会话.put(userUUID, session);

        // 通知被录制的玩家客户端开始录制
        // 客户端收到后每tick发送录制输入上报包（C2S）
        for (UUID entityUUID : entities) {
            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                网络工具.发送给玩家(
                        sp,
                        new com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包(true)
                );
            }
        }

        LOGGER.info("玩家 {} 开始录制（范围内{} 个实体，最大 {} 帧）",
                user.getName().getString(), entities.size(), maxFrames);
        return true;
    }

    /**
     * 采集一帧（每tick调用）
     *
     *采集流程：
     * 1.扫描范围内所有实体
     * 2. 对每个实体：
     *    a. 采集手动字段（位置/朝向/动画等）
     *    b. 采集状态NBT → 与上次比较
     *    c. 有变化 → 帧数据包含NBT → 更新缓存
     *    d. 无变化 → 帧数据不包含NBT → 节省存储
     * 3. 从输入缓冲区读取玩家输入（1:1对齐）
     *
     * 注意：鼠标数据不在这里处理（直接追加模式，不按tick对齐）
     *
     * @param userUUID 使用者UUID
     * @return 是否成功（false表示录制已满或不存在）
     */
    public static boolean 采集帧(UUID userUUID) {
        录制会话 session = 活跃会话.get(userUUID);
        if (session == null) return false;

        ServerLevel level = session.维度;

        // 扫描范围内所有实体
        Map<UUID, 实体帧数据> frameData = new HashMap<>();
        AABB box = AABB.ofSize(session.录制中心, 录制范围 * 2, 录制范围 * 2, 录制范围 * 2);

        for (Entity entity : level.getEntities((Entity) null, box, e ->
                e instanceof LivingEntity
                        || e instanceof net.minecraft.world.entity.projectile.Projectile
                        || e instanceof net.minecraft.world.entity.item.ItemEntity)) {

            UUID entityUUID = entity.getUUID();

            // 动态扩展被录制实体集合
            session.被录制实体.add(entityUUID);

            if (entity instanceof LivingEntity living) {
                // 手动字段
                实体帧数据.Builder builder = 从活体采集到Builder(living);

                // NBT变化检测
                CompoundTag currentNBT = 实体帧数据.采集状态NBT(living);
                CompoundTag lastNBT = session.上次状态NBT缓存.get(entityUUID);

                if (!实体帧数据.NBT相同(currentNBT, lastNBT)) {
                    // 有变化 → 存入帧数据 + 更新缓存
                    builder.状态NBT(currentNBT.copy());
                    session.上次状态NBT缓存.put(entityUUID, currentNBT);
                }

                frameData.put(entityUUID, builder.构建());
            } else {
                // 非活体实体（投射物等）：同样做NBT变化检测
                实体帧数据.Builder builder = 从普通实体采集到Builder(entity);

                CompoundTag currentNBT = 实体帧数据.采集状态NBT(entity);
                CompoundTag lastNBT = session.上次状态NBT缓存.get(entityUUID);

                if (!实体帧数据.NBT相同(currentNBT, lastNBT)) {
                    builder.状态NBT(currentNBT.copy());
                    session.上次状态NBT缓存.put(entityUUID, currentNBT);
                }

                frameData.put(entityUUID, builder.构建());
            }
        }

        boolean added = session.帧数据.添加帧(frameData);
        if (added) {
            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
            if (state != null) {
                state.增加录制帧();
            }

            // 从缓冲区读取玩家输入，和实体帧 1:1 添加
            //缓冲区保留当前值（不清除），下次采集帧时如果没有新包就复用上一次的
            for (Map.Entry<UUID, 玩家输入帧> entry : session.最新输入缓冲.entrySet()) {
                session.玩家输入表.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }

        }

        return added;
    }

    /**
     * 获取方块实体NBT（如果有的话）
     *
     * 注意：只有旧状态也有方块实体时才需要存NBT
     * 如果旧状态是空气，没有方块实体
     */
    @Nullable
    private static CompoundTag 获取方块实体NBT(ServerLevel level, BlockPos pos, BlockState oldState) {
        // 如果旧状态没有方块实体，不需要NBT
        if (!oldState.hasBlockEntity()) {
            return null;
        }
        // 事件触发时方块可能已变，旧状态的NBT已经拿不到了
        // 但如果当前方块实体还在且类型匹配，可以尝试获取
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            return be.saveWithoutMetadata();
        }
        return null;
    }

    /**
     * 记录方块变化
     * 由事件监听器在方块变化时调用
     */
    public static void 记录方块变化(ServerLevel level, BlockPos pos,BlockState oldState, BlockState newState) {
        for (Map.Entry<UUID, 录制会话> entry : 活跃会话.entrySet()) {
            录制会话 session = entry.getValue();

            // 只记录同维度且在范围内的变化
            if (session.维度 != level) continue;
            if (pos.distSqr(BlockPos.containing(session.录制中心)) > 录制范围 * 录制范围) {
                continue;
            }

            int tickIndex = session.帧数据.获取总帧数();
            session.方块变化列表.add(new 方块变化帧(pos, oldState, newState, tickIndex));

            // 同时记录到起点快照中（如果还没记录过这个位置）
            if (!session.起点快照.包含方块(pos)) {
                session.起点快照.添加方块(new 方块快照(pos, oldState,获取方块实体NBT(level, pos, oldState)));
            }
        }
    }

    /**
     * 停止录制
     *
     * @param userUUID 使用者UUID
     * @return 录制会话（用于复刻），null表示不存在
     */
    @Nullable
    public static 录制会话 停止录制(UUID userUUID) {
        录制会话 session = 活跃会话.remove(userUUID);
        if (session != null) {
            // 通知被录制的玩家客户端停止录制
            for (UUID entityUUID : session.被录制实体) {
                Entity entity = findEntityByUUID(session.维度, entityUUID);
                if (entity instanceof ServerPlayer sp) {
                    网络工具.发送给玩家(
                            sp,
                            new com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包(false)
                    );
                }
            }

            // 诊断：对比所有帧数
            int 实体帧数 = session.帧数据.获取总帧数();
            for (Map.Entry<UUID, List<玩家输入帧>> entry :
                    session.玩家输入表.entrySet()) {
                int 输入帧数 = entry.getValue().size();

            }

            // 诊断：鼠标数据
            for (Map.Entry<UUID, List<float[]>> mouseEntry :
                    session.鼠标样本表.entrySet()) {
                int totalSamples = mouseEntry.getValue().size();
                LOGGER.info("玩家 {} 鼠标: {} 样本, 实体帧: {}",
                        mouseEntry.getKey(), totalSamples, 实体帧数);
            }

            //拍录制结束时的玩家快照（校验用）
            // 回放结束时玩家状态应该和这个一致
            session.结束快照表= new HashMap<>();
            for (UUID entityUUID : session.被录制实体) {
                Entity entity = findEntityByUUID(session.维度, entityUUID);
                if (entity instanceof ServerPlayer sp) {
                    session.结束快照表.put(entityUUID, 玩家快照.从玩家采集(sp));
                }
            }

            LOGGER.info("玩家 {} 停止录制（共{} 帧，{} 个方块变化）",
                    userUUID, 实体帧数,
                    session.方块变化列表.size());
        }
        return session;
    }

    /**
     * 取消录制（不保留数据）
     */
    public static void 取消录制(UUID userUUID) {
        录制会话 removed = 活跃会话.remove(userUUID);
        if (removed != null) {
            // 通知客户端停止录制
            for (UUID entityUUID : removed.被录制实体) {
                Entity entity = findEntityByUUID(removed.维度, entityUUID);
                if (entity instanceof ServerPlayer sp) {
                    网络工具.发送给玩家(
                            sp,
                            new com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包(false)
                    );
                }
            }
            LOGGER.debug("玩家 {} 取消录制", userUUID);
        }
    }

    // ==================== 客户端输入接收 ====================

    /**
     * 接收客户端上报的输入帧（覆盖式缓冲）
     *
     * 客户端每tick发C2S包，但频率可能和服务端tick不一致
     * 所以不直接添加到列表，而是更新缓冲区
     * 采集帧时从缓冲区读取 → 保证输入帧和实体帧 1:1
     *
     * @param playerUUID 玩家UUID
     * @param input 客户端上报的输入帧
     */
    public static void 接收客户端输入(UUID playerUUID, 玩家输入帧 input) {
        for (录制会话 session : 活跃会话.values()) {
            if (session.被录制实体.contains(playerUUID)) {
                session.最新输入缓冲.put(playerUUID, input);
                return;
            }
        }
    }

    /**
     * 接收服务端拦截到的C2S交互包
     *
     * 由EpitaphInputInterceptMixin在拦截包时调用
     *录制中才存，不录制时忽略
     *
     * @param playerUUID 发包的玩家
     * @param record     包记录
     */
    public static void 接收交互包(UUID playerUUID, 交互包帧.包记录 record) {
        for (录制会话 session : 活跃会话.values()) {
            if (!session.被录制实体.contains(playerUUID)) continue;

            int currentTick = session.帧数据.获取总帧数();

            Map<UUID, 交互包帧> tickMap = session.交互包表.computeIfAbsent(currentTick, k -> new HashMap<>());
            交互包帧 frame = tickMap.computeIfAbsent(
                    playerUUID, k -> new 交互包帧(currentTick));
            frame.添加(record);
            return;
        }
    }

    // ==================== 客户端鼠标接收 ====================

    /**
     * 接收客户端上报的鼠标增量样本（扁平追加）
     *
     * 白话：客户端发来多少就直接存多少，不管是哪个tick发的
     * 回放时按帧数均匀分配
     */
    public static void 接收客户端鼠标样本(UUID playerUUID, List<float[]> samples) {
        for (录制会话 session : 活跃会话.values()) {
            if (session.被录制实体.contains(playerUUID)) {
                session.鼠标样本表.computeIfAbsent(playerUUID, k -> new ArrayList<>())
                        .addAll(samples);
                return;
            }
        }
    }


    // ==================== 查询====================

    @Nullable
    public static 录制会话 获取会话(UUID userUUID) {
        return 活跃会话.get(userUUID);
    }

    public static boolean 是否录制中(UUID userUUID) {
        return 活跃会话.containsKey(userUUID);
    }

    /**
     * 获取所有活跃录制会话的使用者UUID
     * 返回副本，避免遍历时并发修改
     */
    public static List<UUID> 获取所有活跃使用者() {
        return new ArrayList<>(活跃会话.keySet());
    }

    // ==================== Builder构建工具 ====================

    /**
     * 从活体实体采集手动字段到Builder（不含NBT）
     * NBT由调用方判断后通过 .状态NBT() 传入
     */
    private static 实体帧数据.Builder 从活体采集到Builder(LivingEntity entity) {
        return new 实体帧数据.Builder(entity.getUUID())
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
                .在地面(entity.onGround());
    }

    /**
     * 从普通实体采集手动字段到Builder（不含NBT）
     */
    private static 实体帧数据.Builder 从普通实体采集到Builder(Entity entity) {
        return new 实体帧数据.Builder(entity.getUUID())
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
                .在地面(entity.onGround());
    }

    // ==================== 工具方法 ====================

    private static Set<UUID> 收集范围内实体UUID(ServerLevel level, Vec3 center) {
        Set<UUID> result = new HashSet<>();
        AABB box = AABB.ofSize(center, 录制范围 * 2, 录制范围 * 2, 录制范围 * 2);

        for (Entity entity : level.getEntities((Entity) null, box, e ->
                e instanceof LivingEntity
                        || e instanceof net.minecraft.world.entity.projectile.Projectile
                        || e instanceof net.minecraft.world.entity.item.ItemEntity)) {
            result.add(entity.getUUID());
        }
        return result;
    }

    @Nullable
    private static Entity findEntityByUUID(ServerLevel level, UUID uuid) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    // ==================== 生命周期 ====================

    public static void 玩家下线(UUID playerUUID) {
        取消录制(playerUUID);}

    public static void 清除全部() {
        活跃会话.clear();

        LOGGER.debug("录制管理器已清空");
    }
}
