package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 回放期间阻止客户端自动停止使用物品
 *
 * 正常游戏：
 * 每tick检查"右键按住了吗？"→ 没按→ 停止使用物品
 * → 合理（玩家松手了）
 *
 * 回放期间：
 * 右键根本没注入（注入会导致放方块）→ 永远判定"没按"
 * → 每tick都停止 → 服务端设的"正在吃东西"状态被覆盖
 *
 * 修复：回放中跳过"停止使用物品"
 * 不会有副作用——回放期间所有真实操作被完全拦截模式挡住
 * 玩家按什么键都没用，跳过这个检查不会导致任何意外行为
 */
@Mixin(Minecraft.class)
public class EpitaphReplayMinecraftMixin {

    @Redirect(method = "handleKeybinds",at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void epitaph$preventReleaseUsingItem(MultiPlayerGameMode gameMode, Player player) {
        // 回放中→ 跳过（不停止使用物品）
        if (客户端复刻管理器.本地玩家是否输入回放中()) {
            return;
        }

        // 正常情况 → 执行原方法
        gameMode.releaseUsingItem(player);
    }
}
