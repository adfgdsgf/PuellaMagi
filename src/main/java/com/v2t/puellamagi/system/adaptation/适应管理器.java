// 文件路径: src/main/java/com/v2t/puellamagi/system/adaptation/适应管理器.java

package com.v2t.puellamagi.system.adaptation;

import com.v2t.puellamagi.api.adaptation.I适应效果;
import com.v2t.puellamagi.api.adaptation.I适应源;
import com.v2t.puellamagi.system.adaptation.effect.假死伤害免疫效果;
import com.v2t.puellamagi.system.adaptation.source.空血假死适应源;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 适应管理器
 *
 * 管理玩家的伤害适应状态
 * 协调适应源、适应数据、适应效果三者
 *
 * 设计原则：
 * - 管理器不知道具体参数值（如连续判定时间）
 * - 所有参数通过接口向效果类获取
 * - 符合依赖倒置原则
 */
public final class 适应管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/Adaptation");

    /**玩家UUID ->伤害类型 -> 适应数据 */
    private static final Map<UUID, Map<ResourceKey<DamageType>, 适应数据>> 玩家适应数据 = new ConcurrentHashMap<>();

    private 适应管理器() {}

    // ==================== 初始化 ====================

    /**
     * 注册默认效果和源
     * 在mod初始化时调用
     */
    public static void 初始化() {
        // 注册效果
        适应效果注册表.注册(假死伤害免疫效果.INSTANCE);

        // 注册源
        适应源注册表.注册(空血假死适应源.INSTANCE);LOGGER.info("适应系统初始化完成");
    }

    // ==================== 核心API ====================

    /**
     * 通过适应源触发适应
     *
     * @param player 玩家
     * @param damageSource 伤害源
     * @param sourceId 适应源ID
     * @param context 上下文（可选）
     * @return 是否成功触发
     */
    public static boolean 通过源触发适应(Player player, DamageSource damageSource, ResourceLocation sourceId, Object context) {
        if (player == null || damageSource == null || sourceId == null) return false;

        // 获取适应源
        Optional<I适应源> sourceOpt = 适应源注册表.获取(sourceId);
        if (sourceOpt.isEmpty()) {
            LOGGER.warn("未找到适应源: {}", sourceId);
            return false;
        }

        I适应源 source = sourceOpt.get();

        // 检查是否应该触发
        if (!source.应该触发(player, damageSource, context)) {
            return false;
        }

        // 获取效果ID
        ResourceLocation effectId = source.获取效果ID(player, damageSource);
        if (effectId == null) {
            effectId = 假死伤害免疫效果.ID;
        }

        // 触发适应
        boolean success = 触发适应(player, damageSource, effectId);

        // 触发后回调
        if (success) {
            source.触发后(player, damageSource);
        }

        return success;
    }

    /**
     * 直接触发适应（内部使用或测试）
     *
     * @param player 玩家
     * @param damageSource 伤害源
     * @param effectId 效果ID
     * @return 是否成功
     */
    public static boolean 触发适应(Player player, DamageSource damageSource, ResourceLocation effectId) {
        if (player == null || damageSource == null) return false;

        ResourceKey<DamageType> damageType = damageSource.typeHolder().unwrapKey().orElse(null);
        if (damageType == null) return false;

        // 获取效果实例
        Optional<I适应效果> effectOpt = 适应效果注册表.获取(effectId);
        if (effectOpt.isEmpty()) {
            LOGGER.warn("未找到适应效果: {}", effectId);
            return false;
        }

        I适应效果 effect = effectOpt.get();
        UUID playerUUID = player.getUUID();
        long 当前时间 = player.level().getGameTime();

        Map<ResourceKey<DamageType>, 适应数据> 玩家数据 = 玩家适应数据.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()
        );

        适应数据 data = 玩家数据.get(damageType);

        // 从效果获取连续判定时间
        long 连续判定时间 = effect.获取连续判定时间();

        // 判断是否连续触发
        int 连续次数;
        if (data != null &&当前时间 - data.获取上次触发时间() <= 连续判定时间) {
            连续次数 = data.获取连续触发次数() + 1;
        } else {
            连续次数 = 1;
        }

        // 创建或更新数据
        if (data == null) {
            data = new 适应数据(damageType, effectId);
            玩家数据.put(damageType, data);
        }

        data.设置连续触发次数(连续次数);
        data.设置上次触发时间(当前时间);

        // 通过效果接口计算免疫时长
        long 免疫时长 = effect.计算免疫时长(连续次数);
        data.设置免疫结束时间(当前时间 + 免疫时长);

        // 调用效果激活回调
        effect.激活时(player);

        LOGGER.debug("玩家 {} 触发适应: {} (第{}次，免疫{}秒)",
                player.getName().getString(),
                damageType.location(),
                连续次数,
                免疫时长 / 20);

        return true;
    }

    /**
     * 检查是否免疫该伤害
     */
    public static boolean 是否免疫(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) return false;

        ResourceKey<DamageType> damageType = damageSource.typeHolder().unwrapKey().orElse(null);
        if (damageType == null) return false;

        UUID playerUUID = player.getUUID();
        long 当前时间 = player.level().getGameTime();

        Map<ResourceKey<DamageType>, 适应数据> 玩家数据 = 玩家适应数据.get(playerUUID);
        if (玩家数据 == null) return false;

        适应数据 data = 玩家数据.get(damageType);
        if (data == null) return false;

        // 通过效果接口判断免疫
        Optional<I适应效果> effectOpt = 适应效果注册表.获取(data.获取效果ID());
        if (effectOpt.isEmpty()) return false;

        return effectOpt.get().是否免疫伤害(player, damageSource, data.获取免疫结束时间(), 当前时间);
    }

    /**
     * 获取适应数据（供外部查询）
     */
    public static Optional<适应数据> 获取适应数据(UUID playerUUID, ResourceKey<DamageType> damageType) {
        Map<ResourceKey<DamageType>, 适应数据> 玩家数据 = 玩家适应数据.get(playerUUID);
        if (玩家数据 == null) return Optional.empty();
        return Optional.ofNullable(玩家数据.get(damageType));
    }

    // ==================== 清理 ====================

    public static void 清除适应(UUID playerUUID) {
        玩家适应数据.remove(playerUUID);
    }

    public static void 清除适应(UUID playerUUID, ResourceKey<DamageType> damageType) {
        Map<ResourceKey<DamageType>, 适应数据> 玩家数据 = 玩家适应数据.get(playerUUID);
        if (玩家数据 != null) {
            玩家数据.remove(damageType);
        }
    }

    public static void onPlayerLogout(UUID playerUUID) {
        // 登出不清除，重连后继续生效
    }

    public static void clearAll() {
        玩家适应数据.clear();
    }
}
