// 文件路径: src/main/java/com/v2t/puellamagi/mixin/item/PlayerDeathKeepMixin.java

package com.v2t.puellamagi.mixin.item;

import com.v2t.puellamagi.util.绑定物品工具;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡时保留绑定物品
 *
 * 职责：仅做拦截，业务逻辑委托给 绑定物品工具
 *
 * 流程：
 * 1. dropAll HEAD: 提取保留物品到缓存
 * 2. dropAll RETURN: 恢复到原背包（等待Clone同步）
 */
@Mixin(Inventory.class)
public abstract class PlayerDeathKeepMixin {

    @Shadow
    @Final
    public Player player;

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void puellamagi$beforeDropAll(CallbackInfo ci) {
        绑定物品工具.提取死亡保留物品(player);
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void puellamagi$afterDropAll(CallbackInfo ci) {
        绑定物品工具.恢复到原背包(player);
    }
}
