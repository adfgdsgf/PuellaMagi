// 文件路径: src/main/java/com/v2t/puellamagi/system/series/impl/心之种系列.java

package com.v2t.puellamagi.system.series.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.series.I系列;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 心之种系列
 *
 * 特点：
 * - 核心物品：心之种
 * - 成长阶段系统（种→芽→叶→蕾→花）
 * - 每个阶段解锁不同能力/增强
 */
public class 心之种系列 implements I系列 {

    public static final ResourceLocation ID = 资源工具.本mod("heart_seed");
    public static final 心之种系列 INSTANCE = new 心之种系列();

    // 成长阶段名称
    private static final String[]阶段名称键 = {"series.puellamagi.heart_seed.stage.seed",   // 种
            "series.puellamagi.heart_seed.stage.sprout", // 芽
            "series.puellamagi.heart_seed.stage.leaf",   // 叶
            "series.puellamagi.heart_seed.stage.bud",    // 蕾
            "series.puellamagi.heart_seed.stage.flower"  // 花
    };

    //该系列可用的魔法少女类型
    private final List<ResourceLocation> 可用类型 = new ArrayList<>();

    private 心之种系列() {}

    // ==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return Component.translatable("series.puellamagi.heart_seed");
    }

    @Override
    public Component 获取描述() {
        return Component.translatable("series.puellamagi.heart_seed.desc");
    }

    @Override
    public ResourceLocation 获取图标() {
        return 资源工具.纹理("item/heart_seed");
    }

    @Override
    public ResourceLocation 获取核心物品ID() {
        return 资源工具.本mod("heart_seed");
    }

    // ==================== 生命周期 ====================

    @Override
    public void 加入系列时(Player player) {
        PuellaMagi.LOGGER.info("玩家 {} 加入心之种系列", player.getName().getString());
        // TODO: 给予心之种物品
        // TODO: 初始化成长阶段为0（种）
    }

    @Override
    public void 离开系列时(Player player) {
        PuellaMagi.LOGGER.info("玩家 {} 离开心之种系列", player.getName().getString());}

    @Override
    public void 变身时(Player player) {
        // TODO: 显示成长阶段HUD
    }

    @Override
    public void 解除变身时(Player player) {
        // TODO: 隐藏成长阶段HUD
    }

    @Override
    public void tick(Player player) {
        // TODO: 成长进度累积
    }

    // ==================== 成长系统 ====================

    @Override
    public int 获取成长阶段数() {
        return 5; // 种、芽、叶、蕾、花
    }

    @Override
    public Component 获取阶段名称(int stageIndex) {
        if (stageIndex >= 0 && stageIndex < 阶段名称键.length) {
            return Component.translatable(阶段名称键[stageIndex]);
        }
        return Component.literal("???");
    }

    @Override
    public boolean 可以进阶(Player player, int currentStage) {
        // TODO: 检查进阶条件
        return currentStage < 获取成长阶段数() - 1;
    }

    @Override
    public void 进阶(Player player, int fromStage, int toStage) {
        PuellaMagi.LOGGER.info("玩家 {} 从阶段 {} 进阶到 {}",
                player.getName().getString(), fromStage, toStage);
        // TODO: 触发进阶效果、解锁能力
    }

    // ==================== 类型管理 ====================

    @Override
    public List<ResourceLocation> 获取可用类型() {
        return List.copyOf(可用类型);
    }

    /**
     * 添加可用类型（内部使用）
     */
    public void 添加可用类型(ResourceLocation typeId) {
        if (!可用类型.contains(typeId)) {
            可用类型.add(typeId);
        }
    }
}
