package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能槽位数据;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：技能键按下
 * 用于蓄力、引导、蓄力切换类型技能
 * 携带Ctrl修饰键状态，用于组合键操作（如Ctrl+技能键取消录制）
 */
public class 技能按下请求包 {

    private final int 槽位索引;
    private final boolean 修饰键按下;

    public 技能按下请求包(int slotIndex, boolean ctrlDown) {
        this.槽位索引 = slotIndex;
        this.修饰键按下 = ctrlDown;
    }

    public static void encode(技能按下请求包 packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.槽位索引);
        buf.writeBoolean(packet.修饰键按下);
    }

    public static 技能按下请求包 decode(FriendlyByteBuf buf) {
        return new 技能按下请求包(buf.readInt(), buf.readBoolean());
    }

    public static void handle(技能按下请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            能力工具.获取技能能力(player).ifPresent(cap -> {
                var preset = cap.获取当前预设();
                技能槽位数据 slot = preset.获取槽位(packet.槽位索引);

                if (slot != null && !slot.是否为空()) {
                    ResourceLocation skillId = slot.获取技能ID();
                    技能管理器.按键按下(player, skillId, packet.修饰键按下);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
