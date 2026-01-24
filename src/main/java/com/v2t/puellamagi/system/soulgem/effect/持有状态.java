// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/effect/持有状态.java

package com.v2t.puellamagi.system.soulgem.effect;

import com.v2t.puellamagi.core.config.灵魂宝石配置;

/**
 * 灵魂宝石与所有者的距离状态
 * 影响移速、污浊度恢复等
 *
 * 注意：距离阈值和移速倍率从配置文件动态读取
 */
public enum 持有状态 {

    /**
     * 正常范围
     * 无影响
     */
    正常(0),

    /**
     * 中距离
     * 移速降低，污浊度恢复暂停
     */
    中距离(1),

    /**
     * 远距离
     * 移速大幅降低，虚弱，污浊度上升
     */
    远距离(2),

    /**
     * 超出范围（或找不到）
     * 假死状态
     */
    超出范围(3);

    private final int 阶段索引;

    持有状态(int index) {
        this.阶段索引 = index;
    }

    /**
     * 获取该状态的最小距离
     */
    public double 获取最小距离() {
        return switch (this) {
            case 正常 -> 0;
            case 中距离 -> 灵魂宝石配置.获取正常范围();
            case 远距离 -> 灵魂宝石配置.获取中距离范围();
            case 超出范围 -> 灵魂宝石配置.获取远距离范围();
        };
    }

    /**
     * 获取该状态的最大距离
     */
    public double 获取最大距离() {
        return switch (this) {
            case 正常 -> 灵魂宝石配置.获取正常范围();
            case 中距离 -> 灵魂宝石配置.获取中距离范围();
            case 远距离 -> 灵魂宝石配置.获取远距离范围();
            case 超出范围 -> Double.MAX_VALUE;
        };
    }

    /**
     * 获取移速倍率
     * @return 1.0 = 正常，0.8 = 80%速度
     */
    public double 获取移速倍率() {
        return switch (this) {
            case 正常 ->灵魂宝石配置.获取正常移速倍率();
            case 中距离 -> 灵魂宝石配置.获取中距离移速倍率();
            case 远距离 -> 灵魂宝石配置.获取远距离移速倍率();
            case 超出范围 -> 0.0; // 假死状态不能动
        };
    }

    /**
     * 获取移速减少比例（用于属性修改器）
     * @return 0.0 = 不减速，0.2 = 减速20%
     */
    public float 获取移速减少比例() {
        return (float) (1.0 - 获取移速倍率());
    }

    /**
     * 是否暂停污浊度恢复
     */
    public boolean 是否暂停污浊度恢复() {
        return this != 正常;
    }

    /**
     * 根据距离获取状态
     * @param distance 距离，-1表示找不到/跨维度
     */
    public static 持有状态 fromDistance(double distance) {
        if (distance < 0) {
            return 超出范围;
        }

        int 正常上限 = 灵魂宝石配置.获取正常范围();
        int 中距离上限 = 灵魂宝石配置.获取中距离范围();
        int 远距离上限 = 灵魂宝石配置.获取远距离范围();

        if (distance < 正常上限) {
            return 正常;
        } else if (distance < 中距离上限) {
            return 中距离;
        } else if (distance < 远距离上限) {
            return 远距离;
        } else {
            return 超出范围;
        }
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
