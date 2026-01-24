// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/item/灵魂宝石物品.java

package com.v2t.puellamagi.system.soulgem.item;

import com.v2t.puellamagi.api.item.I绑定物品;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import com.v2t.puellamagi.system.soulgem.location.灵魂宝石区块加载器;
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
 * 实现 I绑定物品 接口，具备唯一性验证功能
 *
 * 核心机制：
 * - 有效性通过世界数据中的时间戳判断
 * - 旧宝石（时间戳不匹配）在inventoryTick中自动消失
 * - 每秒向世界数据汇报位置和持有者
 */
public class 灵魂宝石物品 extends Item implements I绑定物品 {

    public static final String BIND_TYPE = "soul_gem";

    public 灵魂宝石物品() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
        );
    }

    // ==================== I绑定物品 接口实现 ====================

    @Override
    public String 获取绑定类型() {
        return BIND_TYPE;
    }

    @Override
    @Nullable
    public UUID 获取所有者UUID(ItemStack stack) {
        return 灵魂宝石数据.获取所有者UUID(stack);
    }

    @Override
    @Nullable
    public String 获取所有者名称(ItemStack stack) {
        return 灵魂宝石数据.获取所有者名称(stack);
    }

    @Override
    public long 获取时间戳(ItemStack stack) {
        return 灵魂宝石数据.获取时间戳(stack);
    }

    @Override
    public void 当无效消失时(ItemStack stack, UUID ownerUUID) {
        stack.setCount(0);
    }

    @Override
    public boolean 是否受删除保护(ItemStack stack) {
        // 已销毁的灵魂宝石不再保护，允许被删除
        return 灵魂宝石数据.获取状态(stack) != 灵魂宝石状态.DESTROYED;
    }

    @Override
    public void 当创造模式删除时(ServerPlayer 操作者, @Nullable ServerPlayer 所有者, UUID 所有者UUID, long 时间戳) {
        if (操作者.getServer() == null) return;

        // 验证时间戳
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(操作者.getServer());
        var infoOpt = worldData.获取登记信息(所有者UUID);

        if (infoOpt.isEmpty()) {
            // 登记不存在，可能已被其他机制处理
            return;
        }

        if (infoOpt.get().获取有效时间戳() != 时间戳) {
            // 时间戳不匹配，不是当前有效宝石
            return;
        }

        if (所有者 != null) {
            // 所有者在线，根据游戏模式处理
            灵魂宝石管理器.处理宝石销毁(所有者);
        } else {
            // 所有者离线，只清理世界数据
            worldData.移除登记(所有者UUID);
            灵魂宝石区块加载器.释放区块加载(操作者.getServer(), 所有者UUID);
        }
    }

    // ==================== 物品显示 ====================

    @Override
    public Component getName(ItemStack stack) {
        灵魂宝石状态 state = 灵魂宝石数据.获取状态(stack);
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

        UUID ownerUUID = 获取所有者UUID(stack);
        if (ownerUUID == null) {
            tooltip.add(Component.literal("§8[未绑定]"));
        }

        if (flag.isAdvanced()) {
            if (ownerUUID != null) {
                tooltip.add(Component.literal("UUID: " + ownerUUID.toString().substring(0, 8) + "...")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.literal("Timestamp: " + 获取时间戳(stack))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return 灵魂宝石数据.获取状态(stack) == 灵魂宝石状态.NORMAL;
    }

    // ==================== 背包Tick ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer holder)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        UUID ownerUUID = 获取所有者UUID(stack);
        if (ownerUUID == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(serverLevel.getServer());

        long gemTimestamp = 获取时间戳(stack);
        if (!worldData.验证时间戳(ownerUUID, gemTimestamp)) {
            当无效消失时(stack, ownerUUID);
            if (ownerUUID.equals(holder.getUUID())) {
                holder.displayClientMessage(
                        Component.translatable("message.puellamagi.soul_gem.old_vanished")
                                .withStyle(ChatFormatting.GRAY),
                        true
                );
            }
            return;
        }

        if (holder.tickCount % 20 == 0) {
            worldData.更新位置(ownerUUID,
                    level.dimension(),
                    holder.position(),
                    存储类型.玩家背包,
                    holder.getUUID(),
                    level.getGameTime()
            );
        }
    }
}
