package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.ability.能力注册表;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Collectors;

/**
 * 变身相关命令
 *
 * /puellamagi transform- 变身
 * /puellamagi detransform            - 解除变身
 * /puellamagi status                 - 查看状态
 * /puellamagi ability list           - 列出所有能力
 * /puellamagi ability current        - 查看当前能力
 * /puellamagi skill list             - 列出所有技能
 * /puellamagi skill use<技能>- 释放技能
 */
public class 变身命令 {

    private static final int OP_LEVEL = 2;

    private static final SuggestionProvider<CommandSourceStack> 技能补全 = (context, builder) ->SharedSuggestionProvider.suggest(
            技能注册表.获取所有技能ID().stream()
                    .map(ResourceLocation::getPath)
                    .collect(Collectors.toList()),
            builder
    );

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        // transform命令
        root.then(Commands.literal("transform")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .executes(ctx -> 执行变身(ctx.getSource()))
        );

        // detransform 命令
        root.then(Commands.literal("detransform")
                .requires(source -> source.hasPermission(OP_LEVEL))
                .executes(ctx -> 执行解除变身(ctx.getSource()))
        );

        // status 命令
        root.then(Commands.literal("status")
                .executes(ctx -> 查看状态(ctx.getSource()))
        );

        // ability 命令组
        root.then(Commands.literal("ability")
                .then(Commands.literal("list")
                        .executes(ctx -> 列出能力(ctx.getSource()))
                )
                .then(Commands.literal("current")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 查看当前能力(ctx.getSource()))
                ));

        // skill 命令组
        root.then(Commands.literal("skill")
                .then(Commands.literal("list")
                        .executes(ctx -> 列出技能(ctx.getSource()))
                )
                .then(Commands.literal("use")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(技能补全)
                                .executes(ctx -> {
                                    String skill = StringArgumentType.getString(ctx, "skill");
                                    return 释放技能(ctx.getSource(), skill);
                                })
                        )
                )
        );
    }

    //==================== 变身命令实现 ====================

    private static int 执行变身(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (!契约管理器.可以变身(player)) {
            source.sendFailure(Component.literal("尚未签订契约，无法变身"));
            return 0;
        }

        var typeOpt = 契约管理器.获取类型(player);
        if (typeOpt.isEmpty()) {
            source.sendFailure(Component.literal("契约数据异常，无法获取类型"));
            return 0;
        }

        ResourceLocation typeId = typeOpt.get().获取ID();

        boolean success = 变身管理器.尝试变身(player, typeId);
        if (!success) {
            source.sendFailure(Component.literal("变身失败"));
            return 0;
        }

        return 1;
    }

    private static int 执行解除变身(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        boolean success = 变身管理器.解除变身(player);
        if (!success) {
            source.sendFailure(Component.literal("当前未变身"));
            return 0;
        }

        return 1;
    }

    // ==================== 状态命令实现 ====================

    private static int 查看状态(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        StringBuilder sb = new StringBuilder("=== 玩家状态 ===\n");

        能力工具.获取契约能力(player).ifPresent(contract -> {
            sb.append("契约: ").append(contract.是否已契约() ? "已签订" : "未签订").append("\n");
            if (contract.是否已契约()) {
                sb.append("  系列: ").append(contract.获取系列ID()).append("\n");
                sb.append("  类型: ").append(contract.获取类型ID()).append("\n");
            }
        });

        能力工具.获取变身能力完整(player).ifPresent(cap -> {
            sb.append("变身: ").append(cap.是否已变身() ? "已变身" : "未变身").append("\n");
            if (cap.是否已变身()) {
                sb.append("阶段: ").append(cap.获取当前阶段索引()).append("\n");
            }
        });

        能力工具.获取污浊度能力(player).ifPresent(cap -> {
            sb.append("污浊度: ").append(String.format("%.1f / %.1f (%.0f%%)",
                    cap.获取当前值(), cap.获取最大值(), cap.获取百分比() * 100)).append("\n");
        });

        boolean hasAbility = 能力管理器.是否有激活能力(player);
        sb.append("能力激活: ").append(hasAbility ? "是" : "否");

        if (hasAbility) {
            能力管理器.获取激活能力(player).ifPresent(ability -> {
                sb.append("\n  当前能力: ").append(ability.获取ID());});
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    // ==================== 能力命令实现 ====================

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

    // ==================== 技能命令实现 ====================

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

        if (!技能注册表.是否已注册(skillId)) {
            source.sendFailure(Component.literal("技能不存在: " + skillId));
            return 0;
        }

        技能管理器.按键按下(player, skillId);
        source.sendSuccess(() -> Component.literal("技能触发: " + skillId), true);
        return 1;
    }
}
