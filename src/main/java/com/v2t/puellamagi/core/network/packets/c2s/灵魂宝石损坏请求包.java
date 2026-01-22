package com.v2t.puellamagi.core.network.packets.c2s;

import com.v2t.puellamagi.system.soulgem.damage.active.主动损坏注册表;
import com.v2t.puellamagi.system.soulgem.damage.active.主动损坏触发类型;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 灵魂宝石损坏请求包
 *
 * 客户端 → 服务端
 * 当客户端检测到主动损坏条件时发送
 */
public class 灵魂宝石损坏请求包 {

    private final 主动损坏触发类型 触发类型;
    private final int 目标实体ID;      // 掉落物攻击时使用
    private final BlockPos 目标方块;// 撞击方块时使用

    /**
     * 掉落物攻击
     */
    public 灵魂宝石损坏请求包(主动损坏触发类型 类型, int entityId) {
        this.触发类型 = 类型;
        this.目标实体ID = entityId;
        this.目标方块 = BlockPos.ZERO;
    }

    /**
     * 撞击方块
     */
    public 灵魂宝石损坏请求包(主动损坏触发类型 类型, BlockPos pos) {
        this.触发类型 = 类型;
        this.目标实体ID = -1;
        this.目标方块 = pos;
    }

    /**
     * 主副手组合
     */
    public 灵魂宝石损坏请求包(主动损坏触发类型 类型) {
        this.触发类型 = 类型;this.目标实体ID = -1;
        this.目标方块 = BlockPos.ZERO;
    }

    //==================== 序列化 ====================

    public static void encode(灵魂宝石损坏请求包 msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.触发类型);
        buf.writeInt(msg.目标实体ID);
        buf.writeBlockPos(msg.目标方块);
    }

    public static 灵魂宝石损坏请求包 decode(FriendlyByteBuf buf) {
        主动损坏触发类型 type = buf.readEnum(主动损坏触发类型.class);
        int entityId = buf.readInt();
        BlockPos pos = buf.readBlockPos();

        if (entityId >= 0) {
            return new 灵魂宝石损坏请求包(type, entityId);
        } else if (!pos.equals(BlockPos.ZERO)) {
            return new 灵魂宝石损坏请求包(type, pos);
        } else {
            return new 灵魂宝石损坏请求包(type);
        }
    }

    // ==================== 处理 ====================

    public static void handle(灵魂宝石损坏请求包 msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 转发给注册表处理
            主动损坏注册表.处理请求(player, msg.触发类型, msg.目标实体ID, msg.目标方块);
        });
        ctx.get().setPacketHandled(true);
    }

    // ==================== Getter ====================

    public 主动损坏触发类型 获取触发类型() {
        return 触发类型;
    }

    public int 获取目标实体ID() {
        return 目标实体ID;
    }

    public BlockPos 获取目标方块() {
        return 目标方块;
    }
}
