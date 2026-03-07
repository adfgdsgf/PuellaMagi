package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 按键状态上报包（C2S）
 *
 * 录制开始时客户端扫描所有KeyMapping
 * 把当前按住的键名列表发给服务端
 * 服务端存到录制会话里
 *
 * 回放开始时服务端把这些键名发回客户端
 * 客户端设这些键的isDown=true
 * → 所有mod的按键状态都恢复
 * → 不需要知道任何mod的内部状态
 */
public class 按键状态上报包 {

    private final List<String> 按住的键名列表;

    public 按键状态上报包(List<String> heldKeys) {
        this.按住的键名列表 = heldKeys != null ? heldKeys : new ArrayList<>();
    }

    public static void encode(按键状态上报包 packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.按住的键名列表.size());
        for (String key : packet.按住的键名列表) {
            buf.writeUtf(key);
        }
    }

    public static 按键状态上报包 decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<String> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            keys.add(buf.readUtf());
        }
        return new 按键状态上报包(keys);
    }

    public static void handle(按键状态上报包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            录制管理器.接收按键状态上报(player.getUUID(), packet.按住的键名列表);
        });
        ctx.get().setPacketHandled(true);
    }
}
