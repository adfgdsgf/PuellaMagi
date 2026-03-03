// 文件路径: src/main/java/com/v2t/puellamagi/api/team/I队伍成员.java

package com.v2t.puellamagi.api.team;

import java.util.UUID;

/**
 * 队伍成员接口
 *
 * 定义成员的基本信息和个人配置访问
 */
public interface I队伍成员 {

    /**
     * 获取玩家UUID
     */
    UUID 获取UUID();

    /**
     * 获取加入时间（游戏tick）
     */
    long 获取加入时间();

    /**
     * 获取职位ID字符串
     * 用于外部系统查询，不依赖具体枚举类型
     */
    String 获取职位ID();

    /**
     * 获取个人配置
     */
    I队伍个人配置 获取个人配置();

    /**
     * 个人配置接口
     *
     * 每个配置项由成员独立控制
     * 影响的是"自己对队友的行为"，而非"队友对自己的行为"
     */
    interface I队伍个人配置 {

        /**
         * 自己攻击队友是否造成伤害
         * true = 自己打队友有伤害
         */
        boolean 友伤开启();

        /**
         * 是否被时停中的队友唤醒
         * true = 会被唤醒
         */
        boolean 时停觉醒();

        /**
         * 锁定系统是否跳过队友（预备）
         * true = 锁定时跳过队友
         */
        boolean 锁定跳过队友();

        /**
         * 是否允许队友搜身（预备）
         * true = 允许队友搜身自己
         */
        boolean 允许队友搜身();
    }
}
