// 文件路径: src/main/java/com/v2t/puellamagi/mixin/item/ItemEntityProtectionMixin.java

package com.v2t.puellamagi.mixin.item;

import com.v2t.puellamagi.api.item.I绑定物品;
import com.v2t.puellamagi.util.绑定物品工具;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 保护绑定物品掉落物不被非法删除
 *
 * 防范场景：
 * - /kill @e[type=item] 指令
 * - 服务器清理插件
 * - mod 的非法 discard 调用
 *
 * 允许场景：
 * - 玩家正常拾取
 * - 区块卸载/维度切换
 *
 * 注意：remove 方法定义在 Entity 类中，所以 Mixin 目标是 Entity
 */
@Mixin(Entity.class)
public abstract class ItemEntityProtectionMixin {

    /**
     * 拦截删除：检查是否应该阻止
     *仅对 ItemEntity 且是绑定物品时生效
     */
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void puellamagi$protectFromIllegalRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只处理 ItemEntity
        if (!(self instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();

        // 只处理绑定物品
        if (!(stack.getItem() instanceof I绑定物品)) {
            return;
        }

        if (绑定物品工具.应该阻止删除(stack, reason, self.getId())) {
            ci.cancel();
        }
    }
}
