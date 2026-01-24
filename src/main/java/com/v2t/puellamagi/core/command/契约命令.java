package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.transformation.魔法少女类型注册表;
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
 * 契约相关命令
 *
 * /puellamagi contract <系列> <类型> - 签订契约
 * /puellamagi contract remove- 解除契约
 * /puellamagi contract status- 查看契约状态
 * /puellamagi series list- 列出所有系列
 * /puellamagi type list               - 列出所有类型
 * /puellamagi type info <类型>        - 查看类型详情
 */
public class 契约命令 {

    private static final int OP_LEVEL = 2;

    private static final SuggestionProvider<CommandSourceStack> 系列补全 = (context, builder) ->SharedSuggestionProvider.suggest(
            系列注册表.获取所有系列ID().stream()
                    .map(ResourceLocation::getPath)
                    .collect(Collectors.toList()),
            builder
    );

    private static final SuggestionProvider<CommandSourceStack> 类型补全 = (context, builder) -> {
        String seriesArg = StringArgumentType.getString(context, "series");
        ResourceLocation seriesId = 资源工具.本mod(seriesArg);

        var types = 魔法少女类型注册表.获取所有类型().stream()
                .filter(type -> type.获取所属系列().equals(seriesId))
                .map(type -> type.获取ID().getPath())
                .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(types, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> 所有类型补全 = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    魔法少女类型注册表.获取所有类型ID().stream()
                            .map(ResourceLocation::getPath)
                            .collect(Collectors.toList()),
                    builder
            );

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        // contract 命令组
        root.then(Commands.literal("contract")
                .then(Commands.argument("series", StringArgumentType.word())
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .suggests(系列补全)
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(类型补全)
                                .executes(ctx -> {
                                    String series = StringArgumentType.getString(ctx, "series");
                                    String type = StringArgumentType.getString(ctx, "type");
                                    return 执行契约(ctx.getSource(), series, type);
                                })
                        )
                ).then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx ->解除契约(ctx.getSource()))
                )
                .then(Commands.literal("status")
                        .executes(ctx -> 查看契约状态(ctx.getSource()))
                )
        );

        // series 命令组
        root.then(Commands.literal("series")
                .then(Commands.literal("list")
                        .executes(ctx -> 列出系列(ctx.getSource()))
                )
        );

        // type 命令组
        root.then(Commands.literal("type")
                .then(Commands.literal("list")
                        .executes(ctx -> 列出类型(ctx.getSource()))
                ).then(Commands.literal("info")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(所有类型补全)
                                .executes(ctx -> {
                                    String type = StringArgumentType.getString(ctx, "type");
                                    return 查看类型详情(ctx.getSource(), type);
                                })
                        )
                )
        );
    }

    //==================== 命令实现 ====================

    private static int 执行契约(CommandSourceStack source, String series, String type) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        var seriesId = 资源工具.本mod(series);
        var typeId = 资源工具.本mod(type);

        boolean success = 契约管理器.签订契约(player, seriesId, typeId);
        if (success) {
            source.sendSuccess(() -> Component.literal(
                    "契约签订成功！\n系列: " + seriesId + "\n类型: " + typeId), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("契约签订失败，请检查系列和类型是否正确"));
            return 0;
        }
    }

    private static int 解除契约(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        boolean success = 契约管理器.解除契约(player);
        if (success) {
            source.sendSuccess(() -> Component.literal("契约已解除"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("当前没有契约"));
            return 0;
        }
    }

    private static int 查看契约状态(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        能力工具.获取契约能力(player).ifPresentOrElse(
                contract -> {
                    if (contract.是否已契约()) {
                        String seriesName = contract.获取系列ID() != null
                                ? contract.获取系列ID().toString() : "未知";
                        String typeName = contract.获取类型ID() != null
                                ? contract.获取类型ID().toString() : "未知";
                        long contractTime = contract.获取契约时间();

                        source.sendSuccess(() -> Component.literal(
                                "=== 契约状态 ===\n" +
                                        "已签订契约: 是\n" +
                                        "系列: " + seriesName + "\n" +
                                        "类型: " + typeName + "\n" +
                                        "契约时间: " + contractTime + " tick"), false);
                    } else {
                        source.sendSuccess(() -> Component.literal(
                                "=== 契约状态 ===\n" +
                                        "已签订契约: 否"), false);
                    }
                },
                () -> source.sendFailure(Component.literal("无法获取契约数据"))
        );

        return 1;
    }

    private static int 列出系列(CommandSourceStack source) {
        var seriesIds = 系列注册表.获取所有系列ID();

        if (seriesIds.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有注册任何系列"), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("=== 已注册系列 ===\n");
        for (ResourceLocation id : seriesIds) {
            系列注册表.获取(id).ifPresent(series -> {
                sb.append("-").append(id.getPath())
                        .append(" (").append(series.获取名称().getString()).append(")\n");
            });
        }
        sb.append("共").append(seriesIds.size()).append(" 个系列");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
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
            魔法少女类型注册表.获取(id).ifPresent(type -> {
                sb.append("- ").append(id.getPath())
                        .append(" [").append(type.获取所属系列().getPath()).append("]\n");
            });
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
                                    "模型: " + (girlType.获取默认模型() != null ? girlType.获取默认模型() : "无")), false);
                },
                () -> source.sendFailure(Component.literal("未找到类型: " + typeId))
        );

        return 1;
    }
}
