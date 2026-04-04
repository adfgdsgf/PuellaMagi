// 文件路径: src/main/java/com/v2t/puellamagi/api/I技能.java

package com.v2t.puellamagi.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 可释放技能的接口
 *
 * 设计：行为驱动 + 枚举模板
 * - 按键类型枚举作为"快捷模板"，提供行为方法的默认实现
 * - 管理器只调用行为声明方法，不使用switch(类型)
 * - 技能可覆写任何行为方法来定制行为
 *
 * 架构层次：
 *   I技能（本接口） → 抽象基类技能 → 具体技能实现
 *
 * @see com.v2t.puellamagi.system.skill.抽象基类技能
 */
public interface I技能 {

    // ==================== 类型枚举（快捷模板） ====================

    /**
     * 技能按键类型
     * 作为快捷模板使用，行为方法的默认实现基于此枚举
     * 技能可以覆写行为方法来脱离模板约束
     */
    enum 按键类型 {
        瞬发,        // 按下即释放
        引导,        // 按住持续释放，松开停止
        蓄力,        // 按住蓄力，松开释放
        切换,        // 按一次开启，再按一次关闭
        蓄力切换,    // 蓄力完成后进入切换状态（如时停）
        召唤,        // 召唤持续存在的实体/效果
        连击,        // 短时间内多次按下触发连击
        充能,        // 有多层充能，每次使用消耗一层
        被动,        // 满足条件自动触发
        组合,        // 需要按特定按键组合释放
        瞄准         // 需要先瞄准再释放
    }

    /**
     * 污浊度消耗时机
     */
    enum 消耗时机 {
        无,          // 不消耗污浊度
        释放时,      // 释放/开启时一次性消耗
        持续_每秒,   // 开启/引导期间每秒消耗
        持续_每tick,  // 开启/引导期间每tick消耗（高消耗）
        蓄力完成时,  // 蓄力完成释放时消耗
        按蓄力比例,  // 根据蓄力时间比例消耗
        每段连击     // 连击每段消耗
    }

    /**
     * 召唤类技能的消耗模式
     */
    enum 召唤消耗模式 {
        一次性,      // 召唤时消耗一次
        持续消耗     // 存在期间持续消耗
    }

    // ==================== 基础信息 ====================

    /** 获取技能唯一标识 */
    ResourceLocation 获取ID();

    /** 获取技能显示名称（已本地化） */
    Component 获取名称();

    /** 获取技能描述（已本地化） */
    default Component 获取描述() {
        return Component.empty();
    }

    /** 获取技能图标资源路径 */
    ResourceLocation 获取图标();

    /** 获取技能按键类型（快捷模板） */
    按键类型 获取按键类型();

    /**
     * 获取冷却时间（tick）
     * 20tick = 1秒
     */
    int 获取冷却时间();

    // ==================== 行为声明方法 ====================
    // 管理器通过这些方法了解技能的行为，不再用switch(类型)
    // 默认实现基于按键类型枚举，技能可覆写来定制行为

    /**
     * 是否响应按键按下事件
     * 返回false的技能在按下时不做任何处理
     * 默认：被动类不响应
     */
    default boolean 响应按键按下() {
        return 获取按键类型() != 按键类型.被动;
    }

    /**
     * 是否需要按键状态追踪
     * 返回true的技能在按下后进入持续追踪，每tick更新
     * 默认：蓄力、引导、蓄力切换需要追踪
     */
    default boolean 需要按键追踪() {
        return switch (获取按键类型()) {
            case 蓄力, 引导, 蓄力切换 -> true;
            default -> false;
        };
    }

    /**
     * 是否支持开关状态
     * 返回true的技能有"开启/关闭"概念
     * 默认：切换、蓄力切换支持
     */
    default boolean 支持开关状态() {
        return switch (获取按键类型()) {
            case 切换, 蓄力切换 -> true;
            default -> false;
        };
    }

    /**
     * 是否支持蓄力
     * 返回true的技能在按键追踪期间执行蓄力逻辑
     * 默认：蓄力、蓄力切换支持
     */
    default boolean 支持蓄力() {
        return switch (获取按键类型()) {
            case 蓄力, 蓄力切换 -> true;
            default -> false;
        };
    }

