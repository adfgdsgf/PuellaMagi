// 文件路径: src/main/java/com/v2t/puellamagi/api/adaptation/I适应源.java

package com.v2t.puellamagi.api.adaptation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * 适应源接口
 *
 * 定义什么情况触发适应
 *
 * 实现类示例：
 * - 空血假死触发（当前）
 * - 主动能力触发（未来）
 * - 被攻击累计触发（未来）
 */
public interface I适应源 {

    /**
     * 适应源唯一标识
     */
    ResourceLocation 获取ID();

    /**
     * 是否应该触发适应
     *
     * @param player 玩家
     * @param damageSource 伤害源（可能为null）
     * @param context 上下文数据（灵活扩展）
     * @return 是否触发
     */
    boolean 应该触发(Player player, DamageSource damageSource, Object context);

    /**
     * 获取触发后使用的效果ID
     *
     * @param player 玩家
     * @param damageSource 伤害源
     * @return 效果ID，null则使用默认
     */
    default ResourceLocation 获取效果ID(Player player, DamageSource damageSource) {
        return null;  // 默认使用伤害免疫效果
    }

    /**
     * 触发后的额外处理
     */
    default void 触发后(Player player, DamageSource damageSource) {}
}
