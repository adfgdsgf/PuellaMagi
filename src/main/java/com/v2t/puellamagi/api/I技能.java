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
 * 技能是基于能力派生的具体招式
 * 例：时间停止（时间操控能力）、布置丝线（丝线能力）
 */
public interface I技能 {

    // ==================== 技能类型枚举 ====================

    /**
     * 技能按键类型
     */
    enum 按键类型 {瞬发,        // 按下即释放
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
     * 召唤类技能的消耗模式
     */
    enum 召唤消耗模式 {
        一次性,      // 召唤时消耗一次
        持续消耗     // 存在期间持续消耗
    }

    /**
     * 污浊度消耗时机
     */
    enum 消耗时机 {
        无,          // 不消耗污浊度
        释放时,      // 释放/开启时一次性消耗
        持续_每秒,   // 开启/引导期间每秒消耗
        持续_每tick, // 开启/引导期间每tick消耗（高消耗）
        蓄力完成时,  // 蓄力完成释放时消耗
        按蓄力比例,  // 根据蓄力时间比例消耗
        每段连击// 连击每段消耗
    }

    // ==================== 基础信息 ====================

    /**
     * 获取技能唯一标识
     */
    ResourceLocation 获取ID();

    /**
     * 获取技能显示名称（已本地化）
     */
    Component 获取名称();

    /**
     * 获取技能描述（已本地化）
     */
    default Component 获取描述() {
        return Component.empty();
    }

    /**
     * 获取技能图标资源路径
     */
    ResourceLocation 获取图标();

    /**
     * 获取技能按键类型
     */
    按键类型 获取按键类型();

    /**
     * 获取冷却时间（tick）
     *20tick = 1秒
     */
    int 获取冷却时间();

    // ==================== 污浊度消耗 ====================

    /**
     * 获取污浊度消耗时机
     * 默认：释放时消耗
     */
    default 消耗时机 获取消耗时机() {
        return 消耗时机.释放时;
    }

    /**
     * 获取激活时污浊度消耗量
     * 技能激活/释放时一次性消耗
     * 默认：0（不消耗）
     */
    default float 获取激活消耗() {
        return 0f;
    }

    /**
     * 获取持续污浊度消耗量
     * 用于：持续_每秒、持续_每tick
     * 默认：0（不消耗）
     */
    default float 获取持续消耗() {
        return 0f;
    }

    /**
     * 根据蓄力时间计算污浊度消耗
     * 用于：按蓄力比例
     * 默认实现：激活消耗 * 蓄力比例
     *
     * @param 蓄力时间 已蓄力的tick数
     * @return 消耗量
     */
    default float 计算蓄力消耗(int 蓄力时间) {
        int 最大时间 = 获取最大蓄力时间();
        if (最大时间 <= 0) return 获取激活消耗();
        float 比例 = Math.min(1.0f, (float) 蓄力时间 / 最大时间);
        return 获取激活消耗() * 比例;
    }

