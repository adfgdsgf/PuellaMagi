package com.v2t.puellamagi.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 绑定物品NBT数据通用读写工具
 *
 * 提供所有绑定物品共用的NBT操作：
 * - 所有者UUID
 * - 所有者名称
 * - 有效时间戳
 *
 * 使用统一的NBT键名，确保一致性。
 * 特定物品的额外数据（如灵魂宝石状态）由各自的数据类处理。
 */
public final class 绑定物品数据 {

    // 统一的NBT键名
    public static final String TAG_OWNER_UUID = "OwnerUUID";
    public static final String TAG_OWNER_NAME = "OwnerName";
    public static final String TAG_TIMESTAMP = "Timestamp";

    private 绑定物品数据() {}

    //==================== 读取方法 ====================

    /**
     * 获取物品绑定的所有者UUID
     */
    @Nullable
    public static UUID 获取所有者UUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(TAG_OWNER_UUID)) {
            return tag.getUUID(TAG_OWNER_UUID);
        }
        return null;
    }

    /**
     * 获取物品绑定的所有者名称
     */
    @Nullable
    public static String 获取所有者名称(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_OWNER_NAME)) {
            return tag.getString(TAG_OWNER_NAME);
        }
        return null;
    }

    /**
     * 获取物品的有效时间戳
     */
    public static long 获取时间戳(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_TIMESTAMP)) {
            return tag.getLong(TAG_TIMESTAMP);
        }
        return 0L;
    }

    // ==================== 写入方法 ====================

    /**
     * 设置物品的所有者信息
     */
    public static void 设置所有者(ItemStack stack, UUID uuid, String name) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_OWNER_UUID, uuid);
        tag.putString(TAG_OWNER_NAME, name);
    }

    /**
     * 设置物品的有效时间戳
     */
    public static void 设置时间戳(ItemStack stack, long timestamp) {
        stack.getOrCreateTag().putLong(TAG_TIMESTAMP, timestamp);
    }

    // ==================== 便捷方法 ====================

    /**
     * 初始化绑定物品的基础数据
     *
     * @param stack 物品堆
     * @param ownerUUID 所有者UUID
     * @param ownerName 所有者名称
     * @param timestamp 有效时间戳
     */
    public static void 初始化绑定(ItemStack stack, UUID ownerUUID, String ownerName, long timestamp) {
        设置所有者(stack, ownerUUID, ownerName);
        设置时间戳(stack, timestamp);
    }

    /**
     * 检查物品是否已绑定（有所有者信息）
     */
    public static boolean 是已绑定(ItemStack stack) {
        return 获取所有者UUID(stack) != null;
    }

    /**
     * 检查物品是否属于指定玩家
     */
    public static boolean 属于玩家(ItemStack stack, UUID playerUUID) {
        UUID owner = 获取所有者UUID(stack);
        return owner != null && owner.equals(playerUUID);
    }

    /**
     * 清除绑定信息（解绑时使用）
     */
    public static void 清除绑定(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_OWNER_UUID);
            tag.remove(TAG_OWNER_NAME);
            tag.remove(TAG_TIMESTAMP);
        }
    }
}
