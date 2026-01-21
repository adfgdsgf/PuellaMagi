// 文件路径: src/main/java/com/v2t/puellamagi/util/能力工具.java

package com.v2t.puellamagi.util;

import com.v2t.puellamagi.api.I可变身;
import com.v2t.puellamagi.api.contract.I契约;
import com.v2t.puellamagi.api.soulgem.I污浊度;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import com.v2t.puellamagi.system.contract.契约能力;
import com.v2t.puellamagi.system.skill.技能能力;
import com.v2t.puellamagi.system.soulgem.污浊度能力;
import com.v2t.puellamagi.system.transformation.变身能力;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;

/**
 * Capability获取工具
 * 统一封装获取逻辑，避免到处写getCapability
 */
public final class 能力工具 {
    private 能力工具() {}

    // ==================== 通用后门 ====================

    /**
     * 检查玩家是否应该跳过各种限制（创造模式后门）
     *统一入口，避免到处写 isCreative()
     *
     * 跳过的限制包括：
     * - 技能CD
     * - 污浊度消耗
     * - 蓄力时间（瞬发）
     * - 其他资源消耗
     */
    public static boolean 应该跳过限制(Player player) {
        return player != null && player.isCreative();
    }

    // ==================== 变身能力 ====================

    /**
     * 获取玩家的变身能力
     */
    public static Optional<I可变身> 获取变身能力(Player player) {
        if (player == null) return Optional.empty();
        LazyOptional<I可变身> cap = player.getCapability(ModCapabilities.变身能力);
        return cap.resolve();
    }

    /**
     * 获取变身能力（完整类型，用于需要额外方法时）
     */
    public static Optional<变身能力> 获取变身能力完整(Player player) {
        Optional<I可变身> opt = 获取变身能力(player);
        if (opt.isPresent() && opt.get() instanceof 变身能力 full) {
            return Optional.of(full);
        }
        return Optional.empty();
    }

    /**
     * 便捷方法：判断玩家是否已变身
     */
    public static boolean 是否已变身(Player player) {
        return 获取变身能力(player)
                .map(I可变身::是否已变身)
                .orElse(false);
    }

    // ==================== 技能能力 ====================

    /**
     * 获取玩家的技能能力
     */
    public static Optional<技能能力> 获取技能能力(Player player) {
        if (player == null) return Optional.empty();
        LazyOptional<技能能力> cap = player.getCapability(ModCapabilities.技能能力);
        return cap.resolve();
    }

    /**
     * 便捷方法：检查技能是否在冷却中
     */
    public static boolean 技能是否冷却中(Player player, net.minecraft.resources.ResourceLocation skillId) {
        return 获取技能能力(player)
                .map(cap -> cap.是否冷却中(skillId))
                .orElse(false);
    }

    /**
     * 便捷方法：获取技能剩余冷却
     */
    public static int 获取技能剩余冷却(Player player, net.minecraft.resources.ResourceLocation skillId) {
        return 获取技能能力(player)
                .map(cap -> cap.获取剩余冷却(skillId))
                .orElse(0);
    }

    // ==================== 契约能力 ====================

    /**
     * 获取玩家的契约能力
     */
    public static Optional<I契约> 获取契约能力(Player player) {
        if (player == null) return Optional.empty();
        LazyOptional<I契约> cap = player.getCapability(ModCapabilities.契约能力);
        return cap.resolve();
    }

    /**
     * 获取契约能力（完整类型）
     */
    public static Optional<契约能力> 获取契约能力完整(Player player) {
        Optional<I契约> opt = 获取契约能力(player);
        if (opt.isPresent() && opt.get() instanceof 契约能力 full) {
            return Optional.of(full);
        }
        return Optional.empty();
    }

    /**
     * 便捷方法：判断玩家是否已契约
     */
    public static boolean 是否已契约(Player player) {
        return 获取契约能力(player)
                .map(I契约::是否已契约)
                .orElse(false);
    }

    // ==================== 污浊度能力 ====================

    /**
     * 获取玩家的污浊度能力
     */
    public static Optional<I污浊度> 获取污浊度能力(Player player) {
        if (player == null) return Optional.empty();
        LazyOptional<I污浊度> cap = player.getCapability(ModCapabilities.污浊度能力);
        return cap.resolve();
    }

    /**
     * 获取污浊度能力（完整类型）
     */
    public static Optional<污浊度能力> 获取污浊度能力完整(Player player) {
        Optional<I污浊度> opt = 获取污浊度能力(player);
        if (opt.isPresent() && opt.get() instanceof 污浊度能力 full) {
            return Optional.of(full);
        }
        return Optional.empty();
    }

    /**
     * 便捷方法：获取污浊度百分比
     */
    public static float 获取污浊度百分比(Player player) {
        return 获取污浊度能力(player)
                .map(I污浊度::获取百分比)
                .orElse(0f);
    }

    /**
     * 便捷方法：污浊度是否已满
     */
    public static boolean 污浊度是否已满(Player player) {
        return 获取污浊度能力(player)
                .map(I污浊度::是否已满)
                .orElse(false);
    }
}