    /**
     * 检查污浊度是否足够使用技能
     * 默认实现：不限制
     *
     * @param player 玩家
     * @return 是否可以使用
     */
    default boolean 污浊度足够(Player player) {
        return true;
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
     * 瞬发类：直接执行
     * 切换类：切换开关状态
     * 充能类：消耗一层充能并执行
     * 仅在服务端调用
     */
    void 执行(Player player, Level level);

    // ==================== 按键相关方法 ====================

    /**
     * 按键按下时调用
     * -蓄力类：开始蓄力
     * - 引导类：开始引导
     * - 连击类：记录按键
     * - 瞄准类：进入瞄准模式
     * - 蓄力切换类：已激活时关闭，否则开始蓄力
     */
    default void 按下时(Player player, Level level) {}

    /**
     * 按键松开时调用
     * - 蓄力类：根据蓄力时间释放
     * - 引导类：停止引导
     * - 蓄力切换类：保护期内松开则进入正常激活状态
     * @param 已按住时间 按住的tick数
     */
    default void 松开时(Player player, Level level, int 已按住时间) {}

    /**
     * 按住期间每tick调用
     * - 引导类：持续效果
     * - 蓄力类：蓄力中效果（如粒子）
     * @param 已按住时间 已经按住的tick数
     */
    default void 按住tick(Player player, Level level, int 已按住时间) {}

    // ====================蓄力类专用 ====================

    /**
     * 获取最大蓄力时间（tick）
     * 超过此时间自动释放或保持满蓄力
     */
    default int 获取最大蓄力时间() {
        return 40; // 默认2秒
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
     * @param 蓄力时间 已蓄力的tick数
     * @return 效果倍率，1.0为基础
     */
    default float 计算蓄力倍率(int 蓄力时间) {
        int 最大时间 = 获取最大蓄力时间();
        if (最大时间 <= 0) return 1.0f;
        return 1.0f + (float) Math.min(蓄力时间, 最大时间) / 最大时间;
    }

    /**
     * 蓄力满后是否自动释放
     * 默认true，蓄满自动执行
     */
    default boolean 蓄满自动释放() {
        return true;
    }

    /**
     * 蓄力被取消时调用（未蓄满就松开）
     */
    default void 蓄力取消时(Player player, Level level) {}

    // ==================== 切换类专用 ====================

    /**
     * 切换类技能是否处于开启状态
     */
    default boolean 是否开启(Player player) {
        return false;
    }

    /**
     * 切换类技能开启期间每tick调用
     */
    default void 开启tick(Player player, Level level) {}

    /**
     * 切换类技能开启时调用
     */
    default void 开启时(Player player, Level level) {}

    /**
     * 切换类技能关闭时调用
     */
    default void 关闭时(Player player, Level level) {}

    // ==================== 蓄力切换类专用 ====================

    /**
     * 【蓄力切换类】蓄力完成后自动激活时调用
     * 进入保护期之前调用
     */
    default void 蓄力完成激活时(Player player, Level level) {}

    /**
     * 【蓄力切换类】是否处于保护期
     * 保护期内按键不响应，等待玩家松手
     */
    default boolean 是否保护期中(Player player) {
        return false;
    }

    /**
     * 【蓄力切换类】进入保护期
     */
    default void 进入保护期(Player player) {}

    /**
     * 【蓄力切换类】退出保护期（玩家松手后）
     * 进入正常激活状态
     */
    default void 退出保护期(Player player) {}

    /**
     * 【蓄力切换类】获取蓄力语音列表
     * 返回null或空列表表示无语音
     */
    @Nullable
    default ResourceLocation[] 获取蓄力语音列表() {
        return null;
    }

    /**
     * 【蓄力切换类】获取语音保护冷却（毫秒）
     * 防止快速按键导致语音鬼畜
     */
    default long 获取语音保护冷却() {
        return 500; // 默认500毫秒
    }

    // ==================== 引导类专用 ====================

    /**
     * 获取最大引导时间（tick）
     * -1 表示无限制
     */
    default int 获取最大引导时间() {
        return -1;
    }

    /**
     * 引导被打断时调用（如受到攻击）
     */
    default void 引导打断时(Player player, Level level) {}

    // ==================== 召唤类专用 ====================

    /**
     * 获取召唤消耗模式
     */
    default 召唤消耗模式 获取召唤消耗模式() {
        return 召唤消耗模式.一次性;
    }

    /**
     * 获取最大召唤数量
     * 同时存在的召唤物上限
     */
    default int 获取最大召唤数量() {
        return 1;
    }

    /**
     * 获取召唤物持续时间（tick）
     * -1 表示永久（直到被摧毁或手动取消）
     */
    default int 获取召唤持续时间() {
        return -1;
    }

    // ==================== 连击类专用 ====================

    /**
     * 获取连击段数
     */
    default int 获取连击段数() {
        return 3;
    }

    /**
     * 获取连击窗口时间（tick）
     *在此时间内再次按下算作连击
     */
    default int 获取连击窗口时间() {
        return 20; // 1秒
    }

    /**
     * 执行连击某一段
     * @param 当前段数 从1开始
     */
    default void 执行连击(Player player, Level level, int 当前段数) {}

    // ==================== 充能类专用 ====================

    /**
     * 获取最大充能层数
     */
    default int 获取最大充能层数() {
        return 3;
    }

    /**
     * 获取当前充能层数
     */
    default int 获取当前充能层数(Player player) {
        return 0;
    }

    /**
     * 获取充能恢复时间（tick）
     *恢复一层充能所需时间
     */
    default int 获取充能恢复时间() {
        return 100; // 5秒
    }

    /**
     * 消耗一层充能
     * @return 是否成功消耗
     */
    default boolean 消耗充能(Player player) {
        return false;
    }

    // ==================== 被动类专用 ====================

    /**
     * 被动技能的触发条件检查
     * 每tick调用，返回true则触发被动效果
     */
    default boolean 检查被动触发条件(Player player) {
        return false;
    }

    /**
     * 被动技能触发时调用
     */
    default void 被动触发时(Player player, Level level) {}

    /**
     * 被动技能每tick调用（无论是否触发）
     *用于持续性被动效果
     */
    default void 被动tick(Player player, Level level) {}

    // ==================== 组合类专用 ====================

    /**
     * 获取按键组合序列
     * 例：{"DOWN", "DOWN_RIGHT", "RIGHT", "ATTACK"}
     */
    default String[] 获取按键组合() {
        return new String[0];
    }

    /**
     * 获取组合输入窗口时间（tick）
     * 必须在此时间内完成整个组合
     */
    default int 获取组合窗口时间() {
        return 40; // 2秒
    }

    /**
     * 检查按键组合是否匹配
     * @param 输入序列 玩家的输入序列
     */
    default boolean 检查组合匹配(String[] 输入序列) {
        String[] 目标组合 = 获取按键组合();
        if (输入序列.length< 目标组合.length) return false;

        // 检查最后N个输入是否匹配
        int offset = 输入序列.length - 目标组合.length;
        for (int i = 0; i < 目标组合.length; i++) {
            if (!目标组合[i].equals(输入序列[offset + i])) {
                return false;
            }
        }
        return true;
    }

    // ====================瞄准类专用 ====================

    /**
     * 是否处于瞄准模式
     */
    default boolean 是否瞄准中(Player player) {
        return false;
    }

    /**
     * 进入瞄准模式
     */
    default void 进入瞄准模式(Player player) {}

    /**
     * 退出瞄准模式
     */
    default void 退出瞄准模式(Player player) {}

    /**
     * 瞄准模式每tick调用
     * 用于更新瞄准指示器
     */
    default void 瞄准tick(Player player, Level level) {}

    /**
     * 确认瞄准位置并释放技能
     * @param 目标位置 瞄准的位置，可为null表示取消
     */
    default void 确认瞄准(Player player, Level level, @Nullable Vec3 目标位置) {}

    /**
     * 获取最大瞄准距离（方块数）
     */
    default double 获取最大瞄准距离() {
        return 32.0;
    }

    /**
     * 是否需要瞄准实体（而非位置）
     */
    default boolean 是否瞄准实体() {
        return false;
    }
}
