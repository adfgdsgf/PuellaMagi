package com.v2t.puellamagi.util;

import com.v2t.puellamagi.core.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/**
 * 网络发包简化工具
 * 封装常用的发包操作，避免重复代码
 */
public final class 网络工具 {
    private 网络工具() {}

    /**
     * 客户端发送到服务端
     */
    public static void 发送到服务端(Object packet) {
        ModNetwork.getChannel().sendToServer(packet);
    }

    /**
     * 服务端发送给指定玩家
     */
    public static void 发送给玩家(ServerPlayer player, Object packet) {
        ModNetwork.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * 服务端发送给所有玩家
     */
    public static void 发送给所有玩家(Object packet) {
        ModNetwork.getChannel().send(PacketDistributor.ALL.noArg(), packet);
    }

    /**
     * 服务端发送给追踪该实体的所有玩家
     */
    public static void 发送给追踪者(Entity entity, Object packet) {
        ModNetwork.getChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    /**
     * 服务端发送给追踪该实体的所有玩家（包括实体自己，如果是玩家）
     */
    public static void 发送给追踪者和自己(Entity entity, Object packet) {
        ModNetwork.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}
