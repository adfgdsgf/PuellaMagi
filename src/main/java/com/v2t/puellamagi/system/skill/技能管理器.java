// 文件路径: src/main/java/com/v2t/puellamagi/system/skill/技能管理器.java

package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.api.restriction.限制类型;
import com.v2t.puellamagi.system.restriction.行动限制管理器;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
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
 * - 污浊度消耗：根据技能的消耗时机在正确位置处理
 * - 创造模式：通过 能力工具.应该跳过限制() 统一跳过CD、消耗等
 * - 行动限制：通过 行动限制管理器 统一检查（假死、灵魂视角等）
 */
public final class 技能管理器 {
    private 技能管理器() {}

    // 玩家按键状态：UUID -> 技能ID -> 按住时间(tick)
    private static final Map<UUID, Map<ResourceLocation, Integer>> 按键状态表 = new HashMap<>();

    //玩家语音播放时间：UUID -> 技能ID -> 上次播放时间戳
    private static final Map<UUID, Map<ResourceLocation, Long>> 语音时间表 = new HashMap<>();

    // 持续消耗计时：UUID -> 技能ID -> 上次消耗时间(tick)
    private static final Map<UUID, Map<ResourceLocation, Integer>> 持续消耗计时表 = new HashMap<>();

    //==================== 按键按下（统一入口）====================

    /**
     * 按键按下 - 所有技能的统一入口
     * 根据技能类型分流处理
     */
    public static void 按键按下(Player player, ResourceLocation skillId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // ===== 行动限制检查（新增）=====
        // 统一入口，被假死/灵魂视角等状态限制时无法释放技能
        if (行动限制管理器.是否被限制(player, 限制类型.释放技能)) {
            // 不显示提示，避免刷屏（玩家应该知道自己在假死）
            PuellaMagi.LOGGER.debug("玩家 {} 被限制释放技能", player.getName().getString());
            return;
        }

        // 基础检查：是否已变身
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

        // 检查冷却（创造模式跳过）
        if (!能力工具.应该跳过限制(player) && 能力工具.技能是否冷却中(player, skillId)) {
            // 不显示文字提示，技能栏HUD已经有冷却遮罩
            return;
        }

        // 检查可用性（包含污浊度检查，技能内部会检查创造模式）
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
                消耗污浊度_激活(serverPlayer, skill);
                应用冷却(serverPlayer, skill);PuellaMagi.LOGGER.debug("玩家 {} 释放瞬发技能: {}",
                        player.getName().getString(), skillId);
            }

            case 切换 -> {
                if (skill.是否开启(player)) {
                    skill.关闭时(player, level);
                    停止持续消耗(player, skill);
                    应用冷却(serverPlayer, skill);
                } else {
                    skill.开启时(player, level);
                    skill.执行(player, level);
                    消耗污浊度_激活(serverPlayer, skill);
                    开始持续消耗(player, skill);
                }
                PuellaMagi.LOGGER.debug("玩家 {} 切换技能: {}",
                        player.getName().getString(), skillId);
            }

            case 充能 -> {
                if (skill.获取当前充能层数(player) > 0) {
                    skill.消耗充能(player);
                    skill.执行(player, level);
                    消耗污浊度_激活(serverPlayer, skill);
                    PuellaMagi.LOGGER.debug("玩家 {} 释放充能技能: {}",
                            player.getName().getString(), skillId);
                } else {
                    serverPlayer.displayClientMessage(本地化工具.消息("skill_fail_no_charge"), true);
                }
            }

            // ===== 需要按键状态追踪的类型 =====
            case 蓄力切换 -> {
                // 已激活且不在保护期：关闭
                if (skill.是否开启(player) && !skill.是否保护期中(player)) {
                    skill.关闭时(player, level);
                    停止持续消耗(player, skill);
                    应用冷却(serverPlayer, skill);
                    PuellaMagi.LOGGER.debug("玩家 {} 关闭蓄力切换技能: {}",
                            player.getName().getString(), skillId);
                    return;
                }
                // 保护期内：忽略
                if (skill.是否保护期中(player)) {
                    return;
                }
                // 未激活：开始蓄力
                开始按键追踪(player, skill);尝试播放蓄力语音(serverPlayer, skill);
            }

            case 蓄力, 引导 -> {
                开始按键追踪(player, skill);
                if (type == I技能.按键类型.引导) {
                    开始持续消耗(player, skill);
                }
            }

