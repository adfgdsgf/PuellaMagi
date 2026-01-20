// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/时停状态同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.timestop.TimeStop;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 时停状态同步包（S2C）
 *
 * 使用实体ID和坐标而非UUID，与Roundabout保持一致
 */
public class 时停状态同步包 {

    public enum 同步类型 {
        开始时停,
        结束时停
    }

    private final 同步类型 类型;
    private final int 实体ID;
    private final double x;
    private final double y;
    private final double z;
    private final double 范围;

    // 开始时停构造
    public 时停状态同步包(int entityId, double x, double y, double z, double range) {
        this.类型 = 同步类型.开始时停;
        this.实体ID = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.范围 = range;
    }

    // 结束时停构造
    public 时停状态同步包(int entityId) {
        this.类型 = 同步类型.结束时停;
        this.实体ID = entityId;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.范围 = 0;
    }

    // 完整构造
    private 时停状态同步包(同步类型 type, int entityId, double x, double y, double z, double range) {
        this.类型 = type;
        this.实体ID = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.范围 = range;
    }

    public static void encode(时停状态同步包 packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.类型);
        buf.writeInt(packet.实体ID);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeDouble(packet.范围);
    }

    public static 时停状态同步包 decode(FriendlyByteBuf buf) {
        同步类型 type = buf.readEnum(同步类型.class);
        int entityId = buf.readInt();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double range = buf.readDouble();
        return new 时停状态同步包(type, entityId, x, y, z, range);
    }

    public static void handle(时停状态同步包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPacketHandler.handleTimestop(packet);
        });
        ctx.get().setPacketHandled(true);
    }

    //==================== 工厂方法 ====================

    public static 时停状态同步包 开始(int entityId, double x, double y, double z) {
        return new 时停状态同步包(entityId, x, y, z, -1); // -1 = 无限范围
    }

    public static 时停状态同步包 开始(int entityId, double x, double y, double z, double range) {
        return new 时停状态同步包(entityId, x, y, z, range);
    }

    public static 时停状态同步包 结束(int entityId) {
        return new 时停状态同步包(entityId);
    }

    /**
     * 客户端包处理器
     */
    public static class ClientPacketHandler {
        public static void handleTimestop(时停状态同步包 packet) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            TimeStop timeStop = (TimeStop) mc.level;

            PuellaMagi.LOGGER.info("[S2C] 收到时停同步: {} - 实体ID={}", packet.类型, packet.实体ID);

            switch (packet.类型) {
                case 开始时停 -> timeStop.puellamagi$addTimeStopperClient(packet.实体ID, packet.x, packet.y, packet.z, packet.范围);
                case 结束时停 -> timeStop.puellamagi$removeTimeStopperClient(packet.实体ID);
            }
        }
    }
}
