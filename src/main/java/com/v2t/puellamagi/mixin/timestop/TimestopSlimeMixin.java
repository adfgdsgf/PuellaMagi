// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopSlimeMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slime Mixin
 *
 * 史莱姆的攻击基于玩家碰撞，需要特殊处理
 * 对标 Roundabout 的 TimeStopSlime
 */
@Mixin(Slime.class)
public abstract class TimestopSlimeMixin {

    /**
     * 时停中史莱姆不会攻击玩家
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onPlayerTouch(Player player, CallbackInfo ci) {
        Slime self = (Slime) (Object) this;
        TimeStop timeStop = (TimeStop) player.level();

        // 如果史莱姆应该被冻结，取消接触伤害
        if (timeStop.puellamagi$shouldFreezeEntity(self)) {
            ci.cancel();
        }
    }
}
