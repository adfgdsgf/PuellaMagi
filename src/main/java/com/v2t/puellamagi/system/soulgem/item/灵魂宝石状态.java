// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/item/灵魂宝石状态.java

package com.v2t.puellamagi.system.soulgem.item;

/**
 * 灵魂宝石的三种状态
 */
public enum 灵魂宝石状态 {
    NORMAL("normal"),       // 正常
    CRACKED("cracked"),     // 龟裂
    DESTROYED("destroyed"); // 损坏（遗物）

    private final String serializeName;

    灵魂宝石状态(String name) {
        this.serializeName = name;
    }

    public String getSerializeName() {
        return serializeName;
    }

    public static 灵魂宝石状态 fromString(String name) {
        for (灵魂宝石状态 state : values()) {
            if (state.serializeName.equals(name)) {
                return state;
            }
        }
        return NORMAL;
    }

    /**
     * 是否为有效状态（未损坏）
     */
    public boolean 是否有效() {
        return this != DESTROYED;
    }
}
