// 文件路径: src/main/java/com/v2t/puellamagi/api/team/I队伍.java

package com.v2t.puellamagi.api.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 队伍接口
 *
 * 定义队伍的基本行为
 */
public interface I队伍 {

    /**
     * 获取队伍唯一ID
     */
    UUID 获取队伍ID();

    /**
     * 获取队长UUID
     */
    UUID 获取队长UUID();

    /**
     * 设置队长
     */
    void 设置队长(UUID playerUUID);

    /**
     * 获取所有成员UUID（包括队长）
     */
    List<UUID> 获取所有成员UUID();

    /**
     * 获取成员数据
     */
    Optional<I队伍成员> 获取成员(UUID playerUUID);

    /**
     * 是否为队伍成员
     */
    boolean 是成员(UUID playerUUID);

    /**
     * 是否为队长
     */
    boolean 是队长(UUID playerUUID);

    /**
     * 获取成员数量
     */
    int 获取成员数量();

    /**
     * 是否为空队伍（无成员）
     */
    boolean 是否为空();
}
