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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * 主副手组合方式
 *
 * 玩家一只手拿灵魂宝石，另一只手拿武器，按右键
 *
 * 条件：
 * - 主手灵魂宝石 + 副手武器
 * - 或 副手灵魂宝石 + 主手武器
 * - 不是自己的宝石
 */
public class 主副手组合方式 implements I主动损坏方式 {

    private static final ResourceLocation ID = 资源工具.本mod("hand_combo");

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
        return 20;
    }

    @Override
    public Optional<损坏上下文> 检测主副手组合(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 检测两种组合
        检测结果 result = 检测组合(player, mainHand, offHand);
        if (result == null) {
            result = 检测组合(player, offHand, mainHand);
        }

        if (result == null) {
            return Optional.empty();
        }

        return Optional.of(损坏上下文.主动损坏(
                result.宝石,
                result.所有者UUID,
                result.强度,
                player,
                "主副手组合"
        ));
    }

    /**
     * 检测一种组合：潜在宝石 + 潜在武器
     */
    private 检测结果 检测组合(ServerPlayer player, ItemStack 潜在宝石, ItemStack 潜在武器) {
        // 检查是否为灵魂宝石
        if (潜在宝石.isEmpty() || !潜在宝石.is(ModItems.SOUL_GEM.get())) {
            return null;
        }

        // 检查是否为武器
        if (!强度判定.是武器(潜在武器)) {
            return null;
        }

        // 获取所有者
        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(潜在宝石);
        if (ownerUUID == null) {
            return null;
        }

        // 不能损坏自己的宝石
        if (ownerUUID.equals(player.getUUID())) {
            return null;
        }

        // 已销毁的不处理
        if (灵魂宝石数据.获取状态(潜在宝石) == 灵魂宝石状态.DESTROYED) {
            return null;
        }

        // 根据武器判断强度
        损坏强度 强度 = 强度判定.从武器(潜在武器);
        if (强度 == null) {
            强度 = 损坏强度.普通;
        }

        return new 检测结果(潜在宝石, ownerUUID, 强度);
    }

    /**
     * 检测结果内部类
     */
    private record 检测结果(ItemStack 宝石, UUID 所有者UUID, 损坏强度 强度) {}
}
