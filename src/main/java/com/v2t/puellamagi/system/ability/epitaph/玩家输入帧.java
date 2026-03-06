package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家输入帧
 *
 * 记录玩家某一tick的完整输入状态
 *
 * 三种数据：
 * 1. 移动输入：前后/左右/跳跃/潜行/冲刺
 * 2. 视角：yRot / xRot
 * 3. 按键状态：所有按下或有点击的KeyMapping
 *
 * 回放时：
 * - 移动输入 → KeyboardInputMixin注入
 * - 视角→ MouseMixin注入（Catmull-Rom插值）
 * - 按键状态 → 注入到对应KeyMapping → MC自己处理攻击/使用/技能等
 */
public class 玩家输入帧 {

    //==================== 移动 ====================

    private final float 前后输入;
    private final float 左右输入;
    private final boolean 跳跃;
    private final boolean 潜行;
    private final boolean 冲刺;

    // ==================== 视角 ====================

    private final float yRot;
    private final float xRot;

    // ==================== 槽位 ====================

    private final int 选中槽位;

    // ==================== 按键状态 ====================

    /**
     * 单个按键的状态
     *
     * name: KeyMapping的名字（如"key.attack"、"key.puellamagi.skill_1"）
     *不管玩家把按键绑到哪个物理键，名字始终不变
     *        所以改键不影响录制回放
     *
     * isDown: 这个tick末是否按住
     *用于持续按住类操作（拉弓、拿盾）
     *
     * clickCount: 这个tick内点了几次（还没被MC消费的）
     *             MC每帧调consumeClick()消费一次 → 触发一次攻击/使用
     *             快速点3下→ clickCount=3→ MC消费3次 → 挥手3次
     */
    public record 按键状态(String name, boolean isDown, int clickCount) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeBoolean(isDown);
            buf.writeVarInt(clickCount);
        }

        public static 按键状态 decode(FriendlyByteBuf buf) {
            return new 按键状态(
                    buf.readUtf(256),
                    buf.readBoolean(),
                    buf.readVarInt()
            );
        }
    }

    private final List<按键状态> 按键列表;

    // ==================== 构造 ====================

    public 玩家输入帧(float forwardImpulse, float leftImpulse,boolean jumping, boolean sneaking, boolean sprinting,
                      float yRot, float xRot,
                      int selectedSlot,
                      List<按键状态> keyStates) {
        this.前后输入 = forwardImpulse;
        this.左右输入 = leftImpulse;
        this.跳跃 = jumping;
        this.潜行 = sneaking;
        this.冲刺 = sprinting;
        this.yRot = yRot;
        this.xRot = xRot;
        this.选中槽位 = selectedSlot;
        this.按键列表 = keyStates != null ? keyStates : new ArrayList<>();
    }

    /**
     * 兼容旧构造（无按键数据）
     * 用于服务端从ServerPlayer采集的场景（服务端没有KeyMapping）
     */
    public 玩家输入帧(float forwardImpulse, float leftImpulse,
                      boolean jumping, boolean sneaking, boolean sprinting,
                      float yRot, float xRot,
                      int selectedSlot) {
        this(forwardImpulse, leftImpulse, jumping, sneaking, sprinting,
                yRot, xRot, selectedSlot, new ArrayList<>());
    }

    // ==================== 从服务端玩家采集 ====================

    public static 玩家输入帧 从玩家采集(ServerPlayer player) {
        return new 玩家输入帧(
                player.zza,
                player.xxa,
                ((ILivingEntityAccess) player).puellamagi$isJumping(),
                player.isShiftKeyDown(),
                player.isSprinting(),
                player.getYRot(),
                player.getXRot(),
                player.getInventory().selected
        );
    }

    // ==================== 应用到服务端玩家（复刻用） ====================

    public void 应用到(ServerPlayer player) {
        player.zza = 前后输入;
        player.xxa = 左右输入;

        player.setShiftKeyDown(潜行);
        player.setSprinting(冲刺);
        ((ILivingEntityAccess) player).puellamagi$setJumping(跳跃);

        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yRotO = yRot;
        player.xRotO = xRot;
        player.setYHeadRot(yRot);

        player.getInventory().selected = 选中槽位;
    }

    // ==================== 网络序列化 ====================

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(前后输入);
        buf.writeFloat(左右输入);
        buf.writeBoolean(跳跃);
        buf.writeBoolean(潜行);
        buf.writeBoolean(冲刺);
        buf.writeFloat(yRot);
        buf.writeFloat(xRot);
        buf.writeVarInt(选中槽位);

        buf.writeVarInt(按键列表.size());
        for (按键状态 key : 按键列表) {
            key.encode(buf);
        }
    }

    public static 玩家输入帧 decode(FriendlyByteBuf buf) {
        float forward = buf.readFloat();
        float left = buf.readFloat();
        boolean jumping = buf.readBoolean();
        boolean sneaking = buf.readBoolean();
        boolean sprinting = buf.readBoolean();
        float yRot = buf.readFloat();
        float xRot = buf.readFloat();
        int selectedSlot = buf.readVarInt();

        int keyCount = buf.readVarInt();
        List<按键状态> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add(按键状态.decode(buf));
        }

        return new 玩家输入帧(forward, left, jumping, sneaking, sprinting,
                yRot, xRot, selectedSlot, keys);
    }

    // ==================== Getter ====================

    public float 获取前后输入() { return 前后输入; }
    public float 获取左右输入() { return 左右输入; }
    public boolean 是否跳跃() { return 跳跃; }
    public boolean 是否潜行() { return 潜行; }
    public boolean 是否冲刺() { return 冲刺; }
    public float 获取YRot() { return yRot; }
    public float 获取XRot() { return xRot; }
    public int 获取选中槽位() { return 选中槽位; }
    public List<按键状态> 获取按键列表() { return 按键列表; }
}
