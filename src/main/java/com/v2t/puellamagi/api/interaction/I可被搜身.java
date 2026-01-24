// 文件路径: src/main/java/com/v2t/puellamagi/api/interaction/I可被搜身.java

package com.v2t.puellamagi.api.interaction;

import net.minecraft.world.entity.player.Player;

/**
 * 可被搜身接口
 *
 * 由各种"限制来源"实现，判断玩家是否处于可被搜身的状态
 *
 * 设计原则：
 * - 不硬编码具体状态类型
 * - 每种限制来源自己决定是否允许搜身、是否需要提示
 * - 搜身管理器遍历所有来源，任一允许即可搜身
 */
public interface I可被搜身 {
    /**
     * 获取限制来源的唯一标识
     * 用于日志和调试
     */
    String 获取来源ID();

    /**
     * 检查目标玩家是否因为此来源而可被搜身
     *
     * @param target 被搜身的目标玩家
     * @param searcher 执行搜身的玩家
     * @return true表示此来源允许搜身
     */
    boolean 可被搜身(Player target, Player searcher);

    /**
     * 是否需要通知被搜身者
     *
     * 例如：
     * - 时停/假死：不需要提示（感知不到）
     * - 其他情况：可能需要提示
     *
     * @param target 被搜身的目标玩家
     * @return true表示需要发送提示
     */
    default boolean 需要提示被搜身者(Player target) {
        return false;
    }

    /**
     * 获取提示消息的本地化键
     * 仅当需要提示被搜身者() 返回true时调用
     *
     * @return 本地化键，如"message.puellamagi.being_searched"
     */
    default String 获取提示消息键() {
        return "message.puellamagi.being_searched";
    }
}
