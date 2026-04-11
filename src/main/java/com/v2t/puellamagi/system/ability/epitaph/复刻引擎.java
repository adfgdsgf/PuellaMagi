package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.core.network.packets.s2c.复刻帧同步包;
import com.v2t.puellamagi.core.network.packets.s2c.方块批量更新包;
import com.v2t.puellamagi.core.network.packets.s2c.时删方块忽略包;
import com.v2t.puellamagi.core.network.packets.s2c.背包同步包;
import com.v2t.puellamagi.util.network.存在屏蔽器;
import com.v2t.puellamagi.util.network.输入接管器;
import com.v2t.puellamagi.util.recording.实体帧数据;
import com.v2t.puellamagi.util.recording.世界快照;
import com.v2t.puellamagi.util.recording.玩家快照;
import com.v2t.puellamagi.util.实体工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复刻引擎 — 预知/时间删除的核心执行器
 *
 * 架构（纯输入回放）：
 * -玩家：客户端注入全部按键（包括attack/use）→ MC自己处理一切
 * - 怪物：帧驱动（cancel tick + 每tick用帧数据设状态）
 * - 结束快照：保证最终结果一定等于录制结束时的真实状态
 *
 * 不再有包录制/回放：
 * - 不录制任何C2S包
 * - 不重放任何C2S包
 * - 所有操作由MC根据注入的输入自己处理
 * - 动画/音效/物品消耗全部自然产生
 * - 自动兼容所有mod
 *
 * 流程：
 * 1. 回滚世界到录制起点
 * 2. 清除录制期间出现的实体
 * 3. 每tick：驱动怪物帧 → 同步玩家位置 → 发帧同步包给客户端
 * 4. 时间删除：释放使用者，其他人继续按命运行动
 * 5. 结束：恢复录制结束快照 → 同步位置 → 释放锁定
 */
