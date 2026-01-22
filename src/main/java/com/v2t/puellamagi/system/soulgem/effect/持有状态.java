// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/effect/持有状态.java

package com.v2t.puellamagi.system.soulgem.effect;

/**
 * 灵魂宝石与所有者的距离状态
 * 影响移速、污浊度恢复等
 */
public enum 持有状态 {

    /**
     * 正常范围（0-5格）
     * 无影响
     */
    正常(0, 5, 0f, false),

    /**
     * 中距离（5-20格）
     * 移速-20%，污浊度恢复暂停
     */
    中距离(5, 20, 0.2f, true),

    /**
     * 远距离（20-50格）
     * 移速-50%，虚弱I，污浊度上升
     */
    远距离(20, 50, 0.5f, true),

    /**
     * 超出范围（50格+或找不到）
     * 假死状态
     */
    超出范围(50, Double.MAX_VALUE, 1.0f, true);

    private final double 最小距离;
    private final double 最大距离;
    private final float 移速减少比例;
    private final boolean 暂停污浊度恢复;

    持有状态(double min, double max, float speedReduction, boolean pauseRecovery) {
        this.最小距离 = min;
        this.最大距离 = max;
        this.移速减少比例 = speedReduction;
        this.暂停污浊度恢复 = pauseRecovery;
    }

    public double 获取最小距离() {
        return 最小距离;
    }

    public double 获取最大距离() {
        return 最大距离;
    }

    public float 获取移速减少比例() {
        return 移速减少比例;
    }

    public boolean 是否暂停污浊度恢复() {
        return 暂停污浊度恢复;
    }

    /**
     * 根据距离获取状态
     * @param distance 距离，-1表示找不到/跨维度
     */
    public static 持有状态 fromDistance(double distance) {
        if (distance < 0) {
            return 超出范围;
        }

        for (持有状态 state : values()) {
            if (distance >= state.最小距离 && distance < state.最大距离) {
                return state;
            }
        }

        return 超出范围;
    }

    /**
     * 是否需要进入假死
     */
    public boolean 需要假死() {
        return this == 超出范围;
    }

    /**
     * 是否应该增加污浊度（远距离时）
     */
    public boolean 应该增加污浊度() {
        return this == 远距离;
    }
}
