// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/污浊度同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端→ 客户端：同步污浊度状态
 */
public class 污浊度同步包 {

    private final float 当前值;
    private final float 最大值;

    public 污浊度同步包(float current, float max) {
        this.当前值 = current;
        this.最大值 = max;
    }

    // ==================== 编码 ====================

    public static void encode(污浊度同步包 packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.当前值);
        buf.writeFloat(packet.最大值);
    }

    // ==================== 解码 ====================

    public static 污浊度同步包 decode(FriendlyByteBuf buf) {
        float current = buf.readFloat();
        float max = buf.readFloat();
        return new 污浊度同步包(current, max);
    }

    // ==================== 处理（客户端） ====================

    public static void handle(污浊度同步包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            能力工具.获取污浊度能力(player).ifPresent(cap -> {
                cap.设置当前值(packet.当前值);
                // 注意：最大值一般不变，但也同步以防万一
            });
        });
        ctx.setPacketHandled(true);
    }
}
