// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/menu/搜身限制槽位.java

package com.v2t.puellamagi.system.interaction.menu;

import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.槽位限制;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 搜身限制槽位
 *
 * 根据槽位限制类型检查物品是否可放入
 * 支持：无限制、装备限制、自定义限制
 */
public class 搜身限制槽位 extends Slot {

    private final 槽位限制 限制;
    private final Player 目标玩家;

    public 搜身限制槽位(Container container, int slotIndex, int x, int y,槽位限制 限制, Player 目标玩家) {
        super(container, slotIndex, x, y);
        this.限制 = 限制;
        this.目标玩家 = 目标玩家;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (限制 == null) {
            return true;
        }

        if (限制 instanceof 槽位限制.无限制) {
            return true;
        } else if (限制 instanceof 槽位限制.装备限制 装备) {
            return stack.canEquip(装备.装备位置(), 目标玩家);
        } else if (限制 instanceof 槽位限制.自定义限制 自定义) {
            return 自定义.检查器().test(stack, 目标玩家);
        }

        return true;
    }

    @Override
    public int getMaxStackSize() {
        if (限制 instanceof 槽位限制.装备限制) {
            return 1;
        }
        return super.getMaxStackSize();
    }
}
