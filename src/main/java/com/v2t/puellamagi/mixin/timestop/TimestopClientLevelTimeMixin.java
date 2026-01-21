// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopClientLevelTimeMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端世界时间冻结
 *
 * 阻止客户端的时间流逝，让太阳/月亮完全静止
 */
@Mixin(ClientLevel.class)
public abstract class TimestopClientLevelTimeMixin {

    /**
     * 拦截 setDayTime - 时停中不更新时间
     */
    @Inject(method = "setDayTime", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetDayTime(long dayTime, CallbackInfo ci) {
        TimeStop timeStop = (TimeStop) this;
        if (timeStop.puellamagi$hasActiveTimeStop()) {
            ci.cancel();
        }
    }
}
