// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopInputMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.v2t.puellamagi.client.timestop.时停冻结帧;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 *键盘输入拦截
 */
@Mixin(KeyboardHandler.class)
public class TimestopInputMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onKeyPress(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (!时停冻结帧.处于冻结状态()) {
            return;
        }Minecraft mc = Minecraft.getInstance();

        // 有白名单界面时，全部放行
        if (puellamagi$isWhitelistedScreen(mc.screen)) {
            return;
        }

        // 没有界面时，只放行特定按键
        if (mc.screen == null) {
            // ESC
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                return;
            }

            // 聊天键（玩家自定义）
            if (mc.options.keyChat.matches(key, scancode)) {
                return;
            }

            // 命令键（玩家自定义）
            if (mc.options.keyCommand.matches(key, scancode)) {
                return;
            }

            // F3（调试功能，包括F3+F4切换模式）
            if (key == GLFW.GLFW_KEY_F3) {
                return;
            }

            // F4（配合F3使用）
            if (key == GLFW.GLFW_KEY_F4) {
                return;
            }
        }

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
                || name.contains("language")
                || name.contains("gamemode");
    }
}
