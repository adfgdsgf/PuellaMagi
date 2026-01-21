// 文件路径: src/main/java/com/v2t/puellamagi/system/skill/impl/时间停止技能.java

package com.v2t.puellamagi.system.skill.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.core.config.时停配置;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.本地化工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 时间停止技能
 *
 * 按键类型：蓄力切换
 * - 按住蓄力，蓄满自动激活时停
 * - 再次按下关闭时停
 *
 * 污浊度消耗：
 * - 消耗时机：持续_每秒
 * - 激活时消耗：2点
 * - 持续消耗：0.5点/秒
 *
 * 冷却：关闭后3秒CD
 */
public class 时间停止技能 implements I技能 {

    private static final ResourceLocation ID = 资源工具.本mod("time_stop");
    private static final ResourceLocation 图标路径 = 资源工具.技能图标("time_stop");

    // ==================== 配置常量 ====================

    // 污浊度消耗
    private static final float 激活消耗 = 2f;
    private static final float 持续消耗_每秒 = 0.5f;

    // 冷却时间（关闭后开始计算）
    private static final int 关闭后冷却 = 120;  // 6秒

    //语音列表
    private static final ResourceLocation[] 蓄力语音= new ResourceLocation[]{
            资源工具.音效("timestop_charge_1")
    };

    // ==================== 状态管理 ====================

    // 处于保护期的玩家
    private static final Set<UUID> 保护期玩家 = new HashSet<>();

    // ==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return 本地化工具.技能名("time_stop");
    }

    @Override
    public Component 获取描述() {
        return 本地化工具.提示("skill.time_stop.desc");
    }

    @Override
    public ResourceLocation 获取图标() {
        return 图标路径;
    }

    @Override
    public 按键类型 获取按键类型() {
        return 按键类型.蓄力切换;
    }

    @Override
    public int 获取冷却时间() {
        return 关闭后冷却;
    }

    @Override
    public int 获取最大蓄力时间() {
        return 时停配置.获取蓄力时间();
    }

    @Override
    public int 获取最小蓄力时间() {
        return 时停配置.获取蓄力时间();
    }

    @Override
    public int 获取实际蓄力时间(Player player) {
        if (能力工具.应该跳过限制(player)) {
            return 0;
        }
        return 时停配置.获取蓄力时间();
    }

    @Override
    public boolean 蓄满自动释放() {
        return true;
    }

    // ==================== 污浊度消耗 ====================

    @Override
    public 消耗时机 获取消耗时机() {
        return 消耗时机.持续_每秒;
    }

    @Override
    public float 获取激活消耗() {
        return 激活消耗;
    }

    @Override
    public float 获取持续消耗() {
        return 持续消耗_每秒;
    }

    @Override
    public boolean 污浊度足够(Player player) {
        float current = 污浊度管理器.获取百分比(player);
        return current < 0.9f;
    }

    // ==================== 条件判断 ====================

    @Override
    public boolean 可以使用(Player player) {
        if (能力工具.应该跳过限制(player)) {
            return true;
        }
        return 污浊度足够(player);
    }

    // ==================== 执行方法 ====================

    @Override
    public void 执行(Player player, Level level) {
        // 蓄力切换类不使用此方法
    }

    // ==================== 切换类方法 ====================

    @Override
    public boolean 是否开启(Player player) {
        return 时停管理器.是否时停者(player);
    }

    @Override
    public void 开启时(Player player, Level level) {
        // 由蓄力完成激活时处理
    }

    @Override
    public void 关闭时(Player player, Level level) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        时停管理器.结束时停(serverPlayer);
        保护期玩家.remove(player.getUUID());

        PuellaMagi.LOGGER.debug("玩家 {} 关闭时停", player.getName().getString());
    }

    // ==================== 蓄力切换类方法 ====================

    @Override
    public void 蓄力完成激活时(Player player, Level level) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        时停管理器.结束蓄力(player);
        时停管理器.开始时停(serverPlayer);

        PuellaMagi.LOGGER.info("玩家 {} 激活时停", player.getName().getString());
    }

    @Override
    public boolean 是否保护期中(Player player) {
        return 保护期玩家.contains(player.getUUID());
    }

    @Override
    public void 进入保护期(Player player) {
        保护期玩家.add(player.getUUID());PuellaMagi.LOGGER.debug("玩家 {} 进入时停保护期", player.getName().getString());
    }

    @Override
    public void 退出保护期(Player player) {
        保护期玩家.remove(player.getUUID());
        PuellaMagi.LOGGER.debug("玩家 {} 退出时停保护期，进入正常时停状态",
                player.getName().getString());
    }

    @Override
    public ResourceLocation[] 获取蓄力语音列表() {
        return 蓄力语音;
    }

    @Override
    public long 获取语音保护冷却() {
        return 时停配置.获取语音冷却();
    }

    // ==================== 按键处理 ====================

    @Override
    public void 按下时(Player player, Level level) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 创造模式直接激活
        if (能力工具.应该跳过限制(player)) {
            时停管理器.开始时停(serverPlayer);
            进入保护期(player);
            PuellaMagi.LOGGER.debug("创造模式玩家 {} 瞬发时停",
                    player.getName().getString());return;
        }

        // 普通模式开始蓄力
        时停管理器.开始蓄力(player);
    }

    @Override
    public void 蓄力取消时(Player player, Level level) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        时停管理器.结束蓄力(player);
        PuellaMagi.LOGGER.debug("玩家 {} 取消时停蓄力", player.getName().getString());
    }

    // ==================== 清理 ====================

    /**
     * 玩家下线时清理
     */
    public static void 玩家下线(UUID uuid) {
        保护期玩家.remove(uuid);}
}
