package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.影响标记表;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物品堆叠兼容Mixin
 *
 * 问题：时间删除的NBT隐藏tag影响物品堆叠
 * 同类物品一个有tag一个没有→ MC认为不同 → 不堆叠
 *
 * 修复：堆叠判断时临时忽略我们的tag
 * 只影响堆叠判断 → 不影响其他逻辑
 */
@Mixin(ItemStack.class)
public class TimeDeletionStackMixin {

    @Inject(method = "isSameItemSameTags", at = @At("HEAD"), cancellable = true)
    private static void onIsSameItemSameTags(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {

        // 只有当其中一个有时删tag时才需要特殊处理
        boolean selfHasTag = stack.hasTag() && stack.getTag().contains(影响标记表.TAG_KEY);
        boolean otherHasTag = other.hasTag() && other.getTag().contains(影响标记表.TAG_KEY);

        if (!selfHasTag && !otherHasTag) return;

        // 临时移除tag对比
        CompoundTag selfTag = stack.hasTag() ? stack.getTag().copy() : null;
        CompoundTag otherTag = other.hasTag() ? other.getTag().copy() : null;

        if (selfTag != null) selfTag.remove(影响标记表.TAG_KEY);
        if (otherTag != null) otherTag.remove(影响标记表.TAG_KEY);

        if (selfTag != null && selfTag.isEmpty()) selfTag = null;
        if (otherTag != null && otherTag.isEmpty()) otherTag = null;

        if (!stack.is(other.getItem())) {
            cir.setReturnValue(false);
            return;
        }

        boolean tagsEqual;
        if (selfTag == null && otherTag == null) {
            tagsEqual = true;
        } else if (selfTag == null || otherTag == null) {
            tagsEqual = false;
        } else {
            tagsEqual = selfTag.equals(otherTag);
        }

        cir.setReturnValue(tagsEqual);
    }
}
