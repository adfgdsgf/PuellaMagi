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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * 掉落物攻击方式
 *
 * 玩家左键攻击地上的灵魂宝石掉落物
 *
 * 条件：
 * - 目标是灵魂宝石掉落物
 * - 不是自己的宝石
 * - （可选）手持武器
 */
public class 掉落物攻击方式 implements I主动损坏方式 {

    private static final ResourceLocation ID = 资源工具.本mod("drop_attack");

    /** 是否要求手持武器 */
    private static final boolean 要求武器 = false;

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public 主动损坏触发类型 获取触发类型() {
        return 主动损坏触发类型.左键攻击;
    }

    @Override
    public int 获取优先级() {
        return 10;
    }

    @Override
    public Optional<损坏上下文> 检测掉落物攻击(ServerPlayer player, ItemEntity target) {
        ItemStack targetStack = target.getItem();

        // 验证是灵魂宝石
        if (targetStack.isEmpty() || !targetStack.is(ModItems.SOUL_GEM.get())) {
            return Optional.empty();
        }

        // 获取所有者
        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(targetStack);
        if (ownerUUID == null) {
            return Optional.empty();
        }

        // 不能攻击自己的宝石
        if (ownerUUID.equals(player.getUUID())) {
            return Optional.empty();
        }

        // 已销毁的不处理
        if (灵魂宝石数据.获取状态(targetStack) == 灵魂宝石状态.DESTROYED) {
            return Optional.empty();
        }

        // 检查武器（可选）
        ItemStack weapon = player.getMainHandItem();
        损坏强度 强度;

        if (要求武器) {
            if (!强度判定.是武器(weapon)) {
                return Optional.empty();
            }
            强度 = 强度判定.从武器(weapon);
            if (强度 == null) {
                强度 = 损坏强度.普通;
            }
        } else {
            // 不要求武器时，根据手持物品判断强度
            强度 = 强度判定.从武器(weapon);
            if (强度 == null) {
                // 空手或非武器，使用轻微强度
                强度 = 损坏强度.轻微;
            }
        }

        // 构建上下文
        return Optional.of(损坏上下文.主动损坏(targetStack,
                ownerUUID,
                强度,
                player,
                "掉落物攻击"
        ));
    }
}
