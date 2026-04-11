package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.mixin.access.ServerPlayerGameModeAccessor;
import com.v2t.puellamagi.util.recording.*;
import com.v2t.puellamagi.util.实体工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * - 存储录制数据（实体帧 + 玩家输入帧 + 方块变化）
 * - 管理世界快照（回滚用）
 * - 管理NBT变化检测（每tick比较，只在变化时存储NBT）
 *
 * 纯输入回放架构：
 * - 不录制任何C2S包
 * - 玩家操作由客户端输入注入驱动，MC自己处理
 * - 结束快照保证最终结果正确
 */
public final class 录制管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/EpitaphRecorder");

    /** 默认录制范围（方块） */
    private static final double 录制范围 = 128.0;

    public static double 获取录制范围() {
        return 录制范围;
    }

    public static boolean 玩家是否在录制中(UUID playerUUID) {
        for (录制会话 session : 活跃会话.values()) {
            if (session.被录制实体.contains(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 录制会话 ====================

    /**
     * 单个使用者的录制会话
     */
    public static class 录制会话 {
        /** 通用帧数据（实体位置/朝向/动画+ 状态NBT） */
        public final 录制数据 帧数据;

        /** 玩家输入帧（20Hz移动方向） */
        public final Map<UUID, List<玩家输入帧>> 玩家输入表;

        /** 方块变化记录 */
        public final List<方块变化帧> 方块变化列表;

        /** 录制起点世界快照（回滚用） */
        public final 世界快照 起点快照;

        /** 录制范围中心*/
        public final Vec3 录制中心;

        /** 所在维度 */
        public final ServerLevel 维度;

        /** 被录制的实体UUID集合 */
        public final Set<UUID> 被录制实体;

        /** 每个实体上一次存储的状态NBT（用于变化检测） */
        public final Map<UUID, CompoundTag> 上次状态NBT缓存;

        /** 玩家鼠标增量数据 */
        public final Map<UUID, List<float[]>> 鼠标样本表 = new HashMap<>();

        /** 玩家最新输入缓冲区（覆盖式） */
        public final Map<UUID, 玩家输入帧> 最新输入缓冲 = new HashMap<>();

        /** 被录制玩家的快照（录制开始时拍的） */
        public final Map<UUID, 玩家快照> 玩家快照表;

        /** 录制结束时的玩家快照（结果驱动恢复用） */
        public Map<UUID, 玩家快照> 结束快照表;

        /** 录制开始时的初始状态 */
        public 录制初始状态 初始状态 = null;

        /** 方块实体变化记录 */
        public final List<方块实体变化帧> 方块实体变化列表 = new ArrayList<>();

        /** 方块实体NBT缓存（变化检测用） */
        public final Map<BlockPos, CompoundTag> 方块实体NBT缓存 = new HashMap<>();

        public final Map<UUID, List<String>> 玩家初始按键 = new HashMap<>();

        /** 影响标记表（时间删除用） */
        public final 影响标记表 标记表 = new 影响标记表();

        /** 影响记录（时间删除用） */
        public final 影响记录 影响 = new 影响记录();

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
        snapshot.采集范围内方块实体(level, center, 录制范围);

        // 确保正在被挖的方块被快照记录
        for (UUID entityUUID : entities) {
            Entity entity = 实体工具.按UUID查找实体(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                ServerPlayerGameModeAccessor gameMode =
                        (ServerPlayerGameModeAccessor) sp.gameMode;

                if (gameMode.puellamagi$isDestroyingBlock()) {
                    BlockPos pos = gameMode.puellamagi$getDestroyPos();
                    if (pos != null && !snapshot.包含方块(pos)) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            snapshot.添加方块(new 方块快照(pos, state, null));
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
            Entity entity = 实体工具.按UUID查找实体(level, entityUUID);
            if (entity instanceof ServerPlayer) {
                session.玩家输入表.put(entityUUID, new ArrayList<>());
            }
        }

        // 给每个范围内的玩家拍快照
        for (UUID entityUUID : entities) {
            Entity entity = 实体工具.按UUID查找实体(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                session.玩家快照表.put(entityUUID, 玩家快照.从玩家采集(sp));
            }
        }

        // 采集录制开始时的初始状态
        session.初始状态 = 录制初始状态.从玩家采集(user);

        活跃会话.put(userUUID, session);

        // 加入录制组（多人录制自动合并的关键对接点）
        录制组管理器.加入或创建录制组(userUUID, session, gameTime, level);

        // 通知被录制的玩家客户端开始录制
        // 区分录制者和被录制者：录制者收到"开始录制_录制者"，其他人收到"开始录制_被录制"
        // 客户端用这个标记判断是否应该触发过渡保护
        for (UUID entityUUID : entities) {
            Entity entity = 实体工具.按UUID查找实体(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                boolean 是录制者 = entityUUID.equals(userUUID);
                网络工具.发送给玩家(sp,
                        是录制者
                                ? com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包.开始录制_录制者()
                                : com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包.开始录制_被录制());
            }
        }

        LOGGER.info("玩家 {} 开始录制（范围内{} 个实体，最大{} 帧）",
                user.getName().getString(), entities.size(), maxFrames);
        return true;
    }

    /**
     * 采集一帧（每tick调用）
     */
    public static boolean 采集帧(UUID userUUID) {
        录制会话 session = 活跃会话.get(userUUID);
        if (session == null) return false;

        ServerLevel level = session.维度;

        Map<UUID, 实体帧数据> frameData = new HashMap<>();
        AABB box = AABB.ofSize(session.录制中心, 录制范围 * 2, 录制范围 * 2, 录制范围 * 2);

        for (Entity entity : level.getEntities((Entity) null, box, e ->
                e instanceof LivingEntity
                        || e instanceof net.minecraft.world.entity.projectile.Projectile
                        || e instanceof net.minecraft.world.entity.item.ItemEntity)) {

            UUID entityUUID = entity.getUUID();
            session.被录制实体.add(entityUUID);

            // 精简后：直接使用实体帧数据的静态工厂方法
            // 每帧都采集完整NBT，不再差分
            if (entity instanceof LivingEntity living) {
                frameData.put(entityUUID, 实体帧数据.从实体采集(living));
            } else {
                frameData.put(entityUUID, 实体帧数据.从普通实体采集(entity));
            }

            // 更新NBT缓存（帧实体NBT校验的查找期望NBT方法仍使用此缓存作为fallback）
            CompoundTag currentNBT = 实体帧数据.采集状态NBT(entity);
            session.上次状态NBT缓存.put(entityUUID, currentNBT);
        }

        boolean added = session.帧数据.添加帧(frameData);
        if (added) {
            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
            if (state != null) {
                state.增加录制帧();
            }

            for (Map.Entry<UUID, 玩家输入帧> entry : session.最新输入缓冲.entrySet()) {
                session.玩家输入表.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        // 检测方块实体NBT变化
        if (added) {
            检测方块实体变化(session, level);
        }

        return added;
    }

    // ==================== 物品NBT变化 ====================


    // ==================== 方块变化 ====================

    /**
     * 检测范围内方块实体的NBT变化
     * 每tick调用，和方块变化帧同样的模式
     *
     * 触发者判断：检查哪个被录制的玩家正在打开这个方块实体对应的容器
     */
    private static void 检测方块实体变化(录制会话 session, ServerLevel level) {
        int tickIndex = session.帧数据.获取总帧数() - 1;
        double range = 录制范围;
        net.minecraft.world.phys.Vec3 center = session.录制中心;

        int minChunkX = (int) Math.floor(center.x - range) >> 4;
        int maxChunkX = (int) Math.floor(center.x + range) >> 4;
        int minChunkZ = (int) Math.floor(center.z - range) >> 4;
        int maxChunkZ = (int) Math.floor(center.z + range) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;

                var chunk = level.getChunk(cx, cz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();

                    double dx = pos.getX() - center.x;
                    double dy = pos.getY() - center.y;
                    double dz = pos.getZ() - center.z;
                    if (dx * dx + dy * dy + dz * dz > range * range) continue;

                    net.minecraft.world.level.block.entity.BlockEntity be = entry.getValue();
                    CompoundTag currentNBT = be.saveWithoutMetadata();

                    CompoundTag lastNBT = session.方块实体NBT缓存.get(pos);
                    if (lastNBT == null) {
                        session.方块实体NBT缓存.put(pos, currentNBT.copy());
                    } else if (!currentNBT.equals(lastNBT)) {
                        // 判断触发者：谁在打开这个容器
                        UUID triggerUUID = 查找容器操作者(session, level, pos);

                        session.方块实体变化列表.add(new 方块实体变化帧(
                                pos, lastNBT.copy(), currentNBT.copy(), tickIndex, triggerUUID));
                        session.方块实体NBT缓存.put(pos, currentNBT.copy());
                    }
                }
            }
        }
    }
    /*
     * 查找正在操作指定方块实体容器的玩家
     *
     * 通过检查被录制玩家的containerMenu是否关联到这个方块位置
     * 返回第一个匹配的玩家UUID，没有则返回null
     */
    @Nullable
    private static UUID 查找容器操作者(录制会话 session, ServerLevel level, BlockPos pos) {
        for (UUID playerUUID : session.被录制实体) {
            Entity entity = 实体工具.按UUID查找实体(level, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            // 检查玩家当前打开的容器是否关联到这个方块位置
            if (sp.containerMenu != sp.inventoryMenu) {
                //玩家打开了某个容器
                // 检查容器的槽位是否来自这个方块实体
                for (net.minecraft.world.inventory.Slot slot : sp.containerMenu.slots) {
                    if (slot.container instanceof net.minecraft.world.level.block.entity.BlockEntity slotBE) {
                        if (slotBE.getBlockPos().equals(pos)) {
                            return playerUUID;
                        }
                    }
                }
            }
        }
        return null;
    }


    @Nullable
    private static CompoundTag 获取方块实体NBT(ServerLevel level, BlockPos pos, BlockState oldState) {
        if (!oldState.hasBlockEntity()) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            return be.saveWithoutMetadata();
        }
        return null;
    }

    /**
     * 记录方块变化
     *由事件监听器在方块变化时调用
     *
     * @param triggerUUID 触发者UUID，null表示非玩家触发
     */
    public static void 记录方块变化(ServerLevel level, BlockPos pos,BlockState oldState, BlockState newState,
                                    @Nullable UUID triggerUUID) {
        for (Map.Entry<UUID, 录制会话> entry : 活跃会话.entrySet()) {
            录制会话 session = entry.getValue();

            if (session.维度 != level) continue;
            if (pos.distSqr(BlockPos.containing(session.录制中心)) > 录制范围 * 录制范围) {
                continue;
            }

            int tickIndex = session.帧数据.获取总帧数();
            session.方块变化列表.add(new 方块变化帧(pos, oldState, newState, tickIndex, triggerUUID));

            if (!session.起点快照.包含方块(pos)) {
                session.起点快照.添加方块(new 方块快照(pos, oldState,
                        获取方块实体NBT(level, pos, oldState)));
            }
        }
    }

    // ==================== 停止/取消录制 ====================

    /**
     * 停止录制
     *
     * @return 录制会话（用于复刻），null表示不存在
     */
    @Nullable
    public static 录制会话 停止录制(UUID userUUID) {
        录制会话 session = 活跃会话.remove(userUUID);

        if (session != null) {
            // 通知客户端停止录制
            for (UUID entityUUID : session.被录制实体) {
                Entity entity = 实体工具.按UUID查找实体(session.维度, entityUUID);
                if (entity instanceof ServerPlayer sp) {
                    网络工具.发送给玩家(sp,
                            com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包.停止录制());
                }
            }

            // 拍录制结束时的玩家快照（结果驱动恢复用）
            session.结束快照表 = new HashMap<>();
            for (UUID entityUUID : session.被录制实体) {
                Entity entity = 实体工具.按UUID查找实体(session.维度, entityUUID);
                if (entity instanceof ServerPlayer sp) {
                    session.结束快照表.put(entityUUID, 玩家快照.从玩家采集(sp));
                }
            }

            // 通知录制组管理器该录制段结束
            录制组管理器.标记录制段结束(userUUID, session.维度.getGameTime());

            LOGGER.info("玩家 {} 停止录制（共{} 帧，{} 个方块变化）",
                    userUUID, session.帧数据.获取总帧数(),
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
            for (UUID entityUUID : removed.被录制实体) {
                Entity entity = 实体工具.按UUID查找实体(removed.维度, entityUUID);
                if (entity instanceof ServerPlayer sp) {
                    网络工具.发送给玩家(sp,
                            com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包.停止录制());
                }
            }

            // 通知录制组管理器取消
            录制组管理器.取消录制段(userUUID);
        }
    }

    // ==================== 客户端输入接收 ====================

    /**
     * 接收客户端上报的输入帧（覆盖式缓冲）
     */
    public static void 接收客户端输入(UUID playerUUID, 玩家输入帧 input) {
        for (录制会话 session : 活跃会话.values()) {
            if (session.被录制实体.contains(playerUUID)) {
                session.最新输入缓冲.put(playerUUID, input);
                return;
            }
        }
    }

    // ==================== 客户端鼠标接收 ====================

    /**
     * 接收客户端上报的鼠标增量样本
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

    /**
     * 接收客户端上报的初始按键状态
     *录制开始时每个被录制的玩家上报自己按住的键
     */
    public static void 接收按键状态上报(UUID playerUUID, List<String> heldKeys) {
        for (录制会话 session : 活跃会话.values()) {
            if (session.被录制实体.contains(playerUUID)) {
                session.玩家初始按键.put(playerUUID, new ArrayList<>(heldKeys));
                LOGGER.info("收到玩家 {} 的初始按键上报：{} 个键按住", playerUUID, heldKeys.size());
                return;
            }
        }}

    // ==================== 查询====================

    @Nullable
    public static 录制会话 获取会话(UUID userUUID) {
        return 活跃会话.get(userUUID);
    }

    public static boolean 是否录制中(UUID userUUID) {
        return 活跃会话.containsKey(userUUID);
    }

    public static List<UUID> 获取所有活跃使用者() {
        return new ArrayList<>(活跃会话.keySet());
    }

    // ==================== Builder构建工具（精简后不再需要） ====================
    // 实体帧数据精简后，采集逻辑已移到实体帧数据.从实体采集()和从普通实体采集()
    // 不再需要手动构建30+个字段的Builder

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


    // ==================== 生命周期 ====================

    public static void 玩家下线(UUID playerUUID) {
        取消录制(playerUUID);
    }

    public static void 清除全部() {
        活跃会话.clear();
    }
}
