package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 录制输入上报包（C2S）
 *
 * 录制期间客户端每tick发送给服务端
 * 包含：移动输入 + 视角 + 按键状态
 */
public class 录制输入上报包 {

    private final float 前后输入;
    private final float 左右输入;
    private final boolean 跳跃;
    private final boolean 潜行;
    private final boolean 冲刺;
    private final float yRot;
    private final float xRot;
    private final int 选中槽位;
    private final List<玩家输入帧.按键状态> 按键列表;

    public 录制输入上报包(float forward, float left, boolean jumping,boolean sneaking, boolean sprinting,
                          float yRot, float xRot, int selectedSlot,
                          List<玩家输入帧.按键状态> keyStates) {
        this.前后输入 = forward;
        this.左右输入 = left;
        this.跳跃 = jumping;
        this.潜行 = sneaking;
        this.冲刺 = sprinting;
        this.yRot = yRot;
        this.xRot = xRot;
        this.选中槽位 = selectedSlot;
        this.按键列表 = keyStates != null ? keyStates : new ArrayList<>();
    }

    // ==================== 编解码 ====================

    public static void encode(录制输入上报包 packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.前后输入);
        buf.writeFloat(packet.左右输入);
        buf.writeBoolean(packet.跳跃);
        buf.writeBoolean(packet.潜行);
        buf.writeBoolean(packet.冲刺);
        buf.writeFloat(packet.yRot);
        buf.writeFloat(packet.xRot);
        buf.writeVarInt(packet.选中槽位);

        buf.writeVarInt(packet.按键列表.size());
        for (玩家输入帧.按键状态 key : packet.按键列表) {
            key.encode(buf);
        }
    }

    public static 录制输入上报包 decode(FriendlyByteBuf buf) {
        float forward = buf.readFloat();
        float left = buf.readFloat();
        boolean jumping = buf.readBoolean();
        boolean sneaking = buf.readBoolean();
        boolean sprinting = buf.readBoolean();
        float yRot = buf.readFloat();
        float xRot = buf.readFloat();
        int selectedSlot = buf.readVarInt();

        int keyCount = buf.readVarInt();
        List<玩家输入帧.按键状态> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add(玩家输入帧.按键状态.decode(buf));
        }

        return new 录制输入上报包(forward, left, jumping, sneaking, sprinting,
                yRot, xRot, selectedSlot, keys);
    }

    // ==================== 处理 ====================

    public static void handle(录制输入上报包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            玩家输入帧 input = new 玩家输入帧(
                    packet.前后输入, packet.左右输入,
                    packet.跳跃, packet.潜行, packet.冲刺,
                    packet.yRot, packet.xRot,
                    packet.选中槽位,
                    packet.按键列表
            );

            录制管理器.接收客户端输入(player.getUUID(), input);
        });
        ctx.get().setPacketHandled(true);
    }
}
