// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/投射物状态.java

package com.v2t.puellamagi.system.ability.timestop;

/**
 * 投射物在时停中的状态
 */
public enum 投射物状态 {
    /**
     * 正常状态
     * 不受时停影响，正常运行
     */
    正常,

    /**
     * 惯性滑行
     * 时停中释放的投射物，速度逐渐衰减
     * 可以造成伤害和碰撞
     */
    惯性滑行,

    /**
     * 完全静止
     * 惯性结束后悬停空中
     * 不造成任何伤害和碰撞
     * 可被玩家右键捡起
     */
    完全静止,

    /**
     * 被捡起
     * 玩家交互后等待移除
     */
    被捡起
}
