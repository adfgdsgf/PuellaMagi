package com.v2t.puellamagi.system.soulgem.damage.active.impl;

import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.soulgem.damage.强度判定;
import com.v2t.puellamagi.system.soulgem.damage.损坏上下文;
import com.v2t.puellamagi.system.soulgem.damage.损坏强度;
import com.v2t.puellamagi.system.soulgem.damage.active.I主动损坏方式;
import com.v2t.puellamagi.system.soulgem.damage.active.主动损坏触发类型;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

/**
 * 撞击方块方式
 *
 * 玩家手持灵魂宝石右键坚硬方块
 *
 * 条件：
 * - 手持灵魂宝石
 * - 右键方块硬度 >=阈值
 * - 不是自己的宝石
 */
public class 撞击方块方式 implements I主动损坏方式 {

    private static final ResourceLocation ID = 资源工具.本mod("block_hit");

    /** 最小方块硬度阈值 */
    private static final float 最小硬度 = 2.0f;

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public 主动损坏触发类型 获取触发类型() {
        return 主动损坏触发类型.右键交互;
    }

    @Override
    public int 获取优先级() {
        // 优先级低于主副手组合，避免冲突
        return 30;
    }

    @Override
    public Optional<损坏上下文> 检测方块交互(ServerPlayer player, BlockPos blockPos) {
        // 检查主手或副手是否持有灵魂宝石
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        ItemStack 宝石 = null;

        if (是有效灵魂宝石(mainHand, player)) {
            宝石 = mainHand;
        } else if (是有效灵魂宝石(offHand, player)) {
            宝石 = offHand;
        }

        if (宝石 == null) {
            return Optional.empty();
        }

        // 获取方块状态
        BlockState blockState = player.level().getBlockState(blockPos);

        // 检查方块硬度
        float hardness = blockState.getBlock().defaultDestroyTime();
        if (hardness < 最小硬度 && hardness >= 0) {
            // 太软，不造成伤害（负数表示不可破坏）
            return Optional.empty();
        }

        // 根据硬度判断强度
        损坏强度 强度 = 强度判定.从方块硬度(blockState);
        if (强度 == null) {
            return Optional.empty();
        }

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(宝石);

        return Optional.of(损坏上下文.主动损坏方块(
                宝石,
                ownerUUID,
                强度,
                player,
                blockPos,
                "撞击方块: " + blockState.getBlock().getName().getString()
        ));
    }

    /**
     * 检查物品是否为有效的灵魂宝石（且不是自己的）
     */
    private boolean 是有效灵魂宝石(ItemStack stack, ServerPlayer player) {
        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return false;
        }

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null) {
            return false;
        }

        // 不能损坏自己的宝石
        if (ownerUUID.equals(player.getUUID())) {
            return false;
        }

        // 已销毁的不处理
        return 灵魂宝石数据.获取状态(stack) != 灵魂宝石状态.DESTROYED;
    }
}
