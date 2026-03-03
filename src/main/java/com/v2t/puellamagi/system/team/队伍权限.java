// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍权限.java

package com.v2t.puellamagi.system.team;

/**
 * 队伍权限枚举
 *
 * 定义所有可能的队伍操作权限
 * 由队伍职位持有，通过职位判断玩家是否拥有某权限
 *
 * 扩展方式：新增枚举值 → 在对应职位的权限集合中添加
 */
public enum 队伍权限 {

    /**邀请其他玩家加入队伍 */
    邀请成员,

    /** 将成员踢出队伍 */
    踢出成员,

    /** 将队长职位转移给其他成员 */
    转移队长,

    /** 解散整个队伍 */
    解散队伍
}
