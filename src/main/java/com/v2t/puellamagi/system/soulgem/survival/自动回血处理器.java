// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/survival/自动回血处理器.java

package com.v2t.puellamagi.system.soulgem.survival;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自动回血处理器
 *
 * 职责：
 * -灵魂宝石系魔法少女持续缓慢回血
 * - 独立于假死系统，不管是否假死都生效
 * - 无视饱食度，直接恢复生命值
 * - 空血假死时回血速度降低（惩罚机制）
 */
public final class 自动回血处理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/AutoHeal");

    //==================== 配置常量 ====================

    /** 回血间隔（tick） */
    // TODO: 改为配置项soulgem.autoheal.interval
    private static final int 回血间隔 = 40;  // 2秒

    /** 每次回血量*/
    // TODO: 改为配置项 soulgem.autoheal.amount
    private static final float 回血量 = 1.0f;  // 0.5颗心

    /** 空血假死时的回血倍率（惩罚：恢复变慢） */
    // TODO: 改为配置项 soulgem.autoheal.emptyHealthMultiplier
    private static final float 空血回血倍率 = 1.0f / 3.0f;  // 1/3速度

    private 自动回血处理器() {}

    // ==================== 主处理方法 ====================

    /**
     * 每tick调用，处理自动回血
     */
    public static void onPlayerTick(ServerPlayer player) {
        // 只处理灵魂宝石系
        if (!能力工具.是灵魂宝石系(player)) return;

        // 创造模式不需要回血
        if (能力工具.应该跳过限制(player)) return;

        // 按间隔回血
        if (player.tickCount % 回血间隔 != 0) return;

        // 已满血不回
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth >= maxHealth) return;

        // TODO: 检查灵魂宝石是否完整
        // if (灵魂宝石损坏处理器.是否已销毁(player)) return;

        // 计算回血量
        float healAmount = 回血量;

        // 空血假死时恢复变慢（惩罚机制）
        if (能力工具.是否空血假死(player)) {
            healAmount *= 空血回血倍率;
        }

        // 执行回血
        float newHealth = Math.min(currentHealth + healAmount, maxHealth);
        player.setHealth(newHealth);

        // 调试日志（仅空血时输出，避免刷屏）
        if (currentHealth <= 0) {
            LOGGER.debug("玩家 {} 自动回血: {} → {}",
                    player.getName().getString(), currentHealth, newHealth);
        }
    }
}
