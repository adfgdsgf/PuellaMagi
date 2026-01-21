// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/AbstractArrowAccessor.java

package com.v2t.puellamagi.mixin.access;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Invoker("getPickupItem")
    ItemStack puellamagi$getPickupItem();
}
