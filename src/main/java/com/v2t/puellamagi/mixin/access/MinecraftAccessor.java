package com.v2t.puellamagi.mixin.access;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Minecraft字段访问器
 *
 * rightClickDelay：按住右键时每隔几tick放一个方块的内部计数器
 * 录制时读取保存，回放时设回去 → 持续放置节奏精确一致
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("rightClickDelay")
    int puellamagi$getRightClickDelay();

    @Accessor("rightClickDelay")
    void puellamagi$setRightClickDelay(int value);
}
