// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍职位.java

package com.v2t.puellamagi.system.team;

import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 队伍职位枚举
 *
 * 每个职位定义：
 * - 显示名称（翻译键）
 * - 拥有的权限集合
 * - 显示优先级（排序用，数值越小越靠前）
 *
 * 扩展方式：新增枚举值，定义权限集合即可
 * UI层通过 有权限() 方法判断按钮是否显示，无需硬编码职位类型
 */
public enum 队伍职位 {

    队长("leader", 0,
            EnumSet.of(队伍权限.邀请成员, 队伍权限.踢出成员, 队伍权限.转移队长, 队伍权限.解散队伍)),

    队员("member", 10,
            EnumSet.of(队伍权限.邀请成员));

    private final String id;
    private final int 显示优先级;
    private final Set<队伍权限> 权限集合;

    队伍职位(String id, int displayPriority, EnumSet<队伍权限> permissions) {
        this.id = id;
        this.显示优先级 = displayPriority;
        this.权限集合 = permissions;
    }

    public String 获取ID() {
        return id;
    }

    public int 获取显示优先级() {
        return 显示优先级;
    }

    /**
     * 判断该职位是否拥有指定权限
     */
    public boolean 有权限(队伍权限 permission) {
        return 权限集合.contains(permission);
    }

    /**
     * 获取职位显示名称（本地化）
     */
    public Component 获取显示名称() {
        return Component.translatable("gui.puellamagi.team.role." + id);
    }

    /**
     * 获取翻译键
     */
    public String 获取翻译键() {
        return "gui.puellamagi.team.role." + id;
    }

    /**
     * 根据ID查找职位
     *
     * @param id 职位ID字符串
     * @return 对应职位，找不到返回队员
     */
    public static 队伍职位 从ID获取(String id) {
        for (队伍职位 role : values()) {
            if (role.id.equals(id)) {
                return role;
            }
        }
        return 队员;
    }
}
