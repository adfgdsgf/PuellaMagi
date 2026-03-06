package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 交互包帧
 *
 * 记录某一tick内玩家发出的所有C2S交互包
 * 不存原始包对象（不可序列化），存关键字段
 *
 * 回放时用这些字段重建包 → 喂给服务端handle方法
 * → 服务端以为是玩家在操作 → 走MC完整流程
 * → 动画/音效/掉落物/背包消耗全部正确
 *
 * 包类型：
 * USE_ITEM_ON - 右键方块（放方块、开箱子、按按钮等）
 * PLAYER_ACTION - 玩家动作（开始挖/停止挖/完成挖等）
 * USE_ITEM - 使用手中物品（吃东西、拉弓等）
 * INTERACT - 右键实体（交易、骑乘等）
 * SWING - 挥手动画
 */
public class 交互包帧 {

    /**
     * 包类型枚举
     */
    public enum 包类型 {
        USE_ITEM_ON,
        PLAYER_ACTION,
        USE_ITEM,
        INTERACT,
        SWING
    }

    /**
     * 单条交互包记录
     *
     * 用sealed interface +record实现类型安全的多态
     * 每种包类型有自己的字段，不会混淆
     */
    public sealed interface 包记录 {
        包类型 获取类型();
        void encode(FriendlyByteBuf buf);
    }

    /**
     * 右键方块包
     *
     * 包含精确的方块坐标、面朝方向、点击位置
     * 这就是"录结果不录输入"的核心——坐标是MC算完射线后的精确值
     */
    public record 右键方块包(
            BlockPos 方块位置,
            Direction 面朝方向,
            InteractionHand 手,
            Vec3 点击位置,
            int 序列号,
            boolean 在方块内
    ) implements 包记录 {

        @Override
        public 包类型 获取类型() { return 包类型.USE_ITEM_ON; }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(获取类型().ordinal());
            buf.writeBlockPos(方块位置);
            buf.writeEnum(面朝方向);
            buf.writeEnum(手);
            buf.writeDouble(点击位置.x);
            buf.writeDouble(点击位置.y);
            buf.writeDouble(点击位置.z);
            buf.writeVarInt(序列号);
            buf.writeBoolean(在方块内);
        }
    }

    /**
     *玩家动作包（挖方块相关）
     *
     * 动作类型：
     * START_DESTROY_BLOCK - 开始挖
     * ABORT_DESTROY_BLOCK - 中断挖（移开视线）
     * STOP_DESTROY_BLOCK - 完成挖（方块破坏）
     * 还有丢物品等其他动作
     */
    public record 玩家动作包(
            BlockPos 方块位置,
            Direction 面朝方向,
            int 动作序号,
            int 序列号
    ) implements 包记录 {

        @Override
        public 包类型 获取类型() { return 包类型.PLAYER_ACTION; }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(获取类型().ordinal());
            buf.writeBlockPos(方块位置);
            buf.writeEnum(面朝方向);
            buf.writeVarInt(动作序号);
            buf.writeVarInt(序列号);
        }
    }

    /**
     * 使用物品包（非方块交互）
     */
    public record 使用物品包(
            InteractionHand 手,
            int 序列号
    ) implements 包记录 {

        @Override
        public 包类型 获取类型() { return 包类型.USE_ITEM; }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(获取类型().ordinal());
            buf.writeEnum(手);
            buf.writeVarInt(序列号);
        }
    }

    /**
     * 交互实体包
     */
    public record 交互实体包(
            int 实体ID,
            boolean 是攻击,
            InteractionHand 手
    ) implements 包记录 {

        @Override
        public 包类型 获取类型() { return 包类型.INTERACT; }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(获取类型().ordinal());
            buf.writeVarInt(实体ID);
            buf.writeBoolean(是攻击);
            buf.writeEnum(手);
        }
    }

    /**
     * 挥手包
     */
    public record 挥手包(
            InteractionHand 手
    ) implements 包记录 {

        @Override
        public 包类型 获取类型() { return 包类型.SWING; }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(获取类型().ordinal());
            buf.writeEnum(手);
        }
    }

    //==================== 帧数据 ====================

    private final int tick序号;
    private final List<包记录> 包列表;

    public 交互包帧(int tickIndex) {
        this.tick序号 = tickIndex;
        this.包列表 = new ArrayList<>();
    }

    public void 添加(包记录 record) {
        包列表.add(record);
    }

    public int 获取tick序号() { return tick序号; }
    public List<包记录> 获取包列表() { return 包列表; }public boolean 是否为空() { return 包列表.isEmpty(); }

    // ==================== 反序列化 ====================

    public static 包记录 decode(FriendlyByteBuf buf) {
        int typeOrd = buf.readByte();
        包类型 type = 包类型.values()[typeOrd];

        return switch (type) {
            case USE_ITEM_ON -> new 右键方块包(
                    buf.readBlockPos(),
                    buf.readEnum(Direction.class),
                    buf.readEnum(InteractionHand.class),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt(),
                    buf.readBoolean()
            );
            case PLAYER_ACTION -> new 玩家动作包(
                    buf.readBlockPos(),
                    buf.readEnum(Direction.class),
                    buf.readVarInt(),
                    buf.readVarInt()
            );
            case USE_ITEM -> new 使用物品包(
                    buf.readEnum(InteractionHand.class),
                    buf.readVarInt()
            );
            case INTERACT -> new 交互实体包(
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readEnum(InteractionHand.class)
            );
            case SWING -> new 挥手包(
                    buf.readEnum(InteractionHand.class)
            );
        };
    }
}
