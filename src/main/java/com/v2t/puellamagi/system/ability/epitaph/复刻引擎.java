package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.core.network.packets.s2c.复刻帧同步包;
import com.v2t.puellamagi.core.network.packets.s2c.方块批量更新包;
import com.v2t.puellamagi.core.network.packets.s2c.背包同步包;
import com.v2t.puellamagi.util.network.存在屏蔽器;
import com.v2t.puellamagi.util.network.输入接管器;
import com.v2t.puellamagi.util.recording.回放校验器;
import com.v2t.puellamagi.util.recording.实体帧数据;
import com.v2t.puellamagi.util.recording.世界快照;
import com.v2t.puellamagi.util.recording.玩家快照;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复刻引擎
 *
 * 架构核心：Level层帧驱动
 *
 * 与时停的对比：
 * - 时停：Level.tick拦截 → cancel实体tick → 实体保持不动
 * - 复刻：Level.tick拦截 → cancel实体tick → 用帧数据设实体状态
 *
 * 流程：
 * 1. 回滚世界到录制起点
 * 2. 每tick：cancel被控制实体的tick → 应用帧数据（NBT + 手动字段）
 * 3. 时间删除：释放使用者，其他人继续帧驱动
 * 4. 结束/跳到结尾：应用最后一帧 → 清理
 *
 * 被控制实体的正常tick被cancel，所以：
 * - AI不执行（不会产生偏差）
 * - 物理不执行（不会产生偏差）
 * - 所有状态完全由帧数据决定
 *
 * - 以上简介需要根据代码重写，因为已经整改过多次
 */
