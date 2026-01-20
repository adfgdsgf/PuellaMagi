package com.v2t.puellamagi.system.ability;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I能力;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 能力管理器
 * 管理玩家当前激活的能力实例
 *
 * 职责：
 * 1. 变身时激活对应能力
 * 2. 解除变身时清理能力
 * 3. 每tick更新激活中的能力
 */
public final class 能力管理器 {
    private 能力管理器() {}

    //玩家UUID -> 当前激活的能力实例
    private static final Map<UUID, I能力> 激活能力表 = new HashMap<>();

    /**
     * 激活玩家的能力
     *通常在变身时调用
     *
     * @param player 玩家
     * @param abilityId 能力ID
     * @return 是否成功激活
     */
    public static boolean 激活能力(Player player, ResourceLocation abilityId) {
        if (player == null ||abilityId == null) return false;

        UUID uuid = player.getUUID();

        // 如果已有激活的能力，先失效
        if (激活能力表.containsKey(uuid)) {
            失效能力(player);}

        // 创建新能力实例
        Optional<I能力> optAbility = 能力注册表.创建实例(abilityId);
        if (optAbility.isEmpty()) {
            PuellaMagi.LOGGER.warn("玩家 {} 尝试激活不存在的能力: {}",
                    player.getName().getString(), abilityId);
            return false;
        }

        I能力 ability = optAbility.get();

        // 激活能力
        try {
            ability.激活时(player);
            激活能力表.put(uuid, ability);
            PuellaMagi.LOGGER.debug("玩家 {} 激活能力: {}",
                    player.getName().getString(), abilityId);
            return true;
        } catch (Exception e) {
            PuellaMagi.LOGGER.error("激活能力时发生错误: {}", abilityId, e);
            return false;
        }
    }

    /**
     * 失效玩家的能力
     * 通常在解除变身时调用
     */
    public static void 失效能力(Player player) {
        if (player == null) return;

        UUID uuid = player.getUUID();
        I能力 ability = 激活能力表.remove(uuid);

        if (ability != null) {
            try {
                ability.失效时(player);
                PuellaMagi.LOGGER.debug("玩家 {} 能力已失效: {}",
                        player.getName().getString(), ability.获取ID());
            } catch (Exception e) {
                PuellaMagi.LOGGER.error("失效能力时发生错误: {}", ability.获取ID(), e);
            }
        }
    }

    /**
     * 获取玩家当前激活的能力
     */
    public static Optional<I能力> 获取激活能力(Player player) {
        if (player == null) return Optional.empty();
        return Optional.ofNullable(激活能力表.get(player.getUUID()));
    }

    /**
     * 检查玩家是否有激活的能力
     */
    public static boolean 是否有激活能力(Player player) {
        if (player == null) return false;
        return 激活能力表.containsKey(player.getUUID());
    }

    /**
     * 更新所有激活能力的tick
     * 需要在服务端每tick调用
     */
    public static void tickAll(ServerPlayer player) {
        if (player == null) return;

        I能力 ability = 激活能力表.get(player.getUUID());
        if (ability != null && ability.是否激活()) {
            try {
                ability.tick(player);
            } catch (Exception e) {
                PuellaMagi.LOGGER.error("能力tick时发生错误: {}", ability.获取ID(), e);
            }
        }
    }

    /**
     * 玩家下线时清理
     */
    public static void 玩家下线(Player player) {
        if (player == null) return;
        失效能力(player);
    }

    /**
     * 获取当前激活能力的玩家数量
     */
    public static int 获取激活数量() {
        return 激活能力表.size();
    }

    /**
     * 清空所有激活能力（服务器关闭时）
     */
    public static void 清空所有() {
        激活能力表.clear();
    }
}
