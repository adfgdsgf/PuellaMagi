package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 录制状态通知包（S2C）
 *
 * 服务端在录制开始/停止时发给被录制的玩家
 * 客户端收到后设置"录制中"标记
 * 录制中 → 客户端每tick发送录制输入上报包（C2S）
 *
 * 为什么需要服务端通知：
 * 录制由使用者发起，被录制的可能还有其他玩家
 * 只有服务端知道哪些玩家被录制了
 */
public class 录制状态通知包 {

    private final boolean 开始;

    public 录制状态通知包(boolean start) {
        this.开始 = start;
    }

    public static void encode(录制状态通知包 packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.开始);
    }

    public static 录制状态通知包 decode(FriendlyByteBuf buf) {
        return new 录制状态通知包(buf.readBoolean());
    }

    public static void handle(录制状态通知包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            客户端复刻管理器.设置录制中(packet.开始);
        });
        ctx.get().setPacketHandled(true);
    }
}
