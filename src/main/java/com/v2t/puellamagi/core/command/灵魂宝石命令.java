package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 灵魂宝石相关命令
 *
 * /puellamagi soulgem give- 发放灵魂宝石
 * /puellamagi soulgem status- 查看灵魂宝石状态
 * /puellamagi soulgem crack          - 使灵魂宝石龟裂
 * /puellamagi soulgem destroy        - 使灵魂宝石销毁
 * /puellamagi soulgem repair         - 修复灵魂宝石
 * /puellamagi soulgem regenerate     - 重新生成灵魂宝石
 * /puellamagi corruption set<值>    - 设置污浊度
 * /puellamagi corruption add <值>    - 增加/减少污浊度
 * /puellamagi corruption get- 查看污浊度
 * /puellamagi corruption reset       - 重置污浊度
 */
public class 灵魂宝石命令 {

    private static final int OP_LEVEL = 2;

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        // soulgem 命令组
        root.then(Commands.literal("soulgem")
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 发放灵魂宝石(ctx.getSource()))
                )
                .then(Commands.literal("status")
                        .executes(ctx -> 查看灵魂宝石状态(ctx.getSource()))
                )
                .then(Commands.literal("crack")
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
                ));

        // corruption 命令组
        root.then(Commands.literal("corruption")
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
                )
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .executes(ctx -> 重置污浊度(ctx.getSource()))
                )
        );
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
        } else {灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(player.getServer());
            if (worldData.存在登记(player.getUUID())) {
                source.sendFailure(Component.literal("你已经有灵魂宝石了\n使用 /puellamagi soulgem regenerate 重新生成"));
            } else {
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
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(player.getServer());宝石登记信息 info = worldData.获取登记信息(player.getUUID()).orElse(null);

        if (info != null) {
            sb.append("登记状态: 已登记\n");
            sb.append("有效时间戳: ").append(info.获取有效时间戳()).append("\n");
            sb.append("宝石状态: ").append(info.获取状态().getSerializeName()).append("\n");

            if (info.获取维度() != null && info.获取坐标() != null) {
                sb.append("位置信息:\n");
                sb.append("  存储类型: ").append(info.获取存储类型枚举().获取序列化名()).append("\n");
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
                sb.append("  距离: ").append(String.format("%.1f 格", distResult.距离())).append("\n");
            } else {
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

        损坏上下文 context = 损坏上下文.被动销毁(soulGem,
                player.getUUID(),
                损坏强度.普通,
                "测试命令"
        );

        var result = 灵魂宝石损坏处理器.处理损坏(player.getServer(), context);
        source.sendSuccess(() -> Component.literal("处理结果: " + result.name()), true);
        return 1;
    }

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

        损坏上下文 context = 损坏上下文.被动销毁(
                soulGem,
                player.getUUID(),
                损坏强度.严重,
                "测试命令"
        );

        var result = 灵魂宝石损坏处理器.处理损坏(player.getServer(), context);
        source.sendSuccess(() -> Component.literal("处理结果: " + result.name()), true);
        return 1;
    }

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
            return 1;
        } else {
            source.sendFailure(Component.literal("重新生成失败"));
            return 0;
        }
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
}
