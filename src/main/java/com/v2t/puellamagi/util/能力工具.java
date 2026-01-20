package com.v2t.puellamagi.util;

import com.v2t.puellamagi.api.I可变身;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import com.v2t.puellamagi.system.skill.技能能力;
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

    //==================== 变身能力 ====================

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

    // TODO: 后续添加
    // public static Optional<契约能力> 获取契约能力(Player player)
    // public static boolean 是否有契约(Player player)
}
