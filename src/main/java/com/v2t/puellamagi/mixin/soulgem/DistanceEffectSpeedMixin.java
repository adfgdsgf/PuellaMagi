// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/DistanceEffectSpeedMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.system.soulgem.effect.距离效果处理器;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 距离效果 - 移速修改
 * 通过属性修改器实现，不使用药水效果
 */
@Mixin(LivingEntity.class)
public class DistanceEffectSpeedMixin {

    @Unique
    private static final UUID PUELLAMAGI_SPEED_MODIFIER_UUID =
            UUID.fromString("b3f29e7a-5c1d-4e8f-9a2b-1c3d4e5f6789");

    @Unique
    private static final String PUELLAMAGI_SPEED_MODIFIER_NAME = "puellamagi:distance_speed_penalty";

    /**
     * 在tick结束时更新移速修改器
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        // 只在服务端处理
        if (player.level().isClientSide) {
            return;
        }

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        // 移除旧的修改器
        AttributeModifier oldModifier = speedAttr.getModifier(PUELLAMAGI_SPEED_MODIFIER_UUID);
        if (oldModifier != null) {
            speedAttr.removeModifier(oldModifier);
        }

        // 检查是否需要应用效果
        if (!距离效果处理器.需要应用距离效果(player)) {
            return;
        }

        // 计算减速比例
        float speedMultiplier = 距离效果处理器.获取移速倍率(player);
        if (speedMultiplier >= 1.0f) {
            return;
        }

        // 添加新的修改器（负数减速）
        double reduction = speedMultiplier - 1.0;  // 如0.8 - 1.0 = -0.2
        AttributeModifier newModifier = new AttributeModifier(
                PUELLAMAGI_SPEED_MODIFIER_UUID,PUELLAMAGI_SPEED_MODIFIER_NAME,
                reduction,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        speedAttr.addTransientModifier(newModifier);
    }
}
