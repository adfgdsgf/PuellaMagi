package com.v2t.puellamagi.system.interaction.impl;

import com.v2t.puellamagi.api.interaction.I可被搜身;
import com.v2t.puellamagi.system.team.队伍管理器;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * 队友搜身来源
 *
 * 当目标是队友且目标开启了"允许队友搜身"配置时，可被搜身
 * 不需要假死/时停等前提条件
 * 目标是清醒的，需要通知
 */
public class 队友搜身来源 implements I可被搜身 {

    public static final 队友搜身来源 INSTANCE = new 队友搜身来源();

    private 队友搜身来源() {}

    @Override
    public String 获取来源ID() {
        return "puellamagi:teammate";
    }

    @Override
    public boolean 可被搜身(Player target, Player searcher) {
        MinecraftServer server = target.getServer();
        if (server == null) return false;

        // 必须是队友
        if (!队伍管理器.是否同队(server, target.getUUID(), searcher.getUUID())) {
            return false;
        }

        // 检查目标的个人配置：允许队友搜身
        return 队伍管理器.获取个人配置(server, target.getUUID(), "allowTeammateSearch");
    }

    @Override
    public boolean 需要提示被搜身者(Player target) {
        // 队友搜身时目标是清醒的，应该通知
        return true;
    }

    @Override
    public String 获取提示消息键() {
        return "message.puellamagi.being_searched_teammate";
    }
}
