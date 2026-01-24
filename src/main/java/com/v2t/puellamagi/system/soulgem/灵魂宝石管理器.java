// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/灵魂宝石管理器.java

package com.v2t.puellamagi.system.soulgem;

import com.v2t.puellamagi.core.config.灵魂宝石配置;
import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.soulgem.damage.灵魂宝石损坏处理器;
import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import com.v2t.puellamagi.system.soulgem.location.灵魂宝石区块加载器;
import com.v2t.puellamagi.system.soulgem.util.宝石清理工具;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 灵魂宝石管理器
 *
 * 职责：灵魂宝石的发放、验证、重新生成、悲叹之种使用
 *
 * 设计原则：
 * - 门面模式：对外提供统一的API
 * - 委托模式：
 *   - 清理逻辑 → 宝石清理工具
 *   - 状态修改 → 灵魂宝石损坏处理器
 * - 本类不直接修改宝石状态，统一通过损坏处理器
 */
public final class 灵魂宝石管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SoulGem");

    private 灵魂宝石管理器() {}

    //==================== 发放====================

    /**
     *尝试为玩家发放灵魂宝石
     *
     * @param player 目标玩家
     * @return 是否成功发放
     */
    public static boolean 尝试发放灵魂宝石(ServerPlayer player) {
        if (!能力工具.是灵魂宝石系(player)) {
            LOGGER.debug("玩家不是灵魂宝石系，跳过发放: {}", player.getName().getString());
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);

        // 检查是否已有登记
        if (worldData.存在登记(player.getUUID())) {
            LOGGER.debug("玩家已有灵魂宝石登记，跳过重复发放: {}", player.getName().getString());
            return false;
        }

        // 生成时间戳
        long timestamp = System.currentTimeMillis();

        // 登记到世界数据
        宝石登记信息 info = worldData.登记宝石(player.getUUID(), timestamp);
        info.更新位置(player.level().dimension(),
                player.position(),
                存储类型.玩家背包,
                player.getUUID()
        );
        worldData.标记已修改();

        // 创建并发放
        ItemStack soulGem = 创建灵魂宝石(player, timestamp);
        发放物品给玩家(player, soulGem);

        // 检查并退出假死状态
        假死状态处理器.尝试恢复(player);

        LOGGER.info("已为玩家 {} 发放灵魂宝石", player.getName().getString());

        player.displayClientMessage(
                Component.translatable("message.puellamagi.soul_gem.received")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                false
        );

        return true;
    }

    /**
     * 重新生成灵魂宝石
     *
     *流程：
     * 1. 删除旧宝石
     * 2. 更新世界数据时间戳
     * 3. 生成新宝石
     *
     * @param player 目标玩家
     * @return 是否成功
     */
    public static boolean 重新生成灵魂宝石(ServerPlayer player) {
        if (!能力工具.是灵魂宝石系(player)) {
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        UUID playerUUID = player.getUUID();

        // 获取旧登记信息
        宝石登记信息 oldInfo = worldData.获取登记信息(playerUUID).orElse(null);
        long oldTimestamp = oldInfo != null ? oldInfo.获取有效时间戳() : 0;

        // 精确删除旧宝石
        if (oldTimestamp != 0) {
            int deleted = 宝石清理工具.删除旧宝石(server, playerUUID, oldTimestamp);
            LOGGER.debug("精确删除了{} 个旧灵魂宝石", deleted);
        }

        // 生成新时间戳
        long newTimestamp = System.currentTimeMillis();

        // 更新世界数据
        if (oldInfo != null) {
            worldData.更新时间戳(playerUUID, newTimestamp);
        } else {
            worldData.登记宝石(playerUUID, newTimestamp);
        }
        worldData.更新位置(
                playerUUID,
                player.level().dimension(),
                player.position(),
                存储类型.玩家背包,
                player.getUUID()
        );

        // 创建并发放新宝石
        ItemStack soulGem = 创建灵魂宝石(player, newTimestamp);
        发放物品给玩家(player, soulGem);

        // 检查并退出假死状态
        假死状态处理器.尝试恢复(player);

        LOGGER.info("为玩家 {} 重新生成了灵魂宝石", player.getName().getString());

        player.displayClientMessage(
                Component.translatable("message.puellamagi.soul_gem.regenerated")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                false
        );

        return true;
    }

    // ==================== 创建与发放 ====================

    /**
     * 创建一个新的灵魂宝石物品
     */
    public static ItemStack 创建灵魂宝石(ServerPlayer player, long timestamp) {
        ItemStack stack = new ItemStack(ModItems.SOUL_GEM.get());灵魂宝石数据.初始化(
                stack,
                player.getUUID(),
                player.getName().getString(),
                timestamp
        );
        return stack;
    }

    /**
     * 发放物品给玩家（优先放背包，否则掉落）
     */
    private static void 发放物品给玩家(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ==================== 验证====================

    /**
     * 验证灵魂宝石是否为玩家当前有效的宝石
     */
    public static boolean 是有效灵魂宝石(ItemStack stack, ServerPlayer player) {
        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return false;
        }

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null || !ownerUUID.equals(player.getUUID())) {
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        long gemTimestamp = 灵魂宝石数据.获取时间戳(stack);

        return worldData.验证时间戳(ownerUUID, gemTimestamp);
    }

    /**
     * 获取物品的灵魂宝石状态
     */
    @Nullable
    public static 灵魂宝石状态 获取状态(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return null;
        }
        return 灵魂宝石数据.获取状态(stack);
    }

    // ==================== 查找 ====================

    /**
     * 在玩家背包中查找有效的灵魂宝石
     */
    @Nullable
    public static ItemStack 查找玩家背包中的灵魂宝石(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);宝石登记信息 info = worldData.获取登记信息(player.getUUID()).orElse(null);
        if (info == null) return null;

        return 宝石清理工具.查找背包中的有效宝石(player, info.获取有效时间戳());
    }

    // ==================== 悲叹之种 ====================

    /**
     * 使用悲叹之种
     *
     * 效果：减少污浊度+ 修复龟裂状态（如果配置允许）
     */
    public static boolean 使用悲叹之种(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        // 从配置获取净化量
        int 净化量 = 灵魂宝石配置.获取悲叹之种污浊度减少();

        // 减少污浊度
        boolean 污浊度降低成功 = 污浊度管理器.减少污浊度(player, 净化量);

        // 尝试修复（检查配置是否允许）
        boolean 修复成功 = false;
        if (灵魂宝石配置.悲叹之种能修复龟裂()) {
            ItemStack soulGem = 查找玩家背包中的灵魂宝石(player);
            if (soulGem != null) {
                var result = 灵魂宝石损坏处理器.尝试修复(server, soulGem, player.getUUID());
                修复成功 = (result ==灵魂宝石损坏处理器.处理结果.已修复);
            }
        }

        // 显示结果消息
        if (污浊度降低成功 || 修复成功) {
            if (修复成功) {
                player.displayClientMessage(
                        Component.translatable("message.puellamagi.grief_seed.repaired")
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.translatable("message.puellamagi.grief_seed.purified")
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        true
                );
            }
            return true;
        }

        player.displayClientMessage(
                Component.translatable("message.puellamagi.grief_seed.no_effect")
                        .withStyle(ChatFormatting.GRAY),
                true
        );
        return false;
    }

    // ==================== 销毁处理 ====================

    /**
     * 处理灵魂宝石销毁（统一入口）
     *
     * 根据所有者的游戏模式决定后续行为：
     * - 创造模式：只解除契约，不死亡
     * - 非创造模式：解除契约并死亡
     *
     * @param owner 宝石所有者
     */
    public static void 处理宝石销毁(ServerPlayer owner) {
        if (owner.isCreative()) {
            处理创造模式销毁(owner);
        } else {
            处理生存模式销毁(owner);
        }
    }

    /**
     * 处理创造模式下的宝石销毁
     *
     * 只解除契约，不触发死亡
     */
    public static void 处理创造模式销毁(ServerPlayer owner) {
        LOGGER.info("玩家 {} 的灵魂宝石被删除（创造模式）", owner.getName().getString());

        // 发送消息
        owner.displayClientMessage(
                Component.translatable("message.puellamagi.soul_gem.deleted_creative")
                        .withStyle(ChatFormatting.GRAY),
                false
        );

        // 清理数据
        清理销毁数据(owner);

        // 解除契约（创造模式不设置冷却）
        契约管理器.获取契约能力(owner).ifPresent(contract -> {
            if (contract.是否已契约()) {
                contract.解除契约();
                契约管理器.同步契约状态(owner);
            }
        });

        // 强制解除变身
        能力工具.获取变身能力完整(owner).ifPresent(cap -> {
            if (cap.是否已变身()) {
                变身管理器.解除变身(owner);
            }
        });
    }

    /**
     * 处理生存模式下的宝石销毁
     *
     * 解除契约并触发死亡
     * 不发送消息（由损坏处理器发送）
     */
    public static void 处理生存模式销毁(ServerPlayer owner) {
        LOGGER.warn("玩家 {} 的灵魂宝石被销毁，触发死亡", owner.getName().getString());

        // 清理数据
        清理销毁数据(owner);

        // 解除契约状态
        契约管理器.因死亡解除契约(owner);

        // 强制解除变身
        能力工具.获取变身能力完整(owner).ifPresent(cap -> {
            if (cap.是否已变身()) {
                变身管理器.解除变身(owner);
            }
        });

        // 触发真正的死亡
        owner.kill();
    }

    /**
     * 清理销毁相关数据（通用）
     */
    private static void 清理销毁数据(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) return;

        // 清除世界数据登记
        灵魂宝石世界数据.获取(server).移除登记(owner.getUUID());

        // 释放区块加载
        灵魂宝石区块加载器.释放区块加载(server, owner.getUUID());

        // 退出假死状态
        假死状态处理器.强制退出(owner);
    }


    // ==================== 兼容旧调用 ====================

    /**
     * 触发灵魂宝石销毁导致的死亡
     *
     * @deprecated 使用 {@link #处理宝石销毁(ServerPlayer)} 代替
     */
    @Deprecated
    public static void 触发销毁死亡(ServerPlayer owner) {
        处理宝石销毁(owner);
    }
}
