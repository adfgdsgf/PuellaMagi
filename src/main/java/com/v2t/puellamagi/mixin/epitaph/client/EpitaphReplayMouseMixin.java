package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回放时替换鼠标输入为录制的视角数据
 *
 * 三种状态：
 * 1. 过渡保护中→ cancel鼠标 + 同步位置旧值（传送静默发生）
 * 2. 正常回放中 → Catmull-Rom插值（不同步位置旧值，让MC自己管动画）
 * 3. 结尾保护中 →锁定最后角度 + 同步位置旧值
 *
 * 注意：同步位置旧值只在过渡期/结尾期调用
 * 正常回放中调的话会破坏walkDist/bob的插值 → 手部动画掉帧
 */
@Mixin(MouseHandler.class)
public class EpitaphReplayMouseMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    private static float 最后YRot = 0;
    private static float 最后XRot = 0;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void epitaph$replaySmoothedRotation(CallbackInfo ci) {
        if (!客户端复刻管理器.是否需要接管()) {
            return;
        }

        ci.cancel();
        this.accumulatedDX = 0;
        this.accumulatedDY = 0;

        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            // === 正常回放：Catmull-Rom插值 ===
            // 不调同步位置旧值 — MC自己管walkDist/bob的插值
            float pt = this.minecraft.getFrameTime();
            float interpY = 客户端复刻管理器.获取视角插值YRot(pt);
            float interpX = 客户端复刻管理器.获取视角插值XRot(pt);

            if (Float.isNaN(interpY) || Float.isNaN(interpX)) return;

            player.setYRot(interpY);
            player.yRotO = interpY;
            player.setXRot(interpX);
            player.xRotO = interpX;
            player.setYHeadRot(interpY);
            player.yHeadRotO = interpY;

            最后YRot = interpY;
            最后XRot = interpX;

        } else if (客户端复刻管理器.是否过渡保护中()) {
            // === 过渡保护：不改角度，同步位置旧值 ===
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();
            player.yHeadRotO = player.getYHeadRot();

            客户端复刻管理器.同步位置旧值(player);} else if (客户端复刻管理器.是否结尾保护中()) {
            // === 结尾保护：锁定最后角度 + 同步位置旧值 ===
            player.setYRot(最后YRot);
            player.yRotO = 最后YRot;
            player.setXRot(最后XRot);
            player.xRotO = 最后XRot;
            player.setYHeadRot(最后YRot);
            player.yHeadRotO = 最后YRot;

            客户端复刻管理器.同步位置旧值(player);
        }
    }
}
