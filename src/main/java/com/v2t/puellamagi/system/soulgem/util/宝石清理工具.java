// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/util/宝石清理工具.java

package com.v2t.puellamagi.system.soulgem.util;

import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.mixin.access.ChunkMapAccessor;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 宝石清理工具
 *
 * 职责：删除/查找灵魂宝石
 *
 * 设计原则：
 * - 单一职责：只负责宝石的查找和删除
 * - 纯工具类：无状态，所有方法静态
 */
public final class 宝石清理工具 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SoulGemCleanup");

    private 宝石清理工具() {}

    // ==================== 删除旧宝石 ====================

    /**
     * 删除指定时间戳的旧宝石
     *
     * 扫描范围：
     * 1. 所有在线玩家的背包
     * 2. 所有维度的已加载区块中的掉落物
     * 3. 所有维度的已加载区块中的容器
     *
     * @param server 服务器实例
     * @param ownerUUID 宝石所有者UUID
     * @param oldTimestamp 要删除的时间戳
     * @return 删除的数量
     */
    public static int 删除旧宝石(MinecraftServer server, UUID ownerUUID, long oldTimestamp) {
        int count = 0;

        // 1. 扫描所有在线玩家的背包
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            count += 删除背包中的旧宝石(player, ownerUUID, oldTimestamp);
        }

        // 2. 扫描所有维度的已加载区块
        for (ServerLevel level : server.getAllLevels()) {
            count += 删除维度中的旧宝石(level, ownerUUID, oldTimestamp);
        }

        if (count > 0) {
            LOGGER.debug("清理了 {} 个旧灵魂宝石 (所有者: {}, 时间戳: {})",count, ownerUUID.toString().substring(0, 8), oldTimestamp);
        }

        return count;
    }

    /**
     * 删除玩家背包中的旧宝石
     */
    private static int 删除背包中的旧宝石(ServerPlayer player, UUID ownerUUID, long oldTimestamp) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (是目标宝石(stack, ownerUUID, oldTimestamp)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                count++;
            }
        }
        return count;
    }

    /**
     * 删除维度中的旧宝石（掉落物 + 容器）
     */
    private static int 删除维度中的旧宝石(ServerLevel level, UUID ownerUUID, long oldTimestamp) {
        int count = 0;

        // 扫描掉落物
        count += 删除掉落物中的旧宝石(level, ownerUUID, oldTimestamp);

        // 扫描已加载区块中的容器
        count += 删除容器中的旧宝石(level, ownerUUID, oldTimestamp);

        return count;
    }

    /**
     * 删除掉落物中的旧宝石
     */
    private static int 删除掉落物中的旧宝石(ServerLevel level, UUID ownerUUID, long oldTimestamp) {
        int count = 0;

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class,
                level.getWorldBorder().getCollisionShape().bounds())) {
            ItemStack stack = itemEntity.getItem();
            if (是目标宝石(stack, ownerUUID, oldTimestamp)) {
                itemEntity.discard();
                count++;
            }
        }

        return count;
    }

    /**
     * 删除容器中的旧宝石
     */
    private static int 删除容器中的旧宝石(ServerLevel level, UUID ownerUUID, long oldTimestamp) {
        int count = 0;

        for (LevelChunk chunk : 获取已加载区块(level)) {
            for (Map.Entry<net.minecraft.core.BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                BlockEntity be = entry.getValue();
                if (be instanceof Container container) {
                    count += 删除单个容器中的旧宝石(container, ownerUUID, oldTimestamp);
                }
            }
        }

        return count;
    }

    /**
     * 删除单个容器中的旧宝石
     */
    private static int 删除单个容器中的旧宝石(Container container, UUID ownerUUID, long oldTimestamp) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (是目标宝石(stack, ownerUUID, oldTimestamp)) {
                container.setItem(i, ItemStack.EMPTY);
                container.setChanged();
                count++;
            }
        }
        return count;
    }

    // ==================== 查找宝石 ====================

    /**
     * 在玩家背包中查找其拥有的有效灵魂宝石
     *
     * @param player 玩家
     * @param validTimestamp 有效时间戳
     * @return 找到的灵魂宝石，未找到返回null
     */
    public static ItemStack 查找背包中的有效宝石(ServerPlayer player, long validTimestamp) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (是目标宝石(stack, player.getUUID(), validTimestamp)) {
                return stack;
            }
        }
        return null;
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断是否为目标宝石
     */
    private static boolean 是目标宝石(ItemStack stack, UUID ownerUUID, long timestamp) {
        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return false;
        }

        UUID gemOwner = 灵魂宝石数据.获取所有者UUID(stack);
        if (!ownerUUID.equals(gemOwner)) {
            return false;
        }

        long gemTimestamp = 灵魂宝石数据.获取时间戳(stack);
        return gemTimestamp == timestamp;
    }

    /**
     * 判断是否为指定所有者的灵魂宝石（不检查时间戳）
     */
    public static boolean 是玩家的宝石(ItemStack stack, UUID ownerUUID) {
        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return false;
        }
        UUID gemOwner = 灵魂宝石数据.获取所有者UUID(stack);
        return ownerUUID.equals(gemOwner);
    }

    /**
     * 获取维度中所有已加载的区块
     */
    private static Iterable<LevelChunk> 获取已加载区块(ServerLevel level) {
        List<LevelChunk> chunks = new ArrayList<>();

        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        Iterable<ChunkHolder> holders = ((ChunkMapAccessor) chunkMap).puellamagi$getChunks();

        for (ChunkHolder holder : holders) {
            LevelChunk chunk = holder.getFullChunk();
            if (chunk != null) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }
}
