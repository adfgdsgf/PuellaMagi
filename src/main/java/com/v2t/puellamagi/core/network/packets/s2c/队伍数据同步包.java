// 文件路径: src/main/java/com/v2t/puellamagi/core/network/packets/s2c/队伍数据同步包.java

package com.v2t.puellamagi.core.network.packets.s2c;

import com.v2t.puellamagi.client.客户端队伍缓存;
import com.v2t.puellamagi.system.team.队伍数据;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：全量同步队伍数据
 *
 * 触发时机：
 * - 玩家登录/重生/切换维度
 * - 创建/加入/离开/被踢出/解散队伍
 * - 成员个人配置变更
 * - 强制拉人
 *
 * 携带完整队伍数据或"无队伍"标识
 */
public class 队伍数据同步包 {

    private final boolean 有队伍;
    private final 队伍数据 队伍;

    /**
     * 有队伍时的构造
     */
    public 队伍数据同步包(队伍数据 team) {
        this.有队伍 = true;
        this.队伍 = team;
    }

    /**
     * 无队伍时的构造
     */
    public 队伍数据同步包() {
        this.有队伍 = false;
        this.队伍 = null;
    }

    public static void encode(队伍数据同步包 packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.有队伍);
        if (packet.有队伍&& packet.队伍 != null) {
            packet.队伍.写入Buffer(buf);
        }
    }

    public static 队伍数据同步包 decode(FriendlyByteBuf buf) {
        boolean hasTeam = buf.readBoolean();
        if (hasTeam) {
            return new 队伍数据同步包(队伍数据.从Buffer读取(buf));
        }
        return new 队伍数据同步包();
    }

    public static void handle(队伍数据同步包 packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (packet.有队伍 && packet.队伍 != null) {
                客户端队伍缓存.设置队伍(packet.队伍);
                // 已有队伍，清除邀请（加入后邀请无意义）
                客户端队伍缓存.清除所有邀请();
            } else {
                客户端队伍缓存.清除队伍();
            }
        });
        ctx.setPacketHandled(true);
    }
}
