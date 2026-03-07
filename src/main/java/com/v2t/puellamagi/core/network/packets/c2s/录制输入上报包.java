package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.ability.epitaph.录制管理器;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 包含：移动输入 + 视角 + 键盘事件 + 鼠标事件 + 光标位置 + 射线结果
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
    private final List<玩家输入帧.键盘事件> 键盘事件列表;
    private final List<玩家输入帧.鼠标事件> 鼠标事件列表;
    private final double cursorX;
    private final double cursorY;

    private final int hitType;
    private final BlockPos hitBlockPos;
    private final Direction hitDirection;
    private final double hitX, hitY, hitZ;
    private final boolean hitInside;
    private final int hitEntityId;

    public 录制输入上报包(float forward, float left, boolean jumping,boolean sneaking, boolean sprinting,
                          float yRot, float xRot, int selectedSlot,
                          List<玩家输入帧.键盘事件> keyboardEvents,
                          List<玩家输入帧.鼠标事件> mouseEvents,
                          double cursorX, double cursorY,
                          int hitType,
                          BlockPos hitBlockPos, Direction hitDirection,
                          double hitX, double hitY, double hitZ,
                          boolean hitInside, int hitEntityId) {
        this.前后输入 = forward;
        this.左右输入 = left;
        this.跳跃 = jumping;
        this.潜行 = sneaking;
        this.冲刺 = sprinting;
        this.yRot = yRot;
        this.xRot = xRot;
        this.选中槽位 = selectedSlot;
        this.键盘事件列表 = keyboardEvents != null ? keyboardEvents : new ArrayList<>();
        this.鼠标事件列表 = mouseEvents != null ? mouseEvents : new ArrayList<>();
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        this.hitType = hitType;
        this.hitBlockPos = hitBlockPos;
        this.hitDirection = hitDirection;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.hitInside = hitInside;
        this.hitEntityId = hitEntityId;
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

        buf.writeVarInt(packet.键盘事件列表.size());
        for (玩家输入帧.键盘事件 event : packet.键盘事件列表) {
            event.encode(buf);
        }

        buf.writeVarInt(packet.鼠标事件列表.size());
        for (玩家输入帧.鼠标事件 event : packet.鼠标事件列表) {
            event.encode(buf);
        }

        buf.writeDouble(packet.cursorX);
        buf.writeDouble(packet.cursorY);

        buf.writeByte(packet.hitType);
        if (packet.hitType == 1) {
            buf.writeBlockPos(packet.hitBlockPos);
            buf.writeEnum(packet.hitDirection);
            buf.writeDouble(packet.hitX);
            buf.writeDouble(packet.hitY);
            buf.writeDouble(packet.hitZ);
            buf.writeBoolean(packet.hitInside);
        } else if (packet.hitType == 2) {
            buf.writeVarInt(packet.hitEntityId);
            buf.writeDouble(packet.hitX);
            buf.writeDouble(packet.hitY);
            buf.writeDouble(packet.hitZ);
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
        List<玩家输入帧.键盘事件> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add(玩家输入帧.键盘事件.decode(buf));
        }

        int mouseCount = buf.readVarInt();
        List<玩家输入帧.鼠标事件> mouseEvents = new ArrayList<>(mouseCount);
        for (int i = 0; i < mouseCount; i++) {
            mouseEvents.add(玩家输入帧.鼠标事件.decode(buf));
        }

        double cursorX = buf.readDouble();
        double cursorY = buf.readDouble();

        int hitType = buf.readByte();
        BlockPos hitBlockPos = null;
        Direction hitDirection = null;
        double hitX = 0, hitY = 0, hitZ = 0;
        boolean hitInside = false;
        int hitEntityId = -1;

        if (hitType == 1) {
            hitBlockPos = buf.readBlockPos();
            hitDirection = buf.readEnum(Direction.class);
            hitX = buf.readDouble();
            hitY = buf.readDouble();
            hitZ = buf.readDouble();
            hitInside = buf.readBoolean();
        } else if (hitType == 2) {
            hitEntityId = buf.readVarInt();
            hitX = buf.readDouble();
            hitY = buf.readDouble();
            hitZ = buf.readDouble();
        }

        return new 录制输入上报包(forward, left, jumping, sneaking, sprinting,
                yRot, xRot, selectedSlot, keys, mouseEvents,
                cursorX, cursorY,
                hitType, hitBlockPos, hitDirection, hitX, hitY, hitZ, hitInside, hitEntityId);
    }

    // ==================== 处理 ====================

    public static void handle(录制输入上报包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            玩家输入帧 input = new 玩家输入帧(packet.前后输入, packet.左右输入,
                    packet.跳跃, packet.潜行, packet.冲刺,
                    packet.yRot, packet.xRot,
                    packet.选中槽位,
                    packet.键盘事件列表,
                    packet.鼠标事件列表,
                    packet.cursorX, packet.cursorY,
                    packet.hitType,
                    packet.hitBlockPos, packet.hitDirection,
                    packet.hitX, packet.hitY, packet.hitZ,
                    packet.hitInside, packet.hitEntityId
            );

            录制管理器.接收客户端输入(player.getUUID(), input);
        });
        ctx.get().setPacketHandled(true);
    }
}
