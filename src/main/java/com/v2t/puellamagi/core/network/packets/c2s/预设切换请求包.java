// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/预设切换请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.core.event.通用事件;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求切换技能预设
 *
 * 支持：
 * - 上一个/下一个预设（循环）
 * - 直接切换到指定索引
 */
public class 预设切换请求包 {

    private final 切换类型 类型;
    private final int 目标索引;

    public enum 切换类型 {
        下一个,
        上一个,
        指定索引
    }

    //==================== 构造器 ====================

    /**
     * 切换到下一个/上一个预设
     */
    public 预设切换请求包(boolean isNext) {
        this.类型 = isNext ? 切换类型.下一个 : 切换类型.上一个;
        this.目标索引 = -1;
    }

    /**
     * 直接切换到指定索引
     */
    public 预设切换请求包(int targetIndex) {
        this.类型 = 切换类型.指定索引;
        this.目标索引 = targetIndex;
    }

    private 预设切换请求包(切换类型 type, int targetIndex) {
        this.类型 = type;
        this.目标索引 = targetIndex;
    }

    // ==================== 编码 ====================

    public static void encode(预设切换请求包 packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.类型);
        if (packet.类型 == 切换类型.指定索引) {
            buf.writeInt(packet.目标索引);
        }
    }

    // ==================== 解码 ====================

    public static 预设切换请求包 decode(FriendlyByteBuf buf) {
        切换类型 type = buf.readEnum(切换类型.class);
        int targetIndex = -1;
        if (type == 切换类型.指定索引) {
            targetIndex = buf.readInt();
        }
        return new 预设切换请求包(type, targetIndex);
    }

    // ==================== 处理（服务端） ====================

    public static void handle(预设切换请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            能力工具.获取技能能力(player).ifPresent(cap -> {
                switch (packet.类型) {
                    case 下一个 -> cap.下一个预设();
                    case 上一个 -> cap.上一个预设();
                    case 指定索引 -> cap.切换预设(packet.目标索引);
                }

                // 通知玩家
                String presetName = cap.获取当前预设().获取名称();

                // 同步数据回客户端
                通用事件.同步技能能力(player);
            });
        });
        ctx.setPacketHandled(true);
    }
}
