// 文件路径: src/main/java/com/v2t/puellamagi/core/event/玩家生命周期事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.skill.impl.预知技能;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.system.team.队伍同步工具;
import com.v2t.puellamagi.system.team.队伍邀请管理器;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.util.绑定物品工具;
import com.v2t.puellamagi.util.network.存在屏蔽器;
import com.v2t.puellamagi.util.network.输入接管器;
import com.v2t.puellamagi.常量;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家生命周期事件处理
 *
 * 职责：
 * - 玩家登录时的数据同步
 * - 玩家登出时的缓存清理
 * - 玩家重生时的数据恢复
 * - 维度切换时的状态处理
 * - 玩家睡醒时的污浊度处理
 *
 * 不含业务逻辑，只做转发给对应管理器
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class 玩家生命周期事件 {

    @SubscribeEvent
    public static void 玩家登录(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 通用同步
            契约管理器.同步契约状态(serverPlayer);
            变身管理器.同步变身数据(serverPlayer);
            核心事件.同步技能能力(serverPlayer);
            污浊度管理器.同步污浊度(serverPlayer);

            // 队伍数据同步
            队伍同步工具.同步队伍数据(serverPlayer);

            // 同步待处理邀请（登录时将服务端未过期邀请推送给客户端）
            队伍同步工具.同步待处理邀请(serverPlayer);

            // 系列专属处理（分发到对应系列）
            系列注册表.onPlayerLogin(serverPlayer);

            PuellaMagi.LOGGER.debug("玩家 {} 登录，已同步数据", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 玩家登出(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 通用清理：时停状态
            if (时停管理器.是否时停者(serverPlayer)) {
                时停管理器.玩家下线(serverPlayer);
            }

            // 清理预知能力状态
            预知技能.玩家下线(serverPlayer.getUUID());

            // 清理能力管理器（防止内存泄漏）
            能力管理器.玩家下线(serverPlayer);

            // 清理绑定物品缓存（防止内存泄漏）
            绑定物品工具.清理玩家缓存(serverPlayer.getUUID());

            // 清理网络工具状态
            输入接管器.玩家下线(serverPlayer.getUUID());
            存在屏蔽器.玩家下线(serverPlayer.getUUID());

            // 队伍邀请：保留邀请记录（重连后仍可接受），仅通知管理器
            队伍邀请管理器.onPlayerLogout(serverPlayer.getUUID());

            // 系列专属处理（分发到对应系列）
            系列注册表.onPlayerLogout(serverPlayer);

            PuellaMagi.LOGGER.debug("玩家 {} 登出，已清理缓存", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 玩家重生(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 通用同步
            契约管理器.同步契约状态(serverPlayer);
            变身管理器.同步变身数据(serverPlayer);
            核心事件.同步技能能力(serverPlayer);
            污浊度管理器.同步污浊度(serverPlayer);

            // 队伍数据同步
            队伍同步工具.同步队伍数据(serverPlayer);

            // 系列专属处理（分发到对应系列）
            系列注册表.onPlayerRespawn(serverPlayer);

            PuellaMagi.LOGGER.debug("玩家 {} 重生，已同步数据", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 维度切换(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 通用处理：时停结束
            if (时停管理器.是否时停者(serverPlayer)) {
                时停管理器.结束时停(serverPlayer);
                PuellaMagi.LOGGER.debug("时停者 {} 切换维度，时停结束", serverPlayer.getName().getString());
            }

            // 预知能力终止
            预知技能.玩家下线(serverPlayer.getUUID());

            // 清理网络工具状态（跨维度后旧的屏蔽/接管可能无效）
            输入接管器.玩家下线(serverPlayer.getUUID());
            存在屏蔽器.玩家下线(serverPlayer.getUUID());

            // 通用同步
            契约管理器.同步契约状态(serverPlayer);
            变身管理器.同步变身数据(serverPlayer);
            核心事件.同步技能能力(serverPlayer);
            污浊度管理器.同步污浊度(serverPlayer);

            // 队伍数据同步
            队伍同步工具.同步队伍数据(serverPlayer);

            // 系列专属处理（分发到对应系列）
            系列注册表.onDimensionChange(serverPlayer);

            PuellaMagi.LOGGER.debug("玩家 {} 切换维度，已同步数据", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 玩家睡醒(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        if (event.wakeImmediately()) return;

        污浊度管理器.玩家睡醒(serverPlayer);
    }
}
