// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopItemEntityMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 掉落物 Mixin
 *
 * 职责：允许时停者拾取被冻结的掉落物
 * tick 拦截已移至 Level 层
 */
@Mixin(ItemEntity.class)
public abstract class TimestopItemEntityMixin {

    /**
     * 允许时停者拾取被冻结的掉落物
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerTouch(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        TimeStop timeStop = (TimeStop) self.level();

        // 如果物品被冻结
        if (timeStop.puellamagi$shouldFreezeEntity(self)) {
            // 只有时停者可以拾取
            if (timeStop.puellamagi$isTimeStopper(player)) {
                // 不取消，让原方法执行拾取逻辑
                return;
            } else {
                // 非时停者不能拾取冻结的物品
                ci.cancel();
            }
        }
    }
}
