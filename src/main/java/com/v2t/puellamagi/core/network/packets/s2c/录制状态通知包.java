package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端复刻管理器;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 录制状态通知包（S2C）
 *
 * 三种用途：
 * 1. 录制开始/停止：开始=true/false
 * 2. 回放初始按键恢复：按住的键名列表
 */
public class 录制状态通知包 {

        private final boolean 开始;
        private final List<String> 初始按键列表;

        public 录制状态通知包(boolean start) {
                this(start, new ArrayList<>());
        }

        public 录制状态通知包(boolean start, List<String> initialKeys) {
                this.开始 = start;
                this.初始按键列表 = initialKeys != null ? initialKeys : new ArrayList<>();
        }

        public static void encode(录制状态通知包 packet, FriendlyByteBuf buf) {
                buf.writeBoolean(packet.开始);
                buf.writeVarInt(packet.初始按键列表.size());
                for (String key : packet.初始按键列表) {
                        buf.writeUtf(key);
                }
        }

        public static 录制状态通知包 decode(FriendlyByteBuf buf) {
                boolean start = buf.readBoolean();
                int count = buf.readVarInt();
                List<String> keys = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                        keys.add(buf.readUtf());
                }
                return new 录制状态通知包(start, keys);
        }

        public static void handle(录制状态通知包 packet, Supplier<NetworkEvent.Context> ctx) {
                ctx.get().enqueueWork(() -> {
                        if (!packet.初始按键列表.isEmpty()) {
                                // 回放初始按键恢复
                                客户端复刻管理器.恢复初始按键(packet.初始按键列表);
                        } else if (packet.开始) {
                                // 录制开始 → 扫描当前按住的键 → 上报服务端
                                客户端复刻管理器.设置录制中(true);
                                上报按住的键();
                        } else {
                                // 录制停止
                                客户端复刻管理器.设置录制中(false);
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
