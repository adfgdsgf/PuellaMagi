// 文件路径: src/main/java/com/v2t/puellamagi/core/command/测试命令.java

package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.ability.能力注册表;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.transformation.变身管理器;
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
 * 开发测试用命令
 *
 * ===OP指令（权限等级2）===
 * /puellamagi contract<系列> <类型>- 签订契约
 * /puellamagi contract remove- 解除契约
 * /puellamagi transform- 变身
 * /puellamagi detransform             - 解除变身
 * /puellamagi skill use<技能>        - 释放技能
 * /puellamagi ability current- 查看当前激活的能力
 *
 * === 普通指令（所有人可用）===
 * /puellamagi status                  - 查看状态
 * /puellamagi contract status         - 查看契约状态
 * /puellamagi series list             - 列出所有系列
 * /puellamagi type list               - 列出所有类型
 * /puellamagi type info <类型>        - 查看类型详情
 * /puellamagi ability list            - 列出所有注册的能力
 * /puellamagi skill list              - 列出所有注册的技能
 */
public class 测试命令 {

    // ==================== 权限等级常量 ====================

    private static final int OP_LEVEL = 2;

    // ==================== 自动补全提供器 ====================

    /**
     * 系列ID自动补全
     */
    private static final SuggestionProvider<CommandSourceStack> 系列补全 = (context, builder) ->SharedSuggestionProvider.suggest(
            系列注册表.获取所有系列ID().stream()
                    .map(ResourceLocation::getPath)
                    .collect(Collectors.toList()),
            builder
    );

    /**
     *魔法少女类型自动补全（根据已选系列过滤）
     */
    private static final SuggestionProvider<CommandSourceStack> 类型补全 = (context, builder) -> {
        String seriesArg = StringArgumentType.getString(context, "series");
        ResourceLocation seriesId = 资源工具.本mod(seriesArg);

        var types = 魔法少女类型注册表.获取所有类型().stream()
                .filter(type -> type.获取所属系列().equals(seriesId))
                .map(type -> type.获取ID().getPath())
                .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(types, builder);
    };

    /**
     * 所有魔法少女类型自动补全
     */
    private static final SuggestionProvider<CommandSourceStack> 所有类型补全 = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    魔法少女类型注册表.获取所有类型ID().stream()
                            .map(ResourceLocation::getPath)
                            .collect(Collectors.toList()),
                    builder
            );

    /**
     * 技能ID自动补全
     */
    private static final SuggestionProvider<CommandSourceStack> 技能补全 = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    技能注册表.获取所有技能ID().stream()
                            .map(ResourceLocation::getPath)
                            .collect(Collectors.toList()),
                    builder
            );

    // ==================== 命令注册 ====================

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(常量.MOD_ID)

                // ==================== 契约命令 ====================
                .then(Commands.literal("contract")
                        // [OP] /puellamagi contract <系列> <类型>
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
                        )
                        // [OP] /puellamagi contract remove
                        .then(Commands.literal("remove")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 解除契约(ctx.getSource()))
                        )
                        // [普通] /puellamagi contract status
                        .then(Commands.literal("status")
                                .executes(ctx -> 查看契约状态(ctx.getSource()))
                        )
                )

                // ==================== 系列命令 ====================
                // [普通] /puellamagi series list
                .then(Commands.literal("series")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出系列(ctx.getSource()))
                        )
                )

                // ==================== 变身命令 ====================
                // [OP] /puellamagi transform
                .then(Commands.literal("transform")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 执行变身(ctx.getSource()))
                )
                // [OP] /puellamagi detransform
                .then(Commands.literal("detransform")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 执行解除变身(ctx.getSource()))
                )

                // ==================== 状态命令 ====================
                // [普通] /puellamagi status
                .then(Commands.literal("status")
                        .executes(ctx -> 查看状态(ctx.getSource()))
                )

                // ==================== 能力命令 ====================
                .then(Commands.literal("ability")
                        // [普通] /puellamagi ability list
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出能力(ctx.getSource()))
                        )
                        // [OP] /puellamagi ability current
                        .then(Commands.literal("current")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 查看当前能力(ctx.getSource()))
                        )
                )

                // ==================== 类型命令 ====================
                .then(Commands.literal("type")
                        // [普通] /puellamagi type list
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出类型(ctx.getSource()))
                        )
                        // [普通] /puellamagi type info <类型>
                        .then(Commands.literal("info")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(所有类型补全)
                                        .executes(ctx -> {
                                            String type = StringArgumentType.getString(ctx, "type");
                                            return 查看类型详情(ctx.getSource(), type);
                                        })
                                )
                        )
                )

                // ==================== 技能命令 ====================
                .then(Commands.literal("skill")
                        // [普通] /puellamagi skill list
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出技能(ctx.getSource()))
                        )
                        // [OP] /puellamagi skill use <技能>
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
                ));
    }

    // ==================== 契约命令实现 ====================

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
                sb.append("- ").append(id.getPath())
                        .append(" (").append(series.获取名称().getString()).append(")\n");
            });
        }
        sb.append("共 ").append(seriesIds.size()).append(" 个系列");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    // ==================== 变身命令实现 ====================

    private static int 执行变身(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        // 检查是否已契约
        if (!契约管理器.可以变身(player)) {
            source.sendFailure(Component.literal("尚未签订契约，无法变身"));
            return 0;
        }

        // 从契约获取类型
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

        // 契约状态
        能力工具.获取契约能力(player).ifPresent(contract -> {
            sb.append("契约: ").append(contract.是否已契约() ? "已签订" : "未签订").append("\n");
            if (contract.是否已契约()) {
                sb.append("系列: ").append(contract.获取系列ID()).append("\n");
                sb.append("  类型: ").append(contract.获取类型ID()).append("\n");
            }
        });

        // 变身状态
        能力工具.获取变身能力完整(player).ifPresent(cap -> {
            sb.append("变身: ").append(cap.是否已变身() ? "已变身" : "未变身").append("\n");
            if (cap.是否已变身()) {
                sb.append("  阶段: ").append(cap.获取当前阶段索引()).append("\n");
            }
        });

        // 能力状态
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

    // ==================== 类型命令实现 ====================

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
                                    "模型: " + (girlType.获取默认模型() != null ? girlType.获取默认模型() : "无")
                    ), false);
                },
                () -> source.sendFailure(Component.literal("未找到类型: " + typeId))
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
