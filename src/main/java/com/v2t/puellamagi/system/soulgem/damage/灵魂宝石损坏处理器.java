// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/damage/灵魂宝石损坏处理器.java

package com.v2t.puellamagi.system.soulgem.damage;

import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

/**
 * 灵魂宝石损坏处理器
 *
 * 职责：灵魂宝石状态变更的唯一入口
 *
 * 设计原则：
 * - 单一入口：所有状态变更（龟裂、销毁、修复）都通过此类
 * - 统一日志：状态变更有完整的日志记录
 * - 统一通知：玩家通知逻辑集中管理
 *
 * 状态流转：
 * - NORMAL → CRACKED（龟裂）
 * - CRACKED → DESTROYED（销毁）
 * - NORMAL → DESTROYED（严重损坏直接销毁）
 * - CRACKED → NORMAL（修复）
 */
public final class 灵魂宝石损坏处理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SoulGemDamage");
    private static final Random RANDOM = new Random();

    private 灵魂宝石损坏处理器() {}

    // ==================== 处理结果 ====================

    /**
     * 处理结果枚举
     */
    public enum 处理结果 {
        未损坏,// 轻微强度未触发
        龟裂,            // 正常→龟裂
        销毁,            // 龟裂→销毁 或 直接销毁
        已销毁_无效果,   // 已经是销毁状态，无额外效果
        已修复,          // 龟裂→正常
        无需修复         // 已经是正常状态
    }

    // ==================== 损坏处理（主入口） ====================

    /**
     * 处理损坏事件
     *
     * 统一入口，所有损坏都应通过此方法
     *
     * @param server 服务器实例
     * @param context 损坏上下文
     * @return 处理结果
     */
    public static 处理结果 处理损坏(MinecraftServer server, 损坏上下文 context) {
        LOGGER.debug("处理损坏: {}", context);

        // 获取当前状态
        灵魂宝石状态 当前状态 = 灵魂宝石数据.获取状态(context.灵魂宝石());

        // 已销毁的宝石不再处理
        if (当前状态 == 灵魂宝石状态.DESTROYED) {
            return 处理结果.已销毁_无效果;
        }

        // 根据强度和当前状态决定结果
        return switch (当前状态) {
            case NORMAL -> 处理正常状态损坏(server, context);
            case CRACKED -> 处理龟裂状态损坏(server, context);
            case DESTROYED -> 处理结果.已销毁_无效果;
        };
    }

    /**
     * 处理正常状态的宝石损坏
     */
    private static 处理结果 处理正常状态损坏(MinecraftServer server, 损坏上下文 context) {
        // 严重强度：直接销毁
        if (context.强度().直接销毁()) {
            return 执行销毁(server, context);
        }

        // 判断是否造成龟裂
        boolean 造成龟裂 = 判断是否龟裂(context.强度());

        if (造成龟裂) {
            return 执行龟裂(server, context);
        }

        //轻微强度未触发龟裂
        LOGGER.debug("轻微损坏未触发龟裂: {}", context.描述());
        return 处理结果.未损坏;
    }

    /**
     * 处理龟裂状态的宝石损坏
     */
    private static 处理结果 处理龟裂状态损坏(MinecraftServer server, 损坏上下文 context) {
        // 任何有效损坏都会导致销毁
        if (context.强度().获取威力值() > 0) {
            return 执行销毁(server, context);
        }

        return 处理结果.未损坏;
    }

    // ==================== 修复处理 ====================

    /**
     * 尝试修复灵魂宝石
     *
     * @param server 服务器实例
     * @param soulGem 灵魂宝石物品
     * @param ownerUUID 所有者UUID
     * @return 处理结果
     */
    public static 处理结果 尝试修复(MinecraftServer server, ItemStack soulGem, UUID ownerUUID) {
        灵魂宝石状态 当前状态 = 灵魂宝石数据.获取状态(soulGem);

        if (当前状态 == 灵魂宝石状态.DESTROYED) {
            LOGGER.debug("灵魂宝石已销毁，无法修复");
            return 处理结果.已销毁_无效果;
        }

        if (当前状态 == 灵魂宝石状态.NORMAL) {
            LOGGER.debug("灵魂宝石状态正常，无需修复");
            return 处理结果.无需修复;
        }

        // 执行修复
        return 执行修复(server, soulGem, ownerUUID);
    }

    /**
     * 执行修复
     */
    private static 处理结果 执行修复(MinecraftServer server, ItemStack soulGem, UUID ownerUUID) {
        // 更新物品NBT
        灵魂宝石数据.设置状态(soulGem, 灵魂宝石状态.NORMAL);

        // 更新世界数据
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        worldData.更新状态(ownerUUID, 灵魂宝石状态.NORMAL);

        LOGGER.info("灵魂宝石已修复: 所有者={}", ownerUUID);

        return 处理结果.已修复;
    }

    // ==================== 状态变更执行 ====================

    /**
     * 执行龟裂
     */
    private static 处理结果 执行龟裂(MinecraftServer server, 损坏上下文 context) {
        // 更新物品NBT
        灵魂宝石数据.设置状态(context.灵魂宝石(), 灵魂宝石状态.CRACKED);

        // 更新世界数据
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        worldData.更新状态(context.所有者UUID(), 灵魂宝石状态.CRACKED);

        // 通知所有者
        通知所有者_龟裂(server, context);

        LOGGER.info("灵魂宝石龟裂: 所有者={}, 原因={}",
                context.所有者UUID(), context.描述());

        return 处理结果.龟裂;
    }

    /**
     * 执行销毁
     */
    private static 处理结果 执行销毁(MinecraftServer server, 损坏上下文 context) {
        // 更新物品NBT
        灵魂宝石数据.设置状态(context.灵魂宝石(), 灵魂宝石状态.DESTROYED);

        // 通知所有者
        通知所有者_销毁(server, context);

        // 触发死亡流程
        触发死亡(server, context);

        LOGGER.warn("灵魂宝石销毁: 所有者={}, 原因={}",
                context.所有者UUID(), context.描述());

        return 处理结果.销毁;
    }

    // ==================== 死亡处理 ====================

    /**
     * 触发灵魂宝石销毁导致的死亡
     */
    private static void 触发死亡(MinecraftServer server, 损坏上下文 context) {
        UUID ownerUUID = context.所有者UUID();

        // 获取所有者
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUUID);
        if (owner == null) {
            LOGGER.debug("所有者不在线，记录待处理死亡: {}", ownerUUID);
            // TODO: 记录待处理，下次上线时执行
            return;
        }

        // 委托给管理器处理死亡流程
        灵魂宝石管理器.触发销毁死亡(owner);
    }

    // ==================== 通知 ====================

    /**
     * 通知所有者宝石龟裂
     */
    private static void 通知所有者_龟裂(MinecraftServer server, 损坏上下文 context) {
        ServerPlayer owner = server.getPlayerList().getPlayer(context.所有者UUID());
        if (owner == null) return;

        Component message;
        if (context.有攻击者() && context.获取攻击者玩家() != null) {
            // 被玩家攻击
            message = Component.translatable("message.puellamagi.soul_gem.cracked.by_player",
                    context.获取攻击者玩家().getDisplayName());
        } else if (context.是主动损坏()) {
            // 主动损坏（撞击等）
            message = Component.translatable("message.puellamagi.soul_gem.cracked.active");
        } else {
            // 被动损坏
            message = Component.translatable("message.puellamagi.soul_gem.cracked.passive");
        }

        owner.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), false);
    }

    /**
     * 通知所有者宝石销毁
     */
    private static void 通知所有者_销毁(MinecraftServer server, 损坏上下文 context) {
        ServerPlayer owner = server.getPlayerList().getPlayer(context.所有者UUID());
        if (owner == null) return;

        Component message;
        if (context.有攻击者() && context.获取攻击者玩家() != null) {
            message = Component.translatable("message.puellamagi.soul_gem.destroyed.by_player",
                    context.获取攻击者玩家().getDisplayName());
        } else if (context.是主动损坏()) {
            message = Component.translatable("message.puellamagi.soul_gem.destroyed.active");
        } else {
            message = Component.translatable("message.puellamagi.soul_gem.destroyed.passive");
        }

        owner.displayClientMessage(message.copy().withStyle(ChatFormatting.DARK_RED), false);
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据强度判断是否造成龟裂
     */
    private static boolean 判断是否龟裂(损坏强度 强度) {
        if (强度.必定龟裂()) {
            return true;
        }
        return RANDOM.nextFloat() < 强度.获取龟裂概率();
    }
}
