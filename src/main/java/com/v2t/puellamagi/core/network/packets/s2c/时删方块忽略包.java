package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.时间删除客户端处理器;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 时删方块忽略包（S2C）
 *
 * 服务端帧方块修正执行A的操作时，额外发此包给A的客户端
 * A的客户端收到后将这些方块位置加入"忽略列表"
 * 当MC的原版方块更新包到达时，客户端Mixin检查忽略列表
 * 在列表中的方块变化 → 不执行（A看到方块还在原来的状态）
 *
 * 操作类型：
 * - 忽略：将方块位置加入忽略列表（时删期间持续发送）
 * - 清空：清空忽略列表（时删结束时发送）
 *
 * 流程：
 * 1. 帧方块修正检测到使用者触发的方块变化 → 发本包给使用者（忽略）
 * 2. A的客户端收到 → 方块位置加入忽略列表
 * 3. MC原版方块更新包到达A → Mixin检查忽略列表 → 在列表中 → cancel
 * 4. 时删结束 → 发本包（清空） → 清空忽略列表
 */
public class 时删方块忽略包 {

    /**
     * 操作类型
     */
    public enum 操作类型 {
        /** 将方块位置加入忽略列表 */
        忽略,
        /** 清空整个忽略列表 */
        清空
    }

    private final 操作类型 操作;
    private final List<BlockPos> 位置列表;

    // ==================== 构造 ====================

    public 时删方块忽略包(操作类型 type, List<BlockPos> positions) {
        this.操作 = type;
        this.位置列表 = positions;
    }

    /**
     * 构建忽略通知（添加方块到忽略列表）
     *
     * @param positions 需要被A忽略的方块位置
     * @return 忽略通知包
     */
    public static 时删方块忽略包 忽略(List<BlockPos> positions) {
        return new 时删方块忽略包(操作类型.忽略, positions);
    }

    /**
     * 构建清空通知（时删结束时清空忽略列表）
     *
     * @return 清空通知包
     */
    public static 时删方块忽略包 清空() {
        return new 时删方块忽略包(操作类型.清空, new ArrayList<>());
    }

    // ==================== 编解码 ====================

    public static void encode(时删方块忽略包 packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.操作);
        buf.writeVarInt(packet.位置列表.size());
        for (BlockPos pos : packet.位置列表) {
            buf.writeBlockPos(pos);
        }
    }

    public static 时删方块忽略包 decode(FriendlyByteBuf buf) {
        操作类型 type = buf.readEnum(操作类型.class);
        int count = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(buf.readBlockPos());
        }
        return new 时删方块忽略包(type, positions);
    }

    // ==================== 客户端处理 ====================

    public static void handle(时删方块忽略包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            switch (packet.操作) {
                case 忽略 -> 时间删除客户端处理器.添加忽略方块(packet.位置列表);
                case 清空 -> 时间删除客户端处理器.清空忽略方块();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
