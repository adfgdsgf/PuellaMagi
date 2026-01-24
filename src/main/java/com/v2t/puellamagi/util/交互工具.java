// 文件路径: src/main/java/com/v2t/puellamagi/util/交互工具.java

package com.v2t.puellamagi.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

/**
 * 交互工具
 *
 * 封装玩家交互距离相关的计算
 * 自适应读取玩家属性，不硬编码数值
 */
public final class 交互工具 {

    private 交互工具() {}

    /**
     * 获取玩家的实体交互距离
     *
     * @param player 玩家
     * @return 交互距离（方块）
     */
    public static double 获取实体交互距离(Player player) {
        return player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
    }

    /**
     * 获取玩家的方块交互距离
     *
     * @param player 玩家
     * @return 交互距离（方块）
     */
    public static double 获取方块交互距离(Player player) {
        return player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
    }

    /**
     * 检查玩家是否在实体交互范围内
     *
     * @param player 玩家
     * @param target 目标实体
     * @return 是否在范围内
     */
    public static boolean 在实体交互范围内(Player player, Entity target) {
        if (target == null) return false;
        double 距离 = player.distanceTo(target);
        double 交互距离 = 获取实体交互距离(player);
        return 距离 <= 交互距离;
    }

    /**
     * 检查玩家是否在指定范围内（用于需要自定义范围的场景）
     *
     * @param player 玩家
     * @param target 目标实体
     * @param 额外距离 在基础交互距离上增加的额外距离
     * @return 是否在范围内
     */
    public static boolean 在扩展范围内(Player player, Entity target, double 额外距离) {
        if (target == null) return false;
        double 距离 = player.distanceTo(target);
        double 交互距离 = 获取实体交互距离(player) + 额外距离;
        return 距离 <= 交互距离;
    }
}
