// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/布局更新请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.skill.布局配置;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：更新技能栏布局
 */
public class 布局更新请求包 {

    private final CompoundTag 布局数据;

    public 布局更新请求包(布局配置 layout) {
        this.布局数据 = layout.写入NBT();
    }

    private 布局更新请求包(CompoundTag data) {
        this.布局数据 = data;
    }

    public static void encode(布局更新请求包 packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.布局数据);
    }

    public static 布局更新请求包 decode(FriendlyByteBuf buf) {
        return new 布局更新请求包(buf.readNbt());
    }

    public static void handle(布局更新请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || packet.布局数据 == null) return;

            能力工具.获取技能能力(player).ifPresent(cap -> {
                布局配置 layout = 布局配置.从NBT读取(packet.布局数据);
                cap.获取当前预设().设置布局(layout);
            });
        });
        ctx.setPacketHandled(true);
    }
}
