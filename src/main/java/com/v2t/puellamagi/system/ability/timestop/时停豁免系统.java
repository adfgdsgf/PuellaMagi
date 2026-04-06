// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/时停豁免系统.java

package com.v2t.puellamagi.system.ability.timestop;

import com.v2t.puellamagi.api.timestop.时停;
import com.v2t.puellamagi.api.timestop.时停豁免级别;
import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.client.客户端队伍缓存;
import com.v2t.puellamagi.core.config.时停配置;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍数据;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 时停豁免系统
 *
 * 统一管理所有时停豁免判断逻辑
 * 按优先级检查各种豁免条件
 *
 * 觉醒系统使用自定义队伍（非原版Team）：
 * - 服务端：通过队伍管理器查询
 * - 客户端：通过客户端队伍缓存查询
 * -觉醒受目标玩家个人配置 timestopAwakening 控制
 */
public final class 时停豁免系统 {
    private 时停豁免系统() {}

    //时间操控能力ID
    private static final ResourceLocation 时停能力ID = 资源工具.本mod("time_control");

    // 调试开关（测试完后改为false）
    private static final boolean 调试模式 = false;

    // 防止刷屏
    private static long 上次调试时间 = 0;
    private static final long 调试间隔毫秒 = 1000;

    /**
     * 获取实体的时停豁免级别
     */
    public static 时停豁免级别 获取豁免级别(Entity entity) {
        if (entity == null) return 时停豁免级别.无豁免;

        Level level = entity.level();
        时停 时停 = (时停) level;

        // 没有活跃时停 → 所有实体正常行动，返回完全豁免使调用方跳过冻结逻辑
        if (!时停.puellamagi$hasActiveTimeStop()) {
            return 时停豁免级别.完全豁免;
        }

        // 1. 时停者→ 完全豁免
        if (时停.puellamagi$isTimeStopper(entity)) {
            return 时停豁免级别.完全豁免;
        }

        // 2. 创造模式 → 完全豁免
        if (entity instanceof Player player && player.isCreative()) {
            return 时停豁免级别.完全豁免;
        }

        // 3. 观察者模式 → 完全豁免
        if (entity.isSpectator()) {
            return 时停豁免级别.完全豁免;
        }

        // 4. 被觉醒 → 完全豁免
        if (检查觉醒条件(entity, 时停)) {
            return 时停豁免级别.完全豁免;
        }

        // 5. 拥有时停能力 → 视觉豁免
        if (entity instanceof Player player && 拥有时停能力(player)) {
            return 时停豁免级别.视觉豁免;
        }

        // 6. 其他 → 无豁免
        return 时停豁免级别.无豁免;
    }

    /**
     * 检查实体是否应该被冻结（tick层面）
     */
    public static boolean 应该冻结(Entity entity) {
        return 获取豁免级别(entity).需要冻结();
    }

    /**
     * 检查实体是否应该画面冻结（渲染层面）
     */
    public static boolean 应该冻结画面(Entity entity) {
        return 获取豁免级别(entity).需要冻结画面();
    }

    // ==================== 内部检查方法 ====================

    /**
     * 检查觉醒条件
     *
     * 觉醒前置条件：
     * 1. 觉醒功能已启用
     * 2. 实体是玩家（如果配置要求仅玩家）
     * 3. 目标玩家的个人配置 timestopAwakening 为 true
     */
    private static boolean 检查觉醒条件(Entity entity, 时停 时停) {
        boolean shouldDebug = 调试模式 && entity instanceof Player && 可以输出调试();

        if (shouldDebug) {
            调试消息("§e=== 觉醒检查 ===");
            调试消息("§7实体: " + entity.getName().getString() + ", 客户端: " + entity.level().isClientSide);
        }

        if (!时停配置.觉醒启用()) {
            if (shouldDebug) 调试消息("§c觉醒未启用");
            return false;
        }

        int awakeningRange = 时停配置.获取觉醒范围();
        if (awakeningRange <= 0) {
            if (shouldDebug) 调试消息("§c觉醒范围<=0");
            return false;
        }

        if (时停配置.觉醒仅玩家() && !(entity instanceof Player)) {
            if (shouldDebug) 调试消息("§c仅玩家但不是玩家");
            return false;
        }

        double rangeSquared = (double) awakeningRange * awakeningRange;
        boolean requireTeammate = 时停配置.觉醒需要队友();

        Level level = entity.level();

        if (level.isClientSide) {
            return 检查觉醒条件_客户端(entity, 时停, rangeSquared, requireTeammate, shouldDebug);
        } else {
            return 检查觉醒条件_服务端(entity, 时停, rangeSquared, requireTeammate, shouldDebug);
        }
    }

    /**
     * 检查目标玩家是否允许被觉醒（个人配置）
     *
     * @param targetPlayer 被觉醒的目标玩家
     * @return true = 允许被觉醒
     */
    private static boolean 检查觉醒个人配置(Player targetPlayer) {
        if (targetPlayer.level().isClientSide) {
            // 客户端：从缓存中获取自己的配置
            // 客户端队伍缓存中存储的是本机玩家所在队伍的完整数据
            队伍数据 team = 客户端队伍缓存.获取队伍();
            if (team == null) {
                // 没有队伍，不需要队友也能觉醒时返回true
                return true;
            }
            return team.获取成员(targetPlayer.getUUID())
                    .map(member -> {
                        var data = (com.v2t.puellamagi.system.team.队伍成员数据) member;
                        return data.获取配置().获取配置("timestopAwakening");
                    })
                    .orElse(true);
        } else {
            // 服务端：通过队伍管理器查询
            MinecraftServer server = targetPlayer.getServer();
            if (server == null) return true;

            // 没有队伍时，默认允许觉醒（不需要队友时生效）
            if (!队伍管理器.玩家有队伍(server, targetPlayer.getUUID())) {
                return true;
            }

            return 队伍管理器.获取个人配置(server, targetPlayer.getUUID(), "timestopAwakening");
        }
    }