public final class 复刻引擎 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/ReplayEngine");

    private static final String 接管来源 = "epitaph";
    private static final String 屏蔽来源 = "epitaph";
    private static final String 淡出屏蔽来源 = "epitaph_fadeout";

    // ==================== 命运位置缓存 ====================

    /**
     * 命运位置数据
     *
     * 时删期间使用者的命运帧位置（录制中A的位置）
     * 用于sendChanges位置欺骗：发包前临时替换实体位置为命运位置
     */
    public static class 命运位置 {
        public final double x, y, z;
        public final float yRot, xRot;
        public final float yBodyRot, yHeadRot;

        public 命运位置(double x, double y, double z,
                       float yRot, float xRot,
                       float yBodyRot, float yHeadRot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.yBodyRot = yBodyRot;
            this.yHeadRot = yHeadRot;
        }
    }

    /**
     * 命运位置缓存：时删使用者UUID → 命运帧位置
     *
     * 每tick由驱动实体帧()更新
     * sendChanges位置欺骗时由Mixin读取
     * 时删结束时清除
     */
    private static final Map<UUID, 命运位置> 命运位置缓存 = new ConcurrentHashMap<>();

    // ==================== 复刻会话 ====================

    /**
     * 复刻会话 — 一个录制组的回放实例
     *
     * 多人预知：一个录制组 = 一个复刻会话
     * 单人预知：退化为录制组中只有一个录制段
     *
     * 被锁定玩家：客户端输入被替换为录制数据，MC自己处理操作
     * 被控制实体：tick被cancel，由帧数据驱动（怪物/投射物等）
     * 时删中玩家：已从被锁定状态脱离，自由活动+因果追踪
     */
    public static class 复刻会话 {
        /** 录制组ID（唯一标识这个复刻会话） */
        public final UUID 组ID;

        /** 合并后的录制数据（替代原来的单个录制会话） */
        public final 合并录制数据 录制;

        /** 维度 */
        public final ServerLevel 维度;

        /** 被锁定玩家集合（输入被回放控制） */
        public final Set<UUID> 被锁定玩家;

        /** 被控制实体集合（帧驱动的怪物等） */
        public final Set<UUID> 被控制实体;

        /** 时删中玩家集合（从被锁定脱离、自由活动的玩家） */
        public final Set<UUID> 时删中玩家 = new HashSet<>();

        /** 所有录制者UUID（录制组中的使用者们） */
        public final Set<UUID> 录制者集合;

        /**
         * 是否有任何玩家在时删中
         * 替代原来的 boolean 时间删除中
         */
        public boolean 有时删中玩家() {
            return !时删中玩家.isEmpty();
        }

        public 复刻会话(UUID groupID, 合并录制数据 mergedData, ServerLevel level) {
            this.组ID = groupID;
            this.录制 = mergedData;
            this.维度 = level;
            this.被锁定玩家 = new HashSet<>();
            this.被控制实体 = new HashSet<>(mergedData.获取被录制实体集合());
            this.录制者集合 = new HashSet<>(mergedData.获取录制者集合());
        }
    }

    /** 活跃的复刻会话：录制组ID → 会话 */
    private static final Map<UUID, 复刻会话> 活跃会话 = new ConcurrentHashMap<>();

    /** 玩家到录制组的反查映射：玩家UUID → 录制组ID（包括录制者和被锁定者） */
    private static final Map<UUID, UUID> 玩家组映射 = new ConcurrentHashMap<>();

    // ==================== 回放方块变化追踪 ====================

    /**
     * 回放期间实际发生的方块变化追踪
     *
     * 每帧开始前清空，帧末尾用于反向保底对比
     * key = 变化的方块位置，value = 变化前的旧状态
     * 由预知录制事件在方块放置/破坏时调用记录
     */
    private static final Map<BlockPos, BlockState> 回放中实际方块变化 = new ConcurrentHashMap<>();

    /** 是否正在回放中（帧方块修正自身的修改不记录） */
    private static volatile boolean 正在执行帧修正 = false;

    /**
     * 记录回放期间的方块变化（由预知录制事件调用）
     *
     * 只记录非帧修正产生的变化（输入回放导致MC自己做的变化）
     * 帧修正自身产生的变化不记录（避免自循环）
     *
     * @param pos 变化的方块位置
     * @param oldState 变化前的旧状态
     */
    public static void 记录回放中方块变化(BlockPos pos, BlockState oldState) {
        if (正在执行帧修正) return;
        if (活跃会话.isEmpty()) return;
        回放中实际方块变化.put(pos.immutable(), oldState);
    }

    /**
     * 查询是否有活跃的复刻会话（供事件监听器判断是否需要记录）
     */
    public static boolean 有活跃回放() {
        return !活跃会话.isEmpty();
    }

    private 复刻引擎() {}

    // ==================== 核心流程 ====================

    /**
     * 开始复刻（基于合并录制数据）
     *
     * 1. 预更新客户端方块（消除视觉延迟）
     * 2. 回滚世界到录制起点
     * 3. 清除录制期间出现的实体
     * 4. 恢复玩家快照 + 同步背包
     * 5. 标记被锁定玩家 + 从帧驱动中移除玩家（让tick正常跑）
     *
     * @param groupID    录制组ID
     * @param mergedData 合并后的录制数据
     * @return 是否成功
     */
    public static boolean 开始复刻(UUID groupID, 合并录制数据 mergedData) {
        ServerLevel level = mergedData.获取维度();

        // 1. 提前通知客户端方块变化
        发送方块预更新包(mergedData.获取起点快照(), level);

        // 2. 回滚世界
        int[] result = mergedData.获取起点快照().恢复到(level);
        LOGGER.info("世界回滚完成：{}个实体, {} 个方块", result[0], result[1]);

        // 3. 清除录制期间出现的实体（防刷物品/刷箭）
        int 清除数量 = 清除录制期间出现的实体(level, mergedData);
        if (清除数量 > 0) {
            LOGGER.info("清除了 {} 个录制期间出现的实体", 清除数量);
        }

        // 4. 恢复玩家快照 + 强制同步背包
        for (Map.Entry<UUID, 玩家快照> entry : mergedData.获取玩家快照表().entrySet()) {
            Entity entity = 实体工具.按UUID查找实体(level, entry.getKey());
            if (entity instanceof ServerPlayer sp) {
                entry.getValue().恢复到(sp);
                网络工具.发送给玩家(sp, 背包同步包.从玩家构建(sp));
            }
        }

        // 5. 创建会话
        复刻会话 session = new 复刻会话(groupID, mergedData, level);

        for (UUID entityUUID : mergedData.获取被录制实体集合()) {
            Entity entity = 实体工具.按UUID查找实体(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(),
                        sp.getYRot(), sp.getXRot());

                session.被控制实体.remove(entityUUID);
                session.被锁定玩家.add(entityUUID);

                // 服务端恢复使用物品状态（每个录制者有自己的初始状态）
                录制初始状态 initState = mergedData.获取初始状态(entityUUID);
                if (initState != null && initState.有进行中的操作()) {
                    initState.恢复到服务端(sp);
                }

                // 通知客户端恢复按键状态（通用，覆盖所有mod）
                List<String> heldKeys = mergedData.获取初始按键(entityUUID);
                boolean leftHeld = initState != null
                        && initState.是否正在破坏方块();
                boolean rightHeld = initState != null
                        && initState.是否正在使用物品();

                if ((heldKeys != null && !heldKeys.isEmpty()) || leftHeld || rightHeld) {
                    网络工具.发送给玩家(sp,
                            com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包.恢复按键(
                                    heldKeys != null ? heldKeys : new ArrayList<>(),
                                    leftHeld,
                                    rightHeld));
                }

                // 注册玩家到录制组映射
                玩家组映射.put(entityUUID, groupID);
            }
        }

        活跃会话.put(groupID, session);
        LOGGER.info("录制组 {} 开始复刻（帧驱动 {} 个实体，输入回放 {} 个玩家）",
                groupID,
                session.被控制实体.size(),
                session.被锁定玩家.size());
        return true;
    }

    /**
     * 兼容旧接口：单个录制会话直接开始复刻
     * 内部包装为合并录制数据
     */
    public static boolean 开始复刻(ServerPlayer user, 录制管理器.录制会话 recording) {
        // 从录制组管理器获取合并数据
        录制组 group = 录制组管理器.获取玩家所在录制组(user.getUUID());
        if (group != null && group.获取合并数据() != null) {
            return 开始复刻(group.获取组ID(), group.获取合并数据());
        }

        // 兜底：如果没有录制组（不应该发生），创建单段合并数据
        LOGGER.warn("玩家 {} 没有录制组，使用兜底单段合并", user.getName().getString());
        return false;
    }

    /**
     * 每tick推进复刻（按录制组ID驱动）
     *
     * 由预知录制事件在ServerTickEvent.START阶段调用
     * 一个录制组 = 一个复刻会话 = 一个tick驱动
     *
     * @param groupID 录制组ID
     * @return 是否仍在复刻中（false = 自然结束）
     */
    public static boolean tick(UUID groupID) {
        复刻会话 session = 活跃会话.get(groupID);
        if (session == null) return false;

        // 取任意一个录制者的状态来获取帧进度
        // 所有录制者共享同一个帧计数（由预知录制事件统一推进）
        预知状态管理.玩家预知状态 state = null;
        for (UUID 录制者 : session.录制者集合) {
            state = 预知状态管理.获取状态(录制者);
            if (state != null) break;
        }
        if (state == null) return false;

        int currentFrame = state.获取当前复刻帧();
        if (currentFrame >= state.获取总复刻帧数()) {
            LOGGER.debug("录制组 {} 复刻自然结束", groupID);
            return false;
        }

        ServerLevel level = session.维度;

        // 只驱动怪物帧（不干预玩家）
        驱动实体帧(session, level, currentFrame);

        // 注意：帧方块正向修正已移到tickEnd()中执行（Level.tick之后）
        // 这样输入回放产生的方块变化先发生，正向保底再检查有没有漏的
        // 避免正向保底和输入回放同时放方块导致多放

        // 每帧方块实体修正（容器内容保底）
        帧方块实体修正(session, level, currentFrame);

        // 每帧实体NBT校验（被锁定玩家的状态校验 — 通用，无需穷举字段）
        帧实体NBT校验(session, level, currentFrame);

        // 发帧同步包给客户端（输入帧 + 怪物帧数据）
        发送帧同步包给客户端(session, currentFrame);

        // 推进所有录制者的帧计数（多人场景下所有人帧进度同步）
        for (UUID 录制者 : session.录制者集合) {
            预知状态管理.玩家预知状态 录制者状态 = 预知状态管理.获取状态(录制者);
            if (录制者状态 != null) {
                录制者状态.推进复刻帧();
            }
        }
        return true;
    }

    /**
     * 每tick END阶段执行（Level.tick之后，MC已处理完所有C2S包）
     *
     * 在这个时间点执行反向保底：
     * - MC已处理完玩家的所有操作（放方块/破坏方块等）
     * - 多余的方块变化已经发生
     * - 可以准确检测并回退
     *
     * 时序：
     * START → tick()（正向修正+帧驱动）→ Level.tick（MC处理C2S包）→ END → tickEnd()（反向修正）
     *
     * 每帧流程：
     * 1. START阶段清空追踪集合
     * 2. Level.tick期间MC处理C2S包，方块变化被事件监听器记录
     * 3. END阶段执行反向修正，回退多余变化
     */
    public static void tickEnd(UUID groupID) {
        复刻会话 session = 活跃会话.get(groupID);
        if (session == null) return;

        // 取任意录制者的状态获取帧进度
        预知状态管理.玩家预知状态 state = null;
        for (UUID 录制者 : session.录制者集合) {
            state = 预知状态管理.获取状态(录制者);
            if (state != null) break;
        }
        if (state == null) return;

        // 当前帧 = 已推进后的帧 - 1（tick中已推进）
        int lastFrame = state.获取当前复刻帧() - 1;
        if (lastFrame < 0) return;

        // 正向保底：在Level.tick之后执行，此时输入回放产生的方块已经放好了
        // 只修正输入回放漏掉的（实际!=期望 的才修正）
        帧方块修正(session, session.维度, lastFrame);

        // 反向修正：检测并回退多余的方块变化
        帧方块反向修正(session, session.维度, lastFrame);
    }

    /**
     * 每tick START阶段清空方块变化追踪
     * 在tick()之前调用，准备收集本帧的方块变化
     */
    public static void 清空帧方块追踪() {
        回放中实际方块变化.clear();
    }

    /**
     * 每帧方块实体修正
     *
     * 容器内容和帧方块保底同样的模式：
     * → 这一帧方块实体应该变→ 看实际变了没
     * → 没变 → 强制设NBT + 扣/加背包
     * → 已变 → 不管
     */
    private static void 帧方块实体修正(复刻会话 session, ServerLevel level, int frameIndex) {
        for (方块实体变化帧 change : session.录制.获取方块实体变化列表()) {
            if (change.获取tick序号() != frameIndex) continue;

            BlockPos pos = change.获取位置();
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;

            CompoundTag expected = change.获取新NBT();
            CompoundTag actual = be.saveWithoutMetadata();

            if (actual.equals(expected)) continue;

            // 时间删除期间且来源是使用者 → 记录旧NBT用于结算恢复
            // 但仍然执行修正（B的录制世界需要）

            调整玩家背包(session, level, change.获取旧NBT(), expected);

            be.load(expected);
            be.setChanged();
        }
    }


    /**
     * 对比两个容器NBT的Items列表差异 → 从玩家背包扣/加
     */
    private static void 调整玩家背包(复刻会话 session, ServerLevel level,CompoundTag oldNbt, CompoundTag newNbt) {
        // 提取物品列表
        List<net.minecraft.world.item.ItemStack> oldItems = 从NBT提取物品(oldNbt);
        List<net.minecraft.world.item.ItemStack> newItems = 从NBT提取物品(newNbt);

        // 计算差异：新增的从背包扣，减少的往背包加
        Map<String, Integer> oldCounts = 统计物品数量(oldItems);
        Map<String, Integer> newCounts = 统计物品数量(newItems);

        // 找第一个被锁定的玩家来操作背包
        ServerPlayer target = null;
        for (UUID lockedUUID : session.被锁定玩家) {
            Entity entity = 实体工具.按UUID查找实体(level, lockedUUID);
            if (entity instanceof ServerPlayer sp) {
                target = sp;
                break;
            }
        }
        if (target == null) return;

        // 容器里多了的→ 从背包扣
        for (Map.Entry<String, Integer> entry : newCounts.entrySet()) {
            String key = entry.getKey();
            int newCount = entry.getValue();
            int oldCount = oldCounts.getOrDefault(key, 0);
            int diff = newCount - oldCount;

            if (diff > 0) {
                // 容器多了→ 背包扣
                for (int i = 0; i < diff; i++) {
                    for (net.minecraft.world.item.ItemStack newItem : newItems) {
                        if (物品键(newItem).equals(key)) {
                            int slot = target.getInventory().findSlotMatchingItem(newItem);
                            if (slot >= 0) {
                                target.getInventory().removeItem(slot, 1);
                            }
                            break;
                        }
                    }
                }
            }
        }

        // 容器里少了的 → 背包加
        for (Map.Entry<String, Integer> entry : oldCounts.entrySet()) {
            String key = entry.getKey();
            int oldCount = entry.getValue();
            int newCount = newCounts.getOrDefault(key, 0);
            int diff = oldCount - newCount;

            if (diff > 0) {
                for (net.minecraft.world.item.ItemStack oldItem : oldItems) {
                    if (物品键(oldItem).equals(key)) {
                        net.minecraft.world.item.ItemStack toAdd = oldItem.copy();
                        toAdd.setCount(diff);
                        target.getInventory().add(toAdd);
                        break;
                    }
                }
            }
        }
    }

    private static List<net.minecraft.world.item.ItemStack> 从NBT提取物品(CompoundTag nbt) {
        List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
        if (nbt.contains("Items", 9)) {
            net.minecraft.nbt.ListTag list = nbt.getList("Items", 10);
            for (int i = 0; i < list.size(); i++) {
                items.add(net.minecraft.world.item.ItemStack.of(list.getCompound(i)));
            }
        }
        return items;
    }

    private static Map<String, Integer> 统计物品数量(List<net.minecraft.world.item.ItemStack> items) {
        Map<String, Integer> counts = new HashMap<>();
        for (net.minecraft.world.item.ItemStack item : items) {
            if (item.isEmpty()) continue;
            String key = 物品键(item);
            counts.merge(key, item.getCount(), Integer::sum);
        }
        return counts;
    }

    private static String 物品键(net.minecraft.world.item.ItemStack item) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(item.getItem()).toString();
        if (item.hasTag()) {
            return id + item.getTag().toString();
        }
        return id;
    }

    /**
     * 每帧方块修正
     *
     * 输入回放驱动动画和音效
     * 这里保证方块状态和录制时完全一致
     *
     * 每tick检查：这一帧有没有方块应该变化但没变化的
     * → 有 → 强制修正
     * → 没有 → 不管
     *
     * 不会多放/多破坏：
     * → 只处理当前帧应该发生的变化
     * → 输入回放已经正确处理的→ 跳过
     *
     * 时间删除期间特殊处理：
     * - 使用者触发的方块变化直接执行（不扣背包）
     * - 这些方块会被标记，结算时撤销
     * - 其他玩家触发的方块变化仍正常扣背包
     */
    private static void 帧方块修正(复刻会话 session, ServerLevel level, int frameIndex) {
        正在执行帧修正 = true;
        try {
            帧方块正向修正内部(session, level, frameIndex);
        } finally {
            正在执行帧修正 = false;
        }
    }

    /**
     * 每帧方块正向修正（内部实现）
     *
     * 检查录制中当前帧应该发生的方块变化是否实际发生了
     * 没有发生的 → 强制执行
     * 已经发生的 → 跳过
     *
     * 破坏方块时：先发送破坏进度=10（完全破裂贴图）再执行destroyBlock
     * → 客户端会看到裂纹 + 碎裂粒子，而非方块凭空消失
     *
     * 扣物品：使用触发者UUID精确定位从谁的背包扣
     * → 不再遍历所有被锁定玩家
     */
    private static void 帧方块正向修正内部(复刻会话 session, ServerLevel level, int frameIndex) {
        // === 第一遍：收集时删玩家操作方块位置 + 提前发送忽略包 ===
        // 必须在执行destroyBlock/setBlockAndUpdate之前发送忽略包
        // 否则destroyBlock产生的levelEvent(2001)先到达时删玩家客户端
        // 而忽略列表还没更新 → 时删玩家会听到不该听到的破坏声效
        if (session.有时删中玩家()) {
            // 按时删玩家分组收集操作方块
            Map<UUID, List<BlockPos>> 时删玩家操作方块 = new HashMap<>();

            for (方块变化帧 change : session.录制.获取方块变化列表()) {
                if (change.获取tick序号() != frameIndex) continue;

                UUID 触发者 = change.获取触发者UUID();
                if (触发者 != null && session.时删中玩家.contains(触发者)) {
                    时删玩家操作方块.computeIfAbsent(触发者, k -> new ArrayList<>())
                            .add(change.获取位置().immutable());
                }
            }

            // 给每个时删玩家发忽略包
            for (Map.Entry<UUID, List<BlockPos>> entry : 时删玩家操作方块.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    Entity userEntity = 实体工具.按UUID查找实体(level, entry.getKey());
                    if (userEntity instanceof ServerPlayer sp) {
                        网络工具.发送给玩家(sp, 时删方块忽略包.忽略(entry.getValue()));
                    }
                }
            }
        }

        // === 第二遍：执行方块操作 ===
        for (方块变化帧 change : session.录制.获取方块变化列表()) {
            if (change.获取tick序号() != frameIndex) continue;

            BlockPos pos = change.获取位置();
            BlockState expected = change.获取新状态();
            BlockState actual = level.getBlockState(pos);

            if (actual.equals(expected)) continue;

            LOGGER.info("正向保底 帧{} 位置{} 期望={} 实际={} 需要修正",
                    frameIndex, pos, expected, actual);

            // 判断这个方块变化的触发者是否是时删中的玩家
            UUID 触发者 = change.获取触发者UUID();
            boolean 时删玩家操作 = session.有时删中玩家()
                    && 触发者 != null
                    && session.时删中玩家.contains(触发者);

            if (expected.isAir() && !actual.isAir()) {
                if (时删玩家操作) {
                    // 时删玩家操作：不掉落物品（结算会恢复方块）
                    level.destroyBlock(pos, false);
                } else {
                    // 非时删操作：发送破坏进度=10（完全破裂贴图），让客户端有裂纹效果
                    广播方块破坏效果(level, pos, actual);
                    level.destroyBlock(pos, true);
                }
            } else if (!expected.isAir()) {
                if (时删玩家操作) {
                    // 时删期间：直接放方块，不扣背包，标记用于结算撤销
                    level.setBlockAndUpdate(pos, expected);
                    session.录制.获取标记表().标记方块(pos, 触发者);
                } else {
                    // 非时删 或 非时删玩家触发：使用触发者UUID精确扣物品
                    放方块并扣物品(session, level, pos, expected, 触发者);
                }
            }
        }
    }

    /**
     * 每帧方块反向修正
     *
     * 检测回放期间实际发生的方块变化中，是否有"录制中没有记录的多余变化"
     * 多余的变化 → 回退到变化前的状态 + 退还消耗的物品
     *
     * 只检查被锁定玩家直接触发的方块变化（触发者UUID非null）
     * 红石/活塞/水流等自然连锁变化（触发者UUID为null）不会被误回退
     *
     * @param session    当前复刻会话
     * @param level      维度
     * @param frameIndex 当前帧序号
     */
    private static void 帧方块反向修正(复刻会话 session, ServerLevel level, int frameIndex) {
        if (回放中实际方块变化.isEmpty()) return;

        // 构建当前帧期望变化的位置集合
        Set<BlockPos> 期望变化位置 = new HashSet<>();
        for (方块变化帧 change : session.录制.获取方块变化列表()) {
            if (change.获取tick序号() == frameIndex) {
                期望变化位置.add(change.获取位置().immutable());
            }
        }

        // 也包含之前所有帧已经变化过的位置（这些位置的状态已经被正向修正管理）
        for (方块变化帧 change : session.录制.获取方块变化列表()) {
            if (change.获取tick序号() < frameIndex) {
                期望变化位置.add(change.获取位置().immutable());
            }
        }

        正在执行帧修正 = true;
        try {
            int 回退数量 = 0;
            List<BlockPos> 修正位置 = new ArrayList<>();
            List<BlockState> 修正状态 = new ArrayList<>();

            LOGGER.info("反向修正 帧{} 实际变化数={} 期望变化位置数={}",
                    frameIndex, 回放中实际方块变化.size(), 期望变化位置.size());

            for (Map.Entry<BlockPos, BlockState> entry : 回放中实际方块变化.entrySet()) {
                BlockPos pos = entry.getKey();

                // 这个位置在期望变化列表中 → 是正常的变化，不回退
                if (期望变化位置.contains(pos)) {
                    LOGGER.debug("反向修正 帧{} 位置{} 在期望列表中，跳过", frameIndex, pos);
                    continue;
                }

                // 这个位置不在期望列表中 → 是多余的变化
                BlockState 变化前状态 = entry.getValue();
                BlockState 当前状态 = level.getBlockState(pos);

                // 当前状态已经和变化前一样（可能已被其他机制修正），跳过
                if (当前状态.equals(变化前状态)) continue;

                // 回退到变化前的状态
                level.setBlockAndUpdate(pos, 变化前状态);
                修正位置.add(pos);
                修正状态.add(变化前状态);

                // 如果多余变化是放了方块（变化前是空气 → 当前不是空气），退还物品
                if (变化前状态.isAir() && !当前状态.isAir()) {
                    退还方块物品(session, level, 当前状态);
                }

                // 如果多余变化是破坏了方块（变化前不是空气 → 当前是空气 → 恢复方块）
                // 清理该位置附近匹配的掉落物（防止刷物品：方块恢复了但掉落物还在）
                if (!变化前状态.isAir() && 当前状态.isAir()) {
                    清理方块掉落物(level, pos, 变化前状态);
                }

                回退数量++;
            }

            // 同步客户端
            if (!修正位置.isEmpty()) {
                方块批量更新包 packet = 方块批量更新包.从配对构建(修正位置, 修正状态);
                for (ServerPlayer player : level.players()) {
                    网络工具.发送给玩家(player, packet);
                }
                LOGGER.info("帧方块反向修正：回退了 {} 个多余方块变化", 回退数量);
            }
        } finally {
            正在执行帧修正 = false;
            回放中实际方块变化.clear();
        }
    }

    /**
     * 每帧实体NBT校验
     *
     * 通用的状态一致性校验：直接比较实体当前NBT和录制时的NBT
     * 不需要穷举每个字段 — NBT包含实体的所有状态
     *
     * 只校验被锁定玩家（输入回放驱动的玩家可能产生状态偏差）
     * 被控制实体由帧驱动直接设状态，不需要额外校验
     *
     * 工作方式：
     * 1. 获取当前帧对应的录制NBT（从帧数据中查找最近一次有NBT的记录）
     * 2. 采集玩家当前的实际NBT
     * 3. 比较 → 不一致 → load录制的NBT → 覆盖位置朝向（NBT中位置不精确）
     *
     * 不会和输入回放打架：
     * - 输入回放在客户端工作，控制动画和体验
     * - NBT校验在服务端工作，控制实际状态
     * - 服务端修正后MC的正常同步机制会发给客户端
     *
     * @param session    当前复刻会话
     * @param level      维度
     * @param frameIndex 当前帧序号
     */
    private static void 帧实体NBT校验(复刻会话 session, ServerLevel level, int frameIndex) {
        for (UUID playerUUID : session.被锁定玩家) {
            Entity entity = 实体工具.按UUID查找实体(level, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            // 查找当前帧对应的录制期望NBT
            // 从当前帧向前查找最近一次有NBT的帧数据
            CompoundTag 期望NBT = 查找期望NBT(session, frameIndex, playerUUID);
            if (期望NBT == null) continue;

            // 采集当前实际NBT
            CompoundTag 实际NBT = 实体帧数据.采集状态NBT(sp);

            // 比较 → 一致则跳过
            if (实体帧数据.NBT相同(实际NBT, 期望NBT)) continue;

            // 不一致 → 恢复录制时的NBT
            sp.load(期望NBT);

            // 覆盖位置朝向（NBT中的位置不精确，使用帧数据中的精确位置）
            实体帧数据 帧 = session.录制.获取实体帧(frameIndex, playerUUID);
            if (帧 != null) {
                sp.setPos(帧.获取X(), 帧.获取Y(), 帧.获取Z());
                sp.setYRot(帧.获取YRot());
                sp.setXRot(帧.获取XRot());
            }

            LOGGER.debug("帧实体NBT校验：玩家 {} 帧 {} NBT不一致，已恢复", playerUUID, frameIndex);
        }
    }

    /**
     * 从帧数据中查找指定帧的期望NBT
     *
     * 由于NBT只在变化时存储（差分策略），某些帧的帧数据中NBT为null
     * 需要从当前帧向前查找最近一次有NBT的帧数据
     *
     * @param session    录制会话
     * @param frameIndex 目标帧
     * @param entityUUID 实体UUID
     * @return 该帧应有的期望NBT，null表示未找到
     */
    @Nullable
    private static CompoundTag 查找期望NBT(复刻会话 session, int frameIndex, UUID entityUUID) {
        // 从当前帧向前查找最近一次有NBT的帧
        for (int i = frameIndex; i >= 0; i--) {
            实体帧数据 帧 = session.录制.获取实体帧(i, entityUUID);
            if (帧 != null && 帧.有状态NBT()) {
                return 帧.获取状态NBT();
            }
        }

        // 没找到任何帧有NBT → 使用录制时的初始NBT缓存
        return session.录制.获取上次状态NBT缓存().get(entityUUID);
    }

    /**
     * 广播方块破坏效果
     *
     * 在帧方块修正强制破坏方块前调用
     * 发送破坏进度=10（完全破裂）给维度内所有玩家
     * → 客户端显示完全破裂的裂纹贴图
     * → 紧接着destroyBlock产生碎裂粒子
     * → 视觉上接近正常的破坏过程
     *
     * @param level 维度
     * @param pos   方块位置
     * @param state 被破坏的方块状态（用于音效）
     */
    private static void 广播方块破坏效果(ServerLevel level, BlockPos pos, BlockState state) {
        // 破坏进度包：进度10 = 完全破裂（MC客户端会显示裂纹贴图）
        // 使用位置的哈希值取负作为破坏者ID（避免和真实实体冲突）
        int breakerId = -pos.hashCode();
        ClientboundBlockDestructionPacket 破坏进度包 =
                new ClientboundBlockDestructionPacket(breakerId, pos, 9);

        for (ServerPlayer player : level.players()) {
            player.connection.send(破坏进度包);
        }
    }

    /**
     * 放方块并精确扣物品
     *
     * 使用触发者UUID确定从谁的背包扣物品
     * 如果触发者为null（自然变化） → 直接放方块不扣物品
     * 如果触发者是创造模式 → 直接放方块不扣物品
     * 否则 → 从触发者的背包扣物品
     *
     * @param session     复刻会话
     * @param level       维度
     * @param pos         方块位置
     * @param expected    期望的方块状态
     * @param triggerUUID 触发者UUID（可能为null）
     */
    private static void 放方块并扣物品(复刻会话 session, ServerLevel level,
                                        BlockPos pos, BlockState expected,
                                        @Nullable UUID triggerUUID) {
        net.minecraft.world.item.ItemStack needed =
                new net.minecraft.world.item.ItemStack(expected.getBlock().asItem());

        // 无对应物品的方块（如红石线等）→ 直接放
        if (needed.isEmpty()) {
            level.setBlockAndUpdate(pos, expected);
            return;
        }

        // 无触发者（自然变化） → 直接放不扣物品
        if (triggerUUID == null) {
            level.setBlockAndUpdate(pos, expected);
            return;
        }

        // 查找触发者
        Entity triggerEntity = 实体工具.按UUID查找实体(level, triggerUUID);
        if (!(triggerEntity instanceof ServerPlayer trigger)) {
            // 触发者不在线或非玩家 → 直接放
            level.setBlockAndUpdate(pos, expected);
            return;
        }

        // 创造模式 → 直接放不扣物品
        if (trigger.isCreative()) {
            level.setBlockAndUpdate(pos, expected);
            return;
        }

        // 从触发者背包扣物品
        int slot = trigger.getInventory().findSlotMatchingItem(needed);
        if (slot >= 0) {
            trigger.getInventory().removeItem(slot, 1);
            level.setBlockAndUpdate(pos, expected);
        }
        // 如果触发者背包里没有对应物品，不放方块（避免凭空刷方块）
    }

    /**
     * 退还多余方块变化消耗的物品
     *
     * 反向保底回退多放的方块时，把消耗的物品退回被锁定玩家的背包
     *
     * @param session 复刻会话
     * @param level   维度
     * @param state   被回退的方块状态（用于确定退还什么物品）
     */
    private static void 退还方块物品(复刻会话 session, ServerLevel level, BlockState state) {
        net.minecraft.world.item.ItemStack toReturn =
                new net.minecraft.world.item.ItemStack(state.getBlock().asItem());
        if (toReturn.isEmpty()) return;

        // 退还给第一个被锁定的玩家（通常只有一个）
        for (UUID lockedUUID : session.被锁定玩家) {
            Entity entity = 实体工具.按UUID查找实体(level, lockedUUID);
            if (entity instanceof ServerPlayer sp) {
                sp.getInventory().add(toReturn);
                return;
            }
        }
    }

    /**
     * 清理多余破坏操作产生的掉落物
     *
     * 反向保底回退了一个多余的破坏操作（方块被恢复）时调用
     * 只清理和被恢复方块匹配的、且是本帧新生成的掉落物
     * 避免误删玩家自己丢在地上的物品
     *
     * 匹配条件：
     * 1. 掉落物在方块位置2格范围内
     * 2. 掉落物的存活时间很短（本帧刚生成，age < 3tick）
     * 3. 掉落物的物品类型和被恢复的方块掉落物匹配
     *
     * @param level 维度
     * @param pos   被恢复的方块位置
     * @param 被恢复方块 被恢复的方块状态（用于匹配掉落物类型）
     */
    private static void 清理方块掉落物(ServerLevel level, BlockPos pos, BlockState 被恢复方块) {
        AABB 搜索范围 = new AABB(pos).inflate(2.0);

        // 获取该方块可能掉落的物品类型
        net.minecraft.world.item.Item 方块物品 = 被恢复方块.getBlock().asItem();

        List<Entity> 待删除 = new ArrayList<>();

        for (Entity entity : level.getEntities(
                (Entity) null, 搜索范围,
                e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) {

            net.minecraft.world.entity.item.ItemEntity itemEntity =
                    (net.minecraft.world.entity.item.ItemEntity) entity;

            // 只清理刚生成的掉落物（age很小 = 本帧或上一帧生成）
            // 玩家手动丢的物品age会大于这个值
            if (itemEntity.getAge() > 5) continue;

            // 只清理和方块掉落物类型匹配的（防止误删无关物品）
            if (方块物品 != net.minecraft.world.item.Items.AIR
                    && itemEntity.getItem().getItem() == 方块物品) {
                待删除.add(entity);
            }
        }

        for (Entity entity : 待删除) {
            entity.discard();
        }

        if (!待删除.isEmpty()) {
            LOGGER.debug("清理方块掉落物：位置 {} 清除 {} 个匹配的掉落物", pos, 待删除.size());
        }
    }

    /**
     * 清除方块变化位置的破坏进度纹理
     *
     * 回放/时删结束后，客户端可能残留挖方块的裂纹贴图
     * 遍历所有方块变化记录中的位置，发送进度=-1的破坏进度包
     * → 客户端清除该位置的裂纹
     *
     * @param session 当前复刻会话
     */
    private static void 清除方块破坏进度纹理(复刻会话 session) {
        ServerLevel level = session.维度;
        Set<BlockPos> 已处理位置 = new HashSet<>();

        for (方块变化帧 change : session.录制.获取方块变化列表()) {
            BlockPos pos = change.获取位置();
            if (!已处理位置.add(pos)) continue;

            // 发送进度=-1 → 客户端清除该位置的破坏裂纹
            int breakerId = -pos.hashCode();
            ClientboundBlockDestructionPacket 清除包 =
                    new ClientboundBlockDestructionPacket(breakerId, pos, -1);

            for (ServerPlayer player : level.players()) {
                player.connection.send(清除包);
            }
        }
    }

    /**
     * 进入时间删除（Phase 2 → Phase 3）
     *
     * 方案：A真正自由 + sendChanges位置欺骗 + 帧数据驱动命运
     *
     * 服务端：
     * - A从被锁定玩家移除 → A完全自由（正常走路/打开菜单/一切操作）
     * - 不使用存在屏蔽器 → A始终在B的seenBy中 → B客户端上A的实体存在
     * - sendChanges位置欺骗 → B收到的A的位置是命运位置
     * - 帧同步包继续发A的帧数据 → B的客户端用帧数据驱动A的动画
     * - 帧方块修正继续执行（使用者操作直接放方块不扣背包）
     * - 输入接管切换到MOVE_ONLY → A的C2S操作包被拦截（限制系统已禁止攻击等）
     *
     * 客户端：
     * - A收到时删通知 → 停止输入回放 → A完全自由
     * - A客户端拦截位置包 → 不被命运位置覆盖真实位置
     */
    public static boolean 进入时间删除(ServerPlayer user) {
        UUID userUUID = user.getUUID();
        UUID groupID = 玩家组映射.get(userUUID);
        if (groupID == null) return false;
        复刻会话 session = 活跃会话.get(groupID);
        if (session == null) return false;

        // A从被锁定玩家移除 → A获得完全自由
        session.被锁定玩家.remove(userUUID);
        session.被控制实体.remove(userUUID);
        session.时删中玩家.add(userUUID);

        // 不再使用存在屏蔽器
        // 改用sendChanges位置欺骗：A始终在B的seenBy中，但发包时位置被替换为命运位置
        // 这样B客户端上A的实体始终存在，帧数据能正确驱动动画

        // 输入接管切换到MOVE_ONLY模式
        // 移动包正常到达 → A在服务端位置是真实位置
        // 操作包被拦截 → A的攻击/交互不生效（配合限制系统双重保障）
        输入接管器.接管(userUUID, 接管来源, 输入接管器.接管模式.MOVE_ONLY);

        // 通知A的客户端进入时间删除自由状态
        网络工具.发送给玩家(user,
                com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包.时间删除());

        LOGGER.info("玩家 {} 进入时间删除（A自由 + 位置欺骗驱动命运）", user.getName().getString());
        return true;
    }

    /**
     * 跳到结尾（时间删除中按键）
     */
    public static void 跳到结尾(UUID triggerUUID) {
        // 通过玩家组映射找到录制组
        UUID groupID = 玩家组映射.get(triggerUUID);
        if (groupID == null) groupID = triggerUUID;
        // 兼容：如果triggerUUID本身就是groupID
        复刻会话 session = 活跃会话.get(groupID);
        if (session == null) return;

        // 取任意录制者的状态获取帧数
        int lastFrame = -1;
        for (UUID 录制者 : session.录制者集合) {
            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(录制者);
            if (state != null) {
                lastFrame = state.获取总复刻帧数() - 1;
                break;
            }
        }
        if (lastFrame >= 0) {
            驱动实体帧(session, session.维度, lastFrame);
            发送帧同步包给客户端(session, lastFrame);
        }

        LOGGER.info("录制组 {} 复刻跳到结尾（跳到帧 {}）",
                groupID, lastFrame);
        结束复刻(groupID);
    }

    /**
     * 结束复刻（任何原因）
     *
     * 清理顺序：
     * 1. 应用最终帧（确保怪物位置正确）
     * 2. 通知客户端
     * 3. 恢复录制结束快照（结果驱动）
     * 4. 强制同步玩家位置到录制结束时的位置
     * 5. 释放输入锁定
     * 6. 解除使用者隐身/无敌
     */
    /**
     * 防止重入标记：结束复刻过程中的使用者UUID
     * 跳到结尾 → 结束复刻 和 自然结束 → 结束复刻 可能在同一tick内交叉调用
     */
    private static final Set<UUID> 正在结束中 = ConcurrentHashMap.newKeySet();

    /**
     * 结束复刻（按录制组ID）
     * 外部调用统一使用此方法
     */
    public static void 结束复刻(UUID groupID) {
        // 重入保护：防止跳到结尾和自然结束同时触发
        if (!正在结束中.add(groupID)) {
            LOGGER.debug("结束复刻重入保护：录制组 {} 已在结束流程中", groupID);
            return;
        }

        try {
            结束复刻内部(groupID);
        } finally {
            正在结束中.remove(groupID);
        }
    }

    private static void 结束复刻内部(UUID groupID) {
        复刻会话 session = 活跃会话.remove(groupID);
        if (session == null) return;

        // 1. 应用最终帧（取任意录制者的状态获取帧数）
        int lastFrame = -1;
        for (UUID 录制者 : session.录制者集合) {
            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(录制者);
            if (state != null) {
                lastFrame = state.获取总复刻帧数() - 1;
                break;
            }
        }
        if (lastFrame >= 0) {
            驱动实体帧(session, session.维度, lastFrame);
        }

        // 2. 通知客户端
        通知客户端复刻结束(session);

        // 时删结束：通知每个时删玩家的客户端清空方块忽略列表
        for (UUID 时删玩家 : session.时删中玩家) {
            Entity entity = 实体工具.按UUID查找实体(session.维度, 时删玩家);
            if (entity instanceof ServerPlayer sp) {
                网络工具.发送给玩家(sp, 时删方块忽略包.清空());
            }
        }

        // 时间删除结算（per-player独立结算）
        for (UUID 时删玩家 : session.时删中玩家) {
            时间删除结算(session, 时删玩家);
        }

        // 清除所有方块变化位置的破坏进度纹理
        // 回放/时删结束后，客户端可能残留挖方块的裂纹贴图
        // 发送进度=-1的破坏进度包 → 客户端清除裂纹
        清除方块破坏进度纹理(session);

        // 4. 强制同步位置
        // 时删玩家：同步背包（位置已通过MOVE_ONLY模式正常同步）
        for (UUID 时删玩家 : session.时删中玩家) {
            Entity entity = 实体工具.按UUID查找实体(session.维度, 时删玩家);
            if (entity instanceof ServerPlayer sp) {
                网络工具.发送给玩家(sp, 背包同步包.从玩家构建(sp));
            }
        }

        // 被锁定玩家传送到录制结束时的位置（偏差大于阈值才传送）
        if (lastFrame >= 0) {
            Map<UUID, 实体帧数据> lastFrameData = session.录制.获取帧(lastFrame);
            if (lastFrameData != null) {
                for (UUID lockedUUID : session.被锁定玩家) {
                    实体帧数据 data = lastFrameData.get(lockedUUID);
                    if (data == null) continue;

                    Entity entity = 实体工具.按UUID查找实体(session.维度, lockedUUID);
                    if (entity instanceof ServerPlayer sp) {
                        位置修正(sp, data);
                    }
                }
            }
        }

        // 强制同步背包（清除幽灵物品）
        for (UUID lockedUUID : session.被锁定玩家) {
            Entity entity = 实体工具.按UUID查找实体(session.维度, lockedUUID);
            if (entity instanceof ServerPlayer sp) {
                网络工具.发送给玩家(sp, 背包同步包.从玩家构建(sp));
            }
        }

        // 5. 释放输入锁定
        for (UUID lockedUUID : session.被锁定玩家) {
            输入接管器.释放(lockedUUID, 接管来源);
        }
        // 时删中玩家释放MOVE_ONLY接管
        for (UUID 时删玩家 : session.时删中玩家) {
            输入接管器.释放(时删玩家, 接管来源);
        }

        // 6. 清除命运位置缓存
        for (UUID 时删玩家 : session.时删中玩家) {
            命运位置缓存.remove(时删玩家);
        }

        // 7. 时删结束：短暂存在屏蔽让MC的lastSent同步到真实位置
        for (UUID 时删玩家 : session.时删中玩家) {
            存在屏蔽器.屏蔽除外(时删玩家, 淡出屏蔽来源, 时删玩家);
            final UUID finalUUID = 时删玩家;
            session.维度.getServer().tell(new net.minecraft.server.TickTask(
                    session.维度.getServer().getTickCount() + 1,
                    () -> 存在屏蔽器.解除屏蔽(finalUUID, 淡出屏蔽来源)
            ));
        }

        // 8. 清除玩家组映射
        for (UUID playerUUID : session.被锁定玩家) {
            玩家组映射.remove(playerUUID);
        }
        for (UUID playerUUID : session.时删中玩家) {
            玩家组映射.remove(playerUUID);
        }
        for (UUID playerUUID : session.录制者集合) {
            玩家组映射.remove(playerUUID);
        }

        LOGGER.info("录制组 {} 复刻结束", groupID);
    }

    //==================== 帧驱动逻辑 ====================

    /**
     * 驱动实体帧：将录制的完整状态应用到实体
     *
     * 被锁定玩家：设位置、朝向和装备（操作由MC根据输入自己处理）
     * 被控制实体（怪物等）：完整帧驱动（NBT + 手动字段）
     *
     * 时删期间额外功能：缓存使用者的命运帧位置（供sendChanges位置欺骗使用）
     */
    private static void 驱动实体帧(复刻会话 session, ServerLevel level, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.获取帧(frameIndex);
        if (frame == null) return;

        Map<UUID, 实体帧数据> prevFrame = frameIndex > 0
                ? session.录制.获取帧(frameIndex - 1) : null;

        // 时删期间：缓存每个时删玩家的命运帧位置（供sendChanges位置欺骗使用）
        if (session.有时删中玩家()) {
            for (UUID 时删玩家UUID : session.时删中玩家) {
                实体帧数据 时删帧 = frame.get(时删玩家UUID);
                if (时删帧 != null) {
                    命运位置缓存.put(时删玩家UUID, new 命运位置(
                            时删帧.获取X(), 时删帧.获取Y(), 时删帧.获取Z(),
                            时删帧.获取YRot(), 时删帧.获取XRot(),
                            时删帧.获取身体YRot(), 时删帧.获取头部YRot()
                    ));
                }
            }
        }

        // 被控制实体（怪物等）：完整帧驱动
        for (Map.Entry<UUID, 实体帧数据> entry : frame.entrySet()) {
            UUID entityUUID = entry.getKey();
            实体帧数据 当前帧 = entry.getValue();

            if (!session.被控制实体.contains(entityUUID)) continue;

            实体帧数据 上一帧 = (prevFrame != null) ? prevFrame.get(entityUUID) : null;

            Entity entity = 实体工具.按UUID查找实体(level, entityUUID);
            if (entity == null) continue;

            if (entity instanceof LivingEntity living) {
                // 记录应用前的动画状态（用于边沿检测）
                boolean 应用前挥手 = living.swinging;
                int 应用前受击 = living.hurtTime;
                int 应用前死亡 = living.deathTime;

                当前帧.应用到活体(living, 上一帧);

                // 应用后对比检测动画边沿 → 广播动画事件
                广播动画事件(level, living, 应用前挥手, 应用前受击, 应用前死亡);
            } else {
                当前帧.应用到普通实体(entity, 上一帧);
            }
        }
    }

    /**
     * 广播动画事件给客户端（NBT应用前后对比检测）
     *
     * 精简后不再依赖帧数据中的手动字段
     * 改为在应用NBT前后对比实体的实际状态
     * → 自动适配所有动画状态，无需穷举字段
     *
     * @param level      维度
     * @param entity     目标实体
     * @param 应用前挥手  应用帧数据前的挥手状态
     * @param 应用前受击  应用帧数据前的受击时间
     * @param 应用前死亡  应用帧数据前的死亡时间
     */
    private static void 广播动画事件(ServerLevel level, LivingEntity entity,
                                     boolean 应用前挥手, int 应用前受击, int 应用前死亡) {
        // 挥手：从不挥手变成挥手 → 广播挥手包
        if (entity.swinging && !应用前挥手) {
            level.getChunkSource().broadcastAndSend(entity,
                    new ClientboundAnimatePacket(entity, 0));
        }

        // 受击红闪：从0变成大于0 → 广播受击包
        if (entity.hurtTime > 0 && 应用前受击 == 0) {
            level.getChunkSource().broadcastAndSend(entity,
                    new ClientboundHurtAnimationPacket(entity));
        }

        // 死亡：从0变成大于0 → 广播死亡事件
        if (entity.deathTime > 0 && 应用前死亡 == 0) {
            level.broadcastEntityEvent(entity, (byte) 3);
        }
    }

    /**
     * 同步被锁定玩家的位置到服务端内部系统
     */
    private static void 同步玩家状态(复刻会话 session, ServerLevel level, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.获取帧(frameIndex);
        if (frame == null) return;

        for (UUID playerUUID : session.被锁定玩家) {
            实体帧数据 data = frame.get(playerUUID);
            if (data == null) continue;

            Entity entity = 实体工具.按UUID查找实体(level, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            sp.absMoveTo(data.获取X(), data.获取Y(), data.获取Z(),
                    data.获取YRot(), data.获取XRot());
            sp.connection.resetPosition();
        }
    }

    // ==================== 网络同步 ====================

    /**
     * 回滚前发送方块预更新包
     */
    private static void 发送方块预更新包(世界快照 snapshot, ServerLevel level) {
        List<BlockPos> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();

        for (com.v2t.puellamagi.util.recording.方块快照 block : snapshot.获取所有方块快照()) {
            positions.add(block.获取位置());
            states.add(block.获取方块状态());
        }

        if (positions.isEmpty()) return;

        方块批量更新包 packet = 方块批量更新包.从配对构建(positions, states);
        for (ServerPlayer player : level.players()) {
            网络工具.发送给玩家(player, packet);
        }
    }

    /**
     * 发送帧同步包给客户端
     *
     * 时删期间：A已从被锁定玩家移除，但帧数据仍需发送
     * → B的客户端用帧数据驱动A的残影（命运中的A）
     * → A的客户端也收到但不驱动自己（时间删除自由状态）
     */
    private static void 发送帧同步包给客户端(复刻会话 session, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.获取帧(frameIndex);
        if (frame == null) return;

        Map<UUID, 实体帧数据> prevFrame = frameIndex > 0
                ? session.录制.获取帧(frameIndex - 1) : null;

        List<实体帧数据> 当前帧列表 = new ArrayList<>();
        List<实体帧数据> 上一帧列表 = new ArrayList<>();

        for (Map.Entry<UUID, 实体帧数据> entry : frame.entrySet()) {
            UUID entityUUID = entry.getKey();

            // 被控制实体（怪物等）：帧数据驱动（客户端cancel tick）
            // 时删使用者：帧数据用于命运位置欺骗（B的客户端驱动A的残影）
            // 被锁定玩家：帧数据发给其他客户端用于平滑位置插值
            //   → 第三方观察者C需要收到被锁定玩家的帧数据才能平滑渲染
            //   → 被锁定玩家自己的客户端通过输入回放驱动，帧数据作为保底参考
            boolean 需要包含 = session.被控制实体.contains(entityUUID)
                    || session.时删中玩家.contains(entityUUID)
                    || session.被锁定玩家.contains(entityUUID);

            if (!需要包含) continue;

            当前帧列表.add(entry.getValue());

            if (prevFrame != null) {
                实体帧数据 prev = prevFrame.get(entityUUID);
                if (prev != null) {
                    上一帧列表.add(prev);
                }
            }
        }

        // 输入帧：时删中不发送使用者的输入帧（A的客户端已自由，不需要驱动输入回放）
        Map<UUID, 玩家输入帧> 输入帧 = new HashMap<>();
        for (Map.Entry<UUID, List<玩家输入帧>> entry : session.录制.获取玩家输入表().entrySet()) {
            // 时删中的玩家不发送输入帧（客户端已自由）
            if (session.时删中玩家.contains(entry.getKey())) {
                continue;
            }
            List<玩家输入帧> inputs = entry.getValue();
            if (frameIndex < inputs.size()) {
                玩家输入帧 input = inputs.get(frameIndex);
                if (input != null) {
                    输入帧.put(entry.getKey(), input);
                }
            }
        }

        Map<UUID, List<float[]>> 鼠标样本 = new HashMap<>();

        if (当前帧列表.isEmpty() && 输入帧.isEmpty()) return;

        复刻帧同步包 packet = new 复刻帧同步包(
                session.组ID, 当前帧列表, 上一帧列表, 输入帧, 鼠标样本,
                session.被锁定玩家);

        for (ServerPlayer player : session.维度.players()) {
            网络工具.发送给玩家(player, packet);
        }
    }

    /**
     * 通知客户端复刻结束（发送空帧列表）
     */
    private static void 通知客户端复刻结束(复刻会话 session) {
        复刻帧同步包 packet = new 复刻帧同步包(
                session.组ID, new ArrayList<>(), new ArrayList<>());

        for (ServerPlayer player : session.维度.players()) {
            网络工具.发送给玩家(player, packet);
        }
    }

    // ==================== 时删结束方法 ===================

    /**
     * 时间删除结算
     *
     * 撤销使用者在时删激活帧之后造成的所有影响
     * 从后往前撤销，避免依赖问题
     *
     * 结算顺序：
     * 1. 回血（撤销伤害）
     * 2. 移除效果
     * 3. 清除方块（标记表）
     * 4. 恢复容器（方块实体变化帧来源=使用者的）
     * 5. 清除掉落物（标记表）
     * 6. 清除玩家背包中标记物品
     * 7. 清除容器内标记物品
     * 8. 复活被杀实体
     * 9. 强制同步
     */
    private static void 时间删除结算(复刻会话 session, UUID 使用者) {
        ServerLevel level = session.维度;
        影响记录 影响 = session.录制.获取影响();
        影响标记表 标记表 = session.录制.获取标记表();

        // 获取时删激活帧
        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(使用者);
        int 激活帧 = (state != null) ? state.获取时删激活帧() : 0;
        LOGGER.info("时间删除结算开始：使用者={}, 激活帧={}", 使用者, 激活帧);

        // 1. 回血（撤销使用者造成的伤害）
        List<影响记录.伤害条目> 伤害列表 = 影响.获取来源伤害(使用者, 激活帧);
        for (影响记录.伤害条目 entry : 伤害列表) {
            Entity target = 实体工具.按UUID查找实体(level, entry.被攻击者);
            if (target instanceof LivingEntity living) {
                living.heal(entry.伤害量);
                LOGGER.debug("结算回血：{} +{}", entry.被攻击者, entry.伤害量);
            }
        }

        // 2. 移除效果（撤销使用者施加的效果）
        List<影响记录.效果条目> 效果列表 = 影响.获取来源效果(使用者, 激活帧);
        for (影响记录.效果条目 entry : 效果列表) {
            Entity target = 实体工具.按UUID查找实体(level, entry.目标);
            if (target instanceof LivingEntity living) {
                net.minecraft.world.effect.MobEffect effect =
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                                .get(new net.minecraft.resources.ResourceLocation(entry.效果ID));
                if (effect != null) {
                    living.removeEffect(effect);
                    // 恢复到施加效果时的血量（处理debuff持续伤害）
                    if (living.getHealth() < entry.施加时血量) {
                        living.setHealth(entry.施加时血量);
                    }
                    LOGGER.debug("结算移除效果：{} 移除{}, 血量恢复到{}",
                            entry.目标, entry.效果ID, entry.施加时血量);
                }
            }
        }

        // 3. 清除方块（标记表中来源=使用者的 → 使用者放置的方块）
        List<BlockPos> 标记方块 = 标记表.获取来源方块(使用者);
        for (BlockPos pos : 标记方块) {
            if (!level.getBlockState(pos).isAir()) {
                level.removeBlock(pos, false);
                // 触发方块更新（水流/红石/沙子等连锁反应）
                level.updateNeighborsAt(pos, net.minecraft.world.level.block.Blocks.AIR);
                LOGGER.debug("结算清除方块：{}", pos);
            }
            标记表.清除方块标记(pos);
        }

        // 3.5 恢复方块（使用者破坏的方块 → 把旧状态放回去）
        // 遍历方块变化帧，找使用者触发的、新状态是空气的变化 = 破坏操作
        // 从后往前处理，避免同一位置多次变化时的顺序问题
        int 恢复方块数 = 0;
        Set<BlockPos> 已恢复位置 = new HashSet<>();
        List<方块变化帧> 变化列表 = session.录制.获取方块变化列表();
        for (int i = 变化列表.size() - 1; i >= 0; i--) {
            方块变化帧 change = 变化列表.get(i);
            if (change.获取触发者UUID() == null) continue;
            if (!change.获取触发者UUID().equals(使用者)) continue;
            if (change.获取tick序号() < 激活帧) continue;

            // 只处理破坏操作（新状态是空气，旧状态不是空气）
            if (!change.获取新状态().isAir()) continue;
            if (change.获取旧状态().isAir()) continue;

            BlockPos pos = change.获取位置();
            // 同一位置只处理最后一次变化（从后往前遍历保证）
            if (已恢复位置.contains(pos)) continue;
            已恢复位置.add(pos);

            // 服务端当前应该是空气 → 恢复为旧状态
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, change.获取旧状态());
                LOGGER.debug("结算恢复方块：{} → {}", pos, change.获取旧状态());
                恢复方块数++;
            }
        }

        // 4. 恢复容器（方块实体变化帧来源=使用者且tick>=激活帧）
        for (方块实体变化帧 change : session.录制.获取方块实体变化列表()) {
            if (change.获取触发者UUID() == null) continue;
            if (!change.获取触发者UUID().equals(使用者)) continue;
            if (change.获取tick序号() < 激活帧) continue;

            BlockPos pos = change.获取位置();
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.load(change.获取旧NBT());
                be.setChanged();
                LOGGER.debug("结算恢复容器：{}", pos);
            }
        }

        // 5. 清除掉落物（范围内标记=使用者的）
        double range = 录制管理器.获取录制范围();
        net.minecraft.world.phys.AABB box = net.minecraft.world.phys.AABB.ofSize(
                session.录制.获取录制中心(), range * 2, range * 2, range * 2);
        List<Entity> 待删除掉落物 = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, box,
                e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) {
            // 检查掉落物标记
            if (标记表.掉落物有标记(entity.getId())) {
                UUID 来源 = 标记表.获取掉落物来源(entity.getId());
                if (来源 != null && 来源.equals(使用者)) {
                    待删除掉落物.add(entity);
                }
            }
            // 检查物品NBT标记
            net.minecraft.world.entity.item.ItemEntity itemEntity =
                    (net.minecraft.world.entity.item.ItemEntity) entity;
            if (影响标记表.物品来自(itemEntity.getItem(), 使用者)) {
                待删除掉落物.add(entity);
            }
        }
        for (Entity entity : 待删除掉落物) {
            entity.discard();
        }
        if (!待删除掉落物.isEmpty()) {
            LOGGER.debug("结算清除掉落物：{}个", 待删除掉落物.size());
        }

        // 6. 清除玩家背包中标记=使用者的物品
        for (ServerPlayer player : level.players()) {
            boolean 有清除 = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                if (影响标记表.物品来自(stack, 使用者)) {
                    player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                    有清除 = true;
                }
            }
            if (有清除) {
                网络工具.发送给玩家(player, 背包同步包.从玩家构建(player));
                LOGGER.debug("结算清除玩家 {} 背包中的标记物品", player.getName().getString());
            }
        }

        // 7. 清除容器内标记=使用者的物品
        int minChunkX = (int) Math.floor(session.录制.获取录制中心().x - range) >> 4;
        int maxChunkX = (int) Math.floor(session.录制.获取录制中心().x + range) >> 4;
        int minChunkZ = (int) Math.floor(session.录制.获取录制中心().z - range) >> 4;
        int maxChunkZ = (int) Math.floor(session.录制.获取录制中心().z + range) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                var chunk = level.getChunk(cx, cz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    net.minecraft.world.level.block.entity.BlockEntity be = entry.getValue();
                    if (be instanceof net.minecraft.world.Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = container.getItem(i);
                            if (影响标记表.物品来自(stack, 使用者)) {
                                container.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                            }
                        }
                        be.setChanged();
                    }
                }
            }
        }

        // 8. 复活被杀实体
        List<影响记录.击杀条目> 击杀列表 = 影响.获取来源击杀(使用者, 激活帧);
        for (影响记录.击杀条目 entry : 击杀列表) {
            // 从世界快照恢复实体
            CompoundTag entityNBT = session.录制.获取起点快照().获取实体NBT(entry.被杀者);
            if (entityNBT != null) {
                Entity restored = net.minecraft.world.entity.EntityType.loadEntityRecursive(
                        entityNBT, level, e -> {
                            e.moveTo(e.getX(), e.getY(), e.getZ());
                            return e;
                        });
                if (restored != null) {
                    level.addFreshEntity(restored);
                    LOGGER.debug("结算复活实体：{}", entry.被杀者);
                }
            }
        }

        // 9. 清理标记表
        标记表.清除全部();
        影响.清除全部();
        LOGGER.info("时间删除结算完成：回血{}条, 效果{}条, 清除方块{}个, 恢复方块{}个, 击杀{}条",
                伤害列表.size(), 效果列表.size(), 标记方块.size(), 恢复方块数, 击杀列表.size());
    }


    // ==================== 查询接口 ====================

    public static boolean 实体是否被复刻控制(Entity entity) {
        UUID entityUUID = entity.getUUID();
        for (复刻会话 session : 活跃会话.values()) {
            if (session.被控制实体.contains(entityUUID)) {
                return true;
            }
        }
        return false;
    }

    public static List<UUID> 获取维度内活跃使用者(ServerLevel level) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, 复刻会话> entry : 活跃会话.entrySet()) {
            if (entry.getValue().维度 == level) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static List<UUID> 获取所有活跃使用者() {
        return new ArrayList<>(活跃会话.keySet());
    }

    @Nullable
    public static 复刻会话 获取会话(UUID id) {
        // 先按组ID查
        复刻会话 session = 活跃会话.get(id);
        if (session != null) return session;
        // 再按玩家组映射查
        UUID groupID = 玩家组映射.get(id);
        return groupID != null ? 活跃会话.get(groupID) : null;
    }

    public static boolean 玩家是否被锁定(UUID playerUUID) {
        for (复刻会话 session : 活跃会话.values()) {
            if (session.被锁定玩家.contains(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static 实体帧数据 获取当前帧数据(UUID playerUUID) {
        for (复刻会话 session : 活跃会话.values()) {
            if (!session.被控制实体.contains(playerUUID)) continue;

            // 取任意录制者的状态获取帧进度
            for (UUID 录制者 : session.录制者集合) {
                预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(录制者);
                if (state != null) {
                    return session.录制.获取实体帧(state.获取当前复刻帧(), playerUUID);
                }
            }
        }
        return null;
    }

    /**
     * 查找玩家所在的复刻会话
     * 通过玩家组映射反查
     */
    @Nullable
    public static 复刻会话 查找玩家所在会话(UUID playerUUID) {
        UUID groupID = 玩家组映射.get(playerUUID);
        if (groupID == null) return null;
        return 活跃会话.get(groupID);
    }

    /**
     * 是否复刻中（按录制组ID或玩家UUID查询）
     */
    public static boolean 是否复刻中(UUID id) {
        // 先按组ID查
        if (活跃会话.containsKey(id)) return true;
        // 再按玩家组映射查
        UUID groupID = 玩家组映射.get(id);
        return groupID != null && 活跃会话.containsKey(groupID);
    }

    // ==================== 查询方法 ====================

    // ==================== 工具方法 ====================

    private static int 清除录制期间出现的实体(ServerLevel level, 合并录制数据 recording) {
        double range = 录制管理器.获取录制范围();
        AABB box = AABB.ofSize(recording.获取录制中心(), range * 2, range * 2, range * 2);
        Set<UUID> 快照实体 = recording.获取起点快照().获取所有实体UUID();

        List<Entity> 待删除 = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof ServerPlayer) continue;
            if (!box.contains(entity.position())) continue;
            if (快照实体.contains(entity.getUUID())) continue;
            待删除.add(entity);
        }

        for (Entity entity : 待删除) {
            entity.discard();
        }
        return 待删除.size();
    }

    /**
     * 回放结束位置修正
     *
     * 输入回放结束时玩家位置和录制最后一帧可能有微小偏差（浮点精度）
     * 偏差小于阈值 → 不传送（避免瞬移感）
     * 偏差大于阈值 → 强制传送到录制位置（保底修正）
     *
     * @param sp   目标玩家
     * @param data 录制最后一帧的帧数据
     */
    private static final double 位置修正阈值 = 1.5;

    private static void 位置修正(ServerPlayer sp, 实体帧数据 data) {
        double dx = sp.getX() - data.获取X();
        double dy = sp.getY() - data.获取Y();
        double dz = sp.getZ() - data.获取Z();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > 位置修正阈值 * 位置修正阈值) {
            // 偏差过大，强制传送修正
            sp.connection.teleport(
                    data.获取X(), data.获取Y(), data.获取Z(),
                    data.获取YRot(), data.获取XRot());
            LOGGER.debug("位置修正：玩家 {} 偏差 {:.2f} 格，已传送修正",
                    sp.getName().getString(), Math.sqrt(distSq));
        } else {
            // 偏差很小，只同步视角（不传送位置）
            sp.setYRot(data.获取YRot());
            sp.setXRot(data.获取XRot());
            sp.yRotO = data.获取YRot();
            sp.xRotO = data.获取XRot();
            sp.setYHeadRot(data.获取YRot());
        }
    }


    // ==================== 生命周期 ====================

    public static void 玩家下线(UUID playerUUID) {
        命运位置缓存.remove(playerUUID);
        // 通过玩家组映射找到录制组ID
        UUID groupID = 玩家组映射.get(playerUUID);
        if (groupID != null) {
            结束复刻(groupID);
        }
        玩家组映射.remove(playerUUID);
        输入接管器.玩家下线(playerUUID);
    }

    public static void 清除全部() {
        for (复刻会话 session : 活跃会话.values()) {
            for (UUID lockedUUID : session.被锁定玩家) {
                输入接管器.释放(lockedUUID, 接管来源);
            }
            for (UUID 时删玩家 : session.时删中玩家) {
                输入接管器.释放(时删玩家, 接管来源);
                存在屏蔽器.解除屏蔽(时删玩家, 淡出屏蔽来源);
            }
        }
        活跃会话.clear();
        玩家组映射.clear();
        命运位置缓存.clear();
    }

    // ==================== 命运位置查询（供Mixin调用） ====================

    /**
     * 获取时删使用者的命运位置
     *
     * 由 EpitaphReplayServerEntityMixin 在 sendChanges 前调用
     * 返回null表示该实体不需要位置欺骗
     *
     * @param entityUUID 实体UUID
     * @return 命运位置数据，null表示不欺骗
     */
    @Nullable
    public static 命运位置 获取命运位置(UUID entityUUID) {
        return 命运位置缓存.get(entityUUID);
    }

    /**
     * 检查实体是否正在进行位置欺骗
     *
     * @param entityUUID 实体UUID
     * @return true = 该实体是时删使用者且有命运位置缓存
     */
    public static boolean 是否位置欺骗中(UUID entityUUID) {
        return 命运位置缓存.containsKey(entityUUID);
    }

    /**
     * 检查玩家是否在时删中（已从被锁定状态脱离、自由活动）
     *
     * @param playerUUID 玩家UUID
     * @return true = 该玩家正在时删中自由活动
     */
    public static boolean 玩家是否在时删中(UUID playerUUID) {
        for (复刻会话 session : 活跃会话.values()) {
            if (session.时删中玩家.contains(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取与指定时删使用者同一会话中的其他时删玩家列表
     *
     * 用于位置欺骗的差异化：当A在时删中，需要找到同会话中其他也在时删中的玩家
     * 这些玩家应该看到A的真实位置而非命运位置
     *
     * @param entityUUID 被观察的实体UUID（时删使用者）
     * @return 同会话中其他时删玩家的UUID集合，如果没有则返回空集合
     */
    public static Set<UUID> 获取同会话时删玩家(UUID entityUUID) {
        复刻会话 session = 查找玩家所在会话(entityUUID);
        if (session == null || session.时删中玩家.isEmpty()) {
            return Collections.emptySet();
        }
        // 返回所有时删中的玩家（包括自己，由调用方判断是否排除）
        return Collections.unmodifiableSet(session.时删中玩家);
    }
}

