// 文件路径: src/main/java/com/v2t/puellamagi/core/event/服务端时停事件.java

package com.v2t.puellamagi.core.event.server;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端时停相关事件处理
 *
 * 处理边界情况：死亡、下线、切维度
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID)
public class 服务端时停事件 {

    /**
     * 玩家死亡
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LivingEntity entity = event.getEntity();

        // 如果是时停者死亡，结束时停
        if (entity instanceof ServerPlayer player) {
            if (时停管理器.是否时停者(player)) {
                时停管理器.结束时停(player);
            }
        }

        // 清理该实体的累计伤害（如果有）
        时停管理器.清理实体伤害(entity);
    }

    /**
     * 玩家下线
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            时停管理器.玩家下线(player);
        }
    }

    /**
     * 玩家切换维度
     */
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // 时停者切维度，结束时停
            if (时停管理器.是否时停者(player)) {
                时停管理器.结束时停(player);
            }
        }
    }

    /**
     * 玩家重生
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // 重生后确保时停状态干净
        if (event.getEntity() instanceof ServerPlayer player) {
            // 如果之前是时停者，已经在死亡时处理了
            // 这里确保蓄力状态也清理
            时停管理器.结束蓄力(player);
        }
    }
}