            case 被动 -> {
                // 被动技能不响应按键
                PuellaMagi.LOGGER.debug("被动技能不响应按键: {}", skillId);
            }

            // ===== 其他类型暂时按瞬发处理 =====
            default -> {
                skill.执行(player, level);
                消耗污浊度_激活(serverPlayer, skill);
                应用冷却(serverPlayer, skill);
                PuellaMagi.LOGGER.debug("玩家 {} 释放技能: {}",
                        player.getName().getString(), skillId);
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
        if (holdTime == null) return;

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
                    消耗污浊度_蓄力(serverPlayer, skill, holdTime);
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
                停止持续消耗(player, skill);
                应用冷却(serverPlayer, skill);
            }
        }

        PuellaMagi.LOGGER.debug("玩家 {} 松开技能: {}，按住: {}tick",
                player.getName().getString(), skillId, holdTime);
    }

    // ==================== 按键状态追踪 ====================

    /**
     * 开始追踪按键状态
     */
    private static void 开始按键追踪(Player player, I技能 skill) {
        按键状态表.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(skill.获取ID(), 0);
        skill.按下时(player, player.level());
        PuellaMagi.LOGGER.debug("玩家 {} 开始蓄力/引导: {}",
                player.getName().getString(), skill.获取ID());
    }

    /**
     * 每tick更新按键状态
     */
    public static void tickAll(ServerPlayer player) {
        // 更新蓄力/引导状态
        更新按键状态(player);

        // 处理持续消耗
        处理持续消耗tick(player);
    }

    /**
     * 更新按键状态
     */
    private static void 更新按键状态(ServerPlayer player) {
        Map<ResourceLocation, Integer> playerStates = 按键状态表.get(player.getUUID());
        if (playerStates == null || playerStates.isEmpty()) return;

        // ===== 新增：检查是否被限制释放技能 =====
        // 如果玩家在蓄力过程中进入假死等限制状态，应该取消蓄力
        if (行动限制管理器.是否被限制(player, 限制类型.释放技能)) {
            // 通知所有正在蓄力的技能取消
            for (var entry : playerStates.entrySet()) {
                ResourceLocation skillId = entry.getKey();
                Optional<I技能> optSkill = 技能注册表.创建实例(skillId);
                optSkill.ifPresent(skill -> {
                    skill.蓄力取消时(player, player.level());
                    停止持续消耗(player, skill);
                });
                PuellaMagi.LOGGER.debug("玩家 {} 被限制，取消蓄力: {}",
                        player.getName().getString(), skillId);
            }
            // 清空该玩家的所有按键状态
            playerStates.clear();
            return;
        }
        // ===== 新增结束 =====

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

            // 获取实际蓄力时间（创造模式可能为0）
            int 实际蓄力时间 = skill.获取实际蓄力时间(player);

            // 检查蓄满自动释放
            boolean 已蓄满 = 实际蓄力时间 <= 0|| holdTime >= 实际蓄力时间;

            if (已蓄满&& skill.蓄满自动释放()) {
                I技能.按键类型 type = skill.获取按键类型();

                if (type == I技能.按键类型.蓄力切换) {
                    if (!skill.是否开启(player) && !skill.是否保护期中(player)) {
                        skill.蓄力完成激活时(player, level);
                        skill.进入保护期(player);
                        消耗污浊度_激活(player, skill);
                        开始持续消耗(player, skill);
                        PuellaMagi.LOGGER.debug("玩家 {} 蓄力切换技能蓄满激活: {}",
                                player.getName().getString(), skillId);
                    }
                } else if (type == I技能.按键类型.蓄力) {
                    skill.执行(player, level);
                    消耗污浊度_蓄力(player, skill, holdTime);
                    应用冷却(player, skill);
                    iterator.remove();
                    PuellaMagi.LOGGER.debug("玩家 {} 蓄力技能蓄满释放: {}",
                            player.getName().getString(), skillId);
                }
            }
        }
    }

    // ==================== 污浊度消耗处理 ====================

    /**
     * 激活时消耗污浊度
     */
    private static void 消耗污浊度_激活(Player player, I技能 skill) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (能力工具.应该跳过限制(player)) return;

        float amount = skill.获取激活消耗();
        if (amount > 0) {
            污浊度管理器.增加(sp, amount);
            PuellaMagi.LOGGER.debug("技能 {} 激活消耗污浊度: {}",
                    skill.获取ID(), amount);
        }
    }

    /**
     * 蓄力完成时消耗污浊度
     */
    private static void 消耗污浊度_蓄力(Player player, I技能 skill, int holdTime) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (能力工具.应该跳过限制(player)) return;

        I技能.消耗时机 timing = skill.获取消耗时机();
        float amount = 0f;

        if (timing == I技能.消耗时机.蓄力完成时) {
            amount = skill.获取激活消耗();
        } else if (timing == I技能.消耗时机.按蓄力比例) {
            amount = skill.计算蓄力消耗(holdTime);
        }

        if (amount > 0) {
            污浊度管理器.增加(sp, amount);
            PuellaMagi.LOGGER.debug("技能 {} 蓄力消耗污浊度: {}",
                    skill.获取ID(), amount);
        }
    }

    /**
     * 开始持续消耗追踪
     */
    private static void 开始持续消耗(Player player, I技能 skill) {
        I技能.消耗时机 timing = skill.获取消耗时机();
        if (timing != I技能.消耗时机.持续_每秒 && timing != I技能.消耗时机.持续_每tick) {
            return;
        }
        持续消耗计时表.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(skill.获取ID(), 0);
    }

    /**
     * 停止持续消耗追踪
     */
    private static void 停止持续消耗(Player player, I技能 skill) {
        Map<ResourceLocation, Integer> timers = 持续消耗计时表.get(player.getUUID());
        if (timers != null) {
            timers.remove(skill.获取ID());
        }
    }

    /**
     * 处理持续消耗tick
     */
    private static void 处理持续消耗tick(ServerPlayer player) {
        Map<ResourceLocation, Integer> timers = 持续消耗计时表.get(player.getUUID());
        if (timers == null || timers.isEmpty()) return;

        // 创造模式跳过持续消耗
        boolean skipConsume = 能力工具.应该跳过限制(player);

        var iterator = timers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ResourceLocation skillId = entry.getKey();
            int tickCount = entry.getValue() + 1;
            entry.setValue(tickCount);

            Optional<I技能> optSkill = 技能注册表.创建实例(skillId);
            if (optSkill.isEmpty()) {
                iterator.remove();
                continue;
            }

            I技能 skill = optSkill.get();

            // 检查技能是否仍在激活状态
            if (!skill.是否开启(player)) {
                iterator.remove();
                continue;
            }

            // 创造模式跳过消耗但保留追踪
            if (skipConsume) continue;

            I技能.消耗时机 timing = skill.获取消耗时机();
            float amount = skill.获取持续消耗();

            if (amount <= 0) continue;

            if (timing == I技能.消耗时机.持续_每tick) {
                // 每tick消耗
                污浊度管理器.增加(player, amount, false);
                // 每秒同步一次
                if (tickCount % 20 == 0) {
                    污浊度管理器.同步污浊度(player);
                }
            } else if (timing == I技能.消耗时机.持续_每秒) {
                // 每秒消耗
                if (tickCount % 20 == 0) {
                    污浊度管理器.增加(player, amount);PuellaMagi.LOGGER.debug("技能 {} 持续消耗污浊度: {}",
                            skillId, amount);
                }
            }
        }
    }

    // ==================== 状态查询 ====================

    /**
     * 检查玩家是否有正在进行的持续消耗
     * 用于污浊度管理器判断是否暂停自然恢复
     */
    public static boolean 是否有持续消耗中(Player player) {
        if (player == null) return false;
        Map<ResourceLocation, Integer> timers = 持续消耗计时表.get(player.getUUID());
        return timers != null && !timers.isEmpty();
    }

    // ==================== 辅助方法 ====================

    /**
     * 应用冷却（创造模式跳过）
     */
    private static void 应用冷却(ServerPlayer player, I技能 skill) {
        if (能力工具.应该跳过限制(player)) return;

        int cooldown = skill.获取冷却时间();
        if (cooldown > 0) {
            能力工具.获取技能能力(player).ifPresent(cap -> {
                cap.设置冷却(skill.获取ID(), cooldown);});
            com.v2t.puellamagi.core.event.通用事件.同步技能能力(player);}
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

    /**
     * 玩家下线时清理状态
     */
    public static void 玩家下线(Player player) {
        UUID uuid = player.getUUID();
        按键状态表.remove(uuid);
        语音时间表.remove(uuid);
        持续消耗计时表.remove(uuid);
        com.v2t.puellamagi.system.skill.impl.时间停止技能.玩家下线(uuid);
    }
}
