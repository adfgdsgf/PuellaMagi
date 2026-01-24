// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/effect/假死状态处理器.java

package com.v2t.puellamagi.system.soulgem.effect;

import com.v2t.puellamagi.core.network.packets.s2c.假死状态同步包;
import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.util.灵魂宝石距离计算;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 假死状态处理器
 *
 * 职责：
 * - 管理玩家的假死状态
 * - 处理假死进入/退出逻辑
 * - 同步假死状态到客户端
 * - 管理致命伤害标记（跨Mixin传递状态）
 *
 * 注意：行动限制判断已迁移到 行动限制管理器
 * 本类只负责状态管理，不再提供限制判断方法
 *
 * 假死触发条件（满足任一）：
 * - 距离超出范围（50格+）
 * - 跨维度
 * - 持有者离线
 * - 血量 <= 0（空血假死）
 *
 * 假死退出条件（必须全部满足）：
 * - 距离在范围内
 * - 同维度
 * - 持有者在线（或宝石不在玩家背包）
 * - 血量 >=恢复阈值（默认5）
 */
public final class 假死状态处理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/FeignDeath");

    // ==================== 配置常量 ====================

    /**宝石位置未知多久后重新生成（tick） */
    private static final long 宝石重生成延迟 = 20* 60 * 5;  // 5分钟

    /** 假死超时时间（tick） */
    private static final long 假死超时时间 = 20 * 60 * 30;   // 30分钟

    /** 血量恢复阈值 - 血量达到此值以上才能退出空血假死 */
    private static final float 血量恢复阈值 = 5.0f;

    // ==================== 服务端状态存储 ====================

    /** 假死状态记录：玩家UUID -> 假死开始时间（游戏tick） */
    private static final Map<UUID, Long> 假死开始时间 = new ConcurrentHashMap<>();

    /** 位置未知记录：玩家UUID -> 未知开始时间 */
    private static final Map<UUID, Long> 位置未知开始时间 = new ConcurrentHashMap<>();

    /** 假死时的固定位置 */
    private static final Map<UUID, Vec3> 假死位置 = new ConcurrentHashMap<>();

    /** 空血假死标记（区分空血假死和距离假死） */
    private static final Set<UUID> 空血假死标记 = ConcurrentHashMap.newKeySet();

    // ==================== 致命伤害标记 ====================

    /**
     * 致命伤害标记
     * 用于在Mixin 之间传递"当前是致命伤害"的信息
     *
     * 流程：
     * 1. EmptyHealthImmunityMixin.hurt() 检测到致命伤害 → 标记
     * 2. EmptyHealthStateMixin.isDeadOrDying() 检查标记 → 不拦截
     * 3. EmptyHealthDeathMixin.die() 允许死亡 → 清除标记
     */
    private static final Set<UUID> 致命伤害标记 = ConcurrentHashMap.newKeySet();

    // ==================== 客户端状态 ====================

    /** 客户端：本地假死状态（由同步包更新） */
    @OnlyIn(Dist.CLIENT)
    private static boolean 客户端假死状态 = false;

    private 假死状态处理器() {}

    // ==================== 致命伤害标记 API ====================

    /**
     * 标记玩家正在受到致命伤害
     * 由 EmptyHealthImmunityMixin 调用
     */
    public static void 标记致命伤害(UUID playerUUID) {
        致命伤害标记.add(playerUUID);
        LOGGER.debug("玩家 {} 标记为致命伤害中", playerUUID);
    }

    /**
     * 检查玩家是否正在受到致命伤害
     * 由 EmptyHealthStateMixin 和 EmptyHealthDeathMixin 调用
     */
    public static boolean 是致命伤害中(UUID playerUUID) {
        return 致命伤害标记.contains(playerUUID);
    }

    /**
     * 清除致命伤害标记
     * 由 EmptyHealthDeathMixin 在允许死亡后调用
     */
    public static void 清除致命伤害标记(UUID playerUUID) {
        致命伤害标记.remove(playerUUID);
        LOGGER.debug("玩家 {} 清除致命伤害标记", playerUUID);
    }

    // ==================== 客户端 API ====================

    @OnlyIn(Dist.CLIENT)
    public static void 设置客户端假死状态(boolean 假死) {
        客户端假死状态 = 假死;LOGGER.debug("客户端{}假死状态", 假死 ? "进入" : "退出");
    }

    /**
     * 客户端是否处于假死状态
     * 供客户端限制检查使用（通过假死限制来源）
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean 客户端是否假死中() {
        return 客户端假死状态;
    }

    // ==================== 状态查询 API ====================

    /**
     * 检查玩家是否处于假死状态
     */
    public static boolean 是否假死中(UUID playerUUID) {
        return 假死开始时间.containsKey(playerUUID);
    }

    public static boolean 是否假死中(Player player) {
        return player != null && 是否假死中(player.getUUID());
    }

    /**
     * 是否因空血进入的假死
     */
    public static boolean 是否空血假死(UUID playerUUID) {
        return 空血假死标记.contains(playerUUID);
    }

    public static boolean 是否空血假死(Player player) {
        return player != null && 是否空血假死(player.getUUID());
    }

    /**
     * 获取假死剩余秒数
     */
    public static int 获取假死剩余秒数(ServerPlayer player) {
        Long startTime = 假死开始时间.get(player.getUUID());
        if (startTime == null) return -1;

        long elapsed = player.level().getGameTime() - startTime;
        long remaining = 假死超时时间 - elapsed;
        return (int) Math.max(0, remaining / 20);
    }

    // ==================== 核心逻辑：空血假死 ====================

    /**
     * 因空血进入假死（立即触发，由Mixin调用）
     *
     * 与距离假死的区别：
     * - 立即触发，不经过每秒检查
     * - 退出时需要额外检查血量恢复
     * - 会增加污浊度
     */
    public static void 因空血进入假死(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();

        // 标记为空血假死
        空血假死标记.add(playerUUID);

        // 增加污浊度
        污浊度管理器.空血假死惩罚(player);

        if (!是否假死中(playerUUID)) {
            进入假死状态(player, currentTime, true);
        } else {
            LOGGER.debug("玩家 {} 已在假死中，追加空血标记", player.getName().getString());
        }
    }

    // ==================== 核心逻辑：距离假死 ====================

    /**
     * 更新假死状态（统一入口，由距离效果处理器每秒调用）
     *
     * @param player 玩家
     * @param 距离应该假死 当前距离是否满足假死条件
     */
    public static void 更新假死状态(ServerPlayer player, boolean 距离应该假死) {
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();
        boolean 当前假死 = 是否假死中(playerUUID);

        // 计算综合的"应该假死"状态
        boolean 空血中 = 空血假死标记.contains(playerUUID) && player.getHealth() < 血量恢复阈值;
        boolean 应该假死 = 距离应该假死 || 空血中;

        if (应该假死&& !当前假死) {
            // 进入假死
            进入假死状态(player, currentTime, false);
        } else if (!应该假死 && 当前假死) {
            // 所有条件都不满足，可以退出
            退出假死状态(player);
        } else if (当前假死) {
            // 保持假死，处理超时和空血恢复检查
            处理假死中(player, currentTime);
        }
    }

    // ==================== 内部逻辑 ====================

    /**
     * 处理假死中的状态（超时检查、位置未知检查、空血恢复检查）
     */
    private static void 处理假死中(ServerPlayer player, long currentTime) {
        UUID playerUUID = player.getUUID();
        long startTime = 假死开始时间.getOrDefault(playerUUID, currentTime);

        // 检查超时
        if (currentTime - startTime > 假死超时时间) {
            触发超时死亡(player);
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);宝石登记信息 info = worldData.获取登记信息(playerUUID).orElse(null);

        // 检查是否位置未知
        var result = 灵魂宝石距离计算.计算(player, info, server);
        if (result.原因() == 灵魂宝石距离计算.失败原因.位置未知) {
            处理位置未知(player, currentTime);
        } else {
            位置未知开始时间.remove(playerUUID);
        }

        // 检查空血恢复
        if (空血假死标记.contains(playerUUID) && player.getHealth() >= 血量恢复阈值) {
            LOGGER.debug("玩家 {} 血量恢复至{}，移除空血标记",player.getName().getString(), player.getHealth());
            空血假死标记.remove(playerUUID);
        }
    }

    /**
     * 处理位置未知的情况
     */
    private static void 处理位置未知(ServerPlayer player, long currentTime) {
        UUID playerUUID = player.getUUID();

        if (!位置未知开始时间.containsKey(playerUUID)) {
            位置未知开始时间.put(playerUUID, currentTime);
            LOGGER.debug("玩家 {} 灵魂宝石位置未知，开始计时", player.getName().getString());
        }

        long unknownStart = 位置未知开始时间.get(playerUUID);

        if (currentTime - unknownStart >宝石重生成延迟) {
            LOGGER.info("玩家 {} 灵魂宝石位置未知超时，重新生成", player.getName().getString());
            boolean success = 灵魂宝石管理器.重新生成灵魂宝石(player);
            if (success) {
                位置未知开始时间.remove(playerUUID);
            }
        }
    }

    // ==================== 进入/退出假死 ====================

    /**
     * 进入假死状态
     */
    private static void 进入假死状态(ServerPlayer player, long currentTime, boolean 是空血触发) {
        UUID playerUUID = player.getUUID();

        假死开始时间.put(playerUUID, currentTime);
        假死位置.put(playerUUID, player.position());

        // 立即停止移动
        player.setDeltaMovement(Vec3.ZERO);

        //禁用飞行
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        // 同步到客户端
        网络工具.发送给玩家(player, new 假死状态同步包(true));

        LOGGER.info("玩家 {} 进入假死状态（{}）",
                player.getName().getString(), 是空血触发 ? "空血" : "距离");

        String messageKey = 是空血触发? "message.puellamagi.feign_death.enter_empty_health"
                : "message.puellamagi.feign_death.enter";

        player.displayClientMessage(
                Component.translatable(messageKey).withStyle(ChatFormatting.RED),
                false
        );
    }

    /**
     * 退出假死状态
     */
    private static void 退出假死状态(ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        清除玩家状态(playerUUID);

        // 同步到客户端
        网络工具.发送给玩家(player, new 假死状态同步包(false));

        LOGGER.info("玩家 {} 退出假死状态", player.getName().getString());

        player.displayClientMessage(
                Component.translatable("message.puellamagi.feign_death.exit").withStyle(ChatFormatting.GREEN),
                false
        );
    }

    /**
     * 强制退出假死状态（创造模式切换、解除契约等）
     */
    public static void 强制退出(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        if (!是否假死中(playerUUID)) return;

        清除玩家状态(playerUUID);
        网络工具.发送给玩家(player, new 假死状态同步包(false));

        LOGGER.info("玩家 {} 强制退出假死状态", player.getName().getString());
    }

    /**
     * 尝试恢复（灵魂宝石重新发放后调用）
     */
    public static void 尝试恢复(ServerPlayer player) {
        if (!是否假死中(player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        宝石登记信息 info = worldData.获取登记信息(player.getUUID()).orElse(null);

        var result = 灵魂宝石距离计算.计算(player, info, server);

        boolean 空血中 = 空血假死标记.contains(player.getUUID())
                && player.getHealth() < 血量恢复阈值;

        if (!result.应该假死() && !空血中) {
            退出假死状态(player);
        }
    }

    /**
     * 触发超时死亡
     */
    private static void 触发超时死亡(ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        清除玩家状态(playerUUID);
        网络工具.发送给玩家(player, new 假死状态同步包(false));

        LOGGER.warn("玩家 {} 假死超时，判定死亡", player.getName().getString());

        player.displayClientMessage(
                Component.translatable("message.puellamagi.feign_death.timeout")
                        .withStyle(ChatFormatting.DARK_RED),
                false
        );

        player.kill();
    }

    // ==================== Tick处理 ====================

    /**
     * 每tick调用 - 维持假死状态
     * 注意：行动限制由Mixin通过行动限制管理器处理
     * 这里只处理物理状态（速度归零、禁飞）
     */
    public static void onPlayerTick(ServerPlayer player) {
        if (!是否假死中(player)) return;

        player.setDeltaMovement(Vec3.ZERO);

        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    // ==================== 生命周期 ====================

    /**
     *玩家登录时同步假死状态
     */
    public static void onPlayerLogin(ServerPlayer player) {
        boolean 是否假死 = 是否假死中(player);
        网络工具.发送给玩家(player, new 假死状态同步包(是否假死));

        if (是否假死) {
            LOGGER.info("玩家 {} 登录时处于假死状态，已同步", player.getName().getString());
        }
    }

    public static void onPlayerLogout(UUID playerUUID) {
        // 登出不清除假死状态（重连后仍然假死）
    }

    // ==================== 清理 ====================

    /**
     * 清除指定玩家的所有假死相关状态
     */
    public static void 清除玩家状态(UUID playerUUID) {
        假死开始时间.remove(playerUUID);
        位置未知开始时间.remove(playerUUID);
        假死位置.remove(playerUUID);
        空血假死标记.remove(playerUUID);
        致命伤害标记.remove(playerUUID);
    }

    public static void clearAll() {
        假死开始时间.clear();
        位置未知开始时间.clear();
        假死位置.clear();
        空血假死标记.clear();
        致命伤害标记.clear();
    }
}
