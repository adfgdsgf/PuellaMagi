// 文件路径: src/main/java/com/v2t/puellamagi/core/command/测试命令.java

package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
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
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.system.soulgem.damage.灵魂宝石损坏处理器;
import com.v2t.puellamagi.system.soulgem.damage.损坏上下文;
import com.v2t.puellamagi.system.soulgem.damage.损坏强度;
import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.system.soulgem.effect.持有状态;
import com.v2t.puellamagi.system.soulgem.effect.距离效果处理器;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import com.v2t.puellamagi.system.soulgem.location.灵魂宝石区块加载器;
import com.v2t.puellamagi.system.soulgem.util.灵魂宝石距离计算;
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
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 开发测试用命令
 *
 * ===OP指令（权限等级2）===
 * /puellamagi contract<系列> <类型> - 签订契约
 * /puellamagi contract remove- 解除契约
 * /puellamagi transform- 变身
 * /puellamagi detransform            - 解除变身
 * /puellamagi skill use <技能>- 释放技能
 * /puellamagi ability current        - 查看当前激活的能力
 * /puellamagi corruption set <值>    - 设置污浊度
 * /puellamagi corruption add <值>    - 增加/减少污浊度
 * /puellamagi corruption reset       - 重置污浊度
 * /puellamagi soulgem give- 发放灵魂宝石
 * /puellamagi soulgem crack          - 使灵魂宝石龟裂
 * /puellamagi soulgem destroy        - 使灵魂宝石销毁
 * /puellamagi soulgem repair         - 修复灵魂宝石
 * /puellamagi soulgem regenerate     - 重新生成灵魂宝石
 *
 * === 普通指令（所有人可用）===
 * /puellamagi status                 - 查看状态
 * /puellamagi contract status        - 查看契约状态
 * /puellamagi series list- 列出所有系列
 * /puellamagi type list- 列出所有类型
 * /puellamagi type info <类型>       - 查看类型详情
 * /puellamagi ability list           - 列出所有注册的能力
 * /puellamagi skill list             - 列出所有注册的技能
 * /puellamagi corruption get- 查看污浊度
 * /puellamagi soulgem status         - 查看灵魂宝石状态
 */
public class 测试命令 {

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

    private static final SuggestionProvider<CommandSourceStack> 技能补全 = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    技能注册表.获取所有技能ID().stream()
                            .map(ResourceLocation::getPath)
                            .collect(Collectors.toList()),
                    builder
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(常量.MOD_ID)

