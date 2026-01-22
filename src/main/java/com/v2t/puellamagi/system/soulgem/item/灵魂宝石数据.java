package com.v2t.puellamagi.system.soulgem.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 灵魂宝石NBT数据读写工具
 *
 * 简化后的NBT结构（核心数据在世界数据中）：
 * - OwnerUUID: 所有者UUID（校验用）
 * - OwnerName: 所有者名称（显示用）
 * - Timestamp: 有效时间戳（校验用，与世界数据比对）
 * - State: 状态（冗余存储，方便客户端显示）
 */
public final class 灵魂宝石数据 {

    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_TIMESTAMP = "Timestamp";
    private static final String TAG_STATE = "State";

    private 灵魂宝石数据() {}

    //==================== 读取方法 ====================

    @Nullable
    public static UUID 获取所有者UUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(TAG_OWNER_UUID)) {
            return tag.getUUID(TAG_OWNER_UUID);
        }
        return null;
    }

    @Nullable
    public static String 获取所有者名称(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_OWNER_NAME)) {
            return tag.getString(TAG_OWNER_NAME);
        }
        return null;
    }

    public static long 获取时间戳(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_TIMESTAMP)) {
            return tag.getLong(TAG_TIMESTAMP);
        }
        return 0L;
    }

    public static 灵魂宝石状态 获取状态(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STATE)) {
            return 灵魂宝石状态.fromString(tag.getString(TAG_STATE));
        }
        return 灵魂宝石状态.NORMAL;
    }

    // ==================== 写入方法 ====================

    public static void 设置所有者(ItemStack stack, UUID uuid, String name) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_OWNER_UUID, uuid);
        tag.putString(TAG_OWNER_NAME, name);
    }

    public static void 设置时间戳(ItemStack stack, long timestamp) {
        stack.getOrCreateTag().putLong(TAG_TIMESTAMP, timestamp);
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
        设置所有者(stack, ownerUUID, ownerName);
        设置时间戳(stack, timestamp);
        设置状态(stack, 灵魂宝石状态.NORMAL);
    }

    /**
     * 检查是否为已绑定的灵魂宝石（有所有者信息）
     */
    public static boolean 是已绑定宝石(ItemStack stack) {
        return 获取所有者UUID(stack) != null;
    }

    /**
     * 检查是否为空白宝石（/give获得的，无所有者）
     */
    public static boolean 是空白宝石(ItemStack stack) {
        return 获取所有者UUID(stack) == null;
    }

    /**
     * 检查该宝石是否属于指定玩家
     */
    public static boolean 属于玩家(ItemStack stack, UUID playerUUID) {
        UUID owner = 获取所有者UUID(stack);
        return owner != null && owner.equals(playerUUID);
    }
}
