// 文件路径: src/main/java/com/v2t/puellamagi/mixin/item/ItemEntityPickupMixin.java

package com.v2t.puellamagi.mixin.item;

import com.v2t.puellamagi.api.item.I绑定物品;
import com.v2t.puellamagi.util.绑定物品工具;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 标记玩家拾取为合法移除流程
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void puellamagi$onPickupStart(Player player, CallbackInfo ci) {
        ItemStack stack = this.getItem();
        if (stack.getItem() instanceof I绑定物品) {
            绑定物品工具.开始合法拾取();
        }
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void puellamagi$onPickupEnd(Player player, CallbackInfo ci) {
        ItemStack stack = this.getItem();
        if (stack.getItem() instanceof I绑定物品) {
            绑定物品工具.结束合法拾取();
        }
    }
}
