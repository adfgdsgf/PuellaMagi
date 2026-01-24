// 文件路径: src/main/java/com/v2t/puellamagi/api/restriction/I限制来源.java

package com.v2t.puellamagi.api.restriction;

import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * 限制来源接口
 *
 * 任何能对玩家施加行动限制的系统都应实现此接口
 * 并注册到 行动限制管理器
 *
 * 示例实现：
 * - 假死状态 → 限制所有行动
 * - 灵魂出窍 → 限制交互，允许移动
 * - 沉默效果 → 只限制释放技能
 */
public interface I限制来源 {

    /**
     * 获取对指定玩家当前施加的限制
     *
     * @param player 目标玩家
     * @return 当前施加的限制类型集合，无限制时返回空集合
     */
    Set<限制类型> 获取限制(Player player);

    /**
     * 获取限制来源的标识名（用于调试日志）
     */
    String 获取来源名称();
}
