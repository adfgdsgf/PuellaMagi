// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/SoulGemPassiveDestroyMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.soulgem.damage.强度判定;
import com.v2t.puellamagi.system.soulgem.damage.损坏上下文;
import com.v2t.puellamagi.system.soulgem.damage.损坏强度;
import com.v2t.puellamagi.system.soulgem.damage.灵魂宝石损坏处理器;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import com.v2t.puellamagi.util.绑定物品工具;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * 灵魂宝石被动销毁检测
 *
 * 拦截 ItemEntity 的伤害事件
 * 不硬编码具体伤害来源，根据数值判断强度
 */
@Mixin(ItemEntity.class)
public abstract class SoulGemPassiveDestroyMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Unique
    private static final double 虚空阈值Y = -64.0;

    /**
     * 拦截物品受伤（岩浆、火焰、爆炸、仙人掌、铁砧等）
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (self.level().isClientSide) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = getItem();

        if (!puellamagi$是灵魂宝石(stack)) {
            return;
        }

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null) return;

        // 已销毁的不处理，让原版删除逻辑执行
        if (灵魂宝石数据.获取状态(stack) == 灵魂宝石状态.DESTROYED) {
            return;
        }

        损坏强度 强度 = 强度判定.从伤害值(amount);

        损坏上下文 context = 损坏上下文.被动销毁(
                stack,
                ownerUUID,
                强度,
                String.format("被动伤害: %.1f", amount)
        );

        var result = 灵魂宝石损坏处理器.处理损坏(serverLevel.getServer(), context);

        // 如果销毁了，标记合法删除后移除掉落物
        if (result == 灵魂宝石损坏处理器.处理结果.销毁) {
            绑定物品工具.标记合法移除(self.getId());
            self.discard();
        }

        //阻止原版销毁逻辑
        cir.setReturnValue(false);
    }

    /**
     * 在tick 中检测虚空
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickCheckVoid(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (self.level().isClientSide) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        if (self.getY() > 虚空阈值Y) {
            return;
        }

        ItemStack stack = getItem();

        if (!puellamagi$是灵魂宝石(stack)) {
            return;
        }

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null) return;

        // 已销毁的：标记合法删除，让它正常消失
        if (灵魂宝石数据.获取状态(stack) == 灵魂宝石状态.DESTROYED) {
            绑定物品工具.标记合法移除(self.getId());
            self.discard();
            return;
        }

        //虚空 = 严重强度，直接销毁
        损坏上下文 context = 损坏上下文.被动销毁(
                stack,
                ownerUUID,
                损坏强度.严重,
                "掉入虚空"
        );

        var result = 灵魂宝石损坏处理器.处理损坏(serverLevel.getServer(), context);

        if (result == 灵魂宝石损坏处理器.处理结果.销毁) {
            绑定物品工具.标记合法移除(self.getId());
            self.discard();
        }
    }

    @Unique
    private static boolean puellamagi$是灵魂宝石(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.SOUL_GEM.get());
    }
}
