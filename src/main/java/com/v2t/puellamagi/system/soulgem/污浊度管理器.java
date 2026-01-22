// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/污浊度管理器.java

package com.v2t.puellamagi.system.soulgem;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.soulgem.I污浊度;
import com.v2t.puellamagi.core.network.packets.s2c.污浊度同步包;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.series.impl.灵魂宝石系列;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.soulgem.effect.距离效果处理器;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 污浊度管理器
 *
 * 职责：所有污浊度变化的业务逻辑
 */
public final class 污浊度管理器 {
    private 污浊度管理器() {}

    private static final float 自然恢复量 = 0.2f;
    private static final float 睡眠恢复量 = 30f;
    private static final float 空血假死惩罚量 = 2f;

    public static boolean 是否灵魂宝石系玩家(Player player) {
        return 契约管理器.获取契约(player)
                .map(contract -> {
                    ResourceLocation seriesId = contract.获取系列ID();
                    return 灵魂宝石系列.ID.equals(seriesId);
                })
                .orElse(false);
    }

    public static Optional<I污浊度> 获取污浊度(Player player) {
        return 能力工具.获取污浊度能力(player);
    }

    public static float 获取当前值(Player player) {
        return 获取污浊度(player).map(I污浊度::获取当前值).orElse(0f);
    }

    public static float 获取百分比(Player player) {
        return 获取污浊度(player).map(I污浊度::获取百分比).orElse(0f);
    }

    public static boolean 是否已满(Player player) {
        return 获取污浊度(player).map(I污浊度::是否已满).orElse(false);
    }

    public static boolean 增加(ServerPlayer player, float amount, boolean sync) {
        if (player == null || amount <= 0) return false;
        if (!是否灵魂宝石系玩家(player)) return false;

        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            cap.增加污浊度(amount);
            PuellaMagi.LOGGER.debug("玩家 {} 污浊度增加 {}，当前 {}/{}",
                    player.getName().getString(), amount, cap.获取当前值(), cap.获取最大值());
            if (sync) {
                同步污浊度(player);
            }
            if (cap.是否已满()) {
                PuellaMagi.LOGGER.warn("玩家 {} 污浊度已满！", player.getName().getString());
            }
        });
        return true;
    }

    public static boolean 增加(ServerPlayer player, float amount) {
        return 增加(player, amount, true);
    }

    public static boolean 增加污浊度(ServerPlayer player, float amount) {
        return 增加(player, amount, true);
    }

    /**
     * 增加污浊度（带原因，用于日志）
     */
    public static boolean 增加污浊度(ServerPlayer player, float amount, String reason) {
        if (player == null || amount <= 0) return false;
        if (!是否灵魂宝石系玩家(player)) return false;

        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            cap.增加污浊度(amount);
            PuellaMagi.LOGGER.debug("玩家 {} 污浊度增加 {}（{}），当前 {}/{}",
                    player.getName().getString(), amount, reason, cap.获取当前值(), cap.获取最大值());
            同步污浊度(player);if (cap.是否已满()) {
                PuellaMagi.LOGGER.warn("玩家 {} 污浊度已满！", player.getName().getString());
            }
        });
        return true;
    }

    public static boolean 减少(ServerPlayer player, float amount, boolean sync) {
        if (player == null || amount <= 0) return false;
        if (!是否灵魂宝石系玩家(player)) return false;

        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            cap.减少污浊度(amount);
            PuellaMagi.LOGGER.debug("玩家 {} 污浊度减少 {}，当前 {}/{}",
                    player.getName().getString(), amount, cap.获取当前值(), cap.获取最大值());
            if (sync) {
                同步污浊度(player);
            }
        });
        return true;
    }

    public static boolean 减少(ServerPlayer player, float amount) {
        return 减少(player, amount, true);
    }

    public static boolean 减少污浊度(ServerPlayer player, float amount) {
        return 减少(player, amount, true);
    }

    public static void 设置(ServerPlayer player, float value) {
        if (player == null) return;
        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            cap.设置当前值(value);
            同步污浊度(player);
        });
    }

    public static void 重置(ServerPlayer player) {
        if (player == null) return;
        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            cap.重置();
            同步污浊度(player);
            PuellaMagi.LOGGER.debug("玩家 {} 污浊度已重置", player.getName().getString());
        });
    }

    /**
     * 自然恢复Tick
     */
    public static void 自然恢复Tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;

        if (!是否灵魂宝石系玩家(player)) return;

        if (技能管理器.是否有持续消耗中(player)) return;

        if (距离效果处理器.是否暂停污浊度恢复(player)) return;

        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            if (cap.是否为空()) return;
            cap.减少污浊度(自然恢复量);
            同步污浊度(player);
        });
    }

    public static void 玩家睡醒(ServerPlayer player) {
        if (!是否灵魂宝石系玩家(player)) return;

        boolean success = 减少(player, 睡眠恢复量);
        if (success) {
            PuellaMagi.LOGGER.debug("玩家 {} 睡觉恢复，污浊度减少 {}",
                    player.getName().getString(), 睡眠恢复量);
        }
    }

    /**
     * 玩家死亡时调用
     *
     * 注：真正的死亡惩罚已移至空血假死触发时
     * 此方法保留用于处理真正死亡（如虚空、/kill）的后续逻辑
     */
    public static void 玩家死亡(Player newPlayer,污浊度能力 newCap) {
        // 真正死亡不增加污浊度（已在空血假死时增加）
        // 此方法保留用于未来可能的其他处理
    }

    /**
     * 空血假死时调用
     */
    public static void 空血假死惩罚(ServerPlayer player) {
        增加污浊度(player, 空血假死惩罚量, "空血假死");
    }

    public static void 同步污浊度(ServerPlayer player) {
        if (player == null) return;
        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            污浊度同步包 packet = new 污浊度同步包(cap.获取当前值(), cap.获取最大值());
            网络工具.发送给玩家(player, packet);
        });
    }
}
