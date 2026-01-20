// 文件路径: src/main/java/com/v2t/puellamagi/core/event/通用事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.core.command.测试命令;
import com.v2t.puellamagi.core.network.packets.s2c.技能能力同步包;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能能力;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.system.transformation.变身能力;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 通用事件处理
 */
@Mod.EventBusSubscriber(modid =常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class 通用事件 {

    private static final ResourceLocation 变身能力ID = new ResourceLocation(常量.MOD_ID, "transformation");
    private static final ResourceLocation 技能能力ID = new ResourceLocation(常量.MOD_ID, "skill");

    @SubscribeEvent
    public static void 注册命令(RegisterCommandsEvent event) {
        测试命令.register(event.getDispatcher());
        PuellaMagi.LOGGER.info("Puella Magi 命令注册完成");
    }

    @SubscribeEvent
    public static void 附加能力(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(ModCapabilities.变身能力).isPresent()) {
                event.addCapability(变身能力ID, new 变身能力());
            }if (!event.getObject().getCapability(ModCapabilities.技能能力).isPresent()) {
                event.addCapability(技能能力ID, new 技能能力());
            }
        }
    }

    @SubscribeEvent
    public static void 玩家克隆(PlayerEvent.Clone event) {
        Player 原玩家 = event.getOriginal();
        Player 新玩家 = event.getEntity();

        原玩家.reviveCaps();

        try {
            能力工具.获取变身能力完整(原玩家).ifPresent(旧能力 -> {
                能力工具.获取变身能力完整(新玩家).ifPresent(新能力 -> {
                    新能力.复制自(旧能力);
                    if (event.isWasDeath()) {
                        新能力.设置变身状态(false);
                        能力管理器.失效能力(原玩家);
                        // 死亡时结束时停
                        时停管理器.玩家下线(原玩家);
                        PuellaMagi.LOGGER.debug("玩家 {} 死亡重生，已解除变身状态", 新玩家.getName().getString());
                    }
                });
            });
            能力工具.获取技能能力(原玩家).ifPresent(旧能力 -> {
                能力工具.获取技能能力(新玩家).ifPresent(新能力 -> {
                    新能力.复制自(旧能力);
                    if (event.isWasDeath()) {
                        新能力.清除所有冷却();
                    }
                });
            });
        } finally {
            原玩家.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void 玩家登录(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            变身管理器.同步变身数据(serverPlayer);
            同步技能能力(serverPlayer);
            PuellaMagi.LOGGER.debug("玩家 {} 登录，已同步数据", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 玩家登出(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            能力管理器.玩家下线(serverPlayer);
            技能管理器.玩家下线(serverPlayer);
            // 时停者下线，结束时停
            时停管理器.玩家下线(serverPlayer);
            PuellaMagi.LOGGER.debug("玩家 {} 登出，已清理", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 玩家重生(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            变身管理器.同步变身数据(serverPlayer);
            同步技能能力(serverPlayer);
            PuellaMagi.LOGGER.debug("玩家 {} 重生，已同步数据", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 维度切换(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 时停者切换维度，结束时停（根据配置可能需要）
            if (时停管理器.是否时停者(serverPlayer)) {
                时停管理器.结束时停(serverPlayer);PuellaMagi.LOGGER.debug("时停者 {} 切换维度，时停结束", serverPlayer.getName().getString());
            }

            变身管理器.同步变身数据(serverPlayer);
            同步技能能力(serverPlayer);
            PuellaMagi.LOGGER.debug("玩家 {} 切换维度，已同步数据", serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void 玩家Tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;

        能力管理器.tickAll(serverPlayer);
        能力工具.获取技能能力(serverPlayer).ifPresent(技能能力::tick);
        技能管理器.tickAll(serverPlayer);
    }

    // ==================== 同步方法 ====================

    /**
     * 同步技能能力数据给客户端
     */
    public static void 同步技能能力(ServerPlayer player) {
        能力工具.获取技能能力(player).ifPresent(cap -> {
            技能能力同步包 packet = new 技能能力同步包(player.getUUID(), cap.写入NBT());
            网络工具.发送给玩家(player, packet);
        });
    }
}
