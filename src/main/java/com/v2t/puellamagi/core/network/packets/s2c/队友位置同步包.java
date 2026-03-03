package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端队伍缓存;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 队友位置同步包（S2C）
 *
 * 服务端每秒同步一次所有队友的位置信息
 * 用于队友头像HUD显示（包括超出渲染距离的队友）
 *
 * 只发送在线队友，离线/跨维度由客户端根据维度判断是否显示
 */
public class 队友位置同步包 {

    /**
     * 队友位置条目
     *
     * @param uuid      队友UUID
     * @param x         世界坐标X
     * @param y         世界坐标Y（眼睛高度）
     * @param z         世界坐标Z
     * @param dimension 所在维度ID
     */
    public record 条目(UUID uuid, double x, double y, double z, ResourceLocation dimension) {}

    private final List<条目> 位置列表;

    public 队友位置同步包(List<条目> positions) {
        this.位置列表 = positions;
    }

    //==================== 编解码 ====================

    public static void encode(队友位置同步包 packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.位置列表.size());
        for (条目 entry : packet.位置列表) {
            buf.writeUUID(entry.uuid);
            buf.writeDouble(entry.x);
            buf.writeDouble(entry.y);
            buf.writeDouble(entry.z);
            buf.writeResourceLocation(entry.dimension);
        }
    }

    public static 队友位置同步包 decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<条目> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new 条目(buf.readUUID(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readResourceLocation()
            ));
        }
        return new 队友位置同步包(list);
    }

    // ==================== 处理 ====================

    public static void handle(队友位置同步包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            客户端队伍缓存.更新队友位置(packet.位置列表);
        });
        ctx.get().setPacketHandled(true);
    }
}
