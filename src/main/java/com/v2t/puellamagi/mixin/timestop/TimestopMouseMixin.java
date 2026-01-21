// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopMouseMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.client.timestop.时停冻结帧;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鼠标输入拦截
 *
 * 冻结时：
 * - 有白名单界面：全部放行
 * - 没有界面：拦截视角转动和点击
 */
@Mixin(MouseHandler.class)
public class TimestopMouseMixin {

    /**
     * 视角转动 - 冻结时始终拦截（除非有白名单界面，但界面打开时本身就不会转视角）
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTurnPlayer(CallbackInfo ci) {
        if (时停冻结帧.处于冻结状态()) {
            ci.cancel();
        }
    }

    /**
     *鼠标点击
     */
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onMousePress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!时停冻结帧.处于冻结状态()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // 有白名单界面时，放行
        if (puellamagi$isWhitelistedScreen(mc.screen)) {
            return;
        }

        // 没有白名单界面，拦截
        ci.cancel();
    }

    /**
     * 鼠标滚轮
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (!时停冻结帧.处于冻结状态()) {
            return;
        }Minecraft mc = Minecraft.getInstance();

        // 有白名单界面时，放行
        if (puellamagi$isWhitelistedScreen(mc.screen)) {
            return;
        }

        // 没有白名单界面，拦截
        ci.cancel();
    }

    @Unique
    private boolean puellamagi$isWhitelistedScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof PauseScreen) {
            return true;
        }
        if (screen instanceof ChatScreen) {
            return true;
        }
        String name = screen.getClass().getName().toLowerCase();
        return name.contains("option")
                || name.contains("setting")
                || name.contains("controls")
                || name.contains("language");
    }
}
