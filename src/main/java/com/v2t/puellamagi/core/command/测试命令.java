package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.ability.能力注册表;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.system.transformation.魔法少女类型注册表;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 开发测试用命令
 *
 * 用法：
 * /puellamagi transform<类型>- 变身
 * /puellamagi detransform       - 解除变身
 * /puellamagi status            - 查看状态
 * /puellamagi ability list- 列出所有注册的能力
 * /puellamagi ability current   - 查看当前激活的能力
 * /puellamagi type list         - 列出所有魔法少女类型
 * /puellamagi type info <类型>  - 查看类型详情
 * /puellamagi skill list        - 列出所有注册的技能
 * /puellamagi skill use <技能>  - 释放技能
 */
public class 测试命令 {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(常量.MOD_ID)
                // /puellamagi transform <type>
                .then(Commands.literal("transform")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    String type = StringArgumentType.getString(ctx, "type");
                                    return 执行变身(ctx.getSource(), type);
                                }))
                )
                // /puellamagi detransform
                .then(Commands.literal("detransform")
                        .executes(ctx -> 执行解除变身(ctx.getSource()))
                )
                // /puellamagi status
                .then(Commands.literal("status")
                        .executes(ctx -> 查看状态(ctx.getSource()))
                )
                // /puellamagi ability ...
                .then(Commands.literal("ability")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出能力(ctx.getSource()))
                        )
                        .then(Commands.literal("current")
                                .executes(ctx -> 查看当前能力(ctx.getSource()))
                        )
                )
                // /puellamagi type ...
                .then(Commands.literal("type")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出类型(ctx.getSource()))
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String type = StringArgumentType.getString(ctx, "type");
                                            return 查看类型详情(ctx.getSource(), type);
                                        })
                                )
                        )
                )
                // /puellamagi skill ...
                .then(Commands.literal("skill")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出技能(ctx.getSource()))
                        )
                        .then(Commands.literal("use")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String skill = StringArgumentType.getString(ctx, "skill");
                                            return 释放技能(ctx.getSource(), skill);
                                        })
                                )
                        )
                )
        );
    }

    private static int 执行变身(CommandSourceStack source, String type) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        var typeId = 资源工具.本mod(type);

        boolean success = 变身管理器.尝试变身(player, typeId);
        if (success) {
            source.sendSuccess(() -> Component.literal("变身成功: " + typeId), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("变身失败"));
            return 0;
        }
    }

    private static int 执行解除变身(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        boolean success = 变身管理器.解除变身(player);
        if (success) {
            source.sendSuccess(() -> Component.literal("已解除变身"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("当前未变身"));
            return 0;
        }
    }

    private static int 查看状态(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        能力工具.获取变身能力完整(player).ifPresentOrElse(
                cap -> {
                    String status = cap.是否已变身() ? "已变身" : "未变身";
                    String type = cap.获取少女类型() != null ? cap.获取少女类型().toString() : "无";
                    String series = cap.获取所属系列() != null ? cap.获取所属系列().toString() : "无";
                    int stage = cap.获取当前阶段索引();
                    boolean hasAbility = 能力管理器.是否有激活能力(player);

                    source.sendSuccess(() -> Component.literal(
                            "=== 变身状态 ===\n" +
                                    "状态: " + status + "\n" +
                                    "类型: " + type + "\n" +
                                    "系列: " + series + "\n" +
                                    "阶段: " + stage + "\n" +
                                    "能力激活: " + (hasAbility ? "是" : "否")
                    ), false);
                },
                () -> source.sendFailure(Component.literal("无法获取变身数据"))
        );

        return 1;
    }

    private static int 列出能力(CommandSourceStack source) {
        var abilities = 能力注册表.获取所有能力ID();

        if (abilities.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有注册任何能力"), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("=== 已注册能力 ===\n");
        for (ResourceLocation id : abilities) {
            sb.append("- ").append(id.toString()).append("\n");
        }
        sb.append("共 ").append(abilities.size()).append(" 个能力");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int 查看当前能力(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        能力管理器.获取激活能力(player).ifPresentOrElse(
                ability -> {
                    source.sendSuccess(() -> Component.literal(
                            "=== 当前能力 ===\n" +
                                    "ID: " + ability.获取ID() + "\n" +
                                    "名称: " + ability.获取名称().getString() + "\n" +
                                    "激活: " + (ability.是否激活() ? "是" : "否")
                    ), false);
                },
                () -> source.sendSuccess(() -> Component.literal("当前没有激活的能力"), false)
        );

        return 1;
    }

    private static int 列出类型(CommandSourceStack source) {
        var types = 魔法少女类型注册表.获取所有类型ID();

        if (types.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有注册任何魔法少女类型"), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("=== 已注册类型 ===\n");
        for (ResourceLocation id : types) {
            sb.append("- ").append(id.toString()).append("\n");
        }
        sb.append("共 ").append(types.size()).append(" 个类型");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int 查看类型详情(CommandSourceStack source, String type) {
        var typeId = 资源工具.本mod(type);
        魔法少女类型注册表.获取(typeId).ifPresentOrElse(
                girlType -> {
                    source.sendSuccess(() -> Component.literal(
                            "=== 类型详情 ===\n" +
                                    "ID: " + girlType.获取ID() + "\n" +
                                    "名称: " + girlType.获取名称().getString() + "\n" +
                                    "系列: " + girlType.获取所属系列() + "\n" +
                                    "能力: " + girlType.获取固有能力ID() + "\n" +
                                    "模型: " + (girlType.获取默认模型() != null ? girlType.获取默认模型() : "无")
                    ), false);
                },
                () -> source.sendFailure(Component.literal("未找到类型: " + typeId))
        );

        return 1;
    }

    private static int 列出技能(CommandSourceStack source) {
        var skills = 技能注册表.获取所有技能ID();

        if (skills.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有注册任何技能"), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("=== 已注册技能 ===\n");
        for (ResourceLocation id : skills) {
            sb.append("- ").append(id.toString()).append("\n");
        }
        sb.append("共 ").append(skills.size()).append(" 个技能");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int 释放技能(CommandSourceStack source, String skill) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        var skillId = 资源工具.本mod(skill);

        // 检查技能是否存在
        if (!技能注册表.是否已注册(skillId)) {
            source.sendFailure(Component.literal("技能不存在: " + skillId));
            return 0;
        }

        // 通过按键按下触发
        技能管理器.按键按下(player, skillId);
        source.sendSuccess(() -> Component.literal("技能触发: " + skillId), true);
        return 1;
    }
}
