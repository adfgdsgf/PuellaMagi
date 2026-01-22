package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 假死状态同步包
 *
 * 服务端 → 客户端
 * 通知客户端进入/退出假死状态
 */
public class 假死状态同步包 {

    private final boolean 是否假死;

    public 假死状态同步包(boolean 是否假死) {
        this.是否假死 = 是否假死;
    }

    public static void encode(假死状态同步包 msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.是否假死);
    }

    public static 假死状态同步包 decode(FriendlyByteBuf buf) {
        return new 假死状态同步包(buf.readBoolean());
    }

    public static void handle(假死状态同步包 msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                假死状态处理器.设置客户端假死状态(msg.是否假死);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
