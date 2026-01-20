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
 * 服务端 → 客户端：同步变身状态
 * 用于登录、变身、解除变身时同步数据
 */
public class 变身同步包 {

    private final UUID 玩家UUID;
    private final CompoundTag 数据;

    public 变身同步包(UUID playerUUID, CompoundTag data) {
        this.玩家UUID = playerUUID;
        this.数据 = data;
    }

    // ==================== 编码 ====================
    public static void encode(变身同步包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.玩家UUID);
        buf.writeNbt(packet.数据);
    }

    // ==================== 解码 ====================
    public static 变身同步包 decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        CompoundTag data = buf.readNbt();
        return new 变身同步包(uuid, data);
    }

    // ==================== 处理（客户端） ====================
    public static void handle(变身同步包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player player = mc.level.getPlayerByUUID(packet.玩家UUID);
            if (player == null) return;

            能力工具.获取变身能力(player).ifPresent(cap -> {
                cap.从NBT读取(packet.数据);});
        });
        ctx.setPacketHandled(true);
    }
}
