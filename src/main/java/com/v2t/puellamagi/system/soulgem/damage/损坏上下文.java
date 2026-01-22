package com.v2t.puellamagi.system.soulgem.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 损坏上下文
 *
 * 包含一次损坏事件的所有信息，不可变对象
 */
public record 损坏上下文(
        ItemStack 灵魂宝石,
        UUID 所有者UUID,
        损坏强度 强度,
        boolean 是主动损坏,
        @Nullable Entity 来源实体,
        @Nullable BlockPos 相关方块位置,
        String 描述
) {
    // ==================== 工厂方法 ====================

    /**
     * 创建主动损坏上下文（有攻击者）
     */
    public static 损坏上下文 主动损坏(ItemStack 宝石, UUID 所有者, 损坏强度 强度,Entity 攻击者, String 描述) {
        return new 损坏上下文(宝石, 所有者, 强度, true, 攻击者, null, 描述);
    }

    /**
     * 创建主动损坏上下文（与方块相关）
     */
    public static 损坏上下文 主动损坏方块(ItemStack 宝石, UUID 所有者, 损坏强度 强度,
                                          Entity 操作者, BlockPos 方块位置, String 描述) {
        return new 损坏上下文(宝石, 所有者, 强度, true, 操作者, 方块位置, 描述);
    }

    /**
     * 创建被动销毁上下文
     */
    public static 损坏上下文 被动销毁(ItemStack 宝石, UUID 所有者, 损坏强度 强度, String 描述) {
        return new 损坏上下文(宝石, 所有者, 强度, false, null, null, 描述);
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取攻击者（如果是玩家）
     */
    @Nullable
    public ServerPlayer 获取攻击者玩家() {
        if (来源实体 instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    /**
     * 是否有明确的攻击者
     */
    public boolean 有攻击者() {
        return 来源实体 != null;
    }

    @Override
    public String toString() {
        return String.format("损坏上下文[所有者=%s, 强度=%s, 主动=%s, 描述=%s]",
                所有者UUID.toString().substring(0, 8),
                强度.获取序列化名(),
                是主动损坏,
                描述);
    }
}
