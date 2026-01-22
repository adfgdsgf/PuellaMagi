// 文件路径: src/main/java/com/v2t/puellamagi/system/series/impl/灵魂宝石系列.java

package com.v2t.puellamagi.system.series.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.series.I系列;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.system.soulgem.effect.距离效果处理器;
import com.v2t.puellamagi.system.soulgem.location.灵魂宝石区块加载器;
import com.v2t.puellamagi.system.soulgem.survival.自动回血处理器;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵魂宝石系列
 *
 * 特点：
 * - 核心物品：灵魂宝石
 * - 污浊度系统（类似反向魔力）
 * - 空血不死（灵魂宝石完整时）
 * - 魔女化（污浊度满时）
 * - 无成长阶段（或只有1个阶段）
 */
public class 灵魂宝石系列 implements I系列 {

    public static final ResourceLocation ID = 资源工具.本mod("soul_gem");
    public static final 灵魂宝石系列 INSTANCE = new 灵魂宝石系列();

    //该系列可用的魔法少女类型
    private final List<ResourceLocation> 可用类型 = new ArrayList<>();

    private 灵魂宝石系列() {}

    //==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return Component.translatable("series.puellamagi.soul_gem");
    }

    @Override
    public Component 获取描述() {
        return Component.translatable("series.puellamagi.soul_gem.desc");
    }

    @Override
    public ResourceLocation 获取图标() {
        return 资源工具.纹理("item/soul_gem");
    }

    @Override
    public ResourceLocation 获取核心物品ID() {
        return 资源工具.本mod("soul_gem");
    }

    // ==================== 契约生命周期 ====================

    @Override
    public void 加入系列时(Player player) {
        PuellaMagi.LOGGER.info("玩家 {} 加入灵魂宝石系列", player.getName().getString());//灵魂宝石发放由契约管理器调用 灵魂宝石管理器.尝试发放灵魂宝石() 处理// 污浊度初始化由Capability自动处理
    }

    @Override
    public void 离开系列时(Player player) {
        PuellaMagi.LOGGER.info("玩家 {} 离开灵魂宝石系列", player.getName().getString());

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // 清除世界数据中的登记
        var server = serverPlayer.getServer();
        if (server != null) {
            灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
            worldData.移除登记(player.getUUID());
            PuellaMagi.LOGGER.debug("已清除玩家 {} 的灵魂宝石登记", player.getName().getString());
        }

        // 强制退出假死状态
        假死状态处理器.强制退出(serverPlayer);

        // 清理距离效果缓存
        距离效果处理器.onPlayerLogout(player.getUUID());
    }

    // ==================== 变身生命周期 ====================

    @Override
    public void 变身时(Player player) {
        // TODO: 显示污浊度HUD
    }

    @Override
    public void 解除变身时(Player player) {
        // TODO: 隐藏污浊度HUD（或保持显示？）
    }

    // ==================== 玩家生命周期 ====================

    @Override
    public void 玩家登录时(ServerPlayer player) {
        // 同步假死状态
        假死状态处理器.onPlayerLogin(player);

        // 检查旧存档灵魂宝石问题
        检查并发放灵魂宝石(player);

        PuellaMagi.LOGGER.debug("灵魂宝石系玩家 {} 登录处理完成", player.getName().getString());
    }

    @Override
    public void 玩家登出时(ServerPlayer player) {
        // 释放区块加载
        if (player.getServer() != null) {
            灵魂宝石区块加载器.onPlayerLogout(player.getServer(), player.getUUID());
        }

        // 清理距离效果缓存
        距离效果处理器.onPlayerLogout(player.getUUID());

        PuellaMagi.LOGGER.debug("灵魂宝石系玩家 {} 登出处理完成", player.getName().getString());
    }

    @Override
    public void 玩家重生时(ServerPlayer player) {
        // 同步假死状态
        假死状态处理器.onPlayerLogin(player);
    }

    @Override
    public void 维度切换时(ServerPlayer player) {
        // 当前无特殊处理，距离效果处理器会自动处理跨维度情况
    }

    // ==================== Tick ====================

    /**
     * 灵魂宝石系专属Tick
     *
     * 所有灵魂宝石系的tick逻辑集中在此处理，保持高内聚
     */
    @Override
    public void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // 污浊度自然恢复
        污浊度管理器.自然恢复Tick(serverPlayer);

        // 灵魂宝石区块加载
        灵魂宝石区块加载器.onPlayerTick(serverPlayer);

        // 距离效果检测（每秒检查，内部有频率控制）
        距离效果处理器.onPlayerTick(serverPlayer);

        // 假死状态维持（每tick）
        假死状态处理器.onPlayerTick(serverPlayer);

        // 自动回血（内部有频率控制）
        自动回血处理器.onPlayerTick(serverPlayer);
    }

    // ==================== 成长系统 ====================

    @Override
    public int 获取成长阶段数() {
        return 1; // 灵魂宝石系无成长阶段
    }

    // ==================== 类型管理 ====================

    @Override
    public List<ResourceLocation> 获取可用类型() {
        return List.copyOf(可用类型);
    }

    /**
     * 添加可用类型（内部使用）
     */
    public void 添加可用类型(ResourceLocation typeId) {
        if (!可用类型.contains(typeId)) {
            可用类型.add(typeId);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 登录时检查灵魂宝石
     *
     * 处理旧存档问题：已契约但没有灵魂宝石登记
     */
    private void 检查并发放灵魂宝石(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);

        // 已有登记，不需要处理
        if (worldData.存在登记(player.getUUID())) {
            return;
        }

        // 没有登记但是灵魂宝石系 =旧存档问题，自动发放
        PuellaMagi.LOGGER.info("检测到旧存档玩家 {} 缺少灵魂宝石登记，自动发放",player.getName().getString());
        灵魂宝石管理器.尝试发放灵魂宝石(player);
    }
}
