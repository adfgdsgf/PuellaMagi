package com.v2t.puellamagi.system.soulgem.damage.active;

import com.v2t.puellamagi.system.soulgem.damage.损坏上下文;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 主动损坏方式接口
 *
 * 定义一种玩家主动触发的灵魂宝石损坏方式
 * 每种方式负责：
 * 1. 声明自己的触发类型
 * 2. 检测条件是否满足
 * 3. 构建损坏上下文
 */
public interface I主动损坏方式 {

    /**
     * 获取方式ID（用于配置/日志）
     */
    ResourceLocation 获取ID();

    /**
     * 获取触发类型
     */
    主动损坏触发类型 获取触发类型();

    /**
     * 是否启用（可配置）
     */
    default boolean 是否启用() {
        return true;
    }

    /**
     * 检测掉落物攻击
     *
     * @param player 攻击者
     * @param target 目标掉落物
     * @return 如果满足条件，返回损坏上下文；否则返回empty
     */
    default Optional<损坏上下文> 检测掉落物攻击(ServerPlayer player, ItemEntity target) {
        return Optional.empty();
    }

    /**
     * 检测主副手组合
     *
     * @param player 玩家
     * @return 如果满足条件，返回损坏上下文；否则返回empty
     */
    default Optional<损坏上下文> 检测主副手组合(ServerPlayer player) {
        return Optional.empty();
    }

    /**
     * 检测方块交互
     *
     * @param player 玩家
     * @param blockPos 交互的方块位置
     * @return 如果满足条件，返回损坏上下文；否则返回empty
     */
    default Optional<损坏上下文> 检测方块交互(ServerPlayer player, BlockPos blockPos) {
        return Optional.empty();
    }

    /**
     * 获取优先级（数字越小越优先）
     * 当多个方式都满足条件时，优先级高的先执行
     */
    default int 获取优先级() {
        return 100;
    }
}
