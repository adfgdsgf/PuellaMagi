// 文件路径: src/main/java/com/v2t/puellamagi/system/transformation/变身管理器.java

package com.v2t.puellamagi.system.transformation;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import com.v2t.puellamagi.core.network.packets.s2c.变身同步包;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.util.网络工具;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 变身系统的统一入口
 * 所有变身/解除变身操作都通过此类进行
 *
 * 变身流程：
 * 1. 检查契约状态
 * 2. 从类型注册表查询类型定义
 * 3. 清除旧能力（如果有）
 * 4. 激活新能力
 */
public final class 变身管理器 {
    private 变身管理器() {}

    /**
     * 尝试变身
     * @param player 玩家（必须是ServerPlayer）
     * @param girlTypeId 魔法少女类型ID
     * @return 是否成功
     */
    public static boolean 尝试变身(Player player, ResourceLocation girlTypeId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            PuellaMagi.LOGGER.warn("尝试在客户端调用变身，已忽略");
            return false;
        }

        // ===== 检查契约 =====
        if (!契约管理器.可以变身(serverPlayer)) {
            PuellaMagi.LOGGER.debug("玩家 {} 未签订契约，无法变身", serverPlayer.getName().getString());
            return false;
        }

        // ===== 验证类型与契约匹配 =====
        var contractTypeOpt = 契约管理器.获取类型(serverPlayer);
        if (contractTypeOpt.isEmpty() || !contractTypeOpt.get().获取ID().equals(girlTypeId)) {
            PuellaMagi.LOGGER.warn("玩家 {} 请求变身类型 {} 与契约不匹配",serverPlayer.getName().getString(), girlTypeId);
            return false;
        }

        // ===== 查询类型定义 =====
        魔法少女类型 type = 魔法少女类型注册表.获取(girlTypeId).orElse(null);
        if (type == null) {
            PuellaMagi.LOGGER.warn("未知的魔法少女类型: {}", girlTypeId);
            return false;
        }

        return 能力工具.获取变身能力完整(serverPlayer).map(cap -> {
            //检查：已经变身
            if (cap.是否已变身()) {
                return false;
            }

            // TODO: 检查冷却

            // ===== 清除旧能力（确保干净状态）=====
            能力管理器.失效能力(serverPlayer);

            // 执行变身
            cap.设置变身状态(true);
            cap.设置少女类型(girlTypeId);
            cap.设置所属系列(type.获取所属系列());

            // 设置默认模型（如果有）
            if (type.获取默认模型() != null) {
                cap.设置模型(type.获取默认模型());
            }

            // 激活对应的能力
            ResourceLocation abilityId = type.获取固有能力ID();
            激活能力(serverPlayer, abilityId);

            // 同步给客户端
            同步变身数据(serverPlayer);

            PuellaMagi.LOGGER.debug("玩家 {} 变身为 {}，能力:{}",
                    serverPlayer.getName().getString(), girlTypeId, abilityId);

            return true;
        }).orElse(false);
    }

    /**
     * 解除变身
     * @param player 玩家（必须是ServerPlayer）
     * @return 是否成功
     */
    public static boolean 解除变身(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            PuellaMagi.LOGGER.warn("尝试在客户端调用解除变身，已忽略");
            return false;
        }

        return 能力工具.获取变身能力完整(serverPlayer).map(cap -> {
            // 检查：未变身
            if (!cap.是否已变身()) {
                return false;
            }

            // 先失效能力（清理时停、丝线等效果）
            能力管理器.失效能力(serverPlayer);

            // 执行解除
            cap.重置();

            // 同步给客户端
            同步变身数据(serverPlayer);

            PuellaMagi.LOGGER.debug("玩家 {} 解除变身", serverPlayer.getName().getString());

            return true;
        }).orElse(false);
    }

    /**
     * 切换变身状态（变身↔解除）
     */
    public static void 切换变身(Player player) {
        if (能力工具.是否已变身(player)) {
            解除变身(player);
        } else {
            // 从契约获取类型
            契约管理器.获取类型(player).ifPresent(type -> {
                尝试变身(player, type.获取ID());
            });
        }
    }

    /**
     * 强制解除变身（用于死亡、特殊情况）
     */
    public static void 强制解除变身(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        能力工具.获取变身能力完整(serverPlayer).ifPresent(cap -> {
            if (cap.是否已变身()) {
                // 失效能力
                能力管理器.失效能力(serverPlayer);
                // 重置变身状态
                cap.重置();
                // 同步
                同步变身数据(serverPlayer);
                PuellaMagi.LOGGER.debug("玩家 {} 被强制解除变身", serverPlayer.getName().getString());
            }
        });
    }

    /**
     * 激活玩家的能力
     */
    private static void 激活能力(ServerPlayer player, ResourceLocation abilityId) {
        boolean success = 能力管理器.激活能力(player, abilityId);
        if (!success) {
            PuellaMagi.LOGGER.warn("玩家 {} 激活能力失败: {}", player.getName().getString(), abilityId);
        }
    }

    /**
     * 同步变身数据给客户端
     */
    public static void 同步变身数据(ServerPlayer player) {
        能力工具.获取变身能力(player).ifPresent(cap -> {
            变身同步包 packet = new 变身同步包(player.getUUID(), cap.写入NBT());
            // 发送给自己
            网络工具.发送给玩家(player, packet);
            // 发送给附近能看到的玩家
            网络工具.发送给追踪者(player, packet);
        });
    }
}
