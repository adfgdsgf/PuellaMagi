// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/location/灵魂宝石区块加载器.java

package com.v2t.puellamagi.system.soulgem.location;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 灵魂宝石区块加载器
 *
 * 职责：
 * - 当灵魂宝石不在玩家身上时，保持宝石所在区块加载
 * - 玩家在线时才加载，离线时释放
 *
 * 原理：
 * - 灵魂宝石必须始终可访问，才能计算距离效果
 * - 使用 ForgeChunkManager 的 Entity ticket（绑定到玩家）
 */
public final class 灵魂宝石区块加载器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SoulGemChunkLoader");

    /**
     * 当前加载的区块记录
     *玩家UUID → (维度, 区块坐标)
     */
    private static final Map<UUID, LoadedChunkInfo> 加载记录 = new ConcurrentHashMap<>();

    private 灵魂宝石区块加载器() {}

    /**
     * 区块加载信息
     */
    private record LoadedChunkInfo(ResourceKey<Level> 维度, ChunkPos 区块坐标) {}

    // ==================== 心跳更新 ====================

    /**
     * 玩家Tick时调用，更新区块加载状态
     *
     * 使用 player.tickCount 而非全局计数器
     */
    public static void onPlayerTick(ServerPlayer player) {
        // 使用玩家自己的 tickCount，每个玩家独立计数
        if (player.tickCount % 20 != 0) return;

        // 只处理灵魂宝石系玩家
        if (!能力工具.是灵魂宝石系(player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        UUID playerUUID = player.getUUID();

        宝石登记信息 info = worldData.获取登记信息(playerUUID).orElse(null);
        if (info == null) {
            // 没有登记，释放可能存在的加载
            释放区块加载(server, playerUUID);
            return;
        }

        // 判断是否需要区块加载
        if (info.获取存储类型() == 存储类型.玩家背包) {
            // 在玩家身上，不需要额外加载
            释放区块加载(server, playerUUID);
        } else {
            // 不在身上，需要加载宝石所在区块
            更新区块加载(server, playerUUID, info);
        }
    }

    // ==================== 区块加载管理 ====================

    /**
     * 更新区块加载
     */
    private static void 更新区块加载(MinecraftServer server, UUID playerUUID, 宝石登记信息 info) {
        ResourceKey<Level> 目标维度 = info.获取维度();
        Vec3 坐标 = info.获取坐标();

        if (目标维度 == null || 坐标 == null) {
            // 位置未知，无法加载
            return;
        }

        ServerLevel level = server.getLevel(目标维度);
        if (level == null) {LOGGER.warn("无法获取维度: {}", 目标维度.location());
            return;
        }

        ChunkPos newChunkPos = new ChunkPos(BlockPos.containing(坐标));
        LoadedChunkInfo currentInfo = 加载记录.get(playerUUID);

        // 检查是否需要更新
        if (currentInfo != null) {
            if (currentInfo.维度().equals(目标维度) && currentInfo.区块坐标().equals(newChunkPos)) {
                // 位置未变，不需要更新
                return;
            }// 位置变了，先释放旧的
            释放区块加载内部(server, playerUUID, currentInfo);
        }

        // 加载新区块
        boolean success = ForgeChunkManager.forceChunk(
                level,
                常量.MOD_ID,
                playerUUID,
                newChunkPos.x,
                newChunkPos.z,
                true,// add
                true   // ticking
        );

        if (success) {
            加载记录.put(playerUUID, new LoadedChunkInfo(目标维度, newChunkPos));LOGGER.debug("加载区块: 玩家={}, 维度={}, 区块={}",
                    playerUUID, 目标维度.location(), newChunkPos);
        } else {
            LOGGER.warn("区块加载失败: 玩家={}, 维度={}, 区块={}",
                    playerUUID, 目标维度.location(), newChunkPos);
        }
    }

    /**
     * 释放区块加载
     */
    public static void 释放区块加载(MinecraftServer server, UUID playerUUID) {
        LoadedChunkInfo info = 加载记录.remove(playerUUID);
        if (info != null) {
            释放区块加载内部(server, playerUUID, info);
        }
    }

    /**
     * 释放区块加载（内部方法）
     */
    private static void 释放区块加载内部(MinecraftServer server, UUID playerUUID, LoadedChunkInfo info) {
        ServerLevel level = server.getLevel(info.维度());
        if (level == null) return;

        ForgeChunkManager.forceChunk(
                level,
                常量.MOD_ID,
                playerUUID,
                info.区块坐标().x,
                info.区块坐标().z,
                false, // remove
                true
        );LOGGER.debug("释放区块: 玩家={}, 维度={}, 区块={}",
                playerUUID, info.维度().location(), info.区块坐标());
    }

    // ==================== 生命周期 ====================

    /**
     * 玩家登出时释放加载
     */
    public static void onPlayerLogout(MinecraftServer server, UUID playerUUID) {
        释放区块加载(server, playerUUID);
    }

    /**
     * 服务器关闭时清理所有加载
     */
    public static void clearAll(MinecraftServer server) {
        for (UUID playerUUID : 加载记录.keySet()) {
            释放区块加载(server, playerUUID);
        }加载记录.clear();LOGGER.info("已释放所有灵魂宝石区块加载");
    }

    // ==================== 查询 ====================

    /**
     * 检查玩家是否有区块加载
     */
    public static boolean 是否有区块加载(UUID playerUUID) {
        return 加载记录.containsKey(playerUUID);
    }

    /**
     * 获取当前加载的区块数量
     */
    public static int 获取加载数量() {
        return 加载记录.size();
    }
}
