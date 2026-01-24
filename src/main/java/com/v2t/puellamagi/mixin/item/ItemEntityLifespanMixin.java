package com.v2t.puellamagi.mixin.item;

import com.v2t.puellamagi.api.item.I绑定物品;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 绑定物品永不过期处理
 *
 * 拦截 ItemEntity 的tick，阻止实现 I绑定物品 且启用永不过期的物品自然消失
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityLifespanMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public int lifespan;

    @Shadow
    private int age;

    /**
     * 在tick开始时检查，如果是永不过期的绑定物品则重置age
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void puellamagi$preventExpire(CallbackInfo ci) {
        ItemStack stack = getItem();
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof I绑定物品 item) {
            if (item.是否永不过期()) {
                // 保持age不超过lifespan的一半，防止接近过期
                if (this.age > this.lifespan / 2) {
                    this.age = 0;
                }
            }
        }
    }
}
