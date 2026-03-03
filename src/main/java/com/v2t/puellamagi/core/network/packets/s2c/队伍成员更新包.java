// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/队伍成员更新包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：队伍成员变动事件通知
 *
 * 轻量级通知包，用于客户端UI提示（Toast/动画等）
 * 实际数据更新由 队伍数据同步包负责
 * 本包仅告知"发生了什么事"
 */
public class 队伍成员更新包 {

    /**
     * 事件类型
     */
    public enum 事件类型 {
        成员加入,
        成员离开,
        成员被踢,
        队伍解散,
        队长转移;

        public void writeTo(FriendlyByteBuf buf) {
            buf.writeEnum(this);
        }

        public static 事件类型 readFrom(FriendlyByteBuf buf) {
            return buf.readEnum(事件类型.class);
        }
    }

    private final 事件类型 事件;
    /** 相关玩家UUID（加入/离开/被踢的玩家，或新队长） */
    private final UUID 相关玩家UUID;
    /** 相关玩家名称（避免客户端反查） */
    private final String 相关玩家名称;

    public 队伍成员更新包(事件类型 event, UUID relatedUUID, String relatedName) {
        this.事件 = event;
        this.相关玩家UUID = relatedUUID;
        this.相关玩家名称 = relatedName;
    }

    public static void encode(队伍成员更新包 packet, FriendlyByteBuf buf) {
        packet.事件.writeTo(buf);
        buf.writeUUID(packet.相关玩家UUID);
        buf.writeUtf(packet.相关玩家名称);
    }

    public static 队伍成员更新包 decode(FriendlyByteBuf buf) {
        事件类型 event = 事件类型.readFrom(buf);
        UUID uuid = buf.readUUID();
        String name = buf.readUtf();
        return new 队伍成员更新包(event, uuid, name);
    }

    public static void handle(队伍成员更新包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // 目前仅存储事件，供UI层查询
            // 后续Phase T.5实现UI时，可在此触发Toast/动画
            // 例如：客户端队伍缓存.添加事件(packet.事件, packet.相关玩家名称);
        });
        ctx.setPacketHandled(true);
    }

    // ==================== Getter ====================

    public 事件类型 获取事件() {
        return 事件;
    }

    public UUID 获取相关玩家UUID() {
        return 相关玩家UUID;
    }

    public String 获取相关玩家名称() {
        return 相关玩家名称;
    }
}
