// 文件路径: src/main/java/com/v2t/puellamagi/api/interaction/I搜身槽位提供者.java

package com.v2t.puellamagi.api.interaction;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * 搜身槽位提供者接口
 *
 * 用于扩展可搜身的槽位来源
 * 原版背包、饰品mod等各自实现并注册
 */
public interface I搜身槽位提供者 {

    String 获取提供者ID();
    int 获取优先级();
    boolean 是否可用();
    List<搜身容器信息> 获取容器(Player target);

    /**
     * 槽位区域类型
     */
    enum 区域类型 {
        主区域,侧边区域
    }

    /**
     * 槽位限制类型
     */
    sealed interface 槽位限制 {

        /**
         * 无限制（任何物品都能放）
         */
        record 无限制() implements 槽位限制 {}

        /**
         * 装备限制（使用原版 canEquip 检查）
         */
        record 装备限制(EquipmentSlot 装备位置) implements 槽位限制 {}

        /**
         * 自定义限制（使用提供的检查函数）
         *用于饰品mod等需要自定义检查逻辑的场景
         */
        record 自定义限制(BiPredicate<ItemStack, Player> 检查器) implements 槽位限制 {}

        // 便捷工厂方法
        static 槽位限制 无() {
            return new 无限制();
        }

        static 槽位限制 装备(EquipmentSlot slot) {
            return new 装备限制(slot);
        }

        static 槽位限制 自定义(BiPredicate<ItemStack, Player> checker) {
            return new 自定义限制(checker);
        }
    }

    /**
     * 搜身容器信息
     */
    record 搜身容器信息(
            String 容器ID,
            String 显示名称键,
            Container 容器,
            int 起始槽位,
            int 槽位数量,
            区域类型 区域,
            int 列数,
            槽位限制[]槽位限制数组
    ) {
        /**
         * 兼容构造器（无限制）
         */
        public 搜身容器信息(String 容器ID, String 显示名称键, Container 容器,int 起始槽位, int 槽位数量,区域类型 区域, int 列数) {
            this(容器ID, 显示名称键, 容器, 起始槽位, 槽位数量, 区域, 列数, null);
        }

        /**
         * 兼容最旧构造器（默认主区域，9列）
         */
        public 搜身容器信息(String 容器ID, String 显示名称键, Container 容器,
                            int 起始槽位, int 槽位数量) {
            this(容器ID, 显示名称键, 容器, 起始槽位, 槽位数量, 区域类型.主区域, 9, null);
        }

        /**
         * 获取指定索引的槽位限制
         */
        public 槽位限制 获取槽位限制(int index) {
            if (槽位限制数组 == null || index < 0 || index >= 槽位限制数组.length) {
                return null;
            }
            return 槽位限制数组[index];
        }
    }
}
