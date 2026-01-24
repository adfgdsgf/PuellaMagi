package com.v2t.puellamagi.mixin.soulgem.client;

import com.v2t.puellamagi.core.network.packets.c2s.灵魂宝石损坏请求包;
import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.soulgem.damage.active.主动损坏触发类型;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灵魂宝石主动使用检测（客户端）
 *
 * 拦截右键，检测：
 * 1. 主副手组合（一手宝石一手武器）
 * 2.撞击方块（手持宝石右键坚硬方块）
 */
@Mixin(Minecraft.class)
public class SoulGemActiveUseMixin {

    @Shadow
    public HitResult hitResult;

    /**
     * 在 startUseItem 开始时检测
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onStartUseItem(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;

        if (player == null || player.level() == null) return;

        // 假死状态不处理
        if (假死状态处理器.客户端是否假死中()) {
            return;
        }

        // 检测主副手组合
        if (puellamagi$检测主副手组合(player)) {
            网络工具.发送到服务端(new 灵魂宝石损坏请求包(主动损坏触发类型.右键交互));

            // 播放挥动动画
            player.swing(player.getUsedItemHand());

            ci.cancel();
            return;
        }

        // 检测撞击方块
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHit.getBlockPos();

            if (puellamagi$检测撞击方块(player)) {
                网络工具.发送到服务端(new 灵魂宝石损坏请求包(
                        主动损坏触发类型.右键交互,
                        blockPos
                ));

                // 播放挥动动画
                player.swing(player.getUsedItemHand());

                ci.cancel();
            }
        }
    }

    /**
     * 检测主副手组合
     *
     * 条件：一只手灵魂宝石，另一只手武器
     */
    @Unique
    private boolean puellamagi$检测主副手组合(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 主手宝石 + 副手武器
        if (puellamagi$是灵魂宝石(mainHand) && puellamagi$是武器(offHand)) {
            return true;
        }

        // 副手宝石 + 主手武器
        if (puellamagi$是灵魂宝石(offHand) && puellamagi$是武器(mainHand)) {
            return true;
        }

        return false;
    }

    /**
     * 检测撞击方块
     *
     * 条件：手持灵魂宝石（且没有形成主副手组合）
     */
    @Unique
    private boolean puellamagi$检测撞击方块(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 排除主副手组合的情况（优先级更高）
        if (puellamagi$检测主副手组合(player)) {
            return false;
        }

        // 任意一只手持有灵魂宝石
        return puellamagi$是灵魂宝石(mainHand) || puellamagi$是灵魂宝石(offHand);
    }

    /**
     * 检查是否为灵魂宝石
     */
    @Unique
    private boolean puellamagi$是灵魂宝石(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.SOUL_GEM.get());
    }

    /**
     * 检查是否为武器（简单判断）
     *
     * 客户端只做初步判断，详细验证在服务端
     */
    @Unique
    private boolean puellamagi$是武器(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 简单判断：有攻击伤害属性的物品
        // 详细判断在服务端的强度判定.是武器()
        return stack.getItem() instanceof net.minecraft.world.item.TieredItem;
    }
}
