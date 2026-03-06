package com.v2t.puellamagi.util.recording;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 玩家快照
 *
 * 录制开始时给玩家拍一张完整的照片
 * 回滚时按照片还原
 *
 * 包含：
 * - 背包全部物品（36槽位+ 装备4+ 副手1= 41个）
 * - 血量
 * - 饥饿值+饱食度 + 消耗度
 * - 经验等级 + 经验值
 * - 药水效果列表
 * - 选中的快捷栏槽位
 * - 着火时间
 * - 剩余空气
 *
 * 复用场景：预知回滚、时间倒流、存档点技能
 */
public class 玩家快照 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/PlayerSnapshot");

    private final CompoundTag 背包NBT;
    private final float 血量;
    private final int 饥饿值;
    private final float 饱食度;
    private final float 消耗度;
    private final int 经验等级;
    private final float 经验进度;
    private final int 选中槽位;
    private final int 着火剩余;
    private final int 剩余空气;
    private final List<CompoundTag> 药水效果列表;

    //==================== 构造 ====================

    private 玩家快照(CompoundTag inventoryNbt, float health,int foodLevel, float saturation, float exhaustion,
                     int expLevel, float expProgress,
                     int selectedSlot, int fireTicks, int airSupply,
                     List<CompoundTag> effects) {
        this.背包NBT = inventoryNbt;
        this.血量 = health;
        this.饥饿值 = foodLevel;
        this.饱食度 = saturation;
        this.消耗度 = exhaustion;
        this.经验等级 = expLevel;
        this.经验进度 = expProgress;
        this.选中槽位 = selectedSlot;
        this.着火剩余 = fireTicks;
        this.剩余空气 = airSupply;
        this.药水效果列表 = effects;
    }

    // ==================== 从玩家采集 ====================

    /**
     * 从玩家当前状态拍快照
     *
     * 所有物品都做深拷贝（.copy()）
     * 避免后续游戏中物品被修改影响快照
     */
    public static 玩家快照 从玩家采集(ServerPlayer player) {
        // 背包：用MC自带的序列化，最可靠
        ListTag inventoryList = new ListTag();
        player.getInventory().save(inventoryList);
        CompoundTag inventoryNbt = new CompoundTag();
        inventoryNbt.put("Items", inventoryList);

        // 药水效果：逐个序列化
        List<CompoundTag> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(effect.save(new CompoundTag()));
        }

        return new 玩家快照(inventoryNbt,
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.getFoodData().getExhaustionLevel(),
                player.experienceLevel,
                player.experienceProgress,
                player.getInventory().selected,
                player.getRemainingFireTicks(),
                player.getAirSupply(),
                effects
        );
    }

    // ==================== 恢复到玩家 ====================

    /**
     * 将快照恢复到玩家
     *
     * 顺序很重要：
     * 1. 先清空背包（避免物品重复）
     * 2. 再从NBT加载
     * 3. 设置其他状态
     * 4. 最后清除并重新应用药水效果
     */
    public void 恢复到(ServerPlayer player) {
        try {
            // 1. 清空背包
            player.getInventory().clearContent();

            // 2. 从NBT恢复背包
            ListTag inventoryList = 背包NBT.getList("Items", 10);
            player.getInventory().load(inventoryList);
            player.getInventory().selected = 选中槽位;

            // 3. 血量
            player.setHealth(血量);

            // 4. 饥饿
            player.getFoodData().setFoodLevel(饥饿值);
            player.getFoodData().setSaturation(饱食度);
            player.getFoodData().setExhaustion(消耗度);

            // 5. 经验
            player.experienceLevel = 经验等级;
            player.experienceProgress = 经验进度;
            player.totalExperience = 计算总经验(经验等级, 经验进度);

            // 6. 其他
            player.setRemainingFireTicks(着火剩余);
            player.setAirSupply(剩余空气);

            // 7. 药水效果：先清除再重新应用
            player.removeAllEffects();
            for (CompoundTag effectNbt : 药水效果列表) {
                MobEffectInstance effect = MobEffectInstance.load(effectNbt);
                if (effect != null) {
                    player.addEffect(effect);
                }
            }

            // 8. 强制全量同步背包到客户端
            player.inventoryMenu.broadcastChanges();
// broadcastChanges可能只发差异，强制发全量容器内容包
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket(
                            player.inventoryMenu.containerId,
                            player.inventoryMenu.incrementStateId(),
                            player.inventoryMenu.getItems(),
                            player.inventoryMenu.getCarried()
                    )
            );
// 经验同步
            player.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                            经验进度,计算总经验(经验等级, 经验进度), 经验等级
                    )
            );
// 血量同步
            player.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                            血量,饥饿值, 饱食度
                    )
            );
            LOGGER.debug("玩家快照恢复成功: {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("玩家快照恢复失败: {}", player.getName().getString(), e);
        }
    }

    // ==================== 工具 ====================

    /**
     * 根据等级和进度计算总经验值
     * MC的经验公式比较复杂，这里用近似值
     */
    private static int 计算总经验(int level, float progress) {
        int total;
        if (level <= 16) {
            total = (int) (level * level +6* level);
        } else if (level <= 31) {
            total = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            total = (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        // 加上当前等级的进度部分
        int nextLevelXp = 计算下一级所需经验(level);
        total += (int) (progress * nextLevelXp);
        return total;
    }

    private static int 计算下一级所需经验(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }
}
