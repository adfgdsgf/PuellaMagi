package com.v2t.puellamagi.system.skill.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import com.v2t.puellamagi.system.ability.epitaph.预知状态管理;
import com.v2t.puellamagi.system.ability.epitaph.预知状态管理.阶段;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.本地化工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 时间删除技能
 *
 * 常态可用（按了没效果就是了）
 * 实际效果由服务端逻辑决定：
 * - 录制者在复刻中(Phase 2) → 进入时删(Phase 3)
 * - 被锁定的非录制者 → 脱离锁定进入时删
 * - 不在回放中 → 无效果
 *
 * 按下：脱离命运锁定，自由行动但不可干涉
 * 再按：跳到复刻结尾，结束整个录制组的回放
 *
 * 多人场景：任何时删中的玩家按第二段 → 全局跳到结尾
 */
public class 时间删除技能 implements I技能 {

    private static final ResourceLocation ID = 资源工具.本mod("time_erasure");
    private static final ResourceLocation 图标路径 = 资源工具.技能图标("time_erasure");

    // ==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() { return ID; }

    @Override
    public Component 获取名称() { return 本地化工具.技能名("time_erasure"); }

    @Override
    public Component 获取描述() { return 本地化工具.提示("skill.time_erasure.desc"); }

    @Override
    public ResourceLocation 获取图标() { return 图标路径; }

    @Override
    public 按键类型 获取按键类型() { return 按键类型.切换; }

    @Override
    public int 获取冷却时间() { return 0; }

    // ==================== 污浊度 ====================

    @Override
    public 消耗时机 获取消耗时机() { return 消耗时机.无; }

    @Override
    public boolean 污浊度足够(Player player) { return true; }

    // ==================== 条件判断 ====================

    @Override
    public boolean 可以使用(Player player) {
        // 常态可用，不做前置条件判断
        // 实际效果由开启时/关闭时的服务端逻辑决定
        return true;
    }

    // ==================== 切换类判断 ====================

    @Override
    public boolean 是否开启(Player player) {
        // 录制者自己在时删中
        阶段 current = 预知状态管理.获取阶段(player.getUUID());
        if (current == 阶段.时间删除) return true;

        // 非录制者被复刻引擎标记为时删中
        复刻引擎.复刻会话 session = 复刻引擎.查找玩家所在会话(player.getUUID());
        if (session != null && session.时删中玩家.contains(player.getUUID())) return true;

        return false;
    }

    // ==================== 核心逻辑 ====================

    @Override
    public void 执行(Player player, Level level) {
        // 切换类不使用此方法
    }

    @Override
    public void 开启时(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;
        java.util.UUID playerUUID = sp.getUUID();

        // 情况1：录制者在复刻中 → 进入时删
        阶段 current = 预知状态管理.获取阶段(playerUUID);
        if (current == 阶段.复刻中) {
            处理_进入时间删除(sp);
            return;
        }

        // 情况2：被锁定的玩家（非录制者或已在复刻中的录制者）→ 脱离锁定
        if (复刻引擎.玩家是否被锁定(playerUUID)) {
            处理_被锁定玩家进入时删(sp);
            return;
        }

        // 情况3：不在回放中 → 无效果
    }

    @Override
    public void 关闭时(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;
        java.util.UUID playerUUID = sp.getUUID();

        // 录制者自己的时删第二段
        阶段 current = 预知状态管理.获取阶段(playerUUID);
        if (current == 阶段.时间删除) {
            处理_结束能力(sp);
            return;
        }

        // 非录制者的时删第二段（检查复刻引擎的时删中玩家集合）
        复刻引擎.复刻会话 session = 复刻引擎.查找玩家所在会话(playerUUID);
        if (session != null && session.时删中玩家.contains(playerUUID)) {
            处理_结束能力(sp);
        }
    }

    // ==================== 阶段处理 ====================

    /**
     * 录制者 Phase 2 → Phase 3：进入时间删除
     */
    private void 处理_进入时间删除(ServerPlayer player) {
        long gameTime = player.level().getGameTime();

        if (!预知状态管理.开始时间删除(player.getUUID(), gameTime)) {
            return;
        }

        if (!复刻引擎.进入时间删除(player)) {
            预知状态管理.取消(player.getUUID());
            return;
        }

        PuellaMagi.LOGGER.info("玩家 {} 进入时间删除", player.getName().getString());
    }

    /**
     * 被锁定的非录制者 → 脱离锁定进入时删
     * 不经过预知状态管理（非录制者没有预知状态）
     * 直接由复刻引擎管理时删状态
     */
    private void 处理_被锁定玩家进入时删(ServerPlayer player) {
        if (!复刻引擎.进入时间删除(player)) {
            return;
        }

        PuellaMagi.LOGGER.info("被锁定玩家 {} 脱离锁定进入时间删除", player.getName().getString());
    }

    /**
     * 结束能力（跳到结尾）
     *
     * 跳到复刻结尾（应用最后一帧 + 所有剩余方块变化）
     * 任何时删中的玩家按第二段都触发全局跳到结尾
     */
    private void 处理_结束能力(ServerPlayer player) {
        // 跳到结尾会结束整个录制组的回放
        复刻引擎.跳到结尾(player.getUUID());

        // 清理所有录制者的预知状态
        // 由结束复刻流程中处理
        预知状态管理.结束(player.getUUID());

        PuellaMagi.LOGGER.info("玩家 {} 触发时间删除结束（全局跳到结尾）", player.getName().getString());
    }
}
