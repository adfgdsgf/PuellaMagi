// 文件路径: src/main/java/com/v2t/puellamagi/system/series/impl/灵魂宝石系列.java

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
 * 灵魂宝石系列
 *
 * 特点：
 * - 核心物品：灵魂宝石
 * - 污浊度系统（类似反向魔力）
 * - 空血不死（灵魂宝石完整时）
 * - 魔女化（污浊度满时）
 * - 无成长阶段（或只有1个阶段）
 */
public class 灵魂宝石系列 implements I系列 {

    public static final ResourceLocation ID = 资源工具.本mod("soul_gem");
    public static final 灵魂宝石系列 INSTANCE = new 灵魂宝石系列();

    //该系列可用的魔法少女类型
    private final List<ResourceLocation> 可用类型 = new ArrayList<>();

    private 灵魂宝石系列() {}

    // ==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return Component.translatable("series.puellamagi.soul_gem");
    }

    @Override
    public Component 获取描述() {
        return Component.translatable("series.puellamagi.soul_gem.desc");
    }

    @Override
    public ResourceLocation 获取图标() {
        return 资源工具.纹理("item/soul_gem");
    }

    @Override
    public ResourceLocation 获取核心物品ID() {
        return 资源工具.本mod("soul_gem");
    }

    // ==================== 生命周期 ====================

    @Override
    public void 加入系列时(Player player) {
        PuellaMagi.LOGGER.info("玩家 {} 加入灵魂宝石系列", player.getName().getString());
        // TODO: 给予灵魂宝石物品
        // TODO: 初始化污浊度
    }

    @Override
    public void 离开系列时(Player player) {
        PuellaMagi.LOGGER.info("玩家 {} 离开灵魂宝石系列", player.getName().getString());
        // TODO: 清理污浊度数据
    }

    @Override
    public void 变身时(Player player) {
        // TODO: 显示污浊度HUD
    }

    @Override
    public void 解除变身时(Player player) {
        // TODO: 隐藏污浊度HUD（或保持显示？）
    }

    @Override
    public void tick(Player player) {
        // TODO: 污浊度自然减少
        // TODO: 距离检测// TODO: 空血状态恢复
    }

    // ==================== 成长系统 ====================

    @Override
    public int 获取成长阶段数() {
        return 1; // 灵魂宝石系无成长阶段
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
