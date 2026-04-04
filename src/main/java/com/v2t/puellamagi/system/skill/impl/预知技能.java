package com.v2t.puellamagi.system.skill.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import com.v2t.puellamagi.system.ability.epitaph.复刻引擎;
import com.v2t.puellamagi.system.ability.epitaph.预知状态管理;
import com.v2t.puellamagi.system.ability.epitaph.预知状态管理.阶段;
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

/**
 * 预知/时间删除技能
 *
 * 参考：JOJO绯红之王/墓志铭
 * 按键类型：切换（内部管理4阶段状态机）
 *
 * 按键行为：
 * - Phase 0（待机）：按下→ 开始录制（Phase 1）
 * - Phase 1（录制）：按下 → 结束录制，回溯+复刻（Phase 2）
 * - Phase 1（录制）：修饰键+按下 → 取消录制，回到待机
 * - Phase 2（复刻）：按下 → 删除自己时间（Phase 3）
 * - Phase 3（删除）：按下 → 结束能力（Phase 0）
 *
 * 与时停互斥：时停中不能预知，预知中不能时停
 */
public class 预知技能 implements I技能 {

    private static final ResourceLocation ID = 资源工具.本mod("epitaph");
    private static final ResourceLocation 图标路径 = 资源工具.技能图标("epitaph");

    //==================== 配置 ====================

    /** 最大录制秒数 */
    private static final int 最大录制秒数 = 20;

    /** 最大录制帧数 */
    private static final int 最大录制帧数 = 最大录制秒数 * 20;

    /** 激活时污浊度消耗 */
    private static final float 激活消耗 = 0f;

    /** 录制期间每秒污浊度消耗 */
    private static final float 持续消耗_每秒 = 1f;

    /** 冷却时间（tick） */
    private static final int 冷却 = 0;

    // ==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() { return ID; }

    @Override
    public Component 获取名称() { return 本地化工具.技能名("epitaph"); }

    @Override
    public Component 获取描述() { return 本地化工具.提示("skill.epitaph.desc"); }

    @Override
    public ResourceLocation 获取图标() { return 图标路径; }

    @Override
    public 按键类型 获取按键类型() { return 按键类型.切换; }

    @Override
    public int 获取冷却时间() { return 冷却; }

    // ==================== 污浊度消耗 ====================

    @Override
    public 消耗时机 获取消耗时机() { return 消耗时机.持续_每秒; }

    @Override
    public float 获取激活消耗() { return 激活消耗; }

    @Override
    public float 获取持续消耗() { return 持续消耗_每秒; }

    @Override
    public boolean 污浊度足够(Player player) {
        float current = 污浊度管理器.获取百分比(player);
        return current < 0.95f;
    }

    // ==================== 条件判断 ====================

    @Override
    public boolean 可以使用(Player player) {
        if (能力工具.应该跳过限制(player)) return true;

        // 与时停互斥
        if (时停管理器.是否时停者(player)) return false;

        return 污浊度足够(player);
    }

    // ==================== 切换类判断 ====================

    @Override
    public boolean 是否开启(Player player) {
        return 预知状态管理.是否活跃(player.getUUID());
    }

    // ==================== 核心按键逻辑 ====================

    @Override
    public void 执行(Player player, Level level) {
        // 切换类不使用此方法，由开启时/关闭时处理
    }

    /**
     * 技能键按下 — 根据当前阶段分流
     */
    @Override
    public void 开启时(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;
        阶段 current = 预知状态管理.获取阶段(sp.getUUID());

        switch (current) {
            case 待机 -> 处理_开始录制(sp);
            case 录制中 -> 处理_结束录制并复刻(sp);
            // Phase 2/3 由时间删除技能处理
            default -> {}
        }
    }

    /**
     * 关闭时— 预知技能不使用标准开/关模式
     *4阶段状态机统一由开启时处理
     *切换类第二次按下会调用此方法，转发到状态机
     */
    @Override
    public void 关闭时(Player player, Level level) {
        开启时(player, level);
    }

    /**
     * 修饰键+技能键 — 取消录制
     */
    @Override
    public void 修饰键按下时(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;

        阶段 current = 预知状态管理.获取阶段(sp.getUUID());

        if (current == 阶段.录制中) {
            处理_取消录制(sp);
        }// 其他阶段修饰键无效
    }

    // ==================== 阶段处理 ====================

