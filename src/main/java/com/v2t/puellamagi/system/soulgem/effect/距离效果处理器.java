// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/effect/距离效果处理器.java

package com.v2t.puellamagi.system.soulgem.effect;

import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.util.灵魂宝石距离计算;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 距离效果处理器
 *
 * 职责：
 * - 管理玩家的持有状态缓存
 * - 根据距离应用debuff
 * - 触发假死状态
 *
 * 设计原则：
 * - 距离计算委托给 灵魂宝石距离计算 工具类
 * - 本类只负责状态管理和效果应用
 */
public final class 距离效果处理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/DistanceEffect");

    /** 缓存每个玩家的当前状态 */
    private static final Map<UUID, 持有状态> 状态缓存 = new ConcurrentHashMap<>();

    private 距离效果处理器() {}

    //==================== 状态查询（供Mixin使用） ====================

    /**
     * 获取玩家当前的持有状态
     */
    public static 持有状态 获取当前状态(Player player) {
        if (player == null) return 持有状态.正常;
        return 状态缓存.getOrDefault(player.getUUID(), 持有状态.正常);
    }

    /**
     * 获取移速倍率
     * 供Mixin修改移速属性使用
     *
     * @return 1.0 = 正常，0.8 = 减速20%，0.5 = 减速50%
     */
    public static float 获取移速倍率(Player player) {
        if (player == null) return 1.0f;

        if (能力工具.应该跳过限制(player)) {
            return 1.0f;
        }

        持有状态 state = 获取当前状态(player);
        return 1.0f - state.获取移速减少比例();
    }

    /**
     * 是否应该应用虚弱效果（攻击力减少）
     */
    public static boolean 应该减少攻击力(Player player) {
        if (player == null) return false;

        if (能力工具.应该跳过限制(player)) {
            return false;
        }

        持有状态 state = 获取当前状态(player);
        return state == 持有状态.远距离;
    }

    /**
     * 当前状态是否暂停污浊度恢复
     */
    public static boolean 是否暂停污浊度恢复(Player player) {
        if (player == null) return false;

        持有状态 state = 获取当前状态(player);
        return state.是否暂停污浊度恢复();
    }

    /**
     * 是否为灵魂宝石系且需要应用距离效果
     */
    public static boolean 需要应用距离效果(Player player) {
        if (player == null) return false;
        if (能力工具.应该跳过限制(player)) return false;
        if (!能力工具.是灵魂宝石系(player)) return false;

        持有状态 state = 获取当前状态(player);
        return state != 持有状态.正常;
    }

    // ==================== 主处理方法 ====================

    /**
     * 每秒调用，处理距离效果
     *
     * 使用 player.tickCount 而非全局计数器，避免多人环境下的计数混乱
     */
    public static void onPlayerTick(ServerPlayer player) {
        // 使用玩家自己的 tickCount，每个玩家独立计数
        if (player.tickCount % 20 != 0) return;

        if (!能力工具.是灵魂宝石系(player)) return;

        // 创造模式：清除效果并强制退出假死
        if (能力工具.应该跳过限制(player)) {
            状态缓存.put(player.getUUID(), 持有状态.正常);
            if (假死状态处理器.是否假死中(player)) {
                假死状态处理器.强制退出(player);
            }
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        // 获取登记信息
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        宝石登记信息 info = worldData.获取登记信息(player.getUUID()).orElse(null);

        // 使用统一的距离计算工具
        var result = 灵魂宝石距离计算.计算(player, info, server);

        // 确定状态
        持有状态 newState = result.获取持有状态();
        持有状态 oldState = 状态缓存.get(player.getUUID());

        状态缓存.put(player.getUUID(), newState);

        // 状态变化日志
        if (oldState != newState) {
            LOGGER.debug("玩家 {} 距离状态变化: {} → {}（距离: {}, 原因: {}）",player.getName().getString(),
                    oldState != null ? oldState.name() : "null",
                    newState.name(),
                    result.有效() ? String.format("%.1f", result.距离()) : "N/A",
                    result.原因().获取描述()
            );
        }

        // ★ 修复：每次都调用，让假死处理器判断进入/退出
        假死状态处理器.更新假死状态(player, result.应该假死());
    }

    // ==================== 清理====================

    public static void onPlayerLogout(UUID playerUUID) {
        状态缓存.remove(playerUUID);
    }

    public static void clearAll() {
        状态缓存.clear();
    }
}
