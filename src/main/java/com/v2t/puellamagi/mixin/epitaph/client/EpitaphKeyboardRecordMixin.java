package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * 拦截GLFW键盘事件 — 录制 + 回放
 *
 * 录制中：存一份事件到队列，让MC正常处理
 * 回放中：拦截真实键盘输入（录制的事件由客户端复刻管理器直接调keyPress注入）
 *
 * 排除的键（不录制也不拦截）：
 * F2=截图 F3=调试 F11=全屏
 */
@Mixin(KeyboardHandler.class)
public class EpitaphKeyboardRecordMixin {

    @Unique
    private static final Set<Integer> 排除的键 = Set.of(
            GLFW.GLFW_KEY_F2,
            GLFW.GLFW_KEY_F3,
            GLFW.GLFW_KEY_F11
    );

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void epitaph$interceptKeyPress(long window, int key, int scanCode,int action, int modifiers, CallbackInfo ci) {
        // 排除系统键
        if (排除的键.contains(key)) return;

        // 重放中的事件直接放行（避免拦截自己注入的）
        if (客户端复刻管理器.是否正在重放事件()) return;

        // 录制中：存一份，不拦截
        if (客户端复刻管理器.是否录制中()) {
            客户端复刻管理器.添加键盘事件(key, scanCode, action, modifiers);
            return;
        }

        // 回放中：拦截真实键盘输入
        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            ci.cancel();
        }
    }
}
