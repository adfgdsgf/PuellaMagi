package com.v2t.puellamagi.mixin.epitaph.client;

import com.v2t.puellamagi.client.客户端复刻管理器;
import com.v2t.puellamagi.mixin.access.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 在MC消费按键之前拍快照
 *
 * handleKeybinds() 里调 consumeClick() 会把clickCount 清零
 * 我们在它之前把所有按键状态存到客户端复刻管理器里
 * 录制上报时从那里读
 */
@Mixin(Minecraft.class)
public class EpitaphKeySnapshotMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void epitaph$snapshotKeysBeforeConsume(CallbackInfo ci) {
        if (!客户端复刻管理器.是否录制中()) {
            客户端复刻管理器.清除按键快照();
            return;
        }

        Map<String, int[]> snapshot = new HashMap<>();

        Map<String, KeyMapping> allKeys = KeyMappingAccessor.puellamagi$getAll();
        for (Map.Entry<String, KeyMapping> entry : allKeys.entrySet()) {
            KeyMapping key = entry.getValue();
            boolean isDown = key.isDown();
            int clickCount = ((KeyMappingAccessor) key).puellamagi$getClickCount();

            if (isDown || clickCount > 0) {
                snapshot.put(entry.getKey(), new int[]{isDown ? 1 : 0, clickCount});
            }
        }

        客户端复刻管理器.设置按键快照(snapshot);
    }
}
