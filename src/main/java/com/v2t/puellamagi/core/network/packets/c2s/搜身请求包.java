// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/搜身请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.interaction.menu.搜身菜单;
import com.v2t.puellamagi.system.interaction.搜身管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 搜身请求包 (C2S)
 *
 * 客户端按住修饰键+右键点击玩家后发送
 * 服务端验证条件后，打开搜身菜单
 */
public class 搜身请求包 {

    private final UUID targetUUID;

    public 搜身请求包(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    // ==================== 编解码 ====================

    public static void encode(搜身请求包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.targetUUID);
    }

    public static 搜身请求包 decode(FriendlyByteBuf buf) {
        return new 搜身请求包(buf.readUUID());
    }

    // ==================== 处理 ====================

    public static void handle(搜身请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer searcher = ctx.getSender();
            if (searcher == null) return;

            // 查找目标玩家
            ServerPlayer target = searcher.server.getPlayerList().getPlayer(packet.targetUUID);
            if (target == null) return;

            // 尝试开始搜身
            boolean success = 搜身管理器.尝试开始搜身(searcher, target);

            if (success) {
                // 使用NetworkHooks打开菜单
                NetworkHooks.openScreen(searcher, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("gui.puellamagi.search.title",
                                target.getDisplayName());
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
                        return new 搜身菜单(containerId, playerInv, target);
                    }
                }, buf -> {
                    // 写入目标玩家UUID供客户端使用
                    buf.writeUUID(packet.targetUUID);
                });
            }
        });

        ctx.setPacketHandled(true);
    }
}
