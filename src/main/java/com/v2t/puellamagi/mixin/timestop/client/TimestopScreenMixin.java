// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopScreenMixin.java

package com.v2t.puellamagi.mixin.timestop.client;

import com.v2t.puellamagi.client.timestop.时停冻结帧;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screen Mixin - 拦截非白名单界面
 *
 * 白名单界面（完全放行）：
 * - ESC菜单（PauseScreen）及其子菜单（设置等）
 * - 聊天框（ChatScreen）
 * - 退出相关界面
 *
 * 其他界面（被拦截）：
 * - 背包
 * - 容器
 * - 等等
 */
@Mixin(Minecraft.class)
public class TimestopScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onSetScreen(Screen screen, CallbackInfo ci) {
        // 如果没有冻结，不干预
        if (!时停冻结帧.处于冻结状态()) {
            return;
        }

        // null表示关闭界面，始终允许
        if (screen == null) {
            return;
        }

        // 白名单界面，允许打开
        if (是允许的界面(screen)) {
            return;
        }

        // 其他界面，拦截
        ci.cancel();
    }

    /**
     * 判断是否是白名单界面
     */
    private static boolean 是允许的界面(Screen screen) {
        // ESC菜单
        if (screen instanceof PauseScreen) return true;

        // 聊天框
        if (screen instanceof ChatScreen) return true;

        // 退出游戏相关（必须允许，否则无法退出）
        if (screen instanceof TitleScreen) return true;
        if (screen instanceof JoinMultiplayerScreen) return true;

        // 通过类名检测设置/选项界面（包括ESC菜单的子菜单）
        String className = screen.getClass().getName().toLowerCase();
        if (className.contains("option")
                || className.contains("setting")
                || className.contains("config")
                || className.contains("controls")
                || className.contains("language")
                || className.contains("resource")
                || className.contains("sound")) {
            return true;
        }

        // 检查父类（有些mod的设置界面可能继承自原版）
        Class<?> parentClass = screen.getClass().getSuperclass();
        while (parentClass != null && parentClass != Screen.class) {
            String parentName = parentClass.getName().toLowerCase();
            if (parentName.contains("option") || parentName.contains("setting")) {
                return true;
            }
            parentClass = parentClass.getSuperclass();
        }

        return false;
    }
}
