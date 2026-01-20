// 文件路径: src/main/java/com/v2t/puellamagi/system/skill/技能管理器.java

package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 技能管理器
 * 技能释放的统一入口
 *
 * 设计：
 * - 按键按下()是唯一入口，根据技能类型分流处理
 * - 瞬发/切换/充能：按下即释放
 * - 蓄力/引导/蓄力切换：进入状态追踪，松开时处理
 */
public final class 技能管理器 {
    private 技能管理器() {}

    // 玩家按键状态：UUID -> 技能ID -> 按住时间(tick)
    private static final Map<UUID, Map<ResourceLocation, Integer>> 按键状态表 = new HashMap<>();

    //玩家语音播放时间：UUID -> 技能ID -> 上次播放时间戳
    private static final Map<UUID, Map<ResourceLocation, Long>> 语音时间表 = new HashMap<>();

    // ==================== 按键按下（统一入口）====================

    /**
     * 按键按下 - 所有技能的统一入口
     * 根据技能类型分流处理
     */
    public static void 按键按下(Player player, ResourceLocation skillId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // 基础检查
        if (!能力工具.是否已变身(player)) {
            serverPlayer.displayClientMessage(本地化工具.消息("skill_fail_not_transformed"), true);
            return;
        }

        Optional<I技能> optSkill = 技能注册表.创建实例(skillId);
        if (optSkill.isEmpty()) {
            PuellaMagi.LOGGER.warn("技能不存在: {}", skillId);
            return;
        }

        I技能 skill = optSkill.get();

        // 检查冷却
        if (能力工具.技能是否冷却中(player, skillId)) {
            int remaining = 能力工具.获取技能剩余冷却(player, skillId);
            int seconds = (remaining / 20) + 1;
            serverPlayer.displayClientMessage(本地化工具.消息("skill_cooldown", seconds), true);
            return;
        }

        // 检查可用性
        if (!skill.可以使用(player)) {
            serverPlayer.displayClientMessage(本地化工具.消息("skill_fail_cannot_use"), true);
            return;
        }

        // 根据类型分流
        I技能.按键类型 type = skill.获取按键类型();
        Level level = player.level();

        switch (type) {
            //===== 按下即释放的类型 =====
            case 瞬发 -> {
                skill.执行(player, level);
                应用冷却(serverPlayer, skill);
                PuellaMagi.LOGGER.debug("玩家 {} 释放瞬发技能: {}", player.getName().getString(), skillId);
            }

            case 切换 -> {
                if (skill.是否开启(player)) {
                    skill.关闭时(player, level);
                } else {
                    skill.开启时(player, level);skill.执行(player, level);
                }
                应用冷却(serverPlayer, skill);
                PuellaMagi.LOGGER.debug("玩家 {} 切换技能: {}", player.getName().getString(), skillId);
            }

            case 充能 -> {
                if (skill.获取当前充能层数(player) > 0) {
                    skill.消耗充能(player);
                    skill.执行(player, level);
                    PuellaMagi.LOGGER.debug("玩家 {} 释放充能技能: {}", player.getName().getString(), skillId);
                } else {
                    serverPlayer.displayClientMessage(本地化工具.消息("skill_fail_no_charge"), true);
                }
            }

            // ===== 需要按键状态追踪的类型 =====
            case 蓄力切换 -> {
                // 已激活且不在保护期：关闭
                if (skill.是否开启(player) && !skill.是否保护期中(player)) {
                    skill.关闭时(player, level);
                    PuellaMagi.LOGGER.debug("玩家 {} 关闭蓄力切换技能: {}", player.getName().getString(), skillId);return;
                }
                // 保护期内：忽略
                if (skill.是否保护期中(player)) {
                    return;
                }
                // 未激活：开始蓄力
                开始按键追踪(player, skill);
                尝试播放蓄力语音(serverPlayer, skill);
            }

            case 蓄力, 引导 -> {
                开始按键追踪(player, skill);
            }

            case 被动 -> {
                // 被动技能不响应按键
                PuellaMagi.LOGGER.debug("被动技能不响应按键: {}", skillId);
            }

            // ===== 其他类型暂时按瞬发处理 =====
            default -> {
                skill.执行(player, level);
                应用冷却(serverPlayer, skill);
                PuellaMagi.LOGGER.debug("玩家 {} 释放技能: {}", player.getName().getString(), skillId);
            }
        }
    }

    // ==================== 按键松开 ====================