public final class 复刻引擎 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/ReplayEngine");

    private static final String 接管来源 = "epitaph";
    private static final String 屏蔽来源 = "epitaph";

    // ==================== 复刻会话 ====================

    /**
     * 单个使用者的复刻会话
     */
    public static class 复刻会话 {
        public final UUID 使用者UUID;
        public final 录制管理器.录制会话 录制;
        public final ServerLevel 维度;

        /** 被命运锁定的玩家UUID（用于输入屏蔽） */
        public final Set<UUID> 被锁定玩家;

        /**
         * 被帧驱动控制的实体UUID集合
         *
         * 与"被锁定玩家"的区别：
         * - 被锁定玩家：只有玩家，控制输入屏蔽
         * - 被控制实体：所有实体（玩家+怪物+投射物），控制tick拦截
         *
         * 时间删除时使用者从两个集合中都移除
         */
        public final Set<UUID> 被控制实体;

        /** 使用者是否在时间删除中 */
        public boolean 时间删除中= false;

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
     * 1. 回滚世界到录制起点
     * 2. 同步玩家位置到客户端
     * 3. 被录制的玩家从帧驱动中移除（由客户端输入回放驱动）
     * 4. 非玩家实体保留帧驱动（怪物/投射物等）
     *
     * 玩家的命运锁定方式变更：
     * 旧方案：服务端输入接管器丢弃C2S包 → 服务端帧数据驱动位置
     * 新方案：客户端Mixin替换键盘/鼠标输入 → MC自己算移动 → 正常发包
     *
     * 新方案优势：
     * MC自己计算bob、walkDist、手部位置等所有内部状态
     * 不需要手动设几百个字段 → 和正常游玩视觉完全一致
     */
    public static boolean 开始复刻(ServerPlayer user, 录制管理器.录制会话 recording) {
        UUID userUUID = user.getUUID();
        ServerLevel level = recording.维度;

        // 1. 提前通知客户端方块变化，消除视觉延迟
        发送方块预更新包(recording.起点快照, level);

        // 2. 回滚世界
        世界快照 snapshot = recording.起点快照;
        int[] result = snapshot.恢复到(level);
        LOGGER.info("世界回滚完成：{} 个实体, {} 个方块", result[0], result[1]);


        // 清除范围内的掉落物（防止刷物品）
        int 清除数量 = 清除范围内掉落物(level, recording.录制中心);
        if (清除数量 > 0) {
            LOGGER.info("清除了 {} 个掉落物", 清除数量);
        }

        // 3. 恢复玩家快照 + 强制同步到客户端
        for (Map.Entry<UUID, 玩家快照> entry : recording.玩家快照表.entrySet()) {
            Entity entity = findEntityByUUID(level, entry.getKey());
            if (entity instanceof ServerPlayer sp) {
                // 恢复背包/血量/经验等
                entry.getValue().恢复到(sp);

                // 用自己的包强制同步背包（绕过stateId校验）
                网络工具.发送给玩家(sp, 背包同步包.从玩家构建(sp));
            }
        }

        // 恢复录制开始时的使用物品状态
        // 如果玩家在按录制键之前就在使用物品（吃东西/拉弓等）
        // 世界回滚会把使用物品状态清掉
        // 这里从录制第0帧的帧数据里读取使用物品状态，直接在服务端恢复
        // 服务端恢复后传令兵自动同步给客户端 → 动画自然出现
        Map<UUID, 实体帧数据> 第一帧 = recording.帧数据.获取帧(0);
        if (第一帧 != null) {
            for (Map.Entry<UUID, 实体帧数据> entry : 第一帧.entrySet()) {
                实体帧数据 data = entry.getValue();
                if (!data.是否使用物品()) continue;

                Entity entity = findEntityByUUID(level, entry.getKey());
                if (!(entity instanceof ServerPlayer sp)) continue;

                InteractionHand hand = data.获取使用物品的手();
                if (hand != null && !sp.getItemInHand(hand).isEmpty()) {
                    sp.startUsingItem(hand);
                    LOGGER.debug("恢复玩家 {} 的使用物品状态: hand={}",
                            sp.getName().getString(), hand);
                }
            }
        }

        // 4. 创建复刻会话
        复刻会话 session = new 复刻会话(userUUID, recording, level);

        // 5. 处理被录制的玩家
        for (UUID entityUUID : recording.被录制实体) {
            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity instanceof ServerPlayer sp) {
                sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(),
                        sp.getYRot(), sp.getXRot());

                // FULL模式：拦截所有真实C2S包（玩家无法操作）
                // 交互包由复刻引擎按tick重放（精确坐标）
                输入接管器.接管(entityUUID, 接管来源);

                // 从被控制实体中移除 → 玩家tick正常跑
                // 破坏进度在PlayerInteractionManager.tick()里推进
                // 如果tick被cancel → 破坏进度不推进 → 方块挖不坏
                session.被控制实体.remove(entityUUID);

                // 标记为被锁定（客户端由输入回放驱动，服务端由包回放驱动）
                session.被锁定玩家.add(entityUUID);
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
     * 回滚前发送方块预更新包
     *
     * 把快照里所有方块的目标状态提前发给客户端
     * 客户端立即更新显示 → 服务端正式回滚时零延迟
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
        }LOGGER.debug("发送方块预更新包：{} 个方块", positions.size());
    }

    /**
     * 同步被锁定玩家的位置/朝向到服务端内部系统
     *
     * 帧驱动直接用setPos/setYRot设了位置和朝向
     * 但服务端还有些内部状态需要通过正常流程更新：
     * - ServerGamePacketListenerImpl的内部位置缓存
     * - 弓箭/投射物的发射方向
     * - 使用物品时的朝向检查
     *
     * 用connection.teleport()强制同步所有内部状态
     */
    private static void 同步玩家状态(复刻会话 session, ServerLevel level, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        for (UUID playerUUID : session.被锁定玩家) {
            实体帧数据 data = frame.get(playerUUID);
            if (data == null) continue;

            Entity entity = findEntityByUUID(level, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            // 直接设位置（不发包给客户端）
            sp.absMoveTo(
                    data.获取X(), data.获取Y(), data.获取Z(),
                    data.获取YRot(), data.获取XRot()
            );

            // 更新内部反作弊位置缓存
            // 否则服务端handleUseItemOn时检查距离会失败
            sp.connection.resetPosition();
        }
    }

    /**
     * 方块变化帧兜底：在该发生的tick直接应用方块变化
     *
     * 只在包回放没有处理的情况下才生效（方块状态不对时才修正）
     *
     * 正常情况：包回放已经正确破坏了方块 → 状态已经对了 → 这里跳过
     * 边缘情况：录制前开始挖 → 包回放没有START → 方块还在 → 这里兜底破坏
     *
     * 用setBlockAndUpdate → 服务端正常流程 → 但不会有掉落物
     * 因为setBlockAndUpdate不经过destroyBlock流程
     * 需要用destroyBlock才有掉落物
     */
    private static void 兜底方块变化(复刻会话 session, ServerLevel level, int frameIndex) {
        for (方块变化帧 change : session.录制.方块变化列表) {
            if (change.获取tick序号() != frameIndex) continue;

            BlockPos pos = change.获取位置();
            BlockState expected = change.获取新状态();
            BlockState actual = level.getBlockState(pos);

            // 已经是正确状态了（包回放处理过了）→ 跳过
            if (actual.equals(expected)) continue;

            // 包回放没管到 → 兜底
            if (expected.isAir() && !actual.isAir()) {
                // 方块应该被破坏但还在 → 用destroyBlock（有掉落物有粒子有音效）
                level.destroyBlock(pos, true);
                LOGGER.debug("方块兜底破坏: pos={}", pos);
            } else {
                // 其他情况（放置等）→ 直接设
                level.setBlockAndUpdate(pos, expected);
                LOGGER.debug("方块兜底设置: pos={}, state={}", pos, expected);
            }
        }
    }

    /**
     * 每tick推进复刻
     *
     * 由通用事件每tick调用
     * 注意：实体的正常tick已被Level Mixin cancel，
     * 这里只负责应用帧数据
     *
     * @return 是否仍在复刻中（false = 自然结束）
     */
    public static boolean tick(UUID userUUID) {
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return false;

        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        if (state == null) return false;

        int currentFrame = state.获取当前复刻帧();
        int totalFrames = state.获取总复刻帧数();

        if (currentFrame >= totalFrames) {
            LOGGER.debug("玩家 {} 复刻自然结束", userUUID);
            return false;
        }

        ServerLevel level = session.维度;

        // 服务端帧驱动（游戏逻辑 + 动画事件广播）
        // sendChanges被Mixin拦截，不会和帧同步包冲突
        驱动实体帧(session, level, currentFrame);

        // 同步被锁定玩家的移动状态到服务端内部系统
        //帧驱动设了位置/朝向，但服务端某些系统（如弓箭方向）
        // 需要通过正常的移动处理流程才能更新内部状态
        同步玩家状态(session, level, currentFrame);

        // 重放交互包（放方块/破坏/攻击/使用物品）
        重放交互包(session, currentFrame);

        // 恢复使用物品状态（在包回放之后、Level.tick之前设好）
        恢复使用物品状态(session, currentFrame);

        // 方块变化帧兜底
        // 包回放管得到的（整个挖掘在录制期间）→ 包回放管→ 有裂纹有掉落物
        // 包回放管不到的（录制前开始挖的）→ 这里兜底→ 到时间直接破坏
        兜底方块变化(session, level, currentFrame);

        // 帧同步包给客户端（渲染用）
        发送帧同步包给客户端(session, currentFrame);

        // 方块实时校验：输入回放做不到的由帧数据保底
        // 实时校验方块(session, level, currentFrame);

        // 推进帧
        state.推进复刻帧();

        return true;
    }

    /**
     * 进入时间删除（Phase 2 → Phase 3）
     *
     * 使用者脱离帧驱动控制，可自由行动
     * 其他实体继续按帧驱动
     */
    public static boolean 进入时间删除(ServerPlayer user) {
        UUID userUUID = user.getUUID();
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return false;

        // 使用者脱离输入锁定
        输入接管器.释放(userUUID, 接管来源);
        session.被锁定玩家.remove(userUUID);

        // 使用者脱离帧驱动控制
        session.被控制实体.remove(userUUID);

        // 对其他玩家屏蔽使用者（不可见）
        存在屏蔽器.屏蔽除外(userUUID, 屏蔽来源, userUUID);

        // 标记状态
        session.时间删除中 = true;

        // 使用者无敌
        user.setInvulnerable(true);

        LOGGER.info("玩家 {} 进入时间删除", user.getName().getString());
        return true;
    }

    /**
     * 跳到结尾（时间删除中第二次按键）
     *
     * 跳过剩余所有帧，直接应用最后一帧的状态
     * 然后结束复刻
     */
    public static void 跳到结尾(UUID userUUID) {
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return;

        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        if (state == null) return;

        ServerLevel level = session.维度;
        int lastFrame = state.获取总复刻帧数() - 1;

        if (lastFrame >= 0) {
            // 只在结束时把最终状态写回服务端实体
            // 这是复刻期间唯一一次在服务端设实体位置
            驱动实体帧(session, level, lastFrame);

            // 发送最终帧给客户端
            发送帧同步包给客户端(session, lastFrame);

            // 包回放方案替代，不再需要校验
            // int currentFrame = state.获取当前复刻帧();
            // for (int i = currentFrame; i <= lastFrame; i++) {
            //     实时校验方块(session, level, i);
            // }
        }LOGGER.info("玩家 {} 复刻跳到结尾（从帧 {} 跳到帧 {}）",
                userUUID, state.获取当前复刻帧(), lastFrame);

        结束复刻(userUUID);
    }

    /**
     * 结束复刻（任何原因）
     */
    public static void 结束复刻(UUID userUUID) {
        复刻会话 session = 活跃会话.remove(userUUID);
        if (session == null) return;

        // 把最终帧写回服务端实体（确保游戏继续后实体位置正确）
        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        if (state != null) {
            int lastFrame = state.获取总复刻帧数() - 1;
            if (lastFrame >= 0) {
                驱动实体帧(session, session.维度, lastFrame);
            }
        }

        // 回放校验：修正方块偏差+恢复玩家状态
        // 回放校验器.完整校验(session.维度,
        //         session.录制.方块变化列表,
        //         session.录制.结束快照表
        // );

        // 通知客户端清除复刻状态
        通知客户端复刻结束(session);

        // 释放所有被锁定玩家
        for (UUID lockedUUID : session.被锁定玩家) {
            输入接管器.释放(lockedUUID,接管来源);
        }

        // 解除使用者隐身
        if (session.时间删除中) {
            存在屏蔽器.解除屏蔽(userUUID,屏蔽来源);

            Entity user = findEntityByUUID(session.维度, userUUID);
            if (user instanceof ServerPlayer player) {
                player.setInvulnerable(false);
            }
        }

        LOGGER.info("玩家 {} 复刻结束", userUUID);
    }

    // ==================== 帧驱动逻辑 ====================

    /**
     * 驱动实体帧：将录制的完整状态应用到实体
     *
     * 对每个被控制的实体：
     * 1. 从帧数据获取当前帧和上一帧
     * 2. 调用 帧数据.应用到活体/应用到普通实体
     * 3. 内部顺序：load NBT → 设旧值 → 设当前值
     */
    private static void 驱动实体帧(复刻会话 session, ServerLevel level, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        Map<UUID, 实体帧数据> prevFrame = frameIndex > 0
                ? session.录制.帧数据.获取帧(frameIndex - 1) : null;

        for (Map.Entry<UUID, 实体帧数据> entry : frame.entrySet()) {
            UUID entityUUID = entry.getKey();
            实体帧数据 当前帧 = entry.getValue();

            // 时间删除中的使用者不被驱动
            if (!session.被控制实体.contains(entityUUID)) {
                continue;
            }

            // 获取上一帧数据
            实体帧数据 上一帧 = (prevFrame != null) ? prevFrame.get(entityUUID) : null;

            Entity entity = findEntityByUUID(level, entityUUID);
            if (entity == null) continue;

            if (entity instanceof LivingEntity living) {
                boolean 是被锁定玩家 = session.被锁定玩家.contains(entityUUID);

                if (是被锁定玩家) {
                    // 包回放玩家：只设位置和朝向，不设其他状态
                    // 位置必须正确：服务端handle包时检查玩家离方块的距离
                    // 如果位置不对 → "你离得太远" → 放置/破坏失败
                    // 其他状态（背包/血量等）由MC自己处理
                    living.setPos(当前帧.获取X(), 当前帧.获取Y(), 当前帧.获取Z());
                    living.xo = 当前帧.获取X();
                    living.yo = 当前帧.获取Y();
                    living.zo = 当前帧.获取Z();
                    living.xOld = 当前帧.获取X();
                    living.yOld = 当前帧.获取Y();
                    living.zOld = 当前帧.获取Z();
                    living.setYRot(当前帧.获取YRot());
                    living.setXRot(当前帧.获取XRot());
                    living.yRotO = 当前帧.获取YRot();
                    living.xRotO = 当前帧.获取XRot();
                    living.setYHeadRot(当前帧.获取YRot());
                    living.yBodyRot = 当前帧.获取身体YRot();
                    continue;
                } else {
                    // 非玩家实体（怪物等）：正常帧驱动
                    当前帧.应用到活体(living, 上一帧);
                }

                // 广播动画事件给客户端
                广播动画事件(level, living, 当前帧, 上一帧);
            } else {
                当前帧.应用到普通实体(entity, 上一帧);
            }
        }
    }


    /**
     * 广播动画事件给客户端
     *
     * MC的动画状态（swinging/hurtTime/deathTime）是服务端字段，
     * 不会自动同步到客户端。需要在状态变化时发送对应的动画包。
     *
     * 检测方式：比较当前帧和上一帧，发现变化时发包
     */
    private static void 广播动画事件(ServerLevel level, LivingEntity entity,实体帧数据 当前帧, @Nullable 实体帧数据 上一帧) {
        //---- 挥手动画 ----
        // 检测从"未挥手"到"挥手"的边沿
        // 直接发送ClientboundAnimatePacket，不调用entity.swing()避免覆盖帧数据
        boolean 之前挥手 = (上一帧 != null) && 上一帧.是否挥手();
        if (当前帧.是否挥手() && !之前挥手) {
            // 0 = SWING_MAIN_HAND, 3 = SWING_OFF_HAND
            ClientboundAnimatePacket swingPacket =
                    new ClientboundAnimatePacket(entity, 0);
            level.getChunkSource().broadcastAndSend(entity, swingPacket);
        }

        // ---- 受击红闪 ----
        // 检测hurtTime从0变为>0（新的受击事件）
        int 之前受击 = (上一帧 != null) ? 上一帧.获取受击时间() : 0;
        if (当前帧.获取受击时间() > 0 && 之前受击 == 0) {
            ClientboundHurtAnimationPacket hurtPacket =
                    new ClientboundHurtAnimationPacket(entity);
            level.getChunkSource().broadcastAndSend(entity, hurtPacket);
        }

        // ---- 死亡动画 ----
        // 检测deathTime从0变为>0
        int 之前死亡 = (上一帧 != null) ? 上一帧.获取死亡时间() : 0;
        if (当前帧.获取死亡时间() > 0 && 之前死亡 == 0) {
            // 发送死亡状态事件（byte 3 = death）
            level.broadcastEntityEvent(entity, (byte) 3);
        }
    }

    /**
     * 复刻方块变化：在对应tick应用方块变化
     */
    private static void 复刻方块变化(复刻会话 session, int frameIndex) {
        for (方块变化帧 change : session.录制.方块变化列表) {
            if (change.获取tick序号() == frameIndex) {
                session.维度.setBlockAndUpdate(change.获取位置(), change.获取新状态());
            }
        }
    }

    /**
     * 重放交互包：把录制时存的C2S包喂给服务端
     *
     * 按tick编号取出所有玩家的交互包，逐个重放
     * 每个玩家的包喂给对应的玩家connection
     */
    private static void 重放交互包(复刻会话 session, int frameIndex) {
        Map<UUID, 交互包帧> tickMap = session.录制.交互包表.get(frameIndex);
        if (tickMap == null) return;

        for (Map.Entry<UUID, 交互包帧> entry : tickMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            交互包帧 frame = entry.getValue();
            if (frame.是否为空()) continue;

            Entity entity = findEntityByUUID(session.维度, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            for (交互包帧.包记录 record : frame.获取包列表()) {
                重放单个包(sp, record);
            }
        }
    }

    /**
     * 重建并执行单个C2S包
     *
     * 直接调用ServerGamePacketListenerImpl的handle方法
     * 服务端完整处理：距离检查→物品消耗→方块变化→掉落物→S2C通知
     *
     * 注意：调handle前要临时解除输入接管
     * 否则我们自己的Mixin会拦截我们自己重放的包
     */
    private static void 重放单个包(ServerPlayer player,交互包帧.包记录 record) {
        UUID playerUUID = player.getUUID();

        // 临时解除拦截（否则自己的Mixin会拦自己的包）
        输入接管器.释放(playerUUID, 接管来源);
        输入接管器.标记重放中(playerUUID);

        try {
            if (record instanceof 交互包帧.右键方块包 pkg) {
                net.minecraft.world.phys.BlockHitResult hit =
                        new net.minecraft.world.phys.BlockHitResult(
                                pkg.点击位置(), pkg.面朝方向(),
                                pkg.方块位置(), pkg.在方块内()
                        );
                player.connection.handleUseItemOn(
                        new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(
                                pkg.手(), hit, pkg.序列号()
                        )
                );} else if (record instanceof 交互包帧.玩家动作包 pkg) {
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action action =
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
                                .values()[pkg.动作序号()];
                player.connection.handlePlayerAction(
                        new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                                action, pkg.方块位置(), pkg.面朝方向(), pkg.序列号()
                        )
                );

            } else if (record instanceof 交互包帧.使用物品包 pkg) {
                player.connection.handleUseItem(
                        new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                                pkg.手(), pkg.序列号()
                        )
                );

            } else if (record instanceof 交互包帧.交互实体包 pkg) {
                // InteractPacket结构复杂，后续实现

            } else if (record instanceof 交互包帧.挥手包 pkg) {
                player.connection.handleAnimate(
                        new net.minecraft.network.protocol.game.ServerboundSwingPacket(
                                pkg.手()
                        )
                );
            }
        } finally {
            // 恢复拦截 + 取消重放标记
            输入接管器.取消重放标记(playerUUID);
            输入接管器.接管(playerUUID, 接管来源);
        }
    }

    /**
     * 恢复使用物品状态（供通用事件在END阶段调用）
     */
    public static void 恢复使用物品(UUID userUUID) {
        复刻会话 session = 活跃会话.get(userUUID);
        if (session == null) return;

        预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
        if (state == null) return;

        int currentFrame = state.获取当前复刻帧();
        if (currentFrame <= 0) currentFrame = 0;
        // 当前帧已经在START阶段推进了，这里用上一帧（刚推进过的那帧）
        int frameToUse = Math.max(0, currentFrame - 1);恢复使用物品状态(session, frameToUse);
    }

    /**
     * 每tick恢复使用物品状态
     *
     * 两个关键点：
     * 1. 不调startUsingItem（会重置剩余时间→ 永远吃不完/拉不满弓）
     *    直接设内部字段 → 保持帧数据里录制的精确剩余时间
     *
     * 2. 必须在Level.tick之后执行（END阶段）
     *    因为Level.tick → player.tick会清掉使用物品状态
     *    我们在清掉之后设回去→ sendChanges同步给客户端→ 有动画
     *
     * 客户端的清除已经被EpitaphReplayMinecraftMixin拦住了
     * 这里解决的是服务端的清除
     */
    private static void 恢复使用物品状态(复刻会话 session, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        for (UUID playerUUID : session.被锁定玩家) {
            实体帧数据 data = frame.get(playerUUID);
            if (data == null) continue;

            Entity entity = findEntityByUUID(session.维度, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            if (data.是否使用物品()) {
                InteractionHand hand = data.获取使用物品的手();
                if (hand == null) continue;

                ItemStack useStack = sp.getItemInHand(hand);
                if (useStack.isEmpty()) continue;

                // 直接设内部字段，不调startUsingItem
                if (sp instanceof com.v2t.puellamagi.api.access.ILivingEntityAccess access) {
                    access.puellamagi$setUseItem(useStack.copy());
                    access.puellamagi$setUseItemRemaining(data.获取使用物品剩余时间());
                    access.puellamagi$setLivingEntityFlag(1, true);
                    access.puellamagi$setLivingEntityFlag(2,
                            hand == InteractionHand.OFF_HAND);
                }
            } else {
                // 帧数据说没在使用→ 确保停止
                if (sp.isUsingItem()) {
                    if (sp instanceof com.v2t.puellamagi.api.access.ILivingEntityAccess access) {
                        access.puellamagi$setUseItem(ItemStack.EMPTY);
                        access.puellamagi$setUseItemRemaining(0);
                        access.puellamagi$setLivingEntityFlag(1, false);
                        access.puellamagi$setLivingEntityFlag(2, false);
                    }
                }
            }
        }
    }

    /**
     * 实时方块校验：每tick检查方块变化是否正确发生
     *
     * 输入回放让MC自己放置/破坏方块（有动画/音效/掉落物）
     * 但浮点视角偏差可能导致MC没成功
     *
     * 此方法检查该tick应该发生的方块变化是否真的发生了：
     * - 已经正确 → 不做任何事（MC自己干成了）
     * - 不正确 → 强制setBlock修正（MC没干成，帮它补）
     *
     * 这样：
     * MC成功时→ 有完整的动画/音效/掉落物/背包消耗
     * MC失败时 → 方块状态至少是对的（没有动画但不会出错）
     */
    private static void 实时校验方块(复刻会话 session, ServerLevel level, int frameIndex) {
        for (方块变化帧 change : session.录制.方块变化列表) {
            if (change.获取tick序号() == frameIndex) {
                BlockState expected = change.获取新状态();
                BlockState actual = level.getBlockState(change.获取位置());

                if (!actual.equals(expected)) {
                    // MC没做到，强制修正
                    level.setBlockAndUpdate(change.获取位置(), expected);
                    LOGGER.debug("方块校验修正: pos={}, expected={}, actual={}",
                            change.获取位置(), expected, actual);
                }
            }
        }
    }

    /**
     * 发送帧同步包给同维度的所有客户端
     *
     * 包含三种数据：
     * 1. 实体帧数据（怪物/其他玩家）
     * 2. 玩家输入帧（移动方向）
     * 3. 鼠标增量列表（60Hz+视角delta）
     */
    private static void 发送帧同步包给客户端(复刻会话 session, int frameIndex) {
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(frameIndex);
        if (frame == null) return;

        Map<UUID, 实体帧数据> prevFrame = frameIndex > 0
                ? session.录制.帧数据.获取帧(frameIndex - 1) : null;

        // 收集被控制实体的帧数据
        List<实体帧数据> 当前帧列表 = new ArrayList<>();
        List<实体帧数据> 上一帧列表 = new ArrayList<>();

        for (Map.Entry<UUID, 实体帧数据> entry : frame.entrySet()) {
            UUID entityUUID = entry.getKey();
            // 被控制实体（怪物帧驱动）和被锁定玩家（包回放）都需要帧数据
            // 怪物：客户端用帧数据驱动位置/动画
            // 玩家：客户端用帧数据查询使用物品状态等
            if (!session.被控制实体.contains(entityUUID)&& !session.被锁定玩家.contains(entityUUID)) continue;

            当前帧列表.add(entry.getValue());

            if (prevFrame != null) {
                实体帧数据 prev = prevFrame.get(entityUUID);
                if (prev != null) {
                    上一帧列表.add(prev);
                }
            }
        }

        // 收集玩家输入帧
        Map<UUID, 玩家输入帧> 输入帧 = new HashMap<>();
        for (Map.Entry<UUID, List<玩家输入帧>> entry :
                session.录制.玩家输入表.entrySet()) {
            List<玩家输入帧> inputs = entry.getValue();
            if (frameIndex < inputs.size()) {
                输入帧.put(entry.getKey(), inputs.get(frameIndex));
            }
        }

        // 不发鼠标数据，视角由输入帧的yRot驱动
        Map<UUID, List<float[]>> 鼠标样本 = new HashMap<>();

        if (当前帧列表.isEmpty() && 输入帧.isEmpty()) return;

        复刻帧同步包 packet = new 复刻帧同步包(session.使用者UUID, 当前帧列表, 上一帧列表, 输入帧, 鼠标样本);

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

    // ==================== Level Mixin查询接口 ====================

    /**
     * 实体是否被任何复刻会话控制
     *
     * Level Mixin在tickNonPassenger时调用：
     * - 返回true → cancel该实体的tick（不执行AI/物理/自然逻辑）
     * - 返回false → 正常tick
     *
     * 类比时停的shouldFreezeEntity，但作用相反：
     * 时停 =冻结（什么都不做）
     * 复刻 = 帧驱动（由帧数据设状态，但同样不执行原始tick）
     */
    public static boolean 实体是否被复刻控制(Entity entity) {
        UUID entityUUID = entity.getUUID();
        for (复刻会话 session : 活跃会话.values()) {
            if (session.被控制实体.contains(entityUUID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定维度内存在活跃复刻的使用者UUID列表
     * 用于通用事件中驱动tick
     */
    public static List<UUID> 获取维度内活跃使用者(ServerLevel level) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, 复刻会话> entry : 活跃会话.entrySet()) {
            if (entry.getValue().维度 == level) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 获取所有活跃复刻会话的使用者UUID
     * 返回副本，避免并发修改
     */
    public static List<UUID> 获取所有活跃使用者() {
        return new ArrayList<>(活跃会话.keySet());
    }

    // ==================== 其他查询 ====================

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

    /**
     * 获取指定玩家当前帧的实体帧数据
     * 供Mixin调用（如客户端Camera锁定）
     */
    @Nullable
    public static 实体帧数据 获取当前帧数据(UUID playerUUID) {
        for (复刻会话 session : 活跃会话.values()) {
            if (!session.被控制实体.contains(playerUUID)) {
                continue;
            }

            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(session.使用者UUID);
            if (state == null) continue;

            int currentFrame = state.获取当前复刻帧();
            return session.录制.帧数据.获取实体帧(currentFrame, playerUUID);
        }
        return null;
    }

    // ==================== 工具方法 ====================

    /**
     * 清除范围内所有掉落物
     *
     * 回滚世界时方块恢复了但掉落物还在
     * 不清除的话回放时再次破坏会多掉一份→ 刷物品
     */
    private static int 清除范围内掉落物(ServerLevel level, Vec3 center) {
        double range = 录制管理器.获取录制范围();
        AABB box = AABB.ofSize(center, range * 2, range * 2, range * 2);
        List<net.minecraft.world.entity.item.ItemEntity> items =
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box);

        int count = 0;
        for (net.minecraft.world.entity.item.ItemEntity item : items) {
            item.discard();
            count++;
        }
        return count;
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
        // 作为使用者下线
        结束复刻(playerUUID);

        // 作为被锁定玩家下线
        输入接管器.玩家下线(playerUUID);
    }

    public static void 清除全部() {
        for (复刻会话 session : 活跃会话.values()) {
            for (UUID lockedUUID : session.被锁定玩家) {
                输入接管器.释放(lockedUUID, 接管来源);
            }if (session.时间删除中) {
                存在屏蔽器.解除屏蔽(session.使用者UUID, 屏蔽来源);
            }
        }
        活跃会话.clear();LOGGER.debug("复刻引擎已清空");
    }
}
