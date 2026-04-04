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
 * 回放中：拦截真实键盘输入，但放行有可用技能的按键
 *
 * 放行逻辑：
 * 按键对应的技能槽位里的技能.可以使用()返回true → 放行
 * → 改键自动跟随
 * → 新技能自动兼容（只要可以使用()返回true）
 *
 * 排除的键（不录制也不拦截）：
 * F2=截图 F3=调试 F11=全屏
 */
@Mixin(KeyboardHandler.class)
public class EpitaphKeyboardRecordMixin {

    @Unique
    private static final Set<Integer> 排除的键 = Set.of(GLFW.GLFW_KEY_F2,
            GLFW.GLFW_KEY_F3,
            GLFW.GLFW_KEY_F11
    );

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void epitaph$interceptKeyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (排除的键.contains(key)) return;

        if (客户端复刻管理器.是否正在重放事件()) return;

        if (客户端复刻管理器.是否录制中()) {
            客户端复刻管理器.添加键盘事件(key, scanCode, action, modifiers);
            return;
        }

        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            // 时间删除中：放行所有按键
            if (客户端复刻管理器.是否时间删除自由()) {
                return;
            }

            if (有可用技能绑定(key, scanCode)) {
                return;
            }ci.cancel();
        }
    }


    /**
     * 检查是否有可用的技能绑定在这个按键上
     *
     * 遍历技能栏槽位 → 检查槽位的KeyMapping是否匹配
     * → 匹配时检查槽位里的技能.可以使用()
     * → 技能自己决定是否放行
     *
     * 改键自动跟随：KeyMapping.matches()实时检查当前绑定
     * 新技能自动兼容：只要可以使用()返回true就放行
     */
    @Unique
    private static boolean 有可用技能绑定(int key, int scanCode) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return false;

        // 遍历技能键数组
        for (int i = 0; i < com.v2t.puellamagi.client.keybind.按键绑定.技能键.length; i++) {
            net.minecraft.client.KeyMapping keyMapping = com.v2t.puellamagi.client.keybind.按键绑定.技能键[i];

            // 这个键是不是按的那个
            if (!keyMapping.matches(key, scanCode)) continue;

            // 这个槽位里有什么技能
            var skillCap = com.v2t.puellamagi.util.能力工具.获取技能能力(mc.player);
            if (skillCap.isEmpty()) return false;

            var preset = skillCap.get().获取当前预设();
            if (preset == null || i >= preset.获取槽位列表().size()) continue;

            var slotData = preset.获取槽位列表().get(i);
            if (slotData == null || slotData.获取技能ID() == null) continue;

            // 创建技能实例检查可以使用
            var skillOpt = com.v2t.puellamagi.system.skill.技能注册表.创建实例(slotData.获取技能ID());
            if (skillOpt.isEmpty()) continue;

            return skillOpt.get().可以使用(mc.player);
        }

        return false;
    }
}
