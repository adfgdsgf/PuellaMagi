package com.v2t.puellamagi.system.soulgem.item;

import com.v2t.puellamagi.util.绑定物品数据;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 灵魂宝石NBT数据读写工具
 *
 * 基于通用绑定物品数据，添加灵魂宝石特有字段：
 * - State: 状态（冗余存储，方便客户端显示）
 *
 * 通用字段（委托给绑定物品数据）：
 * - OwnerUUID: 所有者UUID
 * - OwnerName: 所有者名称
 * - Timestamp: 有效时间戳
 */
public final class 灵魂宝石数据 {

    // 特有字段的NBT键名
    private static final String TAG_STATE = "State";

    private 灵魂宝石数据() {}

    //==================== 通用字段（委托）====================

    @Nullable
    public static UUID 获取所有者UUID(ItemStack stack) {
        return 绑定物品数据.获取所有者UUID(stack);
    }

    @Nullable
    public static String 获取所有者名称(ItemStack stack) {
        return 绑定物品数据.获取所有者名称(stack);
    }

    public static long 获取时间戳(ItemStack stack) {
        return 绑定物品数据.获取时间戳(stack);
    }

    public static void 设置所有者(ItemStack stack, UUID uuid, String name) {
        绑定物品数据.设置所有者(stack, uuid, name);
    }

    public static void 设置时间戳(ItemStack stack, long timestamp) {
        绑定物品数据.设置时间戳(stack, timestamp);
    }

    public static boolean 是已绑定宝石(ItemStack stack) {
        return 绑定物品数据.是已绑定(stack);
    }

    public static boolean 是空白宝石(ItemStack stack) {
        return !绑定物品数据.是已绑定(stack);
    }

    public static boolean 属于玩家(ItemStack stack, UUID playerUUID) {
        return 绑定物品数据.属于玩家(stack, playerUUID);
    }

    // ==================== 特有字段（状态）====================

    public static 灵魂宝石状态 获取状态(ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STATE)) {
            return 灵魂宝石状态.fromString(tag.getString(TAG_STATE));
        }
        return 灵魂宝石状态.NORMAL;
    }

    public static void 设置状态(ItemStack stack, 灵魂宝石状态 state) {
        stack.getOrCreateTag().putString(TAG_STATE, state.getSerializeName());
    }

    // ==================== 便捷方法 ====================

    /**
     * 初始化一个新的灵魂宝石
     *
     * @param stack 物品堆
     * @param ownerUUID 所有者UUID
     * @param ownerName 所有者名称
     * @param timestamp 有效时间戳（与世界数据一致）
     */
    public static void 初始化(ItemStack stack, UUID ownerUUID, String ownerName, long timestamp) {
        // 使用通用方法初始化绑定信息
        绑定物品数据.初始化绑定(stack, ownerUUID, ownerName, timestamp);
        // 设置特有字段
        设置状态(stack, 灵魂宝石状态.NORMAL);
    }
}
