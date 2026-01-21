// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/时停豁免系统.java

package com.v2t.puellamagi.system.ability.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.api.timestop.时停豁免级别;
import com.v2t.puellamagi.core.config.时停配置;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        TimeStop timeStop = (TimeStop) level;

        // 没有时停，不需要豁免判断
        if (!timeStop.puellamagi$hasActiveTimeStop()) {
            return 时停豁免级别.完全豁免;
        }

        // 1. 时停者→ 完全豁免
        if (timeStop.puellamagi$isTimeStopper(entity)) {
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
        if (检查觉醒条件(entity, timeStop)) {
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

    //==================== 内部检查方法 ====================

    /**
     * 检查觉醒条件
     */
    private static boolean 检查觉醒条件(Entity entity, TimeStop timeStop) {
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
            return 检查觉醒条件_客户端(entity, timeStop, rangeSquared, requireTeammate, shouldDebug);
        } else {
            return 检查觉醒条件_服务端(entity, timeStop, rangeSquared, requireTeammate, shouldDebug);
        }
    }

    /**
     * 服务端觉醒检查
     */
    private static boolean 检查觉醒条件_服务端(Entity entity, TimeStop timeStop,
                                               double rangeSquared, boolean requireTeammate, boolean shouldDebug) {
        List<LivingEntity> stoppers = timeStop.puellamagi$getTimeStoppers();

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
    private static boolean 检查觉醒条件_客户端(Entity entity, TimeStop timeStop,
                                               double rangeSquared, boolean requireTeammate, boolean shouldDebug) {
        Level level = entity.level();
        int stopperCount = 0;

        // 遍历所有玩家，检查谁是时停者
        for (Player player : level.players()) {
            if (timeStop.puellamagi$isTimeStopper(player)) {
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
     */
    public static boolean 是否队友(Entity a, Entity b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;

        if (a.getTeam() != null && a.getTeam().equals(b.getTeam())) {
            return true;
        }

        return false;
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
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(msg), false);
            }
        } catch (Exception ignored) {
        }
    }
}
