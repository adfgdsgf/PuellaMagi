// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/队伍邀请通知包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端队伍缓存;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：通知玩家收到队伍邀请
 *
 * 客户端收到后存入缓存，供队伍UI显示
 * 同时会在聊天栏显示提示消息
 */
public class 队伍邀请通知包 {

    private final UUID 邀请者UUID;
    private final String 邀请者名称;

    public 队伍邀请通知包(UUID inviterUUID, String inviterName) {
        this.邀请者UUID = inviterUUID;
        this.邀请者名称 = inviterName;
    }

    public static void encode(队伍邀请通知包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.邀请者UUID);
        buf.writeUtf(packet.邀请者名称);
    }

    public static 队伍邀请通知包 decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        String name = buf.readUtf();
        return new 队伍邀请通知包(uuid, name);
    }

    public static void handle(队伍邀请通知包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            客户端队伍缓存.添加邀请(
                    new 客户端队伍缓存.邀请信息(
                            packet.邀请者UUID,
                            packet.邀请者名称,
                            System.currentTimeMillis()
                    )
            );
        });
        ctx.setPacketHandled(true);
    }
}
