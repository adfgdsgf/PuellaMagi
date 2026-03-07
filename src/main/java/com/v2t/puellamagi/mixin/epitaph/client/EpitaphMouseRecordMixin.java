package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截GLFW鼠标点击事件 — 录制 + 回放
 *
 * 录制中：存事件+当时的光标位置到队列，让MC正常处理
 * 回放中：拦截真实鼠标点击
 */
@Mixin(MouseHandler.class)
public class EpitaphMouseRecordMixin {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void epitaph$interceptMousePress(long window, int button, int action,
                                             int modifiers, CallbackInfo ci) {
        // 重放中的事件直接放行
        if (客户端复刻管理器.是否正在重放事件()) return;

        // 录制中：存事件 + 当时的光标位置，不拦截
        if (客户端复刻管理器.是否录制中()) {Minecraft mc = Minecraft.getInstance();
            double cursorX = mc.mouseHandler.xpos();
            double cursorY = mc.mouseHandler.ypos();

            客户端复刻管理器.添加鼠标事件(button, action, modifiers, cursorX, cursorY);
            return;
        }

        // 回放中：拦截真实鼠标点击
        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            ci.cancel();
        }
    }
}