    /**
     * Phase 0 → Phase 1：开始录制
     */
    private void 处理_开始录制(ServerPlayer player) {
        // 互斥检查：时停中不能预知
        if (时停管理器.是否时停者(player)) {
            player.displayClientMessage(本地化工具.消息("epitaph_fail_timestop"), true);
            return;
        }

        long gameTime = player.level().getGameTime();

        // 状态机转换
        if (!预知状态管理.开始录制(player.getUUID(), gameTime)) {
            return;
        }

        // 开始录制
        if (!录制管理器.开始录制(player,最大录制帧数)) {
            预知状态管理.取消(player.getUUID());
            return;
        }

        PuellaMagi.LOGGER.info("玩家 {} 开始预知录制", player.getName().getString());
    }

    /**
     * Phase 1 → Phase 2：结束录制，回溯+开始复刻
     */
    private void 处理_结束录制并复刻(ServerPlayer player) {
        // 停止录制，获取录制会话
        录制管理器.录制会话 session = 录制管理器.停止录制(player.getUUID());
        if (session == null) {
            预知状态管理.取消(player.getUUID());
            return;
        }

        int totalFrames = session.帧数据.获取总帧数();
        if (totalFrames == 0) {
            PuellaMagi.LOGGER.warn("玩家 {} 录制数据为空，取消", player.getName().getString());
            预知状态管理.取消(player.getUUID());
            return;
        }

        long gameTime = player.level().getGameTime();

        // 状态机转换
        if (!预知状态管理.开始复刻(player.getUUID(), gameTime, totalFrames)) {
            return;
        }

        // 开始复刻（内含世界回滚）
        if (!复刻引擎.开始复刻(player, session)) {
            预知状态管理.取消(player.getUUID());
            return;
        }

        PuellaMagi.LOGGER.info("玩家 {} 开始复刻（{} 帧）",
                player.getName().getString(), totalFrames);
    }

    /**
     * Phase 2 → Phase 3：使用者删除自己的时间
     */
    private void 处理_进入时间删除(ServerPlayer player) {
        long gameTime = player.level().getGameTime();

        // 状态机转换
        if (!预知状态管理.开始时间删除(player.getUUID(), gameTime)) {
            return;
        }

        // 复刻引擎中处理使用者脱离
        if (!复刻引擎.进入时间删除(player)) {
            预知状态管理.取消(player.getUUID());
            return;
        }

        PuellaMagi.LOGGER.info("玩家 {} 进入时间删除", player.getName().getString());
    }

    /**
     * Phase 3 → Phase 0：结束能力
     */
    private void 处理_结束能力(ServerPlayer player) {
        复刻引擎.结束复刻(player.getUUID());
        预知状态管理.结束(player.getUUID());

        PuellaMagi.LOGGER.info("玩家 {} 结束预知能力", player.getName().getString());
    }

    /**
     * 取消录制（修饰键+技能键）
     */
    private void 处理_取消录制(ServerPlayer player) {
        录制管理器.取消录制(player.getUUID());
        预知状态管理.取消(player.getUUID());

        PuellaMagi.LOGGER.info("玩家 {} 取消预知录制", player.getName().getString());
    }

    // ==================== Tick ====================

    /**
     * 开启期间每tick调用
     * 处理录制帧采集和复刻推进
     */
    @Override
    public void 开启tick(Player player, Level level) {
        if (!(player instanceof ServerPlayer sp)) return;

        阶段 current = 预知状态管理.获取阶段(sp.getUUID());

        switch (current) {
            case 录制中 -> tick_录制(sp);
            case 复刻中, 时间删除 -> tick_复刻(sp);
            default -> {}
        }
    }

    /**
     * 录制阶段tick：采集帧
     */
    private void tick_录制(ServerPlayer player) {
        boolean added = 录制管理器.采集帧(player.getUUID());

        if (!added) {
            // 录制已满，自动进入复刻
            PuellaMagi.LOGGER.info("玩家 {} 录制已满，自动进入复刻",
                    player.getName().getString());
            处理_结束录制并复刻(player);
        }
    }

    /**
     * 复刻阶段tick：推进复刻引擎
     */
    private void tick_复刻(ServerPlayer player) {
        boolean still = 复刻引擎.tick(player.getUUID());

        if (!still) {
            // 复刻自然结束
            PuellaMagi.LOGGER.info("玩家 {} 复刻自然结束",
                    player.getName().getString());
            处理_结束能力(player);
        }
    }

    // ==================== 清理 ====================

    /**
     * 异常终止（死亡/下线/切维度）
     */
    public static void 强制终止(ServerPlayer player) {
        UUID_CLEANUP(player.getUUID());
    }

    private static void UUID_CLEANUP(java.util.UUID uuid) {
        录制管理器.取消录制(uuid);
        复刻引擎.结束复刻(uuid);
        预知状态管理.取消(uuid);
    }

    /**
     * 玩家下线清理
     */
    public static void 玩家下线(java.util.UUID uuid) {
        UUID_CLEANUP(uuid);
    }
}
