// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/DistanceEffectDamageMixin.java

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
 * 距离效果 - 攻击力减少（模拟虚弱）
 */
@Mixin(LivingEntity.class)
public class DistanceEffectDamageMixin {

    @Unique
    private static final UUID PUELLAMAGI_DAMAGE_MODIFIER_UUID =
            UUID.fromString("c4f39e8b-6d2e-5f9a-ab3c-2d4e5f6a7890");

    @Unique
    private static final String PUELLAMAGI_DAMAGE_MODIFIER_NAME = "puellamagi:distance_damage_penalty";

    @Unique
    private static final double DAMAGE_REDUCTION = -0.5;  // 攻击力减少50%

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickDamage(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr == null) {
            return;
        }

        // 移除旧的修改器
        AttributeModifier oldModifier = damageAttr.getModifier(PUELLAMAGI_DAMAGE_MODIFIER_UUID);
        if (oldModifier != null) {
            damageAttr.removeModifier(oldModifier);
        }

        // 检查是否需要减少攻击力
        if (!距离效果处理器.应该减少攻击力(player)) {
            return;
        }

        // 添加新的修改器
        AttributeModifier newModifier = new AttributeModifier(
                PUELLAMAGI_DAMAGE_MODIFIER_UUID,
                PUELLAMAGI_DAMAGE_MODIFIER_NAME,
                DAMAGE_REDUCTION,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        damageAttr.addTransientModifier(newModifier);
    }
}