    /**
     * 按键松开
     * 只处理正在追踪的技能
     */
    public static void 按键松开(Player player, ResourceLocation skillId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Map<ResourceLocation, Integer> playerStates = 按键状态表.get(player.getUUID());
        if (playerStates == null) return;

        Integer holdTime = playerStates.remove(skillId);
        if (holdTime == null) return;  // 没有在追踪这个技能

        Optional<I技能> optSkill = 技能注册表.创建实例(skillId);
        if (optSkill.isEmpty()) return;

        I技能 skill = optSkill.get();
        I技能.按键类型 type = skill.获取按键类型();
        Level level = player.level();

        // 通知技能松开
        skill.松开时(player, level, holdTime);

        switch (type) {
            case 蓄力 -> {
                if (holdTime >= skill.获取最小蓄力时间()) {
                    // 蓄力足够，释放
                    skill.执行(player, level);
                    应用冷却(serverPlayer, skill);
                } else {
                    // 蓄力不足，取消
                    skill.蓄力取消时(player, level);
                }
            }

            case 蓄力切换 -> {
                if (skill.是否保护期中(player)) {
                    // 保护期内松开，退出保护期进入正常激活状态
                    skill.退出保护期(player);
                } else if (holdTime < skill.获取最小蓄力时间()) {
                    // 未蓄满松开，取消
                    skill.蓄力取消时(player, level);
                }
            }

            case 引导 -> {
                // 引导结束
                skill.引导打断时(player, level);
            }
        }

        PuellaMagi.LOGGER.debug("玩家 {} 松开技能: {}，按住: {}tick", player.getName().getString(), skillId, holdTime);
    }

    // ==================== 按键状态追踪 ====================

    /**
     * 开始追踪按键状态
     */
    private static void 开始按键追踪(Player player, I技能 skill) {
        按键状态表.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(skill.获取ID(), 0);
        skill.按下时(player, player.level());
        PuellaMagi.LOGGER.debug("玩家 {} 开始蓄力/引导: {}", player.getName().getString(), skill.获取ID());
    }

    /**
     * 每tick更新按键状态
     */
    public static void tickAll(ServerPlayer player) {
        Map<ResourceLocation, Integer> playerStates = 按键状态表.get(player.getUUID());
        if (playerStates == null || playerStates.isEmpty()) return;

        Level level = player.level();

        var iterator = playerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ResourceLocation skillId = entry.getKey();
            int holdTime = entry.getValue() + 1;
            entry.setValue(holdTime);

            Optional<I技能> optSkill = 技能注册表.创建实例(skillId);
            if (optSkill.isEmpty()) continue;

            I技能 skill = optSkill.get();
            skill.按住tick(player, level, holdTime);

            // 检查蓄满自动释放
            if (holdTime >= skill.获取最大蓄力时间() && skill.蓄满自动释放()) {
                I技能.按键类型 type = skill.获取按键类型();

                if (type == I技能.按键类型.蓄力切换) {
                    if (!skill.是否开启(player) && !skill.是否保护期中(player)) {
                        skill.蓄力完成激活时(player, level);
                        skill.进入保护期(player);PuellaMagi.LOGGER.debug("玩家 {} 蓄力切换技能蓄满激活: {}",
                                player.getName().getString(), skillId);
                    }
                } else if (type == I技能.按键类型.蓄力) {
                    skill.执行(player, level);
                    应用冷却(player, skill);
                    iterator.remove();
                    PuellaMagi.LOGGER.debug("玩家 {} 蓄力技能蓄满释放: {}",
                            player.getName().getString(), skillId);
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    private static void 应用冷却(ServerPlayer player, I技能 skill) {
        int cooldown = skill.获取冷却时间();
        if (cooldown > 0) {
            能力工具.获取技能能力(player).ifPresent(cap -> {
                cap.设置冷却(skill.获取ID(), cooldown);
            });
            com.v2t.puellamagi.core.event.通用事件.同步技能能力(player);
        }
    }

    private static void 尝试播放蓄力语音(ServerPlayer player, I技能 skill) {
        ResourceLocation[]语音列表 = skill.获取蓄力语音列表();
        if (语音列表 == null || 语音列表.length == 0) return;

        UUID uuid = player.getUUID();
        ResourceLocation skillId = skill.获取ID();
        long 当前时间 = System.currentTimeMillis();

        Map<ResourceLocation, Long> skillTimes = 语音时间表.computeIfAbsent(uuid, k -> new HashMap<>());
        long 上次播放 = skillTimes.getOrDefault(skillId, 0L);

        if (当前时间 - 上次播放 < skill.获取语音保护冷却()) return;

        skillTimes.put(skillId, 当前时间);
        ResourceLocation 语音 = 语音列表[player.getRandom().nextInt(语音列表.length)];
        // TODO: 播放语音
        PuellaMagi.LOGGER.debug("播放蓄力语音: {}", 语音);
    }

    // ==================== 清理 ====================

    public static void 玩家下线(Player player) {
        UUID uuid = player.getUUID();
        按键状态表.remove(uuid);
        语音时间表.remove(uuid);
        com.v2t.puellamagi.system.skill.impl.时间停止技能.玩家下线(uuid);
    }

}
