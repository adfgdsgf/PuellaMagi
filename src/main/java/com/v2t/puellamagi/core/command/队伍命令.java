// 文件路径: src/main/java/com/v2t/puellamagi/core/command/队伍命令.java

package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍数据;
import com.v2t.puellamagi.system.team.队伍世界数据;
import com.v2t.puellamagi.system.team.队伍邀请管理器;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 队伍命令
 *
 * /puellamagi team create- 创建队伍
 * /puellamagi team leave             - 离开队伍
 * /puellamagi team kick<玩家>       - 踢出成员（队长）
 * /puellamagi team disband           - 解散队伍（队长）
 * /puellamagi team add <玩家>        - 强制拉人（OP）
 * /puellamagi team invite <玩家>     - 邀请玩家
 * /puellamagi team accept <邀请者>   - 接受邀请
 * /puellamagi team decline <邀请者>  - 拒绝邀请
 * /puellamagi team invites           - 查看待处理邀请
 * /puellamagi team info- 查看队伍信息
 * /puellamagi team list              - 列出所有队伍（OP）
 */
public class 队伍命令 {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("puellamagi")
                .then(Commands.literal("team")
                        .then(Commands.literal("create")
                                .executes(队伍命令::执行创建))
                        .then(Commands.literal("leave")
                                .executes(队伍命令::执行离开))
                        .then(Commands.literal("kick")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(队伍命令::执行踢出)))
                        .then(Commands.literal("disband")
                                .executes(队伍命令::执行解散))
                        .then(Commands.literal("add")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(队伍命令::执行强制拉人)))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(队伍命令::执行邀请)))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("inviter", EntityArgument.player())
                                        .executes(队伍命令::执行接受邀请)))
                        .then(Commands.literal("decline")
                                .then(Commands.argument("inviter", EntityArgument.player())
                                        .executes(队伍命令::执行拒绝邀请)))
                        .then(Commands.literal("invites")
                                .executes(队伍命令::执行查看邀请))
                        .then(Commands.literal("info")
                                .executes(队伍命令::执行查看信息))
                        .then(Commands.literal("list")
                                .requires(source -> source.hasPermission(2))
                                .executes(队伍命令::执行列出所有队伍))));
    }

    // ==================== 基本操作 ====================

    private static int 执行创建(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        var result = 队伍管理器.创建队伍(player);
        发送结果(player, result);
        return result.成功() ? 1 : 0;
    }

    private static int 执行离开(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        var result = 队伍管理器.离开队伍(player);
        发送结果(player, result);
        return result.成功() ? 1 : 0;
    }

    private static int 执行踢出(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            var result = 队伍管理器.踢出成员(player, target.getUUID());
            发送结果(player, result);
            if (result.成功()) {
                target.displayClientMessage(
                        Component.translatable("message.puellamagi.team.kicked")
                                .withStyle(ChatFormatting.YELLOW),
                        false
                );
            }
            return result.成功() ? 1 : 0;
        } catch (Exception e) {
            发送错误(player, "message.puellamagi.team.player_not_found");
            return 0;
        }
    }

    private static int 执行解散(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        var result = 队伍管理器.解散队伍(player);
        发送结果(player, result);
        return result.成功() ? 1 : 0;
    }

    private static int 执行强制拉人(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            var result = 队伍管理器.强制拉人(player, target);
            发送结果(player, result);
            if (result.成功()) {
                target.displayClientMessage(
                        Component.translatable("message.puellamagi.team.force_added",player.getDisplayName())
                                .withStyle(ChatFormatting.GREEN),
                        false
                );
            }
            return result.成功() ? 1 : 0;
        } catch (Exception e) {
            发送错误(player, "message.puellamagi.team.player_not_found");
            return 0;
        }
    }

    // ==================== 邀请操作 ====================

    private static int 执行邀请(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            var result = 队伍邀请管理器.发送邀请(player, target);
            发送结果(player, result);

            if (result.成功()) {
                // 通知被邀请者
                target.displayClientMessage(
                        Component.translatable("message.puellamagi.team.invite_received",
                                        player.getDisplayName())
                                .withStyle(ChatFormatting.AQUA),
                        false
                );
                target.displayClientMessage(
                        Component.translatable("message.puellamagi.team.invite_hint")
                                .withStyle(ChatFormatting.GRAY),
                        false
                );
            }
            return result.成功() ? 1 : 0;
        } catch (Exception e) {
            发送错误(player, "message.puellamagi.team.player_not_found");
            return 0;
        }
    }

    private static int 执行接受邀请(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        try {
            ServerPlayer inviter = EntityArgument.getPlayer(context, "inviter");
            var result = 队伍邀请管理器.接受邀请(player, inviter.getUUID());
            发送结果(player, result);

            if (result.成功()) {
                // 通知邀请者
                inviter.displayClientMessage(
                        Component.translatable("message.puellamagi.team.invite_accepted_notify",
                                        player.getDisplayName())
                                .withStyle(ChatFormatting.GREEN),
                        false
                );}
            return result.成功() ? 1 : 0;
        } catch (Exception e) {
            发送错误(player, "message.puellamagi.team.player_not_found");
            return 0;
        }
    }

    private static int 执行拒绝邀请(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        try {
            ServerPlayer inviter = EntityArgument.getPlayer(context, "inviter");
            var result = 队伍邀请管理器.拒绝邀请(player, inviter.getUUID());
            发送结果(player, result);

            if (result.成功()) {
                inviter.displayClientMessage(
                        Component.translatable("message.puellamagi.team.invite_declined_notify",
                                        player.getDisplayName())
                                .withStyle(ChatFormatting.YELLOW),
                        false
                );
            }
            return result.成功() ? 1 : 0;
        } catch (Exception e) {
            发送错误(player, "message.puellamagi.team.player_not_found");
            return 0;
        }
    }

    private static int 执行查看邀请(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        long currentTime = player.level().getGameTime();
        List<队伍邀请管理器.邀请数据> invites =
                队伍邀请管理器.获取待处理邀请(player.getUUID(), currentTime);

        if (invites.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.puellamagi.team.invites.empty")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 0;
        }

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.invites.header", invites.size())
                        .withStyle(ChatFormatting.AQUA),
                false
        );

        for (队伍邀请管理器.邀请数据 invite : invites) {
            ServerPlayer inviter = server.getPlayerList().getPlayer(invite.邀请者UUID());
            String inviterName = inviter != null
                    ? inviter.getName().getString()
                    : invite.邀请者UUID().toString().substring(0, 8);

            long remainingTicks = (invite.创建时间() + 20 * 60) - currentTime;
            int remainingSeconds = (int) Math.max(0, remainingTicks / 20);

            player.displayClientMessage(
                    Component.translatable("message.puellamagi.team.invites.entry",
                                    inviterName, remainingSeconds)
                            .withStyle(ChatFormatting.WHITE),
                    false
            );
        }

        return 1;
    }

    // ==================== 信息查看 ====================

    private static int 执行查看信息(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        队伍世界数据 worldData = 队伍世界数据.获取(server);
        Optional<队伍数据> teamOpt = worldData.获取玩家队伍(player.getUUID());

        if (teamOpt.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.puellamagi.team.no_team")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 0;
        }

        队伍数据 team = teamOpt.get();

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.info.header")
                        .withStyle(ChatFormatting.GOLD),
                false
        );

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.info.id",team.获取队伍ID().toString().substring(0, 8))
                        .withStyle(ChatFormatting.GRAY),
                false
        );

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.info.member_count",
                                team.获取成员数量())
                        .withStyle(ChatFormatting.GRAY),
                false
        );

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.info.members")
                        .withStyle(ChatFormatting.GRAY),
                false
        );

        List<UUID> members = team.获取所有成员UUID();
        for (UUID memberUUID : members) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
            String name = member != null
                    ? member.getName().getString()
                    : memberUUID.toString().substring(0, 8);
            boolean isLeader = team.是队长(memberUUID);
            boolean isOnline = member != null;

            ChatFormatting nameColor = isOnline ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;
            String prefix = isLeader ? "★ " : "  ";

            Component line;
            if (isOnline) {
                line = Component.literal(prefix + name).withStyle(nameColor);
            } else {
                line = Component.literal(prefix + name + " ")
                        .withStyle(nameColor)
                        .append(Component.translatable("message.puellamagi.team.info.offline")
                                .withStyle(ChatFormatting.DARK_GRAY));
            }

            player.displayClientMessage(line, false);
        }

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.info.footer")
                        .withStyle(ChatFormatting.GOLD),
                false
        );

        return 1;
    }

    private static int 执行列出所有队伍(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        队伍世界数据 worldData = 队伍世界数据.获取(server);
        var allTeams = worldData.获取所有队伍();

        if (allTeams.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.puellamagi.team.list.empty")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 0;
        }

        player.displayClientMessage(
                Component.translatable("message.puellamagi.team.list.header", allTeams.size())
                        .withStyle(ChatFormatting.GOLD),
                false
        );

        for (队伍数据 team : allTeams) {
            ServerPlayer leader = server.getPlayerList().getPlayer(team.获取队长UUID());
            String leaderName = leader != null
                    ? leader.getName().getString()
                    : team.获取队长UUID().toString().substring(0, 8);

            player.displayClientMessage(
                    Component.literal("[" + team.获取队伍ID().toString().substring(0, 8) + "] ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable("message.puellamagi.team.list.entry",
                                            leaderName, team.获取成员数量())
                                    .withStyle(ChatFormatting.WHITE)),
                    false
            );
        }

        return 1;
    }

    // ==================== 工具方法 ====================

    private static void 发送结果(ServerPlayer player, 队伍管理器.操作结果 result) {
        ChatFormatting color = result.成功() ? ChatFormatting.GREEN : ChatFormatting.RED;
        player.displayClientMessage(
                Component.translatable(result.消息()).withStyle(color),
                false
        );
    }

    private static void 发送错误(ServerPlayer player, String translationKey) {
        player.displayClientMessage(
                Component.translatable(translationKey).withStyle(ChatFormatting.RED),
                false
        );
    }
}
