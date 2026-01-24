// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/假死状态同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 假死状态同步包
 *
 * 服务端→ 客户端
 * 支持两种模式：
 * 1. 同步自己的假死状态（targetUUID == null）
 * 2. 同步其他玩家的假死状态（targetUUID != null）
 */
public class 假死状态同步包 {

    private final boolean 是否假死;
    private final UUID 目标玩家UUID;

    /**
     * 同步自己的假死状态
     */
    public 假死状态同步包(boolean 是否假死) {
        this.是否假死 = 是否假死;
        this.目标玩家UUID = null;
    }

    /**
     * 同步其他玩家的假死状态
     */
    public 假死状态同步包(boolean 是否假死, UUID 目标玩家UUID) {
        this.是否假死 = 是否假死;
        this.目标玩家UUID = 目标玩家UUID;
    }

    public static void encode(假死状态同步包 msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.是否假死);
        buf.writeBoolean(msg.目标玩家UUID != null);
        if (msg.目标玩家UUID != null) {
            buf.writeUUID(msg.目标玩家UUID);
        }
    }

    public static 假死状态同步包 decode(FriendlyByteBuf buf) {
        boolean 是否假死 = buf.readBoolean();
        boolean 有目标 = buf.readBoolean();
        UUID 目标UUID = 有目标 ? buf.readUUID() : null;

        if (目标UUID != null) {
            return new 假死状态同步包(是否假死, 目标UUID);
        } else {
            return new 假死状态同步包(是否假死);
        }
    }

    public static void handle(假死状态同步包 msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (msg.目标玩家UUID != null) {
                假死状态处理器.设置其他玩家假死状态(msg.目标玩家UUID, msg.是否假死);
            } else {
                假死状态处理器.设置客户端假死状态(msg.是否假死);
            }
        });
        });ctx.get().setPacketHandled(true);
    }
}
