// 文件路径: src/main/java/com/v2t/puellamagi/core/event/玩家Tick事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.core.network.packets.s2c.队友位置同步包;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能能力;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍数据;
import com.v2t.puellamagi.util.绑定物品工具;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.网络工具;
import com.v2t.puellamagi.常量;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 玩家Tick事件处理
 *
 * 职责：
 * - 玩家每帧(tick)驱动各系统
 * - 定期清理缓存
 * - 队友位置同步
 *
 * 不含业务逻辑，只做转发给对应管理器
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class 玩家Tick事件 {

    /**
     * 清理计数器
     * 用于定期执行缓存清理，防止内存泄漏
     * 每200tick（10秒）执行一次
     */
    private static int 清理计数器 = 0;

    @SubscribeEvent
    public static void 玩家Tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;

        // ==================== 定期清理（每 200 tick） ====================
        清理计数器++;
        if (清理计数器 >= 200) {
            清理计数器 = 0;
            绑定物品工具.清理过期标记();
        }

        // ==================== 通用系统 Tick ====================
        能力管理器.tickAll(serverPlayer);
        能力工具.获取技能能力(serverPlayer).ifPresent(技能能力::tick);
        技能管理器.tickAll(serverPlayer);

        // ==================== 系列专属 Tick（分发到对应系列） ====================
        系列注册表.tickPlayer(serverPlayer);

        // ==================== 队友位置同步（每5tick） ====================
        if (serverPlayer.tickCount % 5 == 0) {
            同步队友位置(serverPlayer);
        }
    }

    /**
     * 同步队友位置到客户端
     * 每秒一次，只同步同队成员的位置
     */
    private static void 同步队友位置(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        var teamOpt = 队伍管理器.获取玩家队伍(server, player.getUUID());
        if (teamOpt.isEmpty()) return;

        队伍数据 team = teamOpt.get();
        List<UUID> memberUUIDs = team.获取所有成员UUID();

        List<队友位置同步包.条目> positions = new ArrayList<>();

        for (UUID memberUUID : memberUUIDs) {
            // 跳过自己
            if (memberUUID.equals(player.getUUID())) continue;

            // 获取在线玩家
            ServerPlayer teammate = server.getPlayerList().getPlayer(memberUUID);
            if (teammate == null) continue;

            positions.add(new 队友位置同步包.条目(
                    memberUUID,
                    teammate.getX(),
                    teammate.getEyeY(),
                    teammate.getZ(),
                    teammate.level().dimension().location()
            ));
        }

        if (!positions.isEmpty()) {
            网络工具.发送给玩家(player, new 队友位置同步包(positions));
        }
    }
}
