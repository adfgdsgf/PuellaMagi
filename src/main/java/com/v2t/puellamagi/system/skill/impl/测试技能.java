package com.v2t.puellamagi.system.skill.impl;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.util.资源工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 测试用技能 - 瞬发类型
 * 用于验证技能框架是否正常工作
 */
public class 测试技能 implements I技能 {

    private static final ResourceLocation ID = 资源工具.本mod("test_skill");
    private static final ResourceLocation 图标 = 资源工具.技能图标("test_skill");

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public Component 获取名称() {
        return 本地化工具.技能名("test");
    }

    @Override
    public Component 获取描述() {
        return 本地化工具.提示("skill.test.desc");
    }

    @Override
    public ResourceLocation 获取图标() {
        return 图标;
    }

    @Override
    public 按键类型 获取按键类型() {
        return 按键类型.瞬发;
    }

    @Override
    public int 获取冷却时间() {
        return 40; // 2秒
    }

    @Override
    public boolean 可以使用(Player player) {
        // 测试技能始终可用（只要变身了）
        return true;
    }

    @Override
    public void 执行(Player player, Level level) {
        // 简单效果：发送消息 + 治疗2点
        player.displayClientMessage(本地化工具.消息("skill_test_executed"), true);
        player.heal(2.0f);
        PuellaMagi.LOGGER.debug("测试技能执行 - 玩家: {}", player.getName().getString());
    }
}
