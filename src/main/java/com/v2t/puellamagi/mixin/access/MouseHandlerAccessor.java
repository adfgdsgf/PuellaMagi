package com.v2t.puellamagi.mixin.access;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * MouseHandler访问器
 * 回放时调用onPress重放鼠标点击事件
 * 回放时设置光标位置（容器操作精确还原）
 * 时间删除时清空累积鼠标delta（防止视角跳变）
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Invoker("onPress")
    void puellamagi$invokeOnPress(long window, int button, int action, int modifiers);

    @Accessor("xpos")
    void puellamagi$setXPos(double x);

    @Accessor("ypos")
    void puellamagi$setYPos(double y);

    @Accessor("accumulatedDX")
    void puellamagi$setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    void puellamagi$setAccumulatedDY(double value);
}