                // ==================== 契约命令 ====================
                .then(Commands.literal("contract")
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
                        .then(Commands.literal("remove")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx ->解除契约(ctx.getSource()))
                        )
                        .then(Commands.literal("status")
                                .executes(ctx -> 查看契约状态(ctx.getSource()))
                        )
                )

                // ==================== 系列命令 ====================
                .then(Commands.literal("series")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出系列(ctx.getSource()))
                        )
                )

                // ==================== 变身命令 ====================
                .then(Commands.literal("transform")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 执行变身(ctx.getSource()))
                )
                .then(Commands.literal("detransform")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 执行解除变身(ctx.getSource()))
                )

                // ==================== 状态命令 ====================
                .then(Commands.literal("status")
                        .executes(ctx -> 查看状态(ctx.getSource()))
                )

                // ==================== 能力命令 ====================
                .then(Commands.literal("ability")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出能力(ctx.getSource()))
                        )
                        .then(Commands.literal("current")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 查看当前能力(ctx.getSource()))
                        )
                )

                // ==================== 类型命令 ====================
                .then(Commands.literal("type")
                        .then(Commands.literal("list")
                                .executes(ctx -> 列出类型(ctx.getSource()))
                        )
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
                )

                // ==================== 污浊度命令 ====================
                .then(Commands.literal("corruption")
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            return 设置污浊度(ctx.getSource(), value);
                                        })
                                )
                        )
                        .then(Commands.literal("add")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            return 增加污浊度(ctx.getSource(), value);
                                        })
                                )
                        )
                        .then(Commands.literal("get")
                                .executes(ctx -> 查看污浊度(ctx.getSource()))
                        ).then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 重置污浊度(ctx.getSource()))
                        )
                )

                // ==================== 灵魂宝石命令 ====================
                .then(Commands.literal("soulgem")
                        .then(Commands.literal("give")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 发放灵魂宝石(ctx.getSource()))
                        )
                        .then(Commands.literal("status")
                                .executes(ctx -> 查看灵魂宝石状态(ctx.getSource()))
                        ).then(Commands.literal("crack")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 使灵魂宝石龟裂(ctx.getSource()))
                        )
                        .then(Commands.literal("destroy")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 使灵魂宝石销毁(ctx.getSource()))
                        )
                        .then(Commands.literal("repair")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 修复灵魂宝石(ctx.getSource()))
                        )
                        .then(Commands.literal("regenerate")
                                .requires(source -> source.hasPermission(OP_LEVEL))
                                .executes(ctx -> 重新生成灵魂宝石(ctx.getSource()))
                        )
                )
        );
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
                sb.append("-").append(id.getPath())
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
                sb.append("系列: ").append(contract.获取系列ID()).append("\n");
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

    // ==================== 污浊度命令实现 ====================

    private static int 设置污浊度(CommandSourceStack source, float value) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        污浊度管理器.设置(player, value);
        source.sendSuccess(() -> Component.literal(
                String.format("污浊度已设置为 %.1f", value)), true);
        return 1;
    }

    private static int 增加污浊度(CommandSourceStack source, float value) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (value > 0) {
            boolean success = 污浊度管理器.增加(player, value);
            if (success) {
                source.sendSuccess(() -> Component.literal(
                        String.format("污浊度增加 %.1f", value)), true);
            } else {
                source.sendFailure(Component.literal("污浊度未变化（非灵魂宝石系）"));
            }
        } else if (value < 0) {
            boolean success = 污浊度管理器.减少(player, -value);
            if (success) {
                source.sendSuccess(() -> Component.literal(
                        String.format("污浊度减少 %.1f", -value)), true);
            } else {
                source.sendFailure(Component.literal("污浊度未变化（非灵魂宝石系）"));
            }
        }
        return 1;
    }

    private static int 查看污浊度(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        能力工具.获取污浊度能力(player).ifPresentOrElse(
                cap -> {
                    boolean isSoulGem = 污浊度管理器.是否灵魂宝石系玩家(player);
                    source.sendSuccess(() -> Component.literal(
                            String.format("=== 污浊度状态 ===\n当前值: %.1f / %.1f\n百分比: %.1f%%\n系列: %s",
                                    cap.获取当前值(), cap.获取最大值(), cap.获取百分比() * 100,
                                    isSoulGem ? "灵魂宝石系（活跃）" : "其他（冻结）")), false);
                },
                () -> source.sendFailure(Component.literal("无法获取污浊度数据"))
        );
        return 1;
    }

    private static int 重置污浊度(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        污浊度管理器.重置(player);
        source.sendSuccess(() -> Component.literal("污浊度已重置"), true);
        return 1;
    }

    // ==================== 灵魂宝石命令实现 ====================

    private static int 发放灵魂宝石(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (!能力工具.是灵魂宝石系(player)) {
            source.sendFailure(Component.literal("你不是灵魂宝石系魔法少女\n请先使用 /puellamagi contract soul_gem <类型> 签订契约"));
            return 0;
        }

        boolean success = 灵魂宝石管理器.尝试发放灵魂宝石(player);
        if (success) {
            source.sendSuccess(() -> Component.literal("灵魂宝石已发放"), true);
            return 1;
        } else {
            // 可能已经有登记了
            灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(player.getServer());
            if (worldData.存在登记(player.getUUID())) {
                source.sendFailure(Component.literal("你已经有灵魂宝石了\n使用 /puellamagi soulgem regenerate 重新生成"));} else {
                source.sendFailure(Component.literal("发放失败，请检查契约状态"));
            }
            return 0;
        }
    }

    private static int 查看灵魂宝石状态(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (!能力工具.是灵魂宝石系(player)) {
            source.sendSuccess(() -> Component.literal("你不是灵魂宝石系魔法少女"), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("=== 灵魂宝石状态 ===\n");

        // 从世界数据获取信息
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(player.getServer());
        宝石登记信息 info = worldData.获取登记信息(player.getUUID()).orElse(null);

        if (info != null) {
            sb.append("登记状态: 已登记\n");
            sb.append("有效时间戳: ").append(info.获取有效时间戳()).append("\n");
            sb.append("宝石状态: ").append(info.获取状态().getSerializeName()).append("\n");

            if (info.获取维度() != null && info.获取坐标() != null) {
                sb.append("位置信息:\n");
                sb.append("  存储类型: ").append(info.获取存储类型().获取序列化名()).append("\n");
                sb.append("  维度: ").append(info.获取维度().location()).append("\n");
                sb.append("  坐标: ").append(String.format("%.1f, %.1f, %.1f",
                        info.获取坐标().x, info.获取坐标().y, info.获取坐标().z)).append("\n");

                // 显示持有者信息
                UUID 持有者UUID = info.获取当前持有者UUID();
                if (持有者UUID != null) {
                    ServerPlayer 持有者 = player.getServer().getPlayerList().getPlayer(持有者UUID);
                    if (持有者 != null) {
                        sb.append("  持有者: ").append(持有者.getName().getString()).append(" (在线)\n");
                    } else {
                        sb.append("  持有者: ").append(持有者UUID.toString().substring(0, 8)).append("... (离线)\n");
                    }
                } else {
                    sb.append("  持有者: 无（掉落物/容器）\n");
                }
            } else {
                sb.append("位置信息: 未知\n");
            }

            // 使用统一的距离计算工具
            var distResult = 灵魂宝石距离计算.计算(player, info, player.getServer());
            sb.append("距离计算:\n");
            if (distResult.有效()) {
                sb.append("  距离: ").append(String.format("%.1f 格", distResult.距离())).append("\n");} else {
                sb.append("  距离: 无法计算 (").append(distResult.原因().获取描述()).append(")\n");
            }
            sb.append("  持有者在线: ").append(distResult.持有者在线() ? "是" : "否").append("\n");
        } else {
            sb.append("登记状态: 未登记\n");
        }

        // 距离效果状态
        持有状态 holdState = 距离效果处理器.获取当前状态(player);
        sb.append("持有状态: ").append(holdState.name()).append("\n");

        // 区块加载状态
        boolean chunkLoaded = 灵魂宝石区块加载器.是否有区块加载(player.getUUID());
        sb.append("区块加载: ").append(chunkLoaded ? "是" : "否").append("\n");

        // 假死状态
        if (假死状态处理器.是否假死中(player)) {
            int remaining = 假死状态处理器.获取假死剩余秒数(player);
            sb.append("假死状态: 是（剩余 ").append(remaining).append(" 秒）\n");
        } else {
            sb.append("假死状态: 否\n");
        }

        // 背包中的灵魂宝石
        ItemStack soulGem = 灵魂宝石管理器.查找玩家背包中的灵魂宝石(player);
        if (soulGem != null) {
            灵魂宝石状态 gemState = 灵魂宝石数据.获取状态(soulGem);
            sb.append("背包中的宝石: ").append(gemState.getSerializeName());} else {
            sb.append("背包中的宝石: 无");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    /**
     * 使灵魂宝石龟裂
     *
     * 通过损坏处理器统一入口处理
     */
    private static int 使灵魂宝石龟裂(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        ItemStack soulGem = 灵魂宝石管理器.查找玩家背包中的灵魂宝石(player);
        if (soulGem == null) {
            source.sendFailure(Component.literal("背包中没有找到灵魂宝石"));
            return 0;
        }

        灵魂宝石状态 currentState = 灵魂宝石数据.获取状态(soulGem);
        if (currentState !=灵魂宝石状态.NORMAL) {
            source.sendFailure(Component.literal("灵魂宝石不是正常状态，当前: " + currentState.getSerializeName()));
            return 0;
        }

        // 使用统一的损坏处理器
        损坏上下文 context = 损坏上下文.被动销毁(
                soulGem,
                player.getUUID(),
                损坏强度.普通,// 普通强度必定龟裂
                "测试命令"
        );

        var result = 灵魂宝石损坏处理器.处理损坏(player.getServer(), context);
        source.sendSuccess(() -> Component.literal("处理结果: " + result.name()), true);
        return 1;
    }

    /**
     * 使灵魂宝石销毁
     *
     * 通过损坏处理器统一入口处理
     */
    private static int 使灵魂宝石销毁(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        ItemStack soulGem = 灵魂宝石管理器.查找玩家背包中的灵魂宝石(player);
        if (soulGem == null) {
            source.sendFailure(Component.literal("背包中没有找到灵魂宝石"));
            return 0;
        }

        灵魂宝石状态 currentState = 灵魂宝石数据.获取状态(soulGem);
        if (currentState == 灵魂宝石状态.DESTROYED) {
            source.sendFailure(Component.literal("灵魂宝石已经销毁"));
            return 0;
        }

        // 使用统一的损坏处理器，严重强度直接销毁
        损坏上下文 context = 损坏上下文.被动销毁(
                soulGem,
                player.getUUID(),
                损坏强度.严重,  // 严重强度直接销毁
                "测试命令"
        );

        var result = 灵魂宝石损坏处理器.处理损坏(player.getServer(), context);
        source.sendSuccess(() -> Component.literal("处理结果: " + result.name()), true);
        return 1;
    }

    /**
     * 修复灵魂宝石
     *
     * 通过损坏处理器统一入口处理
     */
    private static int 修复灵魂宝石(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        ItemStack soulGem = 灵魂宝石管理器.查找玩家背包中的灵魂宝石(player);
        if (soulGem == null) {
            source.sendFailure(Component.literal("背包中没有找到灵魂宝石"));
            return 0;
        }

        // 使用统一的损坏处理器修复
        var result = 灵魂宝石损坏处理器.尝试修复(player.getServer(), soulGem, player.getUUID());

        switch (result) {
            case 已修复 -> source.sendSuccess(() -> Component.literal("灵魂宝石已修复"), true);
            case 无需修复 -> source.sendFailure(Component.literal("灵魂宝石状态正常，无需修复"));
            case 已销毁_无效果 -> source.sendFailure(Component.literal("灵魂宝石已销毁，无法修复"));
            default -> source.sendFailure(Component.literal("修复失败: " + result.name()));
        }

        return 1;
    }

    private static int 重新生成灵魂宝石(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (!能力工具.是灵魂宝石系(player)) {
            source.sendFailure(Component.literal("你不是灵魂宝石系魔法少女"));
            return 0;
        }

        boolean success = 灵魂宝石管理器.重新生成灵魂宝石(player);
        if (success) {
            // 管理器内部已显示消息
            return 1;
        } else {
            source.sendFailure(Component.literal("重新生成失败"));
            return 0;
        }
    }
}
