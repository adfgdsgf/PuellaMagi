package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端复刻管理器;
import com.v2t.puellamagi.system.ability.epitaph.玩家输入帧;
import com.v2t.puellamagi.util.recording.实体帧数据;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * 复刻帧同步包（S2C）
 *
 * 服务端每tick发送，包含三种数据：
 * 1. 实体帧数据：怪物/其他玩家的位置/朝向/动画
 * 2. 玩家输入帧：移动方向/跳跃/潜行/冲刺
 * 3. 鼠标增量列表：60Hz+的视角delta（本tick内所有渲染帧的样本）
 */
public class 复刻帧同步包 {

    private final UUID 使用者UUID;
    private final List<实体帧数据> 帧列表;
    private final List<实体帧数据> 上一帧列表;
    private final Map<UUID, 玩家输入帧> 输入帧表;
    private final Map<UUID, List<float[]>> 鼠标样本表;

    /**
     * 被锁定玩家UUID集合
     *
     * 客户端需要区分"被控制实体"（cancel tick + 帧驱动）和"被锁定玩家"（不cancel tick，只做位置插值）
     * 被锁定玩家自己的tick正常运行（由输入回放驱动），第三方客户端只需要用帧数据做位置插值即可
     * 如果cancel tick会导致动画停止（卡在一个动作不动）
     */
    private final Set<UUID> 被锁定玩家集合;

    /**
     * 完整构造器
     */
    public 复刻帧同步包(UUID userUUID, List<实体帧数据> frames,
                        List<实体帧数据> prevFrames,
                        Map<UUID, 玩家输入帧> inputFrames,
                        Map<UUID, List<float[]>> mouseSamples,
                        Set<UUID> lockedPlayers) {
        this.使用者UUID = userUUID;
        this.帧列表 = frames;
        this.上一帧列表 = prevFrames;
        this.输入帧表 = inputFrames;
        this.鼠标样本表 = mouseSamples;
        this.被锁定玩家集合 = lockedPlayers;
    }

    /**
     * 兼容构造器（无鼠标数据和被锁定玩家，复刻结束通知用）
     */
    public 复刻帧同步包(UUID userUUID,
                        List<实体帧数据> frames,
                        List<实体帧数据> prevFrames) {
        this(userUUID, frames, prevFrames, new HashMap<>(), new HashMap<>(), new HashSet<>());
    }

    // ==================== 编解码 ====================

    public static void encode(复刻帧同步包 packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.使用者UUID);

        // 当前帧
        buf.writeVarInt(packet.帧列表.size());
        for (实体帧数据 frame : packet.帧列表) {
            frame.encode(buf);
        }

        // 上一帧
        buf.writeVarInt(packet.上一帧列表.size());
        for (实体帧数据 frame : packet.上一帧列表) {
            frame.encode(buf);
        }

        // 输入帧
        buf.writeVarInt(packet.输入帧表.size());
        for (Map.Entry<UUID, 玩家输入帧> entry : packet.输入帧表.entrySet()) {
            buf.writeUUID(entry.getKey());
            entry.getValue().encode(buf);
        }

        // 鼠标样本
        buf.writeVarInt(packet.鼠标样本表.size());
        for (Map.Entry<UUID, List<float[]>> entry : packet.鼠标样本表.entrySet()) {
            buf.writeUUID(entry.getKey());
            List<float[]> samples = entry.getValue();
            buf.writeVarInt(samples.size());
            for (float[] sample : samples) {
                buf.writeFloat(sample[0]);
                buf.writeFloat(sample[1]);
            }
        }

        // 被锁定玩家集合
        buf.writeVarInt(packet.被锁定玩家集合.size());
        for (UUID uuid : packet.被锁定玩家集合) {
            buf.writeUUID(uuid);
        }
    }

    public static 复刻帧同步包 decode(FriendlyByteBuf buf) {
        UUID userUUID = buf.readUUID();

        // 当前帧
        int frameCount = buf.readVarInt();
        List<实体帧数据> frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            frames.add(实体帧数据.decode(buf));
        }

        // 上一帧
        int prevCount = buf.readVarInt();
        List<实体帧数据> prevFrames = new ArrayList<>(prevCount);
        for (int i = 0; i < prevCount; i++) {
            prevFrames.add(实体帧数据.decode(buf));
        }

        // 输入帧
        int inputCount = buf.readVarInt();
        Map<UUID, 玩家输入帧> inputFrames = new HashMap<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            UUID uuid = buf.readUUID();
            玩家输入帧 input = 玩家输入帧.decode(buf);
            inputFrames.put(uuid, input);
        }

        // 鼠标样本
        int mouseCount = buf.readVarInt();
        Map<UUID, List<float[]>> mouseSamples = new HashMap<>(mouseCount);
        for (int i = 0; i < mouseCount; i++) {
            UUID uuid = buf.readUUID();
            int sampleCount = buf.readVarInt();
            List<float[]> samples = new ArrayList<>(sampleCount);
            for (int j = 0; j < sampleCount; j++) {
                samples.add(new float[]{buf.readFloat(), buf.readFloat()});
            }
            mouseSamples.put(uuid, samples);
        }

        // 被锁定玩家集合
        int lockedCount = buf.readVarInt();
        Set<UUID> lockedPlayers = new HashSet<>(lockedCount);
        for (int i = 0; i < lockedCount; i++) {
            lockedPlayers.add(buf.readUUID());
        }

        return new 复刻帧同步包(userUUID, frames, prevFrames, inputFrames, mouseSamples, lockedPlayers);
    }

    // ==================== 处理 ====================

    public static void handle(复刻帧同步包 packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            客户端复刻管理器.接收帧(packet.使用者UUID, packet.帧列表, packet.上一帧列表);
            客户端复刻管理器.接收输入帧(packet.输入帧表);
            客户端复刻管理器.接收鼠标样本(packet.鼠标样本表);
            客户端复刻管理器.接收被锁定玩家集合(packet.被锁定玩家集合);
        });
        ctx.get().setPacketHandled(true);
    }
}
