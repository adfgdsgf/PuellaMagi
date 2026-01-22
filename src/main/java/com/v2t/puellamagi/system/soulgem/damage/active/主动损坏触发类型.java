package com.v2t.puellamagi.system.soulgem.damage.active;

/**
 * 主动损坏触发类型
 *
 * 区分不同的客户端交互来源
 */
public enum 主动损坏触发类型 {
    /**
     * 左键（攻击掉落物）
     */
    左键攻击("left_click"),

    /**
     * 右键（主副手组合、撞击方块）
     */
    右键交互("right_click");

    private final String id;

    主动损坏触发类型(String id) {
        this.id = id;
    }

    public String 获取ID() {
        return id;
    }
}
