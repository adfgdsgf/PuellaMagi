// 文件路径: src/main/java/com/v2t/puellamagi/mixin/timestop/WalkAnimationStateAccessor.java

package com.v2t.puellamagi.mixin.timestop;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * WalkAnimationState 访问器
 *
 * 用于访问私有字段，以便在时停中同步动画状态
 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {

    @Accessor("speedOld")
    float getSpeedOld();

    @Accessor("speedOld")
    void setSpeedOld(float speedOld);

    // 新增：访问 speed 字段
    @Accessor("speed")
    float getSpeed();

    @Accessor("speed")
    void setSpeed(float speed);

    @Accessor("position")
    float getPosition();

    @Accessor("position")
    void setPosition(float position);
}
