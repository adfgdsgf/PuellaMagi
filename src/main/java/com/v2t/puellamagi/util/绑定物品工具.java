// 文件路径: src/main/java/com/v2t/puellamagi/util/绑定物品工具.java

package com.v2t.puellamagi.util;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.item.I绑定物品;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 绑定物品生命周期工具类
 *
 * 职责：集中处理绑定物品在玩家生命周期中的行为
 * - 死亡保留（提取、暂存、同步）
 * - 删除保护（防止非法删除）
 * - 创造模式删除处理
 */
public final class 绑定物品工具 {

    private 绑定物品工具() {}

    // ==================== 死亡保留缓存 ====================

    private static final Map<UUID, List<SlotItem>> 死亡保留缓存 = new HashMap<>();

    private record SlotItem(int slot, ItemStack stack) {}

    // ==================== 死亡保留流程 ====================

    public static void 提取死亡保留物品(Player player) {
        UUID playerId = player.getUUID();
        List<SlotItem> 保留列表 = new ArrayList<>();
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof I绑定物品 item && item.是否死亡保留()) {
                UUID 所有者 = item.获取所有者UUID(stack);

                if (所有者 != null && 所有者.equals(playerId)) {
                    保留列表.add(new SlotItem(i, stack.copy()));
                    inv.setItem(i, ItemStack.EMPTY);
                    PuellaMagi.LOGGER.debug("死亡保留: {} (槽位 {})",stack.getDisplayName().getString(), i);
                } else {
                    PuellaMagi.LOGGER.debug("死亡不保留(非本人): {} (所有者: {})",
                            stack.getDisplayName().getString(),
                            所有者 != null ? 所有者.toString().substring(0, 8) : "null");
                }
            }
        }

        if (!保留列表.isEmpty()) {
            死亡保留缓存.put(playerId, 保留列表);
            PuellaMagi.LOGGER.debug("玩家 {} 死亡，提取 {} 个保留物品",
                    player.getName().getString(), 保留列表.size());
        }
    }

    public static void 恢复到原背包(Player player) {
        UUID playerId = player.getUUID();
        List<SlotItem> 保留列表 = 死亡保留缓存.get(playerId);
        if (保留列表 == null ||保留列表.isEmpty()) return;

        Inventory inv = player.getInventory();
        for (SlotItem item : 保留列表) {
            inv.setItem(item.slot(), item.stack());
        }}

    public static void 同步到新玩家(Player 原玩家, Player 新玩家) {
        UUID playerId = 原玩家.getUUID();

        List<SlotItem> 保留列表 = 死亡保留缓存.remove(playerId);

        if (保留列表 != null && !保留列表.isEmpty()) {
            Inventory 新背包 = 新玩家.getInventory();
            for (SlotItem item : 保留列表) {
                新背包.setItem(item.slot(), item.stack().copy());
                PuellaMagi.LOGGER.debug("死亡保留同步(缓存): {} ->槽位 {}",
                        item.stack().getDisplayName().getString(), item.slot());
            }
        } else {
            从原背包同步(原玩家, 新玩家);
        }
    }

    private static void 从原背包同步(Player 原玩家, Player 新玩家) {
        UUID playerId = 原玩家.getUUID();
        Inventory 原背包 = 原玩家.getInventory();Inventory 新背包 = 新玩家.getInventory();
        int 同步数量 = 0;

        for (int i = 0; i < 原背包.getContainerSize(); i++) {
            ItemStack stack = 原背包.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof I绑定物品 item && item.是否死亡保留()) {
                UUID 所有者 = item.获取所有者UUID(stack);

                if (所有者 != null && 所有者.equals(playerId)) {
                    新背包.setItem(i, stack.copy());
                    同步数量++;
                    PuellaMagi.LOGGER.debug("死亡保留同步(备用): {} -> 槽位 {}",
                            stack.getDisplayName().getString(), i);
                }
            }
        }

        if (同步数量 > 0) {
            PuellaMagi.LOGGER.debug("玩家 {} 死亡保留同步完成，共 {} 个物品",
                    新玩家.getName().getString(), 同步数量);
        }
    }

    // ==================== 创造模式删除处理 ====================

    /**
     * 处理创造模式删除绑定物品
     *
     * 由网络包调用，委托给具体物品类型处理
     *
     * @param 操作者 执行删除的玩家（创造模式）
     * @param 物品类型 被删除的物品类型
     * @param 绑定者UUID 物品绑定的玩家UUID
     * @param 时间戳 物品时间戳
     */
    public static void 处理创造模式删除(
            ServerPlayer 操作者,
            I绑定物品 物品类型,
            UUID 绑定者UUID,
            long 时间戳
    ) {
        if (操作者.getServer() == null) return;

        // 获取绑定者（可能在线或离线）
        ServerPlayer 绑定者 = 操作者.getServer().getPlayerList().getPlayer(绑定者UUID);

        PuellaMagi.LOGGER.info("创造模式删除绑定物品: 类型={}, 操作者={}, 绑定者={}",
                物品类型.获取绑定类型(),
                操作者.getName().getString(),
                绑定者 != null ? 绑定者.getName().getString() : 绑定者UUID.toString().substring(0, 8));

        // 委托给具体物品类型处理
        物品类型.当创造模式删除时(操作者, 绑定者, 绑定者UUID, 时间戳);
    }

    // ==================== 删除保护 ====================

    private static final ThreadLocal<Boolean> 合法拾取中 = ThreadLocal.withInitial(() -> false);

    private static final Map<Integer, Long> 合法移除标记 = new HashMap<>();

    public static void 开始合法拾取() {
        合法拾取中.set(true);
    }

    public static void 结束合法拾取() {
        合法拾取中.set(false);
    }

    public static void 标记合法移除(int entityId) {
        合法移除标记.put(entityId, System.currentTimeMillis());
    }

    private static boolean 是合法移除流程(int entityId) {
        if (合法拾取中.get()) {
            return true;
        }

        Long time = 合法移除标记.remove(entityId);
        if (time != null && System.currentTimeMillis() - time < 5000) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否应该阻止删除
     */
    public static boolean 应该阻止删除(ItemStack stack, Entity.RemovalReason reason, int entityId) {
        // 非绑定物品不保护
        if (!(stack.getItem() instanceof I绑定物品 item)) {
            return false;
        }

        // 检查物品是否需要保护（如已销毁的灵魂宝石不保护）
        if (!item.是否受删除保护(stack)) {
            return false;
        }

        // 区块卸载、维度切换：允许
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK ||
                reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER ||
                reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            return false;
        }

        // 合法移除流程中：允许
        if (是合法移除流程(entityId)) {
            return false;
        }

        // KILLED（/kill 命令）：阻止
        if (reason == Entity.RemovalReason.KILLED) {
            PuellaMagi.LOGGER.debug("阻止绑定物品被kill 命令删除: {}",
                    stack.getDisplayName().getString());
            return true;
        }

        // DISCARDED 且不在合法流程中：阻止
        if (reason == Entity.RemovalReason.DISCARDED) {
            PuellaMagi.LOGGER.debug("阻止绑定物品被非法删除: {}",
                    stack.getDisplayName().getString());
            return true;
        }

        return false;
    }

    // ==================== 缓存管理 ====================

    public static void 清理玩家缓存(UUID playerId) {
        死亡保留缓存.remove(playerId);
    }

    public static void 清理过期标记() {
        long now = System.currentTimeMillis();
        合法移除标记.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
    }

    public static void 清理所有缓存() {
        死亡保留缓存.clear();
        合法移除标记.clear();
        合法拾取中.remove();
        PuellaMagi.LOGGER.debug("绑定物品缓存已清理");
    }

    // ==================== 检测方法 ====================

    public static boolean 是死亡保留物品(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof I绑定物品 item && item.是否死亡保留();
    }

    public static boolean 是下线掉落物品(ItemStack stack, UUID 持有者UUID) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof I绑定物品 item)) return false;
        if (!item.是否下线掉落非己()) return false;

        UUID 所有者 = item.获取所有者UUID(stack);
        return 所有者 != null && !所有者.equals(持有者UUID);
    }

    public static boolean 是永不过期物品(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof I绑定物品 item && item.是否永不过期();
    }
}
