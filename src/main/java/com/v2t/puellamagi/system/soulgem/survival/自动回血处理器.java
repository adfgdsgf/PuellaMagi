//文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/survival/自动回血处理器.java

package com.v2t.puellamagi.system.soulgem.survival;

import com.v2t.puellamagi.core.config.灵魂宝石配置;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自动回血处理器
 *
 * 职责：
 * - 灵魂宝石系魔法少女持续缓慢回血
 * - 独立于假死系统，不管是否假死都生效
 * - 无视饱食度，直接恢复生命值
 * - 空血假死时回血速度降低（惩罚机制）
 *
 * 配置使用百分比，自动适配不同最大血量
 */
public final class 自动回血处理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/AutoHeal");

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
        int 回血间隔 = 灵魂宝石配置.获取回血间隔Tick();
        if (player.tickCount % 回血间隔 != 0) return;

        // 已满血不回
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth >= maxHealth) return;

        // TODO: 检查灵魂宝石是否完整
        // if (灵魂宝石损坏处理器.是否已销毁(player)) return;

        // 使用百分比计算回血量
        double 回血百分比 = 灵魂宝石配置.获取回血百分比();
        float healAmount = (float) (maxHealth * 回血百分比);

        // 空血假死时恢复变慢（惩罚机制）
        if (能力工具.是否空血假死(player)) {
            double 空血倍率 = 灵魂宝石配置.获取空血回血倍率();
            healAmount *= (float) 空血倍率;
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
