package com.v2t.puellamagi.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 魔法少女的固有能力接口
 *
 * 能力是魔法少女与生俱来/契约获得的超能力本质
 * 例：时间操控、丝线支配、治愈之力
 *
 * 能力与技能的区别：
 * - 能力：固有超能力，变身即拥有
 * - 技能：基于能力派生的可释放招式
 */
public interface I能力 {

    /**
     * 获取能力唯一标识
     * 例：puellamagi:time_stop
     */
    ResourceLocation 获取ID();

    /**
     * 获取能力显示名称（已本地化）
     */
    Component 获取名称();

    /**
     * 获取能力描述（已本地化）
     */
    Component 获取描述();

    /**
     * 能力激活时调用（玩家变身时）
     * 用于初始化能力状态
     */
    void 激活时(Player player);

    /**
     * 能力失效时调用（玩家解除变身时）
     * 用于清理能力效果、重置状态
     */
    void 失效时(Player player);

    /**
     * 每游戏刻调用（变身期间）
     * 用于持续性效果处理
     */
    void tick(Player player);

    /**
     * 获取该能力派生的技能列表
     * 这些技能会在变身时加入玩家可用技能池
     */
    List<I技能> 获取派生技能();

    /**
     * 保存能力专属数据到NBT
     * 用于持久化能力状态（如时停剩余时间、丝线位置等）
     */
    CompoundTag 保存数据();

    /**
     * 从NBT加载能力专属数据
     */
    void 加载数据(CompoundTag tag);

    /**
     * 能力是否处于激活状态
     */
    boolean 是否激活();
}
