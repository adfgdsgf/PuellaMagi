package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.core.network.packets.s2c.复刻帧同步包;
import com.v2t.puellamagi.core.network.packets.s2c.方块批量更新包;
import com.v2t.puellamagi.core.network.packets.s2c.背包同步包;
import com.v2t.puellamagi.util.network.存在屏蔽器;
import com.v2t.puellamagi.util.network.输入接管器;
import com.v2t.puellamagi.util.recording.实体帧数据;
import com.v2t.puellamagi.util.recording.世界快照;
import com.v2t.puellamagi.util.recording.玩家快照;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    // ==================== 复刻会话 ====================

    /**
     * 单个使用者的复刻会话
     *
     * 被锁定玩家：客户端输入被替换为录制数据，MC自己处理操作
     * 被控制实体：tick被cancel，由帧数据驱动（怪物/投射物等）
     */
    public static class 复刻会话 {
        public final UUID 使用者UUID;
        public final 录制管理器.录制会话 录制;
        public final ServerLevel 维度;

        public final Set<UUID> 被锁定玩家;
        public final Set<UUID> 被控制实体;

        public boolean 时间删除中 = false;

        public 复刻会话(UUID userUUID, 录制管理器.录制会话 recording, ServerLevel level) {
            this.使用者UUID = userUUID;
            this.录制 = recording;
            this.维度 = level;
            this.被锁定玩家 = new HashSet<>();
            this.被控制实体 = new HashSet<>(recording.被录制实体);
        }
    }

    /** 活跃的复刻会话：使用者UUID → 会话 */
    private static final Map<UUID, 复刻会话> 活跃会话 = new ConcurrentHashMap<>();

    private 复刻引擎() {}

    // ==================== 核心流程 ====================

    /**
     * 开始复刻（Phase 1 → Phase 2）
     *
     * 1. 预更新客户端方块（消除视觉延迟）
     * 2. 回滚世界到录制起点
     * 3. 清除录制期间出现的实体
     * 4. 恢复玩家快照 + 同步背包
     * 5. 标记被锁定玩家 + 从帧驱动中移除玩家（让tick正常跑）
     */
    public static boolean 开始复刻(ServerPlayer user, 录制管理器.录制会话 recording) {
        UUID userUUID = user.getUUID();
        ServerLevel level = recording.维度;

        // 1. 提前通知客户端方块变化
        发送方块预更新包(recording.起点快照, level);

        // 2. 回滚世界
        int[] result = recording.起点快照.恢复到(level);LOGGER.info("世界回滚完成：{}个实体, {} 个方块", result[0], result[1]);

        // 3. 清除录制期间出现的实体（防刷物品/刷箭）
        int 清除数量 = 清除录制期间出现的实体(level, recording);
        if (清除数量 > 0) {
            LOGGER.info("清除了 {} 个录制期间出现的实体", 清除数量);
        }

        // 4. 恢复玩家快照 + 强制同步背包
        for (Map.Entry<UUID, 玩家快照> entry : recording.玩家快照表.entrySet()) {
            Entity entity = findEntityByUUID(level, entry.getKey());
            if (entity instanceof ServerPlayer sp) {
                entry.getValue().恢复到(sp);网络工具.发送给玩家(sp, 背包同步包.从玩家构建(sp));
            }
        }

        // 5. 创建会话
        复刻会话 session = new 复刻会话(userUUID, recording, level);

        for (UUID entityUUID : recording.被录制实体) {
            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(),
                        sp.getYRot(), sp.getXRot());

                session.被控制实体.remove(entityUUID);
                session.被锁定玩家.add(entityUUID);

                // 服务端恢复使用物品状态
                if (recording.初始状态 != null && recording.初始状态.有进行中的操作()) {
                    recording.初始状态.恢复到服务端(sp);
                }

                // 通知客户端恢复按键状态（通用，覆盖所有mod）
                List<String> heldKeys = recording.玩家初始按键.get(entityUUID);
                if (heldKeys != null && !heldKeys.isEmpty()) {
                    网络工具.发送给玩家(sp,
                            new com.v2t.puellamagi.core.network.packets.s2c.录制状态通知包(
                                    false, heldKeys));
                }
            }
        }

        活跃会话.put(userUUID, session);
        LOGGER.info("玩家 {} 开始复刻（帧驱动 {} 个实体，输入回放 {} 个玩家）",
                user.getName().getString(),
                session.被控制实体.size(),
                session.被锁定玩家.size());
        return true;
    }

    /**
     * 每tick推进复刻
     *
     * 由通用事件在ServerTickEvent.START阶段调用
     *
     * @return 是否仍在复刻中（false = 自然结束）
     */
    public static boolean tick(UUID userUUID) {
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return false;

        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        if (state == null) return false;

        int currentFrame = state.获取当前复刻帧();
        if (currentFrame >= state.获取总复刻帧数()) {
            LOGGER.debug("玩家 {} 复刻自然结束", userUUID);
            return false;
        }

        ServerLevel level = session.维度;

        // 只驱动怪物帧（不干预玩家）
        驱动实体帧(session, level, currentFrame);

        // 每帧方块修正（输入回放的保底）
        帧方块修正(session, level, currentFrame);

        // 每帧方块实体修正（容器内容保底）
        帧方块实体修正(session, level, currentFrame);

        // 发帧同步包给客户端（输入帧 + 怪物帧数据）
        发送帧同步包给客户端(session, currentFrame);

        state.推进复刻帧();
        return true;
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
        for (方块实体变化帧 change : session.录制.方块实体变化列表) {
            if (change.获取tick序号() != frameIndex) continue;

            BlockPos pos = change.获取位置();
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;

            CompoundTag expected = change.获取新NBT();
            CompoundTag actual = be.saveWithoutMetadata();

            if (actual.equals(expected)) continue;

            // 对比物品差异 → 扣/加背包
            调整玩家背包(session, level, change.获取旧NBT(), expected);

            // 强制设NBT
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
            Entity entity = findEntityByUUID(level, lockedUUID);
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
            String key =物品键(item);
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
     *这里保证方块状态和录制时完全一致
     *
     * 每tick检查：这一帧有没有方块应该变化但没变化的
     * → 有 → 强制修正
     * → 没有 → 不管
     *
     * 不会多放/多破坏：
     * → 只处理当前帧应该发生的变化
     * → 输入回放已经正确处理的→ 跳过
     */
    private static void 帧方块修正(复刻会话 session, ServerLevel level, int frameIndex) {
        for (方块变化帧 change : session.录制.方块变化列表) {
            if (change.获取tick序号() != frameIndex) continue;

            BlockPos pos = change.获取位置();
            BlockState expected = change.获取新状态();
            BlockState actual = level.getBlockState(pos);

            if (actual.equals(expected)) continue;

            if (expected.isAir() && !actual.isAir()) {
                // 应该被破坏但还在→ 破坏（有掉落物）
                level.destroyBlock(pos, true);
            } else if (!expected.isAir()) {
                // 应该有方块但没有/状态不对
                net.minecraft.world.item.ItemStack needed =new net.minecraft.world.item.ItemStack(expected.getBlock().asItem());

                if (needed.isEmpty()) {
                    // 没有对应物品的方块（基岩等）→ 直接放
                    level.setBlockAndUpdate(pos, expected);
                } else {
                    // 检查是否有创造模式玩家
                    boolean 创造模式 = false;
                    for (UUID lockedUUID : session.被锁定玩家) {
                        Entity entity = findEntityByUUID(level, lockedUUID);
                        if (entity instanceof ServerPlayer sp && sp.isCreative()) {
                            创造模式 = true;
                            break;
                        }
                    }

                    if (创造模式) {
                        // 创造模式不扣背包直接放
                        level.setBlockAndUpdate(pos, expected);
                    } else {
                        boolean 已扣除 = false;
                        for (UUID lockedUUID : session.被锁定玩家) {
                            Entity entity = findEntityByUUID(level, lockedUUID);
                            if (!(entity instanceof ServerPlayer sp)) continue;

                            int slot = sp.getInventory().findSlotMatchingItem(needed);
                            if (slot >= 0) {
                                sp.getInventory().removeItem(slot, 1);
                                已扣除 = true;
                                break;
                            }
                        }

                        if (已扣除) {
                            level.setBlockAndUpdate(pos, expected);
                        }
                        // 背包没有→ 不放（防刷）
                    }
                }
            }
        }
    }

    /**
     * 进入时间删除（Phase 2 → Phase 3）
     */
    public static boolean 进入时间删除(ServerPlayer user) {
        UUID userUUID = user.getUUID();
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return false;

        session.被锁定玩家.remove(userUUID);
        session.被控制实体.remove(userUUID);

        存在屏蔽器.屏蔽除外(userUUID,屏蔽来源, userUUID);
        session.时间删除中 = true;
        user.setInvulnerable(true);

        LOGGER.info("玩家 {} 进入时间删除", user.getName().getString());
        return true;
    }

    /**
     * 跳到结尾（时间删除中按键）
     */
    public static void 跳到结尾(UUID userUUID) {
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return;

        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        if (state == null) return;

        int lastFrame = state.获取总复刻帧数() - 1;
        if (lastFrame >=0) {
            驱动实体帧(session, session.维度, lastFrame);
            发送帧同步包给客户端(session, lastFrame);
        }

        LOGGER.info("玩家 {} 复刻跳到结尾（从帧 {} 跳到帧 {}）",
                userUUID, state.获取当前复刻帧(), lastFrame);
        结束复刻(userUUID);
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
    public static void 结束复刻(UUID userUUID) {
        复刻会话 session = 活跃会话.remove(userUUID);
        if (session == null) return;

        // 1. 应用最终帧
        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        int lastFrame = -1;
        if (state != null) {
            lastFrame = state.获取总复刻帧数() - 1;if (lastFrame >= 0) {
                驱动实体帧(session, session.维度, lastFrame);
            }
        }

        // 2. 通知客户端
        通知客户端复刻结束(session);

        // 4. 强制同步位置到录制结束时的位置
        if (lastFrame >= 0) {
            Map<UUID, 实体帧数据> lastFrameData = session.录制.帧数据.获取帧(lastFrame);
            if (lastFrameData != null) {
                for (UUID lockedUUID : session.被锁定玩家) {
                    实体帧数据 data = lastFrameData.get(lockedUUID);
                    if (data == null) continue;

                    Entity entity = findEntityByUUID(session.维度, lockedUUID);
                    if (entity instanceof ServerPlayer sp) {
                        sp.connection.teleport(
                                data.获取X(), data.获取Y(), data.获取Z(),
                                data.获取YRot(), data.获取XRot());
                    }
                }
            }
        }

        // 5. 释放输入锁定
        for (UUID lockedUUID : session.被锁定玩家) {
            输入接管器.释放(lockedUUID,接管来源);
        }

        // 6. 解除使用者隐身/无敌
        if (session.时间删除中) {
            存在屏蔽器.解除屏蔽(userUUID, 屏蔽来源);

            Entity user = findEntityByUUID(session.维度, userUUID);
            if (user instanceof ServerPlayer player) {
                player.setInvulnerable(false);}
        }

        LOGGER.info("玩家 {} 复刻结束", userUUID);
    }
    //==================== 帧驱动逻辑 ====================

    /**
     * 驱动实体帧：将录制的完整状态应用到实体
     *
     * 被锁定玩家：设位置、朝向和装备（操作由MC根据输入自己处理）
     * 被控制实体（怪物等）：完整帧驱动（NBT + 手动字段）
     */
    private static void 驱动实体帧(复刻会话 session, ServerLevel level, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        Map<UUID, 实体帧数据> prevFrame = frameIndex > 0
                ? session.录制.帧数据.获取帧(frameIndex - 1) : null;

        // 被控制实体（怪物等）：完整帧驱动
        for (Map.Entry<UUID, 实体帧数据> entry : frame.entrySet()) {
            UUID entityUUID = entry.getKey();
            实体帧数据 当前帧 = entry.getValue();

            if (!session.被控制实体.contains(entityUUID)) continue;

            实体帧数据 上一帧 = (prevFrame != null) ? prevFrame.get(entityUUID) : null;

            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity == null) continue;

            if (entity instanceof LivingEntity living) {
                当前帧.应用到活体(living, 上一帧);
                广播动画事件(level, living, 当前帧, 上一帧);
            } else {
                当前帧.应用到普通实体(entity, 上一帧);
            }
        }
    }

    /**
     * 广播动画事件给客户端（边沿检测）
     */
    private static void 广播动画事件(ServerLevel level, LivingEntity entity,
                                     实体帧数据 当前帧, @Nullable 实体帧数据 上一帧) {
        //挥手
        boolean 之前挥手 = (上一帧 != null) && 上一帧.是否挥手();
        if (当前帧.是否挥手() && !之前挥手) {
            level.getChunkSource().broadcastAndSend(entity,
                    new ClientboundAnimatePacket(entity, 0));
        }

        // 受击红闪
        int 之前受击 = (上一帧 != null) ? 上一帧.获取受击时间() : 0;
        if (当前帧.获取受击时间() > 0 && 之前受击 == 0) {
            level.getChunkSource().broadcastAndSend(entity,
                    new ClientboundHurtAnimationPacket(entity));
        }

        // 死亡
        int 之前死亡 = (上一帧 != null) ? 上一帧.获取死亡时间() : 0;
        if (当前帧.获取死亡时间() > 0 && 之前死亡 == 0) {
            level.broadcastEntityEvent(entity, (byte) 3);
        }
    }

    /**
     * 同步被锁定玩家的位置到服务端内部系统
     */
    private static void 同步玩家状态(复刻会话 session, ServerLevel level, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        for (UUID playerUUID : session.被锁定玩家) {
            实体帧数据 data = frame.get(playerUUID);
            if (data == null) continue;

            Entity entity = findEntityByUUID(level, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            sp.absMoveTo(data.获取X(), data.获取Y(), data.获取Z(),
                    data.获取YRot(), data.获取XRot());sp.connection.resetPosition();
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
     * 发送帧同步包给同维度所有客户端
     */
    private static void 发送帧同步包给客户端(复刻会话 session, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        Map<UUID, 实体帧数据> prevFrame = frameIndex > 0
                ? session.录制.帧数据.获取帧(frameIndex - 1) : null;

        List<实体帧数据> 当前帧列表 = new ArrayList<>();
        List<实体帧数据> 上一帧列表 = new ArrayList<>();

        for (Map.Entry<UUID, 实体帧数据> entry : frame.entrySet()) {
            UUID entityUUID = entry.getKey();
            if (!session.被控制实体.contains(entityUUID)
                    && !session.被锁定玩家.contains(entityUUID)) continue;

            当前帧列表.add(entry.getValue());

            if (prevFrame != null) {
                实体帧数据 prev = prevFrame.get(entityUUID);
                if (prev != null) {
                    上一帧列表.add(prev);
                }
            }
        }

        Map<UUID, 玩家输入帧>输入帧 = new HashMap<>();
        for (Map.Entry<UUID, List<玩家输入帧>> entry : session.录制.玩家输入表.entrySet()) {
            List<玩家输入帧> inputs = entry.getValue();
            if (frameIndex < inputs.size()) {
                输入帧.put(entry.getKey(), inputs.get(frameIndex));
            }
        }

        Map<UUID, List<float[]>> 鼠标样本 = new HashMap<>();

        if (当前帧列表.isEmpty() && 输入帧.isEmpty()) return;

        复刻帧同步包 packet = new 复刻帧同步包(
                session.使用者UUID, 当前帧列表, 上一帧列表, 输入帧, 鼠标样本);

        for (ServerPlayer player : session.维度.players()) {
            网络工具.发送给玩家(player, packet);
        }
    }

    /**
     * 通知客户端复刻结束（发送空帧列表）
     */
    private static void 通知客户端复刻结束(复刻会话 session) {
        复刻帧同步包 packet = new 复刻帧同步包(
                session.使用者UUID, new ArrayList<>(), new ArrayList<>());

        for (ServerPlayer player : session.维度.players()) {
            网络工具.发送给玩家(player, packet);
        }
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
            if (entry.getValue().维度== level) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static List<UUID> 获取所有活跃使用者() {
        return new ArrayList<>(活跃会话.keySet());
    }

    public static boolean 是否复刻中(UUID userUUID) {
        return 活跃会话.containsKey(userUUID);
    }

    @Nullable
    public static 复刻会话 获取会话(UUID userUUID) {
        return 活跃会话.get(userUUID);
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

            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(session.使用者UUID);
            if (state == null) continue;

            return session.录制.帧数据.获取实体帧(state.获取当前复刻帧(), playerUUID);
        }
        return null;
    }

    // ==================== 工具方法 ====================

    private static int 清除录制期间出现的实体(ServerLevel level, 录制管理器.录制会话 recording) {
        double range = 录制管理器.获取录制范围();
        AABB box = AABB.ofSize(recording.录制中心, range * 2, range * 2, range * 2);
        Set<UUID> 快照实体 = recording.起点快照.获取所有实体UUID();

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
        结束复刻(playerUUID);
        输入接管器.玩家下线(playerUUID);
    }

    public static void 清除全部() {
        for (复刻会话 session : 活跃会话.values()) {
            for (UUID lockedUUID : session.被锁定玩家) {
                输入接管器.释放(lockedUUID, 接管来源);
            }
            if (session.时间删除中) {
                存在屏蔽器.解除屏蔽(session.使用者UUID, 屏蔽来源);
            }
        }
        活跃会话.clear();
    }
}
