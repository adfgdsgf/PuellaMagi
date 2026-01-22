// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/item/灵魂宝石物品.java

package com.v2t.puellamagi.system.soulgem.item;

import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 灵魂宝石物品
 *
 * 灵魂宝石系魔法少女的灵魂本体
 *
 * 核心机制：
 * - 有效性通过世界数据中的时间戳判断
 * - 旧宝石（时间戳不匹配）在inventoryTick中自动消失
 * - 每秒向世界数据汇报位置和持有者
 */
public class 灵魂宝石物品 extends Item {

    public 灵魂宝石物品() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
        );
    }

    @Override
    public Component getName(ItemStack stack) {灵魂宝石状态 state = 灵魂宝石数据.获取状态(stack);
        String ownerName = 灵魂宝石数据.获取所有者名称(stack);

        if (state == 灵魂宝石状态.DESTROYED) {
            if (ownerName != null) {
                return Component.translatable("item.puellamagi.soul_gem.destroyed.named", ownerName);
            }
            return Component.translatable("item.puellamagi.soul_gem.destroyed");
        }

        if (state == 灵魂宝石状态.CRACKED) {
            if (ownerName != null) {
                return Component.translatable("item.puellamagi.soul_gem.cracked.named", ownerName);
            }
            return Component.translatable("item.puellamagi.soul_gem.cracked");
        }

        if (ownerName != null) {
            return Component.translatable("item.puellamagi.soul_gem.named", ownerName);
        }
        return Component.translatable("item.puellamagi.soul_gem");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        灵魂宝石状态 state = 灵魂宝石数据.获取状态(stack);

        switch (state) {
            case NORMAL -> tooltip.add(Component.translatable("tooltip.puellamagi.soul_gem.normal")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            case CRACKED -> tooltip.add(Component.translatable("tooltip.puellamagi.soul_gem.cracked")
                    .withStyle(ChatFormatting.RED));
            case DESTROYED -> tooltip.add(Component.translatable("tooltip.puellamagi.soul_gem.destroyed")
                    .withStyle(ChatFormatting.DARK_RED));
        }

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null) {
            tooltip.add(Component.literal("§8[未绑定]"));
        }

        if (flag.isAdvanced()) {
            if (ownerUUID != null) {
                tooltip.add(Component.literal("UUID: " + ownerUUID.toString().substring(0, 8) + "...")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }tooltip.add(Component.literal("Timestamp: " + 灵魂宝石数据.获取时间戳(stack))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return 灵魂宝石数据.获取状态(stack) == 灵魂宝石状态.NORMAL;
    }

    /**
     * 背包tick
     *
     * 职责：
     * 1. 验证有效性（时间戳与世界数据比对）
     * 2. 汇报位置和持有者到世界数据
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // 只在服务端处理
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer holder)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 获取宝石所有者
        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);

        // 空白宝石不处理
        if (ownerUUID == null) return;

        // 获取世界数据
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(serverLevel.getServer());

        // 1. 验证有效性
        long gemTimestamp = 灵魂宝石数据.获取时间戳(stack);
        if (!worldData.验证时间戳(ownerUUID, gemTimestamp)) {
            // 时间戳不匹配 =旧宝石，消失
            stack.setCount(0);
            // 只对所有者本人显示消息
            if (ownerUUID.equals(holder.getUUID())) {
                holder.displayClientMessage(
                        Component.translatable("message.puellamagi.soul_gem.old_vanished")
                                .withStyle(ChatFormatting.GRAY),
                        true
                );
            }
            return;
        }

        // 2. 每秒汇报位置和持有者（每20tick）
        if (holder.tickCount % 20 == 0) {
            worldData.更新位置(
                    ownerUUID,
                    level.dimension(),
                    holder.position(),
                    存储类型.玩家背包,
                    holder.getUUID()  // 当前持有者
            );
        }
    }
}
