package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 录制状态通知包（S2C）
 *
 * 通过枚举区分用途，每种类型携带各自需要的数据：
 * - 开始录制：是否是录制者（用于区分"自己录制"和"被别人录进去"）
 * - 停止录制：无附加数据
 * - 恢复按键：初始按键列表 + 初始左右键状态
 * - 时间删除：无附加数据，客户端进入时删自由状态
 */
public class 录制状态通知包 {

        /**
         * 通知类型枚举
         */
        public enum 通知类型 {
                开始录制,
                停止录制,
                恢复按键,
                时间删除;

                public static 通知类型 fromOrdinal(int ordinal) {
                        通知类型[] values = values();
                        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : 停止录制;
                }
        }

        private final 通知类型 类型;
        private final List<String> 初始按键列表;
        private final boolean 初始左键;
        private final boolean 初始右键;

        /**
         * 是否是录制者（本地玩家自己使用预知技能触发的录制）
         *
         * 多人场景下区分：
         * - true = 这个玩家使用了预知技能开始录制（录制者）
         * - false = 这个玩家只是在别人的录制范围内被录进去了（被录制者）
         *
         * 客户端用这个标记判断过渡保护是否应该触发
         */
        private final boolean 是录制者;

        // ==================== 构造器 ====================

        /**
         * 通用构造器
         */
        private 录制状态通知包(通知类型 type, List<String> keys, boolean leftHeld, boolean rightHeld,
                                boolean isRecorder) {
                this.类型 = type;
                this.初始按键列表 = keys != null ? keys : new ArrayList<>();
                this.初始左键 = leftHeld;
                this.初始右键 = rightHeld;
                this.是录制者 = isRecorder;
        }

        /**
         * 开始录制（录制者：本地玩家自己使用预知技能）
         */
        public static 录制状态通知包 开始录制_录制者() {
                return new 录制状态通知包(通知类型.开始录制, new ArrayList<>(), false, false, true);
        }

        /**
         * 开始录制（被录制：本地玩家在别人的录制范围内）
         */
        public static 录制状态通知包 开始录制_被录制() {
                return new 录制状态通知包(通知类型.开始录制, new ArrayList<>(), false, false, false);
        }

        /**
         * 兼容旧接口（默认为录制者）
         */
        public static 录制状态通知包 开始录制() {
                return 开始录制_录制者();
        }

        /**
         * 停止录制
         */
        public static 录制状态通知包 停止录制() {
                return new 录制状态通知包(通知类型.停止录制, new ArrayList<>(), false, false, false);
        }

        /**
         * 恢复按键状态
         */
        public static 录制状态通知包 恢复按键(List<String> keys, boolean leftHeld, boolean rightHeld) {
                return new 录制状态通知包(通知类型.恢复按键, keys, leftHeld, rightHeld, false);
        }

        /**
         * 时间删除通知
         */
        public static 录制状态通知包 时间删除() {
                return new 录制状态通知包(通知类型.时间删除, new ArrayList<>(), false, false, false);
        }

        // ==================== 编解码 ====================

        public static void encode(录制状态通知包 packet, FriendlyByteBuf buf) {
                buf.writeVarInt(packet.类型.ordinal());

                buf.writeVarInt(packet.初始按键列表.size());
                for (String key : packet.初始按键列表) {
                        buf.writeUtf(key);
                }
                buf.writeBoolean(packet.初始左键);
                buf.writeBoolean(packet.初始右键);
                buf.writeBoolean(packet.是录制者);
        }

        public static 录制状态通知包 decode(FriendlyByteBuf buf) {
                通知类型 type = 通知类型.fromOrdinal(buf.readVarInt());

                int count = buf.readVarInt();
                List<String> keys = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                        keys.add(buf.readUtf());
                }
                boolean left = buf.readBoolean();
                boolean right = buf.readBoolean();
                boolean isRecorder = buf.readBoolean();

                return new 录制状态通知包(type, keys, left, right, isRecorder);
        }

        // ==================== 处理 ====================

        public static void handle(录制状态通知包 packet, Supplier<NetworkEvent.Context> ctx) {
                ctx.get().enqueueWork(() -> {
                        switch (packet.类型) {
                                case 开始录制 -> {
                                        客户端复刻管理器.设置录制中(true);
                                        // 如果是录制者（自己使用预知技能），标记本地玩家自己录制中
                                        if (packet.是录制者) {
                                                客户端复刻管理器.标记本地玩家录制开始();
                                        }
                                        上报按住的键();
                                }
                                case 停止录制 -> {
                                        客户端复刻管理器.设置录制中(false);
                                }
                                case 恢复按键 -> {
                                        if (!packet.初始按键列表.isEmpty()) {
                                                客户端复刻管理器.恢复初始按键(packet.初始按键列表);
                                        }
                                        Minecraft mc = Minecraft.getInstance();
                                        if (packet.初始左键) {
                                                mc.options.keyAttack.setDown(true);
                                        }
                                        if (packet.初始右键) {
                                                mc.options.keyUse.setDown(true);
                                        }
                                }
                                case 时间删除 -> {
                                        客户端复刻管理器.进入时间删除();
                                }
                        }
                });
                ctx.get().setPacketHandled(true);
        }

        /**
         * 扫描所有KeyMapping，找出按住的键，上报服务端
         */
        private static void 上报按住的键() {
                java.util.Map<String, net.minecraft.client.KeyMapping> allKeys =
                        com.v2t.puellamagi.mixin.access.KeyMappingAccessor.puellamagi$getAll();

                List<String> heldKeys = new ArrayList<>();
                for (java.util.Map.Entry<String, net.minecraft.client.KeyMapping> entry : allKeys.entrySet()) {
                        if (entry.getValue().isDown()) {
                                heldKeys.add(entry.getKey());
                        }
                }

                if (!heldKeys.isEmpty()) {
                        com.v2t.puellamagi.util.网络工具.发送到服务端(
                                new com.v2t.puellamagi.core.network.packets.c2s.按键状态上报包(heldKeys));
                }
        }
}
