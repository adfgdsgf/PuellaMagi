package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端纯输入回放 — 移动+ 视角 + 射线补正
 *
 * 键盘/鼠标事件已经由客户端复刻管理器在tick开始时重放了
 * → KeyMapping状态已正确更新
 * → Screen已收到按键（ESC关箱子等）
 *
 * 这里只负责：
 * 1. 覆盖移动计算值（确保和录制时完全一致）
 * 2. 注入射线结果（确保放方块/交互位置完全一致）
 * 3. 设置冲刺/槽位/视角
 */
@Mixin(KeyboardInput.class)
public class EpitaphReplayKeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void epitaph$replaceWithRecordedInput(boolean sneaking, float sneakSpeed, CallbackInfo ci) {
        if (!客户端复刻管理器.本地玩家是否输入回放中()) {
            return;
        }玩家输入帧 input = 客户端复刻管理器.获取本地玩家输入帧();
        if (input == null) {
            return;
        }

        //---- 移动输入覆盖 ----
        this.forwardImpulse = input.获取前后输入();
        this.leftImpulse = input.获取左右输入();
        this.jumping = input.是否跳跃();
        this.shiftKeyDown = input.是否潜行();

        this.up = input.获取前后输入() > 0.001f;
        this.down = input.获取前后输入() < -0.001f;
        this.left = input.获取左右输入() > 0.001f;
        this.right = input.获取左右输入() < -0.001f;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // ---- 冲刺 +槽位 + 视角 ----
        player.setSprinting(input.是否冲刺());
        player.getInventory().selected = input.获取选中槽位();
        player.setYRot(input.获取YRot());
        player.setXRot(input.获取XRot());}

}
