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
 * 仅在预知复刻阶段（Phase 2）可用
 * 按下：脱离命运锁定，自由行动但不可干涉（Phase 3）
 * 再按：跳到复刻结尾，结束整个能力（Phase 0）
 *
 * 与预知技能配合使用：
 * - 预知技能负责 Phase 0→1→2
 * - 时间删除技能负责 Phase 2→3→0
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
        if (能力工具.应该跳过限制(player)) return true;阶段 current = 预知状态管理.获取阶段(player.getUUID());
        return current == 阶段.复刻中|| current == 阶段.时间删除;
    }

    // ==================== 切换类判断 ====================

    @Override
    public boolean 是否开启(Player player) {阶段 current = 预知状态管理.获取阶段(player.getUUID());
        return current == 阶段.时间删除;
    }

    // ==================== 核心逻辑 ====================

    @Override
    public void 执行(Player player, Level level) {
        // 切换类不使用此方法
    }

    @Override
    public void 开启时(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;阶段 current = 预知状态管理.获取阶段(sp.getUUID());

        if (current == 阶段.复刻中) {
            处理_进入时间删除(sp);
        }
    }

    @Override
    public void 关闭时(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;

        阶段 current = 预知状态管理.获取阶段(sp.getUUID());

        if (current == 阶段.时间删除) {
            处理_结束能力(sp);
        }
    }

    // ==================== 阶段处理 ====================

    /**
     * Phase 2→ Phase 3：进入时间删除
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
     * Phase 3 → Phase 0：结束能力
     *
     * 跳到复刻结尾（应用最后一帧 + 所有剩余方块变化）
     * 然后清理状态
     */
    private void 处理_结束能力(ServerPlayer player) {
        复刻引擎.跳到结尾(player.getUUID());
        预知状态管理.结束(player.getUUID());

        PuellaMagi.LOGGER.info("玩家 {} 结束时间删除", player.getName().getString());
    }
}
