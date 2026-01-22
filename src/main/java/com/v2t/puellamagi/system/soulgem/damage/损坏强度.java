package com.v2t.puellamagi.system.soulgem.damage;

/**
 * 灵魂宝石损坏强度
 *
 * 决定单次损坏的效果：
 * -轻微：概率龟裂
 * - 普通：必定龟裂
 * - 严重：直接销毁（跳过龟裂阶段）
 */
public enum 损坏强度 {轻微(0.3f, "light"),
    普通(1.0f, "normal"),
    严重(999f, "severe");

    private final float 威力值;
    private final String 序列化名;

    损坏强度(float 威力值, String 序列化名) {
        this.威力值 = 威力值;
        this.序列化名 = 序列化名;
    }

    public float 获取威力值() {
        return 威力值;
    }

    public String 获取序列化名() {
        return 序列化名;
    }

    /**
     * 是否直接销毁（跳过龟裂）
     */
    public boolean 直接销毁() {
        return 威力值 >= 2.0f;
    }

    /**
     * 是否必定造成龟裂
     */
    public boolean 必定龟裂() {
        return 威力值 >= 1.0f;
    }

    /**
     * 获取龟裂概率（0.0-1.0）
     */
    public float 获取龟裂概率() {
        if (威力值 >= 1.0f) {
            return 1.0f;
        }
        return Math.min(1.0f, 威力值);
    }

    /**
     * 从序列化名解析
     */
    public static 损坏强度 从序列化名(String name) {
        for (损坏强度 强度 : values()) {
            if (强度.序列化名.equals(name)) {
                return 强度;
            }
        }
        return 普通;
    }
}
