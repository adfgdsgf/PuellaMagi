// 文件路径: src/main/java/com/v2t/puellamagi/api/timestop/时停豁免级别.java

package com.v2t.puellamagi.api.timestop;

/**
 * 时停豁免级别
 *
 * 决定实体在时停中的状态
 */
public enum 时停豁免级别 {
    /**
     * 无豁免 - 完全冻结
     * 不能动 + 画面冻结（看不见时停中发生的事）
     */
    无豁免,

    /**
     * 视觉豁免 - 能看不能动
     * 不能动 + 画面正常（能看见时停中发生的事）
     * 适用于：拥有时停能力但未激活的人
     */
    视觉豁免,

    /**
     * 完全豁免 - 能动能看
     * 正常行动 + 画面正常
     * 适用于：时停者、创造模式、被觉醒的实体
     */
    完全豁免;

    /**
     * 是否可以行动
     */
    public boolean 可以行动() {
        return this == 完全豁免;
    }

    /**
     * 是否可以看见（画面不冻结）
     */
    public boolean 可以看见() {
        return this == 完全豁免 || this == 视觉豁免;
    }

    /**
     * 是否需要冻结实体tick
     */
    public boolean 需要冻结() {
        return this != 完全豁免;
    }

    /**
     * 是否需要冻结画面
     */
    public boolean 需要冻结画面() {
        return this == 无豁免;
    }
}
