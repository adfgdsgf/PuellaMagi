// 文件路径: src/main/java/com/v2t/puellamagi/core/network/ModNetwork.java

package com.v2t.puellamagi.core.network;

import com.v2t.puellamagi.core.network.packets.c2s.*;
import com.v2t.puellamagi.core.network.packets.s2c.变身同步包;
import com.v2t.puellamagi.core.network.packets.s2c.技能能力同步包;
import com.v2t.puellamagi.core.network.packets.s2c.时停状态同步包;
import com.v2t.puellamagi.常量;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 网络通道注册和管理
 */
public class ModNetwork {
    private static SimpleChannel CHANNEL;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(常量.MOD_ID, "main"),
                () -> 常量.网络协议版本,
                常量.网络协议版本::equals,
                常量.网络协议版本::equals
        );

        // ==================== C2S 包 ====================
        registerPacket(
                变身请求包.class,
                变身请求包::encode,
                变身请求包::decode,
                变身请求包::handle
        );

        registerPacket(
                技能按下请求包.class,
                技能按下请求包::encode,
                技能按下请求包::decode,
                技能按下请求包::handle
        );

        registerPacket(
                技能松开请求包.class,
                技能松开请求包::encode,
                技能松开请求包::decode,
                技能松开请求包::handle
        );

        registerPacket(
                预设切换请求包.class,
                预设切换请求包::encode,
                预设切换请求包::decode,
                预设切换请求包::handle
        );

        registerPacket(
                布局更新请求包.class,
                布局更新请求包::encode,
                布局更新请求包::decode,
                布局更新请求包::handle
        );

        registerPacket(
                槽位配置请求包.class,
                槽位配置请求包::encode,
                槽位配置请求包::decode,
                槽位配置请求包::handle
        );

        registerPacket(
                预设管理请求包.class,
                预设管理请求包::encode,
                预设管理请求包::decode,
                预设管理请求包::handle
        );

        registerPacket(
                掉落物拾取请求包.class,
                掉落物拾取请求包::encode,
                掉落物拾取请求包::decode,
                掉落物拾取请求包::handle
        );

        registerPacket(
                投射物拾取请求包.class,
                投射物拾取请求包::encode,
                投射物拾取请求包::decode,
                投射物拾取请求包::handle
        );

        // ==================== S2C 包 ====================
        registerPacket(
                变身同步包.class,
                变身同步包::encode,
                变身同步包::decode,
                变身同步包::handle
        );

        registerPacket(
                技能能力同步包.class,
                技能能力同步包::encode,
                技能能力同步包::decode,
                技能能力同步包::handle
        );

        registerPacket(
                时停状态同步包.class,
                时停状态同步包::encode,
                时停状态同步包::decode,
                时停状态同步包::handle
        );
    }

    public static <T> void registerPacket(
            Class<T> clazz,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler
    ) {
        CHANNEL.messageBuilder(clazz, nextId())
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(handler)
                .add();
    }

    public static SimpleChannel getChannel() {
        return CHANNEL;
    }
}
