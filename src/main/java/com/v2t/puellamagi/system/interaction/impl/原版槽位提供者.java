// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/impl/原版槽位提供者.java

package com.v2t.puellamagi.system.interaction.impl;

import com.v2t.puellamagi.api.interaction.I搜身槽位提供者;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 原版槽位提供者
 *
 * Inventory槽位布局：
 * - 0-35: 主背包 + 快捷栏
 * - 36:靴子 (FEET)
 * - 37: 护腿 (LEGS)
 * - 38:胸甲 (CHEST)
 * - 39: 头盔 (HEAD)
 * - 40: 副手（无限制）
 */
public class 原版槽位提供者 implements I搜身槽位提供者 {

    public static final 原版槽位提供者 INSTANCE = new 原版槽位提供者();

    // 装备槽位限制
    private static final 槽位限制[] 装备槽位限制 = {
            槽位限制.装备(EquipmentSlot.FEET),   // 36: 靴子
            槽位限制.装备(EquipmentSlot.LEGS),   // 37: 护腿
            槽位限制.装备(EquipmentSlot.CHEST),  // 38: 胸甲
            槽位限制.装备(EquipmentSlot.HEAD),   // 39: 头盔
            槽位限制.无()                         // 40: 副手（无限制）
    };

    private 原版槽位提供者() {}

    @Override
    public String 获取提供者ID() {
        return "puellamagi:vanilla_inventory";
    }

    @Override
    public int 获取优先级() {
        return 0;
    }

    @Override
    public boolean 是否可用() {
        return true;
    }

    @Override
    public List<搜身容器信息> 获取容器(Player target) {
        List<搜身容器信息> result = new ArrayList<>();
        Inventory inventory = target.getInventory();

        // 主背包 + 快捷栏（主区域，无限制）
        result.add(new 搜身容器信息(
                "vanilla_main",
                "gui.puellamagi.search.inventory",
                inventory,
                0,
                36,
                区域类型.主区域,
                9,
                null
        ));

        // 装备栏 + 副手（侧边区域，有限制）
        result.add(new 搜身容器信息(
                "vanilla_equipment",
                "gui.puellamagi.search.equipment",
                inventory,
                36,
                5,
                区域类型.侧边区域,
                1,
                装备槽位限制
        ));

        return result;
    }
}
