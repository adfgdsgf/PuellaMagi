// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/时停管理器.java

package com.v2t.puellamagi.system.ability.timestop;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时停管理器- 对外API入口
 *
 * 职责：
 * - 提供时停操作的统一入口
 * - 委托给 Level（TimeStop 接口）执行实际逻辑
 * - 管理伤害累计
 * - 管理蓄力状态
 *
 * 伤害释放机制（对标 Roundabout）：
 * - 不在结束时停时主动释放
 * - 而是每tick检查每个实体，当实体"解冻"时自动释放
 * - 伤害累加存储，一次性释放总伤害（避免无敌帧问题）
 */
public final class 时停管理器 {
    private 时停管理器() {}

    // ==================== 伤害累计 ====================

    private static final Map<Integer, 累计伤害数据> 累计伤害表 = new ConcurrentHashMap<>();

    /**
     * 累计伤害数据 - 存储累加值而非多条记录
     */
    public static class 累计伤害数据 {
        private float 总伤害 = 0;
        private DamageSource 最后伤害源;
        private int 攻击次数 = 0;

        public void 添加伤害(DamageSource source, float amount) {
            总伤害 += amount;
            最后伤害源 = source;
            攻击次数++;
        }

        public float 获取总伤害() { return 总伤害; }
        public DamageSource 获取伤害源() { return 最后伤害源; }
        public int 获取攻击次数() { return 攻击次数; }
    }

    // ==================== 蓄力状态管理 ====================

    private static final Set<UUID> 蓄力中玩家 = ConcurrentHashMap.newKeySet();

    // ==================== 时停控制 ====================

    /**
     * 开始时停
     */
    public static void 开始时停(ServerPlayer player) {
        TimeStop timeStop = (TimeStop) player.level();

        if (timeStop.puellamagi$isTimeStopper(player)) {
            PuellaMagi.LOGGER.warn("玩家 {} 已经在时停中", player.getName().getString());
            return;
        }

        timeStop.puellamagi$addTimeStopper(player);
        PuellaMagi.LOGGER.info("时停管理器:玩家 {} 开始时停", player.getName().getString());
    }

    /**
     * 结束时停
     *
     * 注意：不再在这里主动释放伤害！
     * 伤害释放由 tickNonPassenger 中的检查自动处理
     * 当实体不再被冻结时，会自动释放累计伤害
     */
    public static void 结束时停(ServerPlayer player) {
        TimeStop timeStop = (TimeStop) player.level();

        if (!timeStop.puellamagi$isTimeStopper(player)) {
            return;
        }

        timeStop.puellamagi$removeTimeStopper(player);
        PuellaMagi.LOGGER.info("时停管理器: 玩家 {} 结束时停", player.getName().getString());
    }

    /**
     * 强制结束所有时停
     */
    public static void 强制结束所有() {
        累计伤害表.clear();
        蓄力中玩家.clear();
        PuellaMagi.LOGGER.info("已强制结束所有时停");
    }

    // ==================== 伤害累计与释放 ====================

    /**
     * 存储伤害 - 累加到总值
     */
    public static void 存储伤害(Entity entity, DamageSource source, float amount) {
        int id = entity.getId();

        累计伤害表.compute(id, (key, existing) -> {
            累计伤害数据 data = existing != null ? existing : new 累计伤害数据();
            data.添加伤害(source, amount);
            return data;
        });
    }

    /**
     * 获取实体累计伤害总值
     */
    public static float 获取累计伤害(Entity entity) {
        累计伤害数据 data = 累计伤害表.get(entity.getId());
        return data != null ? data.获取总伤害() : 0;
    }

    /**
     * 检查实体是否有累计伤害
     */
    public static boolean 有累计伤害(Entity entity) {
        累计伤害数据 data = 累计伤害表.get(entity.getId());
        return data != null && data.获取总伤害() > 0;
    }

    /**
     * 尝试释放单个实体的累计伤害 - 一次性释放总伤害
     *
     * 由TimestopServerLevelMixin.tickNonPassenger 调用
     * 条件：实体不再被冻结时调用此方法
     *
     * @param living 要释放伤害的实体
     */
    public static void 尝试释放实体伤害(LivingEntity living) {
        int id = living.getId();
        累计伤害数据 data = 累计伤害表.remove(id);

        if (data == null || data.获取总伤害() <= 0) {
            return;
        }

        if (!living.isAlive()) {
            return;
        }

        // 清除无敌帧，确保伤害能生效
        living.invulnerableTime = 0;

        // 一次性释放总伤害
        living.hurt(data.获取伤害源(), data.获取总伤害());

        // 标记需要同步
        living.hurtMarked = true;
    }

    /**
     * 强制释放所有累计伤害（备用方法）
     *
     * 用于特殊情况，如服务器关闭前清理
     */
    public static void 强制释放所有伤害(ServerLevel level) {
        if (累计伤害表.isEmpty()) {
            return;
        }

        Map<Integer, 累计伤害数据> snapshot = new HashMap<>(累计伤害表);
        累计伤害表.clear();

        for (var entry : snapshot.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living && living.isAlive()) {
                累计伤害数据 data = entry.getValue();
                living.hurt(data.获取伤害源(), data.获取总伤害());
            }
        }
    }

    // ==================== 状态查询（全部委托给 Level）====================

    /**
     * 检查是否存在时停
     */
    public static boolean 存在时停(Level level) {
        return ((TimeStop) level).puellamagi$hasActiveTimeStop();
    }

    /**
     * 检查玩家是否是时停者
     */
    public static boolean 是否时停者(Player player) {
        if (player == null) return false;
        return ((TimeStop) player.level()).puellamagi$isTimeStopper(player);
    }

    /**
     * 检查实体是否被冻结
     */
    public static boolean 是否被冻结(Entity entity) {
        if (entity == null) return false;
        return ((TimeStop) entity.level()).puellamagi$shouldFreezeEntity(entity);
    }

    // ==================== 蓄力状态管理 ====================

    public static void 开始蓄力(Player player) {
        蓄力中玩家.add(player.getUUID());
    }

    public static void 结束蓄力(Player player) {
        蓄力中玩家.remove(player.getUUID());
    }

    public static boolean 是否正在蓄力(Player player) {
        return 蓄力中玩家.contains(player.getUUID());
    }

    public static void 打断蓄力(Player player) {
        if (是否正在蓄力(player)) {
            结束蓄力(player);}
    }

    // ==================== 玩家下线/死亡处理 ====================

    /**
     * 玩家下线时调用
     */
    public static void 玩家下线(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TimeStop timeStop = (TimeStop) serverPlayer.level();

            if (timeStop.puellamagi$isTimeStopper(serverPlayer)) {
                timeStop.puellamagi$removeTimeStopper(serverPlayer);
                PuellaMagi.LOGGER.info("时停者 {} 下线，时停结束", player.getName().getString());
            }
        }
        蓄力中玩家.remove(player.getUUID());
    }
}
