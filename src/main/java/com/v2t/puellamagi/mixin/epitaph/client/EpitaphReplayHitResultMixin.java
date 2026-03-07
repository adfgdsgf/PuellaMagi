package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * handleKeybinds HEAD — 录制和回放射线结果
 *
 * 为什么在这里：
 * pick()刚算完hitResult → handleKeybinds即将用hitResult放方块/攻击
 * 这是MC"决定在哪放方块"的精确时刻
 *
 * 录制：在这里存hitResult → 存的是MC即将使用的值→ 精确
 * 回放：在这里覆盖hitResult → MC用我们的值 → 位置一致
 *
 * 之前的问题：
 * hitResult在ClientTickEvent.END录制 →那时MC可能已经用hitResult放了方块
 * → 世界变了 → hitResult指向变化后的世界 → 录到错误的值
 */
@Mixin(Minecraft.class)
public class EpitaphReplayHitResultMixin {

    @Shadow
    @Nullable
    public HitResult hitResult;

    @Shadow
    @Nullable
    public net.minecraft.world.entity.Entity crosshairPickEntity;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void epitaph$handleHitResult(CallbackInfo ci) {
        // === 录制：存当前hitResult ===
        if (客户端复刻管理器.是否录制中()) {
            客户端复刻管理器.设置当前射线结果(this.hitResult);
            return;
        }

        // === 回放：注入录制的hitResult ===
        if (!客户端复刻管理器.本地玩家是否输入回放中()) return;
        玩家输入帧 input = 客户端复刻管理器.获取本地玩家输入帧();
        if (input == null) return;

        int hitType = input.获取射线类型();

        if (hitType == 1) {
            HitResult result = input.重建射线结果();
            if (result != null) {
                this.hitResult = result;
            }
        } else if (hitType == 2) {
            Minecraft mc = (Minecraft) (Object) this;
            if (mc.level != null) {
                net.minecraft.world.entity.Entity target =
                        mc.level.getEntity(input.获取射线实体ID());
                if (target != null) {
                    this.hitResult = new net.minecraft.world.phys.EntityHitResult(
                            target,
                            new net.minecraft.world.phys.Vec3(
                                    input.获取射线X(),
                                    input.获取射线Y(),
                                    input.获取射线Z()));this.crosshairPickEntity = target;
                }
            }
        } else {
            this.hitResult = input.重建射线结果();
        }
    }
}
