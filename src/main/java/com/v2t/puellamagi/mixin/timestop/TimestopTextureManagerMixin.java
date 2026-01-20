// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopTextureManagerMixin.java

package com.v2t.puellamagi.mixin.timestop;

import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 纹理管理器 Mixin -冻结动态纹理
 *
 * 让岩浆、水、火焰等动态纹理在时停中停止动画
 * 注意：纹理是全局的，所以所有同类型方块都会停在同一帧
 */
@Mixin(TextureManager.class)
public class TimestopTextureManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onTick(CallbackInfo ci) {
        Entity player = Minecraft.getInstance().player;
        if (player != null && ((TimeStop) player.level()).puellamagi$inTimeStopRange(player)) {
            ci.cancel();
        }
    }
}
