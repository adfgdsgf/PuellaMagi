// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/impl/时间停止能力.java

package com.v2t.puellamagi.system.ability.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I能力;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import com.v2t.puellamagi.system.skill.impl.时间停止技能;
import com.v2t.puellamagi.util.资源工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间停止能力
 * 灵魂宝石系的固有能力，允许使用者冻结时间
 */
public class 时间停止能力 implements I能力 {

    private static final ResourceLocation ID = 资源工具.本mod("time_control");

    private boolean 已激活 = false;
    private int 累计时停时长 = 0;  // tick，用于统计/成就等

    // 派生技能缓存
    private List<I技能> 派生技能缓存 = null;

    //==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return 本地化工具.能力名("time_control");
    }

    @Override
    public Component 获取描述() {
        return 本地化工具.提示("ability.time_control.desc");
    }

    // ==================== 生命周期 ====================

    @Override
    public void 激活时(Player player) {
        已激活 = true;
        PuellaMagi.LOGGER.info("时间停止能力激活 - 玩家: {}", player.getName().getString());
    }

    @Override
    public void 失效时(Player player) {
        已激活 = false;

        // 如果正在时停，强制结束
        if (player instanceof ServerPlayer serverPlayer) {
            if (时停管理器.是否时停者(serverPlayer)) {
                时停管理器.结束时停(serverPlayer);
            }
            // 如果正在蓄力，也要结束
            if (时停管理器.是否正在蓄力(serverPlayer)) {
                时停管理器.结束蓄力(serverPlayer);
            }
        }

        PuellaMagi.LOGGER.info("时间停止能力失效 - 玩家: {}, 累计时停: {} tick",
                player.getName().getString(), 累计时停时长);
    }

    @Override
    public void tick(Player player) {
        if (!已激活) return;

        // 统计时停时长
        if (时停管理器.是否时停者(player)) {
            累计时停时长++;

            // TODO: 每tick增加污浊度
            // 污浊度管理器.增加污浊度(player, 配置.时停污浊度消耗);
        }
    }

    @Override
    public boolean 是否激活() {
        return 已激活;
    }

    // ==================== 派生技能 ====================

    @Override
    public List<I技能> 获取派生技能() {
        if (派生技能缓存 == null) {
            派生技能缓存 = new ArrayList<>();
            派生技能缓存.add(new 时间停止技能());
            // TODO: 后续可能添加更多派生技能，如时间减速等
        }
        return 派生技能缓存;
    }

    // ==================== 数据持久化 ====================

    private static final String KEY_TOTAL_TIMESTOP = "TotalTimestop";

    @Override
    public CompoundTag 保存数据() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_TOTAL_TIMESTOP, 累计时停时长);
        return tag;
    }

    @Override
    public void 加载数据(CompoundTag tag) {
        累计时停时长 = tag.getInt(KEY_TOTAL_TIMESTOP);
    }

    // ==================== 额外方法 ====================

    /**
     * 获取累计时停时长（tick）
     */
    public int 获取累计时停时长() {
        return 累计时停时长;
    }
}
