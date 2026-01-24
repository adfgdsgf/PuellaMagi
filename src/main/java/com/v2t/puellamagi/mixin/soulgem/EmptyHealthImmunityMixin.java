//文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/EmptyHealthImmunityMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.system.adaptation.适应管理器;
import com.v2t.puellamagi.system.adaptation.source.空血假死适应源;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 伤害处理 Mixin
 *
 * 职责：
 * 1. 检查适应系统免疫
 * 2. 血量变成0 时触发假死和适应
 * 3. 致命伤害（/kill、虚空）标记并放行
 */
@Mixin(LivingEntity.class)
public abstract class EmptyHealthImmunityMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/EmptyHealth");

    /**
     * 受伤前：检查适应免疫 / 标记致命伤害
     */
    @Inject(method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void puellamagi$onHurtHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;

        if (!能力工具.是灵魂宝石系(player)) return;

        if (能力工具.应该跳过限制(player)) return;

        // 致命伤害：标记并放行，让玩家正常死亡
        if (能力工具.是致命伤害(source)) {
            假死状态处理器.标记致命伤害(player.getUUID());
            LOGGER.info("玩家 {} 受到致命伤害 {}，标记放行",
                    player.getName().getString(), source.type().msgId());
            return;  // 不拦截，让伤害正常处理
        }

        // 检查适应系统是否免疫
        if (适应管理器.是否免疫(player, source)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 受伤后：检查是否需要进入假死 / 触发适应
     */
    @Inject(
            method = "hurt",
            at = @At("RETURN")
    )
    private void puellamagi$onHurtReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        if (!能力工具.是灵魂宝石系(player)) return;

        if (能力工具.应该跳过限制(player)) return;

        // 致命伤害已标记，不进入假死
        if (假死状态处理器.是致命伤害中(player.getUUID())) {
            return;
        }

        // 血量 <= 0
        if (player.getHealth() <= 0) {
            // 环境伤害触发适应（不管是否已在假死中）
            if (puellamagi$是环境伤害(source)) {LOGGER.debug("环境伤害 {}，触发适应", source.type().msgId());
                适应管理器.通过源触发适应(player, source,空血假死适应源.ID, null);
            }

            // 只有第一次进入假死
            if (!能力工具.是否空血假死(player)) {
                LOGGER.info("玩家 {} 血量归零，进入空血假死", player.getName().getString());
                假死状态处理器.因空血进入假死(player);
            }
        }
    }

    /**
     * 判断是否为环境伤害（无攻击者）
     */
    @Unique
    private boolean puellamagi$是环境伤害(DamageSource source) {
        if (source.getDirectEntity() != null) {
            return false;
        }
        if (source.getEntity() != null) {
            return false;
        }
        return true;
    }
}
