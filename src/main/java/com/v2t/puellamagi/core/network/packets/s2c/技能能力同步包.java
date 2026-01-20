// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/技能能力同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.util.能力工具;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端→ 客户端：同步技能能力数据
 */
public class 技能能力同步包 {

    private final UUID 玩家UUID;
    private final CompoundTag 数据;

    public 技能能力同步包(UUID playerUUID, CompoundTag data) {
        this.玩家UUID = playerUUID;
        this.数据 = data;
    }

    public static void encode(技能能力同步包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.玩家UUID);
        buf.writeNbt(packet.数据);
    }

    public static 技能能力同步包 decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        CompoundTag data = buf.readNbt();
        return new 技能能力同步包(uuid, data);
    }

    public static void handle(技能能力同步包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player player = mc.level.getPlayerByUUID(packet.玩家UUID);
            if (player == null) return;

            能力工具.获取技能能力(player).ifPresent(cap -> {
                cap.从NBT读取(packet.数据);
            });
        });
        ctx.setPacketHandled(true);
    }
}
