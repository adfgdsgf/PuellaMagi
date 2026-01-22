// 文件路径: src/main/java/com/v2t/puellamagi/api/adaptation/I适应效果.java

package com.v2t.puellamagi.api.adaptation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * 适应效果接口
 *
 * 定义适应产生的效果行为
 *
 * 设计原则：
 * - 效果实例无状态（可单例）
 * - 状态存储在适应数据中
 * - 通过数据判断效果是否生效
 *
 * 扩展方向：
 * - 伤害免疫（当前）
 * - 环境适应（水下呼吸、岩浆游泳）
 * - 攻击适应（护甲穿透递增）
 */
public interface I适应效果 {

    /**
     * 效果唯一标识
     */
    ResourceLocation 获取ID();

    /**
     * 效果显示名称
     */
    default String 获取名称() {
        return 获取ID().toString();
    }

    // ==================== 伤害相关 ====================

    /**
     * 是否免疫该伤害
     *
     * @param player 玩家
     * @param source 伤害源
     * @param 免疫结束时间 该效果的免疫结束时间
     * @param 当前时间 当前游戏时间
     * @return 是否免疫
     */
    default boolean 是否免疫伤害(Player player, DamageSource source, long 免疫结束时间, long 当前时间) {
        return false;
    }

    // ==================== 生命周期 ====================

    /**
     * 效果激活时
     */
    default void 激活时(Player player) {}

    /**
     * 效果结束时
     */
    default void 结束时(Player player) {}

    /**
     * 每tick调用（用于持续效果，如水下加速）
     */
    default void tick(Player player, long 剩余时间) {}

    // ==================== 时长计算 ====================

    /**
     * 计算免疫时长
     *
     * @param连续触发次数 短时间内连续触发的次数
     * @return 免疫时长（tick）
     */
    long 计算免疫时长(int 连续触发次数);

    /**
     * 获取连续触发判定时间（tick）
     *
     * 在此时间内再次触发同类型伤害，视为连续触发，免疫时长递增
     */
    default long 获取连续判定时间() {
        return 20 * 30;  // 默认30秒
    }
}
