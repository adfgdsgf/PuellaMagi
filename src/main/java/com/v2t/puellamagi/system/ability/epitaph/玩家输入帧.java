package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.api.access.ILivingEntityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家输入帧
 *
 * 记录玩家某一tick的完整输入状态
 *
 * 五种数据：
 * 1. 移动输入：前后/左右/跳跃/潜行/冲刺
 * 2. 视角：yRot / xRot
 * 3. 键盘事件：原始GLFW键盘事件
 * 4. 鼠标事件 + 光标位置：原始GLFW鼠标事件 + 屏幕坐标
 * 5. 射线结果：MC每帧的hitResult
 */
public class 玩家输入帧 {

    // ==================== 移动 ====================

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

    // ==================== 键盘事件 ====================

    /**
     * 单个原始键盘事件
     *
     * 直接录制GLFW层的键盘回调参数
     * 回放时喂回Keyboard.keyPress → MC自己处理一切
     */
    public record 键盘事件(int keyCode, int scanCode, int action, int modifiers) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(keyCode);
            buf.writeVarInt(scanCode);
            buf.writeByte(action);
            buf.writeByte(modifiers);
        }

        public static 键盘事件 decode(FriendlyByteBuf buf) {
            return new 键盘事件(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readByte());
        }
    }

    private final List<键盘事件> 键盘事件列表;

    // ==================== 鼠标事件 ====================

    /**
     * 单个原始鼠标按键事件 + 当时的光标位置
     *
     * 光标位置在事件发生的那一刻采集
     * 不是tick结束时采集→ 精确对应点击位置
     */
    public record 鼠标事件(int button, int action, int modifiers, double cursorX, double cursorY) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(button);
            buf.writeByte(action);
            buf.writeByte(modifiers);
            buf.writeDouble(cursorX);
            buf.writeDouble(cursorY);
        }

        public static 鼠标事件 decode(FriendlyByteBuf buf) {
            return new 鼠标事件(buf.readByte(), buf.readByte(), buf.readByte(),
                    buf.readDouble(), buf.readDouble());
        }
    }

    private final List<鼠标事件> 鼠标事件列表;

    // ==================== 鼠标光标位置 ====================

    /**
     *鼠标光标在窗口内的绝对坐标
     *
     * 用于容器操作的精确还原
     * 回放时用glfwSetCursorPos设到録制时的位置
     * → MC算出正确的槽位 → 容器操作正确
     */
    private final double cursorX;
    private final double cursorY;

    // ==================== 射线结果 ====================

    private final int hitType;
    private final BlockPos hitBlockPos;
    private final Direction hitDirection;
    private final double hitX, hitY, hitZ;
    private final boolean hitInside;
    private final int hitEntityId;

    // ==================== 构造 ====================

    public 玩家输入帧(float forwardImpulse, float leftImpulse,boolean jumping, boolean sneaking, boolean sprinting,
                      float yRot, float xRot,
                      int selectedSlot,
                      List<键盘事件> keyboardEvents,
                      List<鼠标事件> mouseEvents,
                      double cursorX, double cursorY,
                      int hitType,
                      BlockPos hitBlockPos,
                      Direction hitDirection,
                      double hitX, double hitY, double hitZ,
                      boolean hitInside,
                      int hitEntityId) {
        this.前后输入 = forwardImpulse;
        this.左右输入 = leftImpulse;
        this.跳跃 = jumping;
        this.潜行 = sneaking;
        this.冲刺 = sprinting;
        this.yRot = yRot;
        this.xRot = xRot;
        this.选中槽位 = selectedSlot;
        this.键盘事件列表 = keyboardEvents != null ? keyboardEvents : new ArrayList<>();
        this.鼠标事件列表 = mouseEvents != null ? mouseEvents : new ArrayList<>();
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        this.hitType = hitType;
        this.hitBlockPos = hitBlockPos;
        this.hitDirection = hitDirection;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.hitInside = hitInside;
        this.hitEntityId = hitEntityId;
    }

    /**
     * 兼容旧构造（服务端采集，没有键盘/鼠标事件和hitResult）
     */
    public 玩家输入帧(float forwardImpulse, float leftImpulse,
                      boolean jumping, boolean sneaking, boolean sprinting,
                      float yRot, float xRot,
                      int selectedSlot) {
        this(forwardImpulse, leftImpulse, jumping, sneaking, sprinting,
                yRot, xRot, selectedSlot, new ArrayList<>(), new ArrayList<>(),
                0, 0,
                0, null, null, 0, 0, 0, false, -1);
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

    // ==================== 应用到服务端玩家 ====================

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

        // 键盘事件
        buf.writeVarInt(键盘事件列表.size());
        for (键盘事件 event : 键盘事件列表) {
            event.encode(buf);
        }

        // 鼠标事件
        buf.writeVarInt(鼠标事件列表.size());
        for (鼠标事件 event : 鼠标事件列表) {
            event.encode(buf);
        }

        // 鼠标光标位置
        buf.writeDouble(cursorX);
        buf.writeDouble(cursorY);

        // 射线结果
        buf.writeByte(hitType);
        if (hitType == 1) {
            buf.writeBlockPos(hitBlockPos);
            buf.writeEnum(hitDirection);
            buf.writeDouble(hitX);
            buf.writeDouble(hitY);
            buf.writeDouble(hitZ);
            buf.writeBoolean(hitInside);
        } else if (hitType == 2) {
            buf.writeVarInt(hitEntityId);buf.writeDouble(hitX);
            buf.writeDouble(hitY);
            buf.writeDouble(hitZ);
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

        // 键盘事件
        int keyCount = buf.readVarInt();
        List<键盘事件> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            keys.add(键盘事件.decode(buf));
        }

        // 鼠标事件
        int mouseCount = buf.readVarInt();
        List<鼠标事件> mouseEvents = new ArrayList<>(mouseCount);
        for (int i = 0; i < mouseCount; i++) {
            mouseEvents.add(鼠标事件.decode(buf));
        }

        // 鼠标光标位置
        double cursorX = buf.readDouble();
        double cursorY = buf.readDouble();

        // 射线结果
        int hitType = buf.readByte();
        BlockPos hitBlockPos = null;
        Direction hitDirection = null;
        double hitX = 0, hitY = 0, hitZ = 0;
        boolean hitInside = false;
        int hitEntityId = -1;

        if (hitType == 1) {
            hitBlockPos = buf.readBlockPos();
            hitDirection = buf.readEnum(Direction.class);
            hitX = buf.readDouble();
            hitY = buf.readDouble();
            hitZ = buf.readDouble();
            hitInside = buf.readBoolean();
        } else if (hitType == 2) {
            hitEntityId = buf.readVarInt();
            hitX = buf.readDouble();
            hitY = buf.readDouble();
            hitZ = buf.readDouble();
        }

        return new 玩家输入帧(forward, left, jumping, sneaking, sprinting,
                yRot, xRot, selectedSlot, keys, mouseEvents,
                cursorX, cursorY,
                hitType, hitBlockPos, hitDirection, hitX, hitY, hitZ, hitInside, hitEntityId);
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
    public List<键盘事件> 获取键盘事件列表() { return 键盘事件列表; }
    public List<鼠标事件> 获取鼠标事件列表() { return 鼠标事件列表; }
    public double 获取光标X() { return cursorX; }
    public double 获取光标Y() { return cursorY; }

    public int 获取射线类型() { return hitType; }
    public BlockPos 获取射线方块位置() { return hitBlockPos; }
    public Direction 获取射线方向() { return hitDirection; }
    public double 获取射线X() { return hitX; }
    public double 获取射线Y() { return hitY; }
    public double 获取射线Z() { return hitZ; }
    public boolean 获取射线是否在内部() { return hitInside; }
    public int 获取射线实体ID() { return hitEntityId; }

    @Nullable
    public HitResult 重建射线结果() {
        return switch (hitType) {
            case 1 -> new BlockHitResult(
                    new Vec3(hitX, hitY, hitZ),
                    hitDirection, hitBlockPos, hitInside);
            case 2 -> null;
            default -> BlockHitResult.miss(
                    new Vec3(hitX, hitY, hitZ),
                    Direction.UP,
                    BlockPos.ZERO);
        };
    }
}
