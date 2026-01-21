// 文件路径: src/main/java/com/v2t/puellamagi/api/series/I系列.java

package com.v2t.puellamagi.api.series;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 魔法少女系列接口
 *
 * 每个系列有独特的机制：
 * - 灵魂宝石系：污浊度、空血不死、魔女化
 * - 心之种系：成长阶段（种→芽→叶→蕾→花）
 */
public interface I系列 {

    // ==================== 基础信息 ====================

    /**
     * 获取系列ID
     *如: puellamagi:soul_gem, puellamagi:heart_seed
     */
    ResourceLocation 获取ID();

    /**
     * 获取系列显示名称
     */
    Component 获取名称();

    /**
     * 获取系列描述
     */
    Component 获取描述();

    /**
     * 获取系列图标（用于UI显示）
     */
    ResourceLocation 获取图标();

    // ==================== 核心物品 ====================

    /**
     * 获取核心物品ID
     * 如: puellamagi:soul_gem, puellamagi:heart_seed
     */
    ResourceLocation 获取核心物品ID();

    // ==================== 生命周期 ====================

    /**
     * 玩家加入此系列时调用（契约时）
     */
    void 加入系列时(Player player);

    /**
     * 玩家离开此系列时调用（如果允许的话）
     */
    void 离开系列时(Player player);

    /**
     * 变身时调用
     */
    void 变身时(Player player);

    /**
     * 解除变身时调用
     */
    void 解除变身时(Player player);

    /**
     * 每tick调用（变身状态下）
     */
    void tick(Player player);

    // ==================== 成长系统 ====================

    /**
     * 获取成长阶段数量
     *灵魂宝石系可能返回1（无成长）
     * 心之种系返回5（种芽叶蕾花）
     */
    default int 获取成长阶段数() {
        return 1;
    }

    /**
     * 获取阶段名称
     */
    default Component 获取阶段名称(int stageIndex) {
        return Component.empty();
    }

    /**
     * 检查是否可以进阶
     */
    default boolean 可以进阶(Player player, int currentStage) {
        return false;
    }

    /**
     * 执行进阶
     */
    default void 进阶(Player player, int fromStage, int toStage) {
    }

    // ==================== 系列特有数据 ====================

    /**
     * 获取该系列可用的魔法少女类型ID列表
     */
    List<ResourceLocation> 获取可用类型();
}
