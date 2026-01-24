// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/FeignDeathGameModeMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.api.restriction.限制类型;
import com.v2t.puellamagi.system.restriction.行动限制管理器;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 行动限制 - 服务端游戏模式拦截
 *
 * 统一调用行动限制管理器
 */
@Mixin(ServerPlayerGameMode.class)
public class FeignDeathGameModeMixin {

    @Shadow
    protected ServerPlayer player;

    /**
     * 拦截破坏方块
     */
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (行动限制管理器.是否被限制(this.player, 限制类型.破坏方块)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截挖掘动作（开始挖掘、挖掘中、停止挖掘）
     */
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreak(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                    Direction direction, int maxBuildHeight, int sequence,
                                    CallbackInfo ci) {
        if (行动限制管理器.是否被限制(this.player, 限制类型.破坏方块)) {
            ci.cancel();
        }
    }

    /**
     * 拦截右键使用物品/交互方块
     */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(ServerPlayer player, Level level, ItemStack stack,
                             InteractionHand hand, BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (行动限制管理器.是否被限制(player, 限制类型.交互方块)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    /**
     * 拦截使用物品（如吃东西、射箭等）
     */
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(ServerPlayer player, Level level, ItemStack stack,
                           InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (行动限制管理器.是否被限制(player, 限制类型.使用物品)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
