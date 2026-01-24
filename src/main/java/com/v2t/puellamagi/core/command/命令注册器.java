package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.v2t.puellamagi.常量;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * 命令统一注册入口
 *
 * 将各模块的子命令注册到 /puellamagi 下
 */
public class 命令注册器 {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(常量.MOD_ID);

        // 注册各模块子命令
        契约命令.register(root);
        变身命令.register(root);
        灵魂宝石命令.register(root);
        调试命令.register(root);

        dispatcher.register(root);
    }
}
