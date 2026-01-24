// 文件路径: src/main/java/com/v2t/puellamagi/api/item/I绑定物品.java

package com.v2t.puellamagi.api.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 绑定物品接口
 *
 * 定义与玩家绑定、具有唯一性的物品的通用行为。
 *
 * 应用场景：
 * - 灵魂宝石（灵魂宝石系）
 * - 心之种/变身器（心之种系）
 * - 其他需要绑定玩家的特殊物品
 *
 * 特性：
 * - 唯一性：时间戳机制防止复制
 * - 永不过期：作为掉落物时不会自然消失
 * - 死亡保留：玩家死亡时不掉落
 */
public interface I绑定物品 {

    /**
     * 获取绑定物品的类型标识
     * 用于区分不同类型的绑定物品（如灵魂宝石、心之种等）
     *
     * @return 类型标识字符串，如 "soul_gem", "heart_seed"
     */
    String 获取绑定类型();

    /**
     * 检查物品是否已绑定玩家
     */
    default boolean 是已绑定(ItemStack stack) {
        return 获取所有者UUID(stack) != null;
    }

    /**
     * 获取绑定的玩家UUID
     */
    @Nullable
    UUID 获取所有者UUID(ItemStack stack);

    /**
     * 获取绑定的玩家名称（用于显示）
     */
    @Nullable
    String 获取所有者名称(ItemStack stack);

    /**
     * 获取物品的有效时间戳
     */
    long 获取时间戳(ItemStack stack);

    /**
     * 当物品因时间戳无效而需要消失时调用
     * 子类可重写以添加额外处理（如播放音效、发送消息等）
     *
     * @param stack 即将消失的物品
     * @param ownerUUID 物品所有者UUID
     */
    default void 当无效消失时(ItemStack stack, UUID ownerUUID) {
        stack.setCount(0);
    }

    //==================== 创造模式删除回调 ====================

    /**
     * 当物品在创造模式被删除时调用
     *
     * @param 操作者 执行删除操作的玩家（创造模式）
     * @param 绑定者 物品的绑定玩家（可能在线或离线）
     * @param 绑定者UUID 绑定玩家的UUID
     * @param 时间戳 物品的时间戳
     */
    default void 当创造模式删除时(ServerPlayer 操作者, @Nullable ServerPlayer 绑定者, UUID 绑定者UUID, long 时间戳) {
        // 默认无操作，子类重写
    }

    // ==================== 掉落物行为 ====================

    /**
     * 当前物品是否应该受到删除保护
     *
     * 默认为true，子类可根据状态重写
     * 例如：已销毁的灵魂宝石不需要保护
     *
     * @param stack 物品堆
     * @return 是否应该保护
     */
    default boolean 是否受删除保护(ItemStack stack) {
        return true;
    }

    /**
     * 是否永不过期（作为掉落物时）
     *
     * 绑定物品通常很重要，默认永不过期
     * 子类可根据状态重写（如已销毁的灵魂宝石可以过期）
     */
    default boolean 是否永不过期() {
        return true;
    }

    /**
     * 是否死亡时保留在背包（不掉落）
     *
     * 绑定物品默认死亡保留，防止丢失
     */
    default boolean 是否死亡保留() {
        return true;
    }

    /**
     * 玩家下线时是否掉落非自己的绑定物品
     *
     * 默认为true，防止别人拿着你的核心物品下线
     */
    default boolean 是否下线掉落非己() {
        return true;
    }
}
