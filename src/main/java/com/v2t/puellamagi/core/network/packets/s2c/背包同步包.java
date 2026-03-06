package com.v2t.puellamagi.core.network.packets.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 背包同步包（S2C）
 *
 * 强制同步玩家背包到客户端
 *
 * 为什么不用MC原版的ClientboundContainerSetContentPacket：
 * 原版包有stateId校验，如果stateId不匹配客户端会忽略
 * 我们的包没有校验，无条件覆盖客户端背包内容
 *
 * 包含：36个背包槽 + 4个装备槽 + 1个副手 + 选中槽位= 42项
 */
public class 背包同步包 {

    /** 41个物品（36背包 + 4装备 + 1副手） */
    private final ItemStack[] 物品列表;
    private final int 选中槽位;

    public 背包同步包(ItemStack[] items, int selectedSlot) {
        this.物品列表 = items;
        this.选中槽位 = selectedSlot;
    }

    /**
     * 从服务端玩家构建
     */
    public static 背包同步包 从玩家构建(net.minecraft.server.level.ServerPlayer player) {
        // Inventory: 0-35背包, 36-39装备, 40副手
        ItemStack[] items = new ItemStack[41];
        for (int i = 0; i < 41; i++) {
            items[i] = player.getInventory().getItem(i).copy();
        }
        return new 背包同步包(items, player.getInventory().selected);
    }

    //==================== 编解码 ====================

    public static void encode(背包同步包 packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.选中槽位);
        for (int i = 0; i < 41; i++) {
            buf.writeItem(packet.物品列表[i]);
        }
    }

    public static 背包同步包 decode(FriendlyByteBuf buf) {
        int selectedSlot = buf.readVarInt();
        ItemStack[] items = new ItemStack[41];
        for (int i = 0; i < 41; i++) {
            items[i] = buf.readItem();
        }
        return new 背包同步包(items, selectedSlot);
    }

    // ==================== 客户端处理 ====================

    public static void handle(背包同步包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            // 无条件覆盖背包
            for (int i = 0; i < 41 && i < player.getInventory().getContainerSize(); i++) {
                player.getInventory().setItem(i, packet.物品列表[i].copy());
            }
            player.getInventory().selected = packet.选中槽位;
        });
        ctx.get().setPacketHandled(true);
    }
}
