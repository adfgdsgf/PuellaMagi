// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/TimestopPauseMixin.java

package com.v2t.puellamagi.mixin.timestop;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 暂时不需要这个Mixin
 * 保留空类避免mixin配置报错，或者从配置中移除
 */
@Mixin(Minecraft.class)
public class TimestopPauseMixin {
    // 不做任何处理，让ESC正常工作
}
