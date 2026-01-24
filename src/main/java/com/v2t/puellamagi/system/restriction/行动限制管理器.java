// 文件路径: src/main/java/com/v2t/puellamagi/system/restriction/行动限制管理器.java

package com.v2t.puellamagi.system.restriction;

import com.v2t.puellamagi.api.restriction.I限制来源;
import com.v2t.puellamagi.api.restriction.限制类型;
import com.v2t.puellamagi.system.soulgem.effect.假死限制来源;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 行动限制管理器
 *
 * 职责：
 * - 聚合所有限制来源
 * - 提供统一的限制查询入口
 * - 供Mixin和各系统调用
 */
public final class 行动限制管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/Restriction");

    /** 所有注册的限制来源 */
    private static final List<I限制来源> 限制来源列表 = new ArrayList<>();

    /** 初始化标记 */
    private static boolean 已初始化 = false;

    private 行动限制管理器() {}

    // ==================== 初始化 ====================

    /**
     * 初始化行动限制系统
     * 在模组启动时调用一次
     */
    public static void 初始化() {
        if (已初始化) {LOGGER.warn("行动限制系统已初始化，跳过重复调用");
            return;
        }

        // 注册内置限制来源
        注册来源(new 假死限制来源());
        // 未来：注册来源(new 灵魂视角限制来源());

        已初始化 = true;
        LOGGER.info("行动限制系统初始化完成，已注册 {} 个限制来源", 限制来源列表.size());
    }

    // ==================== 注册 ====================

    /**
     * 注册限制来源
     */
    public static void 注册来源(I限制来源 来源) {
        限制来源列表.add(来源);
        LOGGER.debug("注册限制来源: {}", 来源.获取来源名称());
    }

    // ==================== 查询API ====================

    /**
     * 检查玩家是否被限制某种行动
     */
    public static boolean 是否被限制(Player player, 限制类型 类型) {
        if (player == null) return false;
        if (能力工具.应该跳过限制(player)) return false;

        for (I限制来源 来源 : 限制来源列表) {
            Set<限制类型> 限制集合 = 来源.获取限制(player);
            if (限制集合 != null && 限制集合.contains(类型)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取玩家当前所有被限制的类型
     */
    public static Set<限制类型> 获取所有限制(Player player) {
        if (player == null) return EnumSet.noneOf(限制类型.class);
        if (能力工具.应该跳过限制(player)) return EnumSet.noneOf(限制类型.class);

        Set<限制类型> 结果 = EnumSet.noneOf(限制类型.class);

        for (I限制来源 来源 : 限制来源列表) {
            Set<限制类型> 限制集合 = 来源.获取限制(player);
            if (限制集合 != null) {
                结果.addAll(限制集合);
            }
        }

        return 结果;
    }

    /**
     * 检查玩家是否有任何限制
     */
    public static boolean 有任何限制(Player player) {
        if (player == null) return false;
        if (能力工具.应该跳过限制(player)) return false;

        for (I限制来源 来源 : 限制来源列表) {
            Set<限制类型> 限制集合 = 来源.获取限制(player);
            if (限制集合 != null && !限制集合.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ==================== 便捷方法 ====================

    public static boolean 可以移动(Player player) {
        return !是否被限制(player, 限制类型.移动);
    }

    public static boolean 可以攻击(Player player) {
        return !是否被限制(player, 限制类型.攻击);
    }

    public static boolean 可以释放技能(Player player) {
        return !是否被限制(player, 限制类型.释放技能);
    }

    public static boolean 可以交互(Player player) {
        return !是否被限制(player, 限制类型.交互方块)
                && !是否被限制(player, 限制类型.交互实体);
    }
}
