// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/c2s/槽位配置请求包.java

package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.core.event.通用事件;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求修改技能槽位配置
 *
 * 支持：
 * - 设置槽位技能
 * - 清空槽位
 * - 交换两个槽位（移动）
 */
public class 槽位配置请求包 {

    private final 操作类型 操作;
    private final int 槽位索引;
    private final ResourceLocation 技能ID;  // 设置时使用
    private final int 源槽位索引;           // 移动时使用

    public enum 操作类型 {
        设置,// 设置槽位技能
        清空,    // 清空槽位
        移动     // 从源槽位移动到目标槽位
    }

    //==================== 构造器 ====================

    /**
     * 设置槽位技能
     */
    public static 槽位配置请求包 设置(int slotIndex, ResourceLocation skillId) {
        return new 槽位配置请求包(操作类型.设置, slotIndex, skillId, -1);
    }

    /**
     * 清空槽位
     */
    public static 槽位配置请求包 清空(int slotIndex) {
        return new 槽位配置请求包(操作类型.清空, slotIndex, null, -1);
    }

    /**
     * 移动技能（从源槽位到目标槽位）
     */
    public static 槽位配置请求包 移动(int sourceIndex, int targetIndex) {
        return new 槽位配置请求包(操作类型.移动, targetIndex, null, sourceIndex);
    }

    private 槽位配置请求包(操作类型 op, int slotIndex, ResourceLocation skillId, int sourceIndex) {
        this.操作 = op;
        this.槽位索引 = slotIndex;
        this.技能ID = skillId;
        this.源槽位索引 = sourceIndex;
    }

    // ==================== 编码 ====================

    public static void encode(槽位配置请求包 packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.操作);
        buf.writeInt(packet.槽位索引);

        // 根据操作类型写入额外数据
        switch (packet.操作) {
            case 设置 -> {
                buf.writeBoolean(packet.技能ID != null);
                if (packet.技能ID != null) {
                    buf.writeResourceLocation(packet.技能ID);
                }
            }
            case 移动 -> buf.writeInt(packet.源槽位索引);
            case 清空 -> {} // 无额外数据
        }
    }

    // ==================== 解码 ====================

    public static 槽位配置请求包 decode(FriendlyByteBuf buf) {
        操作类型 op = buf.readEnum(操作类型.class);
        int slotIndex = buf.readInt();

        ResourceLocation skillId = null;
        int sourceIndex = -1;

        switch (op) {
            case 设置 -> {
                if (buf.readBoolean()) {
                    skillId = buf.readResourceLocation();
                }
            }
            case 移动 -> sourceIndex = buf.readInt();
            case 清空 -> {}}

        return new 槽位配置请求包(op, slotIndex, skillId, sourceIndex);
    }

    // ==================== 处理（服务端） ====================

    public static void handle(槽位配置请求包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            能力工具.获取技能能力(player).ifPresent(cap -> {
                var preset = cap.获取当前预设();
                int slotCount = preset.获取槽位数量();

                // 验证槽位索引
                if (packet.槽位索引 < 0 || packet.槽位索引 >= slotCount) {
                    return;
                }

                switch (packet.操作) {
                    case 设置-> {
                        preset.设置槽位技能(packet.槽位索引, packet.技能ID);
                    }
                    case 清空 -> {
                        preset.设置槽位技能(packet.槽位索引, null);
                    }
                    case 移动 -> {
                        // 验证源槽位索引
                        if (packet.源槽位索引 < 0 || packet.源槽位索引 >= slotCount) {
                            return;
                        }
                        if (packet.源槽位索引 == packet.槽位索引) {
                            return;
                        }

                        // 获取源槽位技能
                        var sourceSlot = preset.获取槽位(packet.源槽位索引);
                        ResourceLocation sourceSkill = (sourceSlot != null && !sourceSlot.是否为空())
                                ? sourceSlot.获取技能ID() : null;

                        // 清空源槽位，设置目标槽位
                        preset.设置槽位技能(packet.源槽位索引, null);
                        preset.设置槽位技能(packet.槽位索引, sourceSkill);
                    }
                }

                // 同步数据回客户端
                通用事件.同步技能能力(player);
            });
        });
        ctx.setPacketHandled(true);
    }
}
