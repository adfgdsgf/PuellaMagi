// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/预设管理请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.core.event.通用事件;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：预设管理操作
 *
 * 支持：
 * - 新建预设
 * - 重命名预设
 * - 删除预设
 */
public class 预设管理请求包 {

    private final 操作类型 操作;
    private final int 预设索引;      // 重命名/删除时使用
    private final String 预设名称;   // 新建/重命名时使用

    public enum 操作类型 {
        新建,
        重命名,
        删除
    }

    // ==================== 构造器 ====================

    /**
     * 新建预设
     */
    public static 预设管理请求包 新建(String name) {
        return new 预设管理请求包(操作类型.新建, -1, name);
    }

    /**
     * 重命名预设
     */
    public static 预设管理请求包 重命名(int index, String newName) {
        return new 预设管理请求包(操作类型.重命名, index, newName);
    }

    /**
     * 删除预设
     */
    public static 预设管理请求包 删除(int index) {
        return new 预设管理请求包(操作类型.删除, index, null);
    }

    private 预设管理请求包(操作类型 op, int index, String name) {
        this.操作 = op;
        this.预设索引 = index;
        this.预设名称 = name;
    }

    // ==================== 编码 ====================

    public static void encode(预设管理请求包 packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.操作);
        buf.writeInt(packet.预设索引);
        buf.writeBoolean(packet.预设名称 != null);
        if (packet.预设名称 != null) {
            buf.writeUtf(packet.预设名称, 32);
        }
    }

    // ==================== 解码 ====================

    public static 预设管理请求包 decode(FriendlyByteBuf buf) {
        操作类型 op = buf.readEnum(操作类型.class);
        int index = buf.readInt();
        String name = buf.readBoolean() ? buf.readUtf(32) : null;
        return new 预设管理请求包(op, index, name);
    }

    // ==================== 处理（服务端） ====================

    public static void handle(预设管理请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            能力工具.获取技能能力(player).ifPresent(cap -> {
                switch (packet.操作) {
                    case 新建 -> {
                        if (packet.预设名称 != null && !packet.预设名称.isBlank()) {
                            // 限制预设数量（可选）
                            if (cap.获取预设数量() < 10) {
                                cap.添加预设(new com.v2t.puellamagi.system.skill.技能预设(packet.预设名称));player.displayClientMessage(
                                        本地化工具.消息("preset_created", packet.预设名称), true);
                            } else {
                                player.displayClientMessage(
                                        本地化工具.消息("preset_limit_reached"), true);
                            }
                        }
                    }
                    case 重命名 -> {
                        if (packet.预设索引 >= 0 && packet.预设索引 < cap.获取预设数量()) {
                            if (packet.预设名称 != null && !packet.预设名称.isBlank()) {
                                cap.获取所有预设().get(packet.预设索引).设置名称(packet.预设名称);
                                player.displayClientMessage(
                                        本地化工具.消息("preset_renamed", packet.预设名称), true);
                            }
                        }
                    }

                    case 删除 -> {
                        if (cap.删除预设(packet.预设索引)) {
                            player.displayClientMessage(
                                    本地化工具.消息("preset_deleted"), true);
                        } else {
                            player.displayClientMessage(
                                    本地化工具.消息("preset_delete_failed"), true);
                        }
                    }
                }

                // 同步数据回客户端
                通用事件.同步技能能力(player);
            });
        });
        ctx.setPacketHandled(true);
    }
}
