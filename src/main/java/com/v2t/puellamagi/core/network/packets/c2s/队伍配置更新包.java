// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/队伍配置更新包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.team.队伍同步工具;
import com.v2t.puellamagi.system.team.队伍世界数据;
import com.v2t.puellamagi.system.team.队伍成员数据;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：更新个人配置
 *
 * 玩家修改自己的队伍个人配置项（友伤、觉醒等开关）
 * 修改后同步给所有队友
 */
public class 队伍配置更新包 {

    private final String 配置键;
    private final boolean 值;

    public 队伍配置更新包(String key, boolean value) {
        this.配置键 = key;
        this.值 = value;
    }

    public static void encode(队伍配置更新包 packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.配置键);
        buf.writeBoolean(packet.值);
    }

    public static 队伍配置更新包 decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        boolean value = buf.readBoolean();
        return new 队伍配置更新包(key, value);
    }

    public static void handle(队伍配置更新包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            队伍世界数据 worldData = 队伍世界数据.获取(server);

            UUID teamId = worldData.获取玩家队伍ID(player.getUUID());
            if (teamId == null) {
                player.sendSystemMessage(
                        Component.translatable("message.puellamagi.team.error.no_team")
                );
                return;
            }

            // 获取成员数据并修改配置
            worldData.获取玩家队伍(player.getUUID()).ifPresent(team -> {
                队伍成员数据 memberData = team.获取成员数据(player.getUUID());
                if (memberData == null) return;

                boolean success = memberData.获取配置().设置配置(packet.配置键, packet.值);

                if (success) {
                    worldData.标记已修改();

                    // 同步给所有队友（配置变更需要被队友知晓）
                    队伍同步工具.同步给所有在线成员(server, teamId);
                } else {
                    player.sendSystemMessage(
                            Component.translatable("message.puellamagi.team.error.invalid_config_key")
                    );
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
