package com.v2t.puellamagi.core.network.packets.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 方块批量更新包（S2C）
 *
 * 服务端回滚前发给客户端，客户端立即在本地更新方块显示
 * 消除"服务端回滚了但更新包还没到"导致的1-2帧方块残留
 *
 * 流程：
 * 1. 服务端准备回滚 → 发本包（带所有要变的方块）
 * 2. 客户端收到 → 立即设方块（视觉瞬间生效）
 * 3. 服务端执行回滚 → 正式更新包到达 → 客户端已经是对的了
 */
public class 方块批量更新包 {

    /**
     * 单个方块的更新信息
     */
    private record 方块条目(BlockPos pos, BlockState state) {}

    private final List<方块条目> 条目列表;

    //==================== 构造 ====================

    public 方块批量更新包(List<方块条目> entries) {
        this.条目列表 = entries;
    }

    /**
     * 从位置和状态的配对列表构建
     *复刻引擎调用时用这个
     */
    public static 方块批量更新包 从配对构建(List<BlockPos> positions, List<BlockState> states) {
        List<方块条目> entries = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            entries.add(new 方块条目(positions.get(i), states.get(i)));
        }
        return new 方块批量更新包(entries);
    }

    // ==================== 编解码 ====================

    public static void encode(方块批量更新包 packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.条目列表.size());
        for (方块条目 entry : packet.条目列表) {
            buf.writeBlockPos(entry.pos());
            buf.writeVarInt(Block.getId(entry.state()));
        }
    }

    public static 方块批量更新包 decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<方块条目> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos pos = buf.readBlockPos();
            BlockState state = Block.stateById(buf.readVarInt());
            entries.add(new 方块条目(pos, state));
        }
        return new 方块批量更新包(entries);
    }

    // ==================== 客户端处理 ====================

    public static void handle(方块批量更新包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) return;

            for (方块条目 entry : packet.条目列表) {
                // 直接在客户端设方块，视觉瞬间生效
                // flag=0：不触发方块更新、不通知邻居，纯视觉
                level.setBlock(entry.pos(), entry.state(), 0);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
