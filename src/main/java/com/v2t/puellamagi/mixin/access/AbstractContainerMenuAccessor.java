// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/AbstractContainerMenuAccessor.java

package com.v2t.puellamagi.mixin.access;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问AbstractContainerMenu的private字段
 * 用于动态重建槽位
 */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {

    @Accessor("lastSlots")
    NonNullList<ItemStack> puellamagi$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<ItemStack> puellamagi$getRemoteSlots();
}
