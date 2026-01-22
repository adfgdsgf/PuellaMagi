package com.v2t.puellamagi.system.soulgem.damage;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 强度判定工具
 *
 * 根据数值/条件判断损坏强度，不硬编码具体来源
 */
public final class 强度判定 {

    // ===阈值常量（可配置化） ===

    /** 被动销毁：高伤害阈值（直接销毁） */
    private static final float 高伤害阈值 = 10.0f;

    /** 被动销毁：中等伤害阈值（普通龟裂） */
    private static final float 中伤害阈值 = 1.0f;

    /** 方块硬度阈值 */
    private static final float 方块硬度_严重 = 50.0f;  // 黑曜石级
    private static final float 方块硬度_普通 = 5.0f;   // 铁块级
    private static final float 方块硬度_轻微 = 2.0f;   // 石头级

    /** 武器伤害阈值 */
    private static final float 武器伤害_严重 = 8.0f;
    private static final float 武器伤害_普通 = 4.0f;

    /** 下落速度阈值 */
    private static final double 下落速度_严重 = 1.0;
    private static final double 下落速度_普通 = 0.5;

    private 强度判定() {}

    //==================== 被动销毁 ====================

    /**
     * 从伤害值判断强度
     *
     * @param damage 原版伤害值
     * @return 对应的损坏强度
     */
    public static 损坏强度 从伤害值(float damage) {
        if (damage >= 高伤害阈值) {
            return 损坏强度.严重;
        }
        if (damage >= 中伤害阈值) {
            return 损坏强度.普通;
        }
        return 损坏强度.轻微;
    }

    /**
     * 物品被移除时的强度
     * 通常是虚空等不可恢复的情况
     */
    public static 损坏强度 从移除() {
        return 损坏强度.严重;
    }

    // ==================== 主动损坏 ====================

    /**
     * 从方块硬度判断强度
     *
     * @param state 方块状态
     * @return 强度，如果方块太软则返回null
     */
    @Nullable
    public static 损坏强度 从方块硬度(BlockState state) {
        // getDestroySpeed 需要 level 和 pos，但我们只需要大致判断
        // 使用 destroySpeed 属性
        float hardness = state.getBlock().defaultDestroyTime();

        if (hardness < 0) {
            // 不可破坏的方块（基岩等）
            return 损坏强度.严重;
        }
        if (hardness >= 方块硬度_严重) {
            return 损坏强度.严重;
        }
        if (hardness >= 方块硬度_普通) {
            return 损坏强度.普通;
        }
        if (hardness >= 方块硬度_轻微) {
            return 损坏强度.轻微;
        }

        // 太软，不造成伤害
        return null;
    }

    /**
     * 从武器/工具判断强度
     *
     * @param weapon 武器物品
     * @return 强度，如果不是有效武器则返回null
     */
    @Nullable
    public static 损坏强度 从武器(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return null;
        }

        // 获取攻击伤害
        float attackDamage = 获取攻击伤害(weapon);

        if (attackDamage >= 武器伤害_严重) {
            return 损坏强度.严重;
        }
        if (attackDamage >= 武器伤害_普通) {
            return 损坏强度.普通;
        }
        if (attackDamage > 0) {
            return 损坏强度.轻微;
        }

        return null;
    }

    /**
     * 从下落实体判断强度
     *
     * @param entity 下落中的实体
     * @return 强度
     */
    public static 损坏强度 从下落实体(Entity entity) {
        double fallSpeed = Math.abs(entity.getDeltaMovement().y);

        if (fallSpeed >= 下落速度_严重) {
            return 损坏强度.严重;
        }
        if (fallSpeed >= 下落速度_普通) {
            return 损坏强度.普通;
        }
        return 损坏强度.轻微;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取物品的攻击伤害值
     */
    private static float 获取攻击伤害(ItemStack stack) {
        if (stack.getItem() instanceof TieredItem tieredItem) {
            return tieredItem.getTier().getAttackDamageBonus();
        }

        // 尝试从属性修饰符获取
        // 简化处理：非TieredItem视为1点伤害
        return 1.0f;
    }

    /**
     * 检查物品是否为武器
     */
    public static boolean 是武器(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof TieredItem;
    }
}
