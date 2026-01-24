// 文件路径: src/main/java/com/v2t/puellamagi/api/interaction/搜身上下文.java

package com.v2t.puellamagi.api.interaction;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 搜身上下文
 *
 * 记录一次搜身操作的完整信息
 * 在网络包和界面之间传递
 */
public record 搜身上下文(
        UUID 搜身者UUID,
        UUID 被搜身者UUID,
        String 触发来源ID,      // 哪个I可被搜身来源触发的
        long 开始时间           // 游戏tick
) {

    /**
     * 从玩家创建上下文
     */
    public static 搜身上下文 创建(Player searcher, Player target, String sourceId, long gameTime) {
        return new 搜身上下文(
                searcher.getUUID(),
                target.getUUID(),
                sourceId,
                gameTime
        );
    }
}