    /**
     * 服务端觉醒检查
     */
    private static boolean 检查觉醒条件_服务端(Entity entity, 时停 时停, double rangeSquared, boolean requireTeammate, boolean shouldDebug) {
        List<LivingEntity> stoppers = 时停.puellamagi$getTimeStoppers();

        if (shouldDebug) {
            调试消息("§7[服务端] 时停者: " + stoppers.size());
        }

        for (LivingEntity stopper : stoppers) {
            if (检查单个时停者(entity, stopper, rangeSquared, requireTeammate, shouldDebug)) {
                return true;
            }
        }

        if (shouldDebug) 调试消息("§c觉醒失败");
        return false;
    }

    /**
     * 客户端觉醒检查
     */
    private static boolean 检查觉醒条件_客户端(Entity entity, 时停 时停,
                                               double rangeSquared, boolean requireTeammate, boolean shouldDebug) {
        Level level = entity.level();
        int stopperCount = 0;

        // 遍历所有玩家，检查谁是时停者
        for (Player player : level.players()) {
            if (时停.puellamagi$isTimeStopper(player)) {
                stopperCount++;
                if (检查单个时停者(entity, player, rangeSquared, requireTeammate, shouldDebug)) {
                    return true;
                }
            }
        }

        if (shouldDebug) {
            调试消息("§7[客户端] 时停者: " + stopperCount);
            调试消息("§c觉醒失败");
        }
        return false;
    }

    /**
     * 检查单个时停者是否能唤醒实体
     */
    private static boolean 检查单个时停者(Entity entity, Entity stopper,double rangeSquared, boolean requireTeammate, boolean shouldDebug) {
        double distSq = entity.distanceToSqr(stopper);
        boolean inRange = distSq <= rangeSquared;
        boolean isTeammate = 是否队友(stopper, entity);

        // 检查时停者的觉醒配置（时停者决定是否唤醒队友）
        if (stopper instanceof Player stopperPlayer) {
            if (!检查觉醒个人配置(stopperPlayer)) {
                if (shouldDebug) 调试消息("§c  时停者 " + stopper.getName().getString() + " 关闭了觉醒");
                return false;
            }
        }

        if (shouldDebug) {
            调试消息("§7  - " + stopper.getName().getString() +": 距离²=" + String.format("%.1f", distSq) +
                    ", 范围内=" + inRange +
                    ", 队友=" + isTeammate);
        }

        if (inRange && (!requireTeammate || isTeammate)) {
            if (shouldDebug) 调试消息("§a觉醒成功！");
            return true;
        }

        return false;
    }

    /**
     * 检查玩家是否拥有时停能力
     */
    private static boolean 拥有时停能力(Player player) {
        return 契约管理器.获取类型(player)
                .map(type -> type.获取固有能力ID())
                .map(id -> id.equals(时停能力ID))
                .orElse(false);
    }

    /**
     * 检查两个实体是否是队友
     *
     * 使用自定义队伍系统替代原版Team：
     * - 服务端：通过队伍管理器查询WorldData
     * - 客户端：通过客户端队伍缓存查询同步数据
     * - 非玩家实体：不视为队友
     */
    public static boolean 是否队友(Entity a, Entity b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;

        //仅玩家之间有队伍关系
        if (!(a instanceof Player) || !(b instanceof Player)) {
            return false;
        }

        Level level = a.level();

        if (level.isClientSide) {
            return 是否队友_客户端(a, b);
        } else {
            return 是否队友_服务端(a, b);
        }
    }

    /**
     * 服务端队友判断
     * 通过队伍管理器查询WorldData
     */
    private static boolean 是否队友_服务端(Entity a, Entity b) {
        if (!(a instanceof ServerPlayer playerA)) return false;

        MinecraftServer server = playerA.getServer();
        if (server == null) return false;

        return 队伍管理器.是否同队(server, a.getUUID(), b.getUUID());
    }

    /**
     * 客户端队友判断
     * 通过客户端队伍缓存检查成员列表
     */
    private static boolean 是否队友_客户端(Entity a, Entity b) {
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null) return false;

        // 两个人都在缓存的队伍中即为队友
        return team.是成员(a.getUUID()) && team.是成员(b.getUUID());
    }

    // ==================== 调试工具 ====================

    private static boolean 可以输出调试() {
        long now = System.currentTimeMillis();
        if (now - 上次调试时间 >= 调试间隔毫秒) {
            上次调试时间 = now;
            return true;
        }
        return false;
    }

    private static void 调试消息(String msg) {
        // 服务端环境下Minecraft类不存在，直接用日志输出
        try {
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal(msg), false);
                }
            } else {
                PuellaMagi.LOGGER.debug("[时停豁免调试] {}", msg);
            }
        } catch (Exception ignored) {
        }
    }
}
