package com.v2t.puellamagi.system.soulgem.data;

/**
 * 灵魂宝石的存储类型
 *
 * 用于追踪宝石当前在哪种容器中
 */
public enum 存储类型 {
    /**
     * 在玩家背包中（包括主手、副手、护甲槽）
     */
    玩家背包("inventory"),

    /**
     * 作为掉落物在世界中
     */
    掉落物("item_entity"),

    /**
     * 在容器方块中（箱子、漏斗等）
     */
    容器("container"),

    /**
     * 位置未知（初始状态或异常情况）
     */
    未知("unknown");

    private final String serializeName;

    存储类型(String serializeName) {
        this.serializeName = serializeName;
    }

    public String 获取序列化名() {
        return serializeName;
    }
    /**
     * 从序列化名称获取枚举
     */
    public static 存储类型 从序列化名(String name) {
        for (存储类型 type : values()) {
            if (type.serializeName.equals(name)) {
                return type;
            }
        }
        return 未知;
    }
}
