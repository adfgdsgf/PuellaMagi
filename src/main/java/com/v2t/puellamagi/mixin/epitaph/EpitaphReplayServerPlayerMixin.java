package com.v2t.puellamagi.mixin.epitaph;

import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import com.v2t.puellamagi.util.recording.实体帧数据;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/**
 * 回放期间阻止服务端清除使用物品状态
 *
 * 和客户端的EpitaphReplayMinecraftMixin一样的思路：
 * 服务端玩家tick里也会检查"你还在使用物品吗？"
 * 包回放没有"持续按着右键"→ 检查失败 → 清掉
 *
 * 这里拦住releaseUsingItem → 服务端保持使用状态
 * → sendChanges同步"在使用"给客户端 → 动画出现
 */
@Mixin(ServerPlayer.class)
public class EpitaphReplayServerPlayerMixin {

    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void epitaph$preventReleaseIfReplaying(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        UUID uuid = self.getUUID();

        if (!复刻引擎.玩家是否被锁定(uuid)) return;

        // 检查帧数据：如果帧数据说"应该在使用物品"→ 不让清除
        复刻引擎.复刻会话 session = null;
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 s = 复刻引擎.获取会话(userUUID);
            if (s != null && s.被锁定玩家.contains(uuid)) {
                session = s;
                break;
            }
        }
        if (session == null) return;

        com.v2t.puellamagi.system.ability.epitaph.预知状态管理.玩家预知状态 state =
                com.v2t.puellamagi.system.ability.epitaph.预知状态管理.获取状态(session.使用者UUID);
        if (state == null) return;

        int currentFrame = state.获取当前复刻帧();
        Map<UUID, 实体帧数据> frame = session.录制.帧数据.获取帧(currentFrame);
        if (frame == null) return;

        实体帧数据 data = frame.get(uuid);
        if (data == null) return;

        // 帧数据说在使用物品 → 不让释放
        if (data.是否使用物品()) {
            ci.cancel();
        }
    }
}
