package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时删期间A的客户端通用效果过滤（音效+粒子）
 *
 * 时删后A在服务端仍按命运走（输入回放），但A的客户端已自由。
 * 服务端A的命运行动产生的效果会通过S2C包发到A的客户端。
 *
 * 过滤条件：是否时间删除自由()
 * → true = 时删已激活且回放还在跑
 * → 回放结束时自动变false → 过滤自动解除
 * → A正常游戏中的声音/粒子不受影响
 *
 * A自己客户端本地产生的声音/粒子（客户端LocalPlayer的playSound等）
 * 不走S2C包，不会被此Mixin拦截 → A的自由行动体验完全正常
 *
 * 通用过滤，不需要逐个声音/粒子类型判断
 */
@Mixin(ClientPacketListener.class)
public class EpitaphTimeDeletionSoundMixin {

    /**
     * 拦截实体音效包
     *
     * ClientboundSoundEntityPacket 携带实体ID
     * 如果是本地玩家且时删中 → cancel
     *
     * 自动覆盖所有实体音效：落地声、受击声、死亡声、吃东西声、
     * 游泳声、着火声等，无需穷举
     */
    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"), cancellable = true)
    private void epitaph$filterEntitySound(ClientboundSoundEntityPacket packet,
                                            CallbackInfo ci) {
        if (!客户端复刻管理器.是否时间删除自由()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 只拦截绑定在本地玩家身上的音效
        if (packet.getId() == player.getId()) {
            ci.cancel();
        }
    }

    /**
     * 拦截粒子包
     *
     * ClientboundLevelParticlesPacket 携带位置坐标
     * 用A在命运中的精确位置（帧数据）做判断：
     * - 粒子在命运位置2格范围内 → 是A命运行动产生的 → 拦截
     * - 超出范围 → 环境/其他实体的粒子 → 放行
     *
     * 命运位置来自帧同步包中A的帧数据
     * 时删期间帧同步包仍包含A的帧数据（用于B驱动A的残影）
     * A的客户端也收到 → 可以精确知道A在命运中的位置
     */
    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void epitaph$filterParticles(ClientboundLevelParticlesPacket packet,
                                          CallbackInfo ci) {
        if (!客户端复刻管理器.是否时间删除自由()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 获取A在命运中的精确位置（从帧数据中）
        com.v2t.puellamagi.util.recording.实体帧数据 命运帧 =
                客户端复刻管理器.获取当前帧(player.getUUID());
        if (命运帧 == null) return;

        // 用命运位置做精确判断
        double dx = packet.getX() - 命运帧.获取X();
        double dy = packet.getY() - 命运帧.获取Y();
        double dz = packet.getZ() - 命运帧.获取Z();
        double distSq = dx * dx + dy * dy + dz * dz;

        // 粒子在命运位置2格范围内 → 是A命运行动产生的 → 拦截
        if (distSq < 4.0) {
            ci.cancel();
        }
    }
}
