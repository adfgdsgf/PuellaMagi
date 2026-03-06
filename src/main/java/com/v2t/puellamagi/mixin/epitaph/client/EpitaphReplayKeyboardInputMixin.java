package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import com.v2t.puellamagi.mixin.access.KeyMappingAccessor;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import com.v2t.puellamagi.util.recording.实体帧数据;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

/**
 * 客户端输入回放 — 移动 + 视角 + 按键注入
 *
 * 按键注入流程：
 * 1. 清空所有KeyMapping（防止真实键盘输入泄漏）
 * 2. 按录制数据设置对应按键
 * 3. MC在handleKeybinds()中消费 → 触发正确的动作
 */
@Mixin(KeyboardInput.class)
public class EpitaphReplayKeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void epitaph$replaceWithRecordedInput(boolean sneaking, float sneakSpeed, CallbackInfo ci) {
        if (!客户端复刻管理器.本地玩家是否输入回放中()) {
            return;
        }

        玩家输入帧 input = 客户端复刻管理器.获取本地玩家输入帧();
        if (input == null) {
            return;
        }

        //---- 移动输入 ----
        this.forwardImpulse = input.获取前后输入();
        this.leftImpulse = input.获取左右输入();
        this.jumping = input.是否跳跃();
        this.shiftKeyDown = input.是否潜行();

        this.up = input.获取前后输入() > 0.001f;
        this.down = input.获取前后输入() < -0.001f;
        this.left = input.获取左右输入() > 0.001f;
        this.right = input.获取左右输入() < -0.001f;

        // ---- 冲刺 +槽位 + 视角 ----
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setSprinting(input.是否冲刺());
            player.getInventory().selected = input.获取选中槽位();

            player.setYRot(input.获取YRot());
            player.setXRot(input.获取XRot());}

        // ---- 按键注入 ----
        注入按键状态(input);


        // ---- 使用物品状态同步 ----
        // 服务端通过包回放处理使用物品，但客户端不知道（use键没注入）
        // 客户端每tick检查"我按了use键吗？"→ 没有 → stopUsingItem
        // 覆盖了服务端同步过来的isUsingItem状态
        // 这里从帧数据读取使用物品状态，直接设到本地玩家上
    }

    /**
     * 攻击和使用键完全不注入（isDown也不注入）
     *
     * 原因：MC客户端检查isDown来触发放置/攻击/持续使用
     * 任何注入都会导致客户端自己预测放方块 → 幽灵方块
     *
     * 实际动作由服务端包回放处理
     *手臂动画由服务端广播挥手包驱动
     */
    /** 完全不注入（isDown和clickCount都不给） */
    @Unique
    private static final Set<String> 禁止注入的键 = Set.of("key.attack");

    /** 只注入isDown不注入clickCount */
    @Unique
    private static final Set<String>仅注入isDown的键 = Set.of("key.use");

    @Unique
    private void 注入按键状态(玩家输入帧 input) {
        Map<String, KeyMapping> allKeys = KeyMappingAccessor.puellamagi$getAll();

        for (KeyMapping key : allKeys.values()) {
            key.setDown(false);
            ((KeyMappingAccessor) key).puellamagi$setClickCount(0);
        }

        for (玩家输入帧.按键状态 recorded : input.获取按键列表()) {
            KeyMapping key = allKeys.get(recorded.name());
            if (key == null) continue;

            if (禁止注入的键.contains(recorded.name())) {
                continue;
            }

            if (仅注入isDown的键.contains(recorded.name())) {
                key.setDown(recorded.isDown());
                continue;
            }

            key.setDown(recorded.isDown());((KeyMappingAccessor) key).puellamagi$setClickCount(recorded.clickCount());
        }
    }
}
