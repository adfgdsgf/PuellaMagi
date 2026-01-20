// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/impl/测试能力.java

package com.v2t.puellamagi.system.ability.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I能力;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.util.资源工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用能力
 * 用于验证能力框架是否正常工作
 * 特性：解锁所有已注册的技能，方便测试
 */
public class 测试能力 implements I能力 {

    private static final ResourceLocation ID = 资源工具.本mod("test");

    private boolean 已激活 = false;
    private int tick计数 = 0;

    // 派生技能缓存（所有技能）
    private List<I技能> 派生技能缓存 = null;

    //==================== 基础信息 ====================

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return 本地化工具.能力名("test");
    }

    @Override
    public Component 获取描述() {
        return 本地化工具.提示("ability.test.desc");
    }

    // ==================== 生命周期 ====================

    @Override
    public void 激活时(Player player) {
        已激活 = true;
        tick计数 = 0;
        PuellaMagi.LOGGER.info("测试能力激活 -玩家: {}（已解锁所有技能）", player.getName().getString());
    }

    @Override
    public void 失效时(Player player) {
        已激活 = false;
        PuellaMagi.LOGGER.info("测试能力失效 - 玩家: {}, 持续tick: {}", player.getName().getString(), tick计数);
    }

    @Override
    public void tick(Player player) {
        tick计数++;

        // 每100tick（5秒）输出一次日志
        if (tick计数 % 100 == 0) {
            PuellaMagi.LOGGER.debug("测试能力tick - 玩家: {}, 计数: {}",
                    player.getName().getString(), tick计数);
        }
    }

    @Override
    public boolean 是否激活() {
        return 已激活;
    }

    // ==================== 派生技能（解锁所有）====================

    @Override
    public List<I技能> 获取派生技能() {
        if (派生技能缓存 == null) {
            派生技能缓存 = new ArrayList<>();

            // 获取所有已注册的技能ID，创建实例加入列表
            for (ResourceLocation skillId : 技能注册表.获取所有技能ID()) {
                技能注册表.创建实例(skillId).ifPresent(skill -> {
                    派生技能缓存.add(skill);
                    PuellaMagi.LOGGER.debug("测试能力解锁技能: {}", skillId);
                });
            }

            PuellaMagi.LOGGER.info("测试能力已解锁 {} 个技能", 派生技能缓存.size());
        }
        return 派生技能缓存;
    }

    // ==================== 数据持久化 ====================

    private static final String KEY_TICK_COUNT = "TickCount";

    @Override
    public CompoundTag 保存数据() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_TICK_COUNT, tick计数);
        return tag;
    }

    @Override
    public void 加载数据(CompoundTag tag) {
        tick计数 = tag.getInt(KEY_TICK_COUNT);
    }

    // ==================== 额外方法（测试用） ====================

    /**
     * 获取tick计数（测试用）
     */
    public int 获取Tick计数() {
        return tick计数;
    }
}
