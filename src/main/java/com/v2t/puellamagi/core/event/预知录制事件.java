// 文件路径: src/main/java/com/v2t/puellamagi/core/event/预知录制事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import com.v2t.puellamagi.system.ability.epitaph.影响标记表;
import com.v2t.puellamagi.system.ability.epitaph.预知状态管理;
import com.v2t.puellamagi.常量;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 预知录制事件处理
 *
 * 职责：
 * - 服务端Tick驱动录制采集和复刻引擎
 * - 方块变化记录（放置/破坏）
 * - 时间删除传染标记系统（方块→掉落物→物品→合成产物）
 *
 * 所有事件都是预知能力（Epitaph）的录制/复刻相关
 * 不含业务逻辑，只做事件拦截和转发
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class 预知录制事件 {

    // ==================== 服务端Tick ====================

    /**
     * 服务端Tick — 驱动录制和复刻
     *
     * 录制：END阶段采集（Level.tick执行完毕，所有实体状态已更新）
     * 复刻：START阶段驱动（在Level.tick之前设好位置和重放包）
     *
     * 为什么复刻必须在START：
     * Level.tick → player.tick → ServerPlayerGameMode.tick
     * → 检查"玩家还在看着那个方块吗？"
     * → 如果我们还没设好位置/朝向 → 判定"移开了" → 破坏进度中断
     * → 放在START → 位置/朝向已就位 → 破坏进度正常推进
     */
    @SubscribeEvent
    public static void 服务端Tick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (event.phase == TickEvent.Phase.START) {
            // ==================== 方块变化追踪清空（每帧开始前） ====================
            // 必须在tick()之前清空，准备收集Level.tick期间MC处理C2S包产生的方块变化
            复刻引擎.清空帧方块追踪();

            // ==================== 复刻引擎驱动（Level.tick之前） ====================
            // 位置/朝向 + 正向修正必须在Level.tick之前
            // 否则破坏进度检查时玩家位置不对
            for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
                boolean still = 复刻引擎.tick(userUUID);
                if (!still) {
                    复刻引擎.结束复刻(userUUID);
                    预知状态管理.结束(userUUID);
                    PuellaMagi.LOGGER.info("玩家 {} 复刻自然播放完毕", userUUID);
                }
            }
        }

        if (event.phase == TickEvent.Phase.END) {
            // ==================== 反向保底（Level.tick之后） ====================
            // MC已处理完玩家的所有C2S包（放方块/破坏方块等）
            // 现在可以准确检测并回退多余的方块变化
            for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
                复刻引擎.tickEnd(userUUID);
            }

            // ==================== 录制帧采集（Level.tick之后） ====================
            for (UUID userUUID : 录制管理器.获取所有活跃使用者()) {
                录制管理器.采集帧(userUUID);
            }
        }
    }

    // ==================== 方块变化记录 ====================

    /**
     * 方块放置事件 — 录制方块变化 + 回放变化追踪
     *
     * 两个职责：
     * 1. 录制期间：记录方块变化到录制会话
     * 2. 回放期间：记录实际发生的方块变化到复刻引擎（供反向保底用）
     */
    @SubscribeEvent
    public static void 方块放置(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // 获取触发者UUID（放置方块的玩家）
        UUID triggerUUID = null;
        if (event.getEntity() instanceof Player player) {
            triggerUUID = player.getUUID();
        }

        // 录制期间：记录到录制会话
        录制管理器.记录方块变化(serverLevel, event.getPos(),
                event.getBlockSnapshot().getReplacedBlock(), event.getPlacedBlock(), triggerUUID);

        // 回放期间：记录到复刻引擎的方块变化追踪（供反向保底对比）
        if (复刻引擎.有活跃回放()) {
            复刻引擎.记录回放中方块变化(event.getPos(),
                    event.getBlockSnapshot().getReplacedBlock());
        }
    }

    /**
     * 方块破坏事件 — 录制方块变化 + 回放变化追踪
     */
    @SubscribeEvent
    public static void 方块破坏(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // 获取触发者UUID（破坏方块的玩家）
        UUID triggerUUID = event.getPlayer() != null ? event.getPlayer().getUUID() : null;

        // 录制期间：记录到录制会话
        录制管理器.记录方块变化(serverLevel, event.getPos(),
                event.getState(), Blocks.AIR.defaultBlockState(), triggerUUID);

        // 回放期间：记录到复刻引擎的方块变化追踪（供反向保底对比）
        if (复刻引擎.有活跃回放()) {
            复刻引擎.记录回放中方块变化(event.getPos(), event.getState());
        }
    }

    // ==================== 时间删除 — 传染标记 ====================

    /**
     * 方块破坏时：检查方块是否有来源标记 → 掉落物继承标记
     *
     * 场景：A（使用者）放了方块 → B破坏了 → 掉落物应该标记=A
     * 结算时掉落物被清除 → 不刷物品
     */
    @SubscribeEvent
    public static void 方块破坏传染(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();

        // 检查所有活跃复刻会话的标记表
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session == null || !session.时间删除中) continue;

            UUID 方块来源 = session.录制.标记表.获取方块来源(pos);
            if (方块来源 == null) continue;

            // 方块有标记 → 掉落物继承
            // 延迟一tick标记掉落物（掉落物在破坏后才生成）
            final UUID source = 方块来源;
            serverLevel.getServer().tell(new TickTask(
                    serverLevel.getServer().getTickCount() + 1, () -> {
                // 搜索方块位置附近的掉落物
                AABB area = new AABB(pos).inflate(2.0);
                for (Entity entity : serverLevel.getEntities(
                        (Entity) null, area,
                        e -> e instanceof ItemEntity)) {
                    session.录制.标记表.标记掉落物(entity.getId(), source);
                }
            }));

            // 清除方块标记（方块已经没了）
            session.录制.标记表.清除方块标记(pos);
            break;
        }
    }

    /**
     * 玩家拾取物品时：检查掉落物是否有来源标记 → 物品NBT加tag
     *
     * 场景：B捡起了来源=A的掉落物 → 物品NBT标记A
     * 结算时从B背包清除
     */
    @SubscribeEvent
    public static void 拾取传染(EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide) return;

        ItemEntity itemEntity = event.getItem();
        int entityId = itemEntity.getId();

        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session == null || !session.时间删除中) continue;

            UUID 来源 = session.录制.标记表.获取掉落物来源(entityId);
            if (来源 == null) continue;

            // 掉落物有标记 → 物品NBT加tag
            ItemStack stack = itemEntity.getItem();
            影响标记表.标记物品(stack, 来源);

            // 清除掉落物标记（已被捡起）
            session.录制.标记表.清除掉落物标记(entityId);
            break;
        }
    }

    /**
     * 合成时：检查原料是否有来源标记 → 产物继承标记 + 玩家自己的原料退回
     *
     * 场景：B用A的钻石+自己的木棍合成了东西
     * → 产物标记=A → 结算时删除
     * → 木棍退回B背包 → B不亏
     */
    @SubscribeEvent
    public static void 合成传染(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 检查是否在复刻的时间删除期间
        boolean 在时删中 = false;
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session != null && session.时间删除中) {
                在时删中 = true;
                break;
            }
        }
        if (!在时删中) return;

        // 检查原料中是否有标记的物品
        net.minecraft.world.Container craftMatrix = event.getInventory();
        UUID 标记来源 = null;
        List<ItemStack> 玩家自己的原料 = new ArrayList<>();

        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack ingredient = craftMatrix.getItem(i);
            if (ingredient.isEmpty()) continue;

            UUID 物品来源 = 影响标记表.获取物品来源(ingredient);
            if (物品来源 != null) {
                标记来源 = 物品来源;
            } else {
                // 不是标记物品 → 是玩家自己的原料 → 记录用于退回
                玩家自己的原料.add(ingredient.copy());
            }
        }

        if (标记来源 == null) return;

        // 产物标记来源
        影响标记表.标记物品(event.getCrafting(), 标记来源);

        // 退回玩家自己的原料
        for (ItemStack 退回物品 : 玩家自己的原料) {
            if (!player.getInventory().add(退回物品)) {
                // 背包满了就丢地上
                player.drop(退回物品, false);
            }
        }
    }
}