    /**
     * 是否支持充能
     * 返回true的技能在按下时检查并消耗充能层数
     * 默认：充能类支持
     */
    default boolean 支持充能() {
        return 获取按键类型() == 按键类型.充能;
    }

    /**
     * 是否需要tick驱动
     * 返回true的技能在开启/激活期间每tick调用开启tick()
     * 默认：切换、蓄力切换需要
     */
    default boolean 需要开启tick() {
        return 支持开关状态();
    }

    /**
     * 是否支持修饰键操作
     * 返回true的技能在修饰键+技能键按下时调用修饰键按下时()
     * 默认：切换、蓄力切换支持
     */
    default boolean 支持修饰键() {
        return 支持开关状态();
    }

    // ==================== 条件判断 ====================

    /**
     * 判断技能是否可以使用
     * 检查前置条件（变身状态、冷却、资源等）
     */
    boolean 可以使用(Player player);

    // ==================== 通用执行方法 ====================

    /**
     * 执行技能效果
     * 仅在服务端调用
     */
    void 执行(Player player, Level level);

    // ==================== 按键生命周期 ====================

    /**
     * 按键按下时调用
     */
    default void 按下时(Player player, Level level) {}

    /**
     * 按键松开时调用
     * @param 已按住时间 按住的tick数
     */
    default void 松开时(Player player, Level level, int 已按住时间) {}

    /**
     * 按住期间每tick调用
     * @param 已按住时间 已经按住的tick数
     */
    default void 按住tick(Player player, Level level, int 已按住时间) {}

    /**
     * 修饰键+技能键按下时调用
     */
    default void 修饰键按下时(Player player, Level level) {}

    // ==================== 开关状态 ====================

    /**
     * 是否处于开启状态
     * 仅 支持开关状态()=true 的技能使用
     */
    default boolean 是否开启(Player player) {
        return false;
    }

    /** 开启时调用 */
    default void 开启时(Player player, Level level) {}

    /** 关闭时调用 */
    default void 关闭时(Player player, Level level) {}

    /** 开启期间每tick调用 */
    default void 开启tick(Player player, Level level) {}

    // ==================== 蓄力相关 ====================

    /**
     * 获取最大蓄力时间（tick）
     */
    default int 获取最大蓄力时间() {
        return 40;
    }

    /**
     * 获取实际蓄力时间（考虑玩家状态，如创造模式）
     * @param player 玩家
     * @return 实际蓄力时间，0表示瞬发
     */
    default int 获取实际蓄力时间(Player player) {
        return 获取最大蓄力时间();
    }

    /**
     * 获取最小蓄力时间（tick）
     * 未达到此时间松开则取消技能
     */
    default int 获取最小蓄力时间() {
        return 0;
    }

    /**
     * 根据蓄力时间计算效果倍率
     */
    default float 计算蓄力倍率(int 蓄力时间) {
        int 最大时间 = 获取最大蓄力时间();
        if (最大时间 <= 0) return 1.0f;
        return 1.0f + (float) Math.min(蓄力时间, 最大时间) / 最大时间;
    }

    /**
     * 蓄力满后是否自动释放
     */
    default boolean 蓄满自动释放() {
        return true;
    }

    /** 蓄力被取消时调用 */
    default void 蓄力取消时(Player player, Level level) {}

    // ==================== 蓄力切换专用 ====================

    /**
     * 蓄力完成后自动激活时调用
     * 进入保护期之前调用
     */
    default void 蓄力完成激活时(Player player, Level level) {}

    /** 是否处于保护期 */
    default boolean 是否保护期中(Player player) {
        return false;
    }

    /** 进入保护期 */
    default void 进入保护期(Player player) {}

    /** 退出保护期 */
    default void 退出保护期(Player player) {}

    /**
     * 获取蓄力语音列表
     * 返回null或空列表表示无语音
     */
    @Nullable
    default ResourceLocation[] 获取蓄力语音列表() {
        return null;
    }

    /**
     * 获取语音保护冷却（毫秒）
     */
    default long 获取语音保护冷却() {
        return 500;
    }

    // ==================== 引导相关 ====================

    /**
     * 获取最大引导时间（tick）
     * -1 表示无限制
     */
    default int 获取最大引导时间() {
        return -1;
    }

    /** 引导被打断时调用 */
    default void 引导打断时(Player player, Level level) {}

