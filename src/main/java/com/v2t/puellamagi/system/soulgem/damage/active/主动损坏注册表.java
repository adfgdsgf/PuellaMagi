package com.v2t.puellamagi.system.soulgem.damage.active;

import com.v2t.puellamagi.system.soulgem.damage.损坏上下文;
import com.v2t.puellamagi.system.soulgem.damage.灵魂宝石损坏处理器;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 主动损坏方式注册表
 *
 * 管理所有注册的主动损坏方式
 * 提供统一的检测入口
 */
public final class 主动损坏注册表 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/ActiveDamage");

    /** 已注册的方式（按优先级排序） */
    private static final List<I主动损坏方式> 已注册方式 = new ArrayList<>();

    /** ID索引 */
    private static final Map<ResourceLocation, I主动损坏方式> ID索引 = new HashMap<>();

    private 主动损坏注册表() {}

    //==================== 注册 ====================

    /**
     * 注册一种损坏方式
     */
    public static void 注册(I主动损坏方式 方式) {
        if (ID索引.containsKey(方式.获取ID())) {
            LOGGER.warn("重复注册损坏方式: {}", 方式.获取ID());
            return;
        }

        已注册方式.add(方式);
        ID索引.put(方式.获取ID(), 方式);

        // 按优先级排序
        已注册方式.sort(Comparator.comparingInt(I主动损坏方式::获取优先级));

        LOGGER.debug("注册主动损坏方式: {} (优先级: {})", 方式.获取ID(), 方式.获取优先级());
    }

    /**
     * 获取所有已注册方式（只读）
     */
    public static List<I主动损坏方式> 获取所有方式() {
        return Collections.unmodifiableList(已注册方式);
    }

    /**
     * 根据ID获取方式
     */
    public static Optional<I主动损坏方式> 获取(ResourceLocation id) {
        return Optional.ofNullable(ID索引.get(id));
    }

    // ==================== 请求处理 ====================

    /**
     * 处理客户端请求（由网络包调用）
     *
     * @param player 发起请求的玩家
     * @param 触发类型 触发类型
     * @param 目标实体ID 目标掉落物ID（-1表示无）
     * @param 目标方块 目标方块位置
     */
    public static void 处理请求(ServerPlayer player, 主动损坏触发类型 触发类型,int 目标实体ID, BlockPos 目标方块) {
        LOGGER.debug("收到损坏请求: 玩家={}, 类型={}, 实体ID={}, 方块={}",
                player.getName().getString(), 触发类型, 目标实体ID, 目标方块);

        Optional<损坏上下文>匹配结果 = Optional.empty();

        // 遍历所有启用的方式，找到第一个匹配的
        for (I主动损坏方式 方式 : 已注册方式) {
            if (!方式.是否启用()) continue;
            if (方式.获取触发类型() != 触发类型) continue;

            匹配结果 = switch (触发类型) {
                case 左键攻击 -> 处理左键攻击(player, 方式, 目标实体ID);
                case 右键交互 -> 处理右键交互(player, 方式, 目标方块);
            };

            if (匹配结果.isPresent()) {LOGGER.debug("匹配到损坏方式: {}", 方式.获取ID());
                break;
            }
        }

        // 执行损坏
        匹配结果.ifPresent(context -> {
        灵魂宝石损坏处理器.处理损坏(player.getServer(), context);
    });
}

    /**
     * 处理左键攻击
     */
    private static Optional<损坏上下文> 处理左键攻击(ServerPlayer player, I主动损坏方式 方式, int 目标实体ID) {
        if (目标实体ID < 0) return Optional.empty();

        // 获取目标实体
        var entity = player.level().getEntity(目标实体ID);
        if (!(entity instanceof ItemEntity itemEntity)) {
            return Optional.empty();
        }

        // 验证距离（防作弊）
        if (player.distanceTo(itemEntity) > 6.0) {
            LOGGER.debug("距离过远，拒绝请求");
            return Optional.empty();
        }

        return 方式.检测掉落物攻击(player, itemEntity);
    }

    /**
     * 处理右键交互
     */
    private static Optional<损坏上下文> 处理右键交互(ServerPlayer player, I主动损坏方式 方式, BlockPos 目标方块) {
        // 先检测主副手组合（不需要目标方块）
        var result = 方式.检测主副手组合(player);
        if (result.isPresent()) {
            return result;
        }

        // 再检测方块交互
        if (目标方块 != null && !目标方块.equals(BlockPos.ZERO)) {
            // 验证距离（防作弊）
            if (player.distanceToSqr(目标方块.getX(), 目标方块.getY(), 目标方块.getZ()) > 36) {
                LOGGER.debug("距离过远，拒绝请求");
                return Optional.empty();
            }

            return 方式.检测方块交互(player, 目标方块);
        }

        return Optional.empty();
    }

    // ==================== 初始化 ====================

    /**
     * 注册所有内置方式（在mod初始化时调用）
     */
    public static void 初始化() {
        LOGGER.info("初始化主动损坏注册表");

        // 注册内置方式
        注册(new com.v2t.puellamagi.system.soulgem.damage.active.impl.掉落物攻击方式());
        注册(new com.v2t.puellamagi.system.soulgem.damage.active.impl.主副手组合方式());
        注册(new com.v2t.puellamagi.system.soulgem.damage.active.impl.撞击方块方式());LOGGER.info("主动损坏注册表初始化完成，共 {} 种方式", 已注册方式.size());
    }
}
