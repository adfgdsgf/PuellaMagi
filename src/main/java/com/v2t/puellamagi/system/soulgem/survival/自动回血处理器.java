// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/survival/自动回血处理器.java

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

        // 计算回血量
        double healAmount = 灵魂宝石配置.获取回血量();

        // 空血假死时恢复变慢（惩罚机制）
        // 注意：配置中是"倍率"，例如2.0表示双倍速度
        // 但假死时应该是减速，所以取倒数
        if (能力工具.是否空血假死(player)) {
            double 空血倍率 = 灵魂宝石配置.获取空血回血倍率();
            // 倍率>1表示加速，<1表示减速
            // 这里假死时应该减速，所以用1/倍率
            // 但配置描述是"空血时倍率"，2.0=双倍恢复
            // 根据原代码逻辑，假死时是1/3速度，所以配置值应该是0.33
            // 修正：配置值直接就是倍率，不需要取倒数
            healAmount *= 空血倍率;
        }

        // 执行回血
        float newHealth = Math.min(currentHealth + (float) healAmount, maxHealth);
        player.setHealth(newHealth);

        // 调试日志（仅空血时输出，避免刷屏）
        if (currentHealth <= 0) {
            LOGGER.debug("玩家 {} 自动回血: {} → {}",
                    player.getName().getString(), currentHealth, newHealth);
        }
    }
}