    // ==================== 污浊度消耗 ====================

    /**
     * 获取污浊度消耗时机
     */
    default 消耗时机 获取消耗时机() {
        return 消耗时机.释放时;
    }

    /**
     * 获取激活时污浊度消耗量
     */
    default float 获取激活消耗() {
        return 0f;
    }

    /**
     * 获取持续污浊度消耗量
     */
    default float 获取持续消耗() {
        return 0f;
    }

    /**
     * 根据蓄力时间计算污浊度消耗
     */
    default float 计算蓄力消耗(int 蓄力时间) {
        int 最大时间 = 获取最大蓄力时间();
        if (最大时间 <= 0) return 获取激活消耗();
        float 比例 = Math.min(1.0f, (float) 蓄力时间 / 最大时间);
        return 获取激活消耗() * 比例;
    }

    /**
     * 检查污浊度是否足够使用技能
     */
    default boolean 污浊度足够(Player player) {
        return true;
    }

    // ==================== 充能相关 ====================

    /** 获取最大充能层数 */
    default int 获取最大充能层数() {
        return 3;
    }

    /** 获取当前充能层数 */
    default int 获取当前充能层数(Player player) {
        return 0;
    }

    /** 获取充能恢复时间（tick） */
    default int 获取充能恢复时间() {
        return 100;
    }

    /**
     * 消耗一层充能
     * @return 是否成功消耗
     */
    default boolean 消耗充能(Player player) {
        return false;
    }

    // ==================== 被动相关 ====================

    /** 被动技能触发条件检查 */
    default boolean 检查被动触发条件(Player player) {
        return false;
    }

    /** 被动技能触发时调用 */
    default void 被动触发时(Player player, Level level) {}

    /** 被动技能每tick调用 */
    default void 被动tick(Player player, Level level) {}

    // ==================== 召唤相关 ====================

    /** 获取召唤消耗模式 */
    default 召唤消耗模式 获取召唤消耗模式() {
        return 召唤消耗模式.一次性;
    }

    /** 获取最大召唤数量 */
    default int 获取最大召唤数量() {
        return 1;
    }

    /** 获取召唤物持续时间（tick），-1表示永久 */
    default int 获取召唤持续时间() {
        return -1;
    }

    // ==================== 连击相关 ====================

    /** 获取连击段数 */
    default int 获取连击段数() {
        return 3;
    }

    /** 获取连击窗口时间（tick） */
    default int 获取连击窗口时间() {
        return 20;
    }

    /**
     * 执行连击某一段
     * @param 当前段数 从1开始
     */
    default void 执行连击(Player player, Level level, int 当前段数) {}

    // ==================== 组合相关 ====================

    /** 获取按键组合序列 */
    default String[] 获取按键组合() {
        return new String[0];
    }

    /** 获取组合输入窗口时间（tick） */
    default int 获取组合窗口时间() {
        return 40;
    }

    /**
     * 检查按键组合是否匹配
     * @param 输入序列 玩家的输入序列
     */
    default boolean 检查组合匹配(String[] 输入序列) {
        String[] 目标组合 = 获取按键组合();
        if (输入序列.length < 目标组合.length) return false;

        int offset = 输入序列.length - 目标组合.length;
        for (int i = 0; i < 目标组合.length; i++) {
            if (!目标组合[i].equals(输入序列[offset + i])) {
                return false;
            }
        }
        return true;
    }

    // ==================== 瞄准相关 ====================

    /** 是否处于瞄准模式 */
    default boolean 是否瞄准中(Player player) {
        return false;
    }

    /** 进入瞄准模式 */
    default void 进入瞄准模式(Player player) {}

    /** 退出瞄准模式 */
    default void 退出瞄准模式(Player player) {}

    /** 瞄准模式每tick调用 */
    default void 瞄准tick(Player player, Level level) {}

    /**
     * 确认瞄准位置并释放技能
     * @param 目标位置 瞄准的位置，可为null表示取消
     */
    default void 确认瞄准(Player player, Level level, @Nullable Vec3 目标位置) {}

    /** 获取最大瞄准距离（方块数） */
    default double 获取最大瞄准距离() {
        return 32.0;
    }

    /** 是否需要瞄准实体（而非位置） */
    default boolean 是否瞄准实体() {
        return false;
    }
}
