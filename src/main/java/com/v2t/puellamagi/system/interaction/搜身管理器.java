// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/搜身管理器.java

package com.v2t.puellamagi.system.interaction;

import com.v2t.puellamagi.api.interaction.I可被搜身;
import com.v2t.puellamagi.api.interaction.搜身上下文;
import com.v2t.puellamagi.core.config.搜身配置;
import com.v2t.puellamagi.util.交互工具;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 搜身管理器
 *
 * 职责：
 * - 管理I可被搜身来源的注册
 * - 统一判断玩家是否可被搜身
 * - 处理搜身请求
 * - 管理活跃的搜身会话
 */
public final class 搜身管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/Search");

    /** 已注册的搜身来源 */
    private static final List<I可被搜身> 搜身来源列表 = new CopyOnWriteArrayList<>();

    /** 活跃的搜身会话：搜身者UUID -> 上下文 */
    private static final Map<UUID, 搜身上下文> 活跃会话 = new ConcurrentHashMap<>();

    private 搜身管理器() {}

    //==================== 来源注册 ====================

    /**
     * 注册搜身来源
     */
    public static void 注册来源(I可被搜身 source) {
        if (source == null) return;

        for (I可被搜身 existing : 搜身来源列表) {
            if (existing.获取来源ID().equals(source.获取来源ID())) {
                LOGGER.warn("搜身来源 {} 已存在，跳过重复注册", source.获取来源ID());
                return;
            }
        }

        搜身来源列表.add(source);
        LOGGER.info("注册搜身来源: {}", source.获取来源ID());
    }

    // ==================== 核心判断 ====================

    /**
     * 检查目标玩家是否可被搜身
     *
     * @param target 被搜身的目标
     * @param searcher 执行搜身的玩家
     * @return 允许搜身的来源，null表示不可搜身
     */
    @Nullable
    public static I可被搜身 检查可被搜身(Player target, Player searcher) {
        if (target == null || searcher == null) return null;
        if (target.equals(searcher)) return null;

        // 全局开关检查
        if (!搜身配置.是否启用()) {
            return null;
        }

        // 距离检查 - 使用交互工具
        if (!交互工具.在实体交互范围内(searcher, target)) {
            return null;
        }

        //遍历所有来源，找到第一个允许的
        for (I可被搜身 source : 搜身来源列表) {
            try {
                if (source.可被搜身(target, searcher)) {
                    return source;
                }
            } catch (Exception e) {
                LOGGER.error("搜身来源 {} 检查时出错", source.获取来源ID(), e);
            }
        }

        return null;
    }

    /**
     * 快速检查是否可被搜身（不返回具体来源）
     */
    public static boolean 是否可被搜身(Player target, Player searcher) {
        return 检查可被搜身(target, searcher) != null;
    }

    // ==================== 搜身操作 ====================

    /**
     * 尝试开始搜身
     * 由服务端网络包处理器调用
     *
     * @param searcher 搜身者
     * @param target 被搜身者
     * @return 是否成功开始
     */
    public static boolean 尝试开始搜身(ServerPlayer searcher, ServerPlayer target) {
        I可被搜身 source = 检查可被搜身(target, searcher);

        if (source == null) {
            LOGGER.debug("玩家 {} 尝试搜身 {} 失败：不满足条件",
                    searcher.getName().getString(), target.getName().getString());
            return false;
        }

        // 创建上下文
        搜身上下文 context = 搜身上下文.创建(
                searcher, target, source.获取来源ID(), searcher.level().getGameTime()
        );

        // 记录会话
        活跃会话.put(searcher.getUUID(), context);

        // 处理提示（遵循配置和来源设置）
        if (搜身配置.是否通知被搜身者() && source.需要提示被搜身者(target)) {
            target.displayClientMessage(
                    Component.translatable(source.获取提示消息键(), searcher.getDisplayName()),
                    false
            );
        }

        LOGGER.info("玩家 {} 开始搜身 {}（来源: {}）",
                searcher.getName().getString(),
                target.getName().getString(),
                source.获取来源ID());

        return true;
    }

    /**
     * 结束搜身会话
     */
    public static void 结束搜身(UUID searcherUUID) {
        搜身上下文 context = 活跃会话.remove(searcherUUID);
        if (context != null) {
            LOGGER.debug("搜身会话结束: {} -> {}", context.搜身者UUID(), context.被搜身者UUID());
        }
    }

    /**
     * 获取活跃的搜身上下文
     */
    @Nullable
    public static 搜身上下文 获取活跃会话(UUID searcherUUID) {
        return 活跃会话.get(searcherUUID);
    }

    /**
     * 检查搜身会话是否仍然有效
     * 用于持续验证（目标可能恢复行动能力）
     */
    public static boolean 会话是否有效(ServerPlayer searcher, ServerPlayer target) {
        搜身上下文 context = 活跃会话.get(searcher.getUUID());
        if (context == null) return false;
        if (!context.被搜身者UUID().equals(target.getUUID())) return false;

        // 重新检查条件
        return 是否可被搜身(target, searcher);
    }

    // ==================== 生命周期 ====================

    /**
     * 玩家登出时清理
     */
    public static void onPlayerLogout(UUID playerUUID) {
        // 作为搜身者登出
        活跃会话.remove(playerUUID);

        // 作为被搜身者登出- 结束相关会话
        活跃会话.entrySet().removeIf(entry ->
                entry.getValue().被搜身者UUID().equals(playerUUID));
    }

    /**
     * 清空所有会话
     */
    public static void clearAll() {
        活跃会话.clear();
    }
}
