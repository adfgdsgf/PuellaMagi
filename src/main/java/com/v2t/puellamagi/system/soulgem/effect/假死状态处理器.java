// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/effect/假死状态处理器.java

package com.v2t.puellamagi.system.soulgem.effect;

import com.v2t.puellamagi.core.config.灵魂宝石配置;
import com.v2t.puellamagi.core.network.packets.s2c.假死状态同步包;
import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.util.灵魂宝石距离计算;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 假死状态处理器
 *
 * 职责：
 * - 管理玩家的假死状态
 * - 处理假死进入/退出逻辑
 * - 同步假死状态到客户端（自己+广播给追踪者）
 * - 管理致命伤害标记（跨Mixin传递状态）
 */
public final class 假死状态处理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/FeignDeath");

    // ==================== 服务端状态存储 ====================

    private static final Map<UUID, Long> 假死开始时间 = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> 位置未知开始时间 = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> 假死位置 = new ConcurrentHashMap<>();
    private static final Set<UUID> 空血假死标记 = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> 致命伤害标记 = ConcurrentHashMap.newKeySet();

    // ==================== 客户端状态 ====================

    @OnlyIn(Dist.CLIENT)
    private static boolean 客户端假死状态 = false;

    @OnlyIn(Dist.CLIENT)
    private static final Set<UUID> 客户端其他玩家假死 = ConcurrentHashMap.newKeySet();

    private 假死状态处理器() {}

    // ==================== 致命伤害标记 API ====================

    public static void 标记致命伤害(UUID playerUUID) {
        致命伤害标记.add(playerUUID);
        LOGGER.debug("玩家 {} 标记为致命伤害中", playerUUID);
    }

    public static boolean 是致命伤害中(UUID playerUUID) {
        return 致命伤害标记.contains(playerUUID);
    }

    public static void 清除致命伤害标记(UUID playerUUID) {
        致命伤害标记.remove(playerUUID);
        LOGGER.debug("玩家 {} 清除致命伤害标记", playerUUID);
    }

    // ==================== 客户端 API ====================

    @OnlyIn(Dist.CLIENT)
    public static void 设置客户端假死状态(boolean 假死) {
        客户端假死状态 = 假死;
        LOGGER.debug("客户端{}假死状态", 假死 ? "进入" : "退出");
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean 客户端是否假死中() {
        return 客户端假死状态;
    }

    @OnlyIn(Dist.CLIENT)
    public static void 设置其他玩家假死状态(UUID playerUUID, boolean 假死) {
        if (假死) {
            客户端其他玩家假死.add(playerUUID);
        } else {
            客户端其他玩家假死.remove(playerUUID);
        }
        LOGGER.debug("客户端：其他玩家 {} {}假死", playerUUID, 假死 ? "进入" : "退出");
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean 客户端应该显示假死效果(Player player) {
        if (player == null) return false;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(player.getUUID())) {
            return 客户端假死状态;
        }

        return 客户端其他玩家假死.contains(player.getUUID());
    }

    @OnlyIn(Dist.CLIENT)
    public static void 清理客户端状态() {
        客户端假死状态 = false;
        客户端其他玩家假死.clear();}

    // ==================== 状态查询 API ====================

    public static boolean 是否假死中(UUID playerUUID) {
        return 假死开始时间.containsKey(playerUUID);
    }

    public static boolean 是否假死中(Player player) {
        return player != null && 是否假死中(player.getUUID());
    }

    public static boolean 是否空血假死(UUID playerUUID) {
        return 空血假死标记.contains(playerUUID);
    }

    public static boolean 是否空血假死(Player player) {
        return player != null && 是否空血假死(player.getUUID());
    }

    public static int 获取假死剩余秒数(ServerPlayer player) {
        Long startTime = 假死开始时间.get(player.getUUID());
        if (startTime == null) return -1;

        long elapsed = player.level().getGameTime() - startTime;
        long remaining = 灵魂宝石配置.获取假死超时Tick() - elapsed;
        return (int) Math.max(0, remaining / 20);
    }

    // ==================== 核心逻辑：空血假死 ====================

    public static void 因空血进入假死(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();

        空血假死标记.add(playerUUID);
        污浊度管理器.空血假死惩罚(player);

        if (!是否假死中(playerUUID)) {
            进入假死状态(player, currentTime, true);
        } else {
            LOGGER.debug("玩家 {} 已在假死中，追加空血标记", player.getName().getString());
        }
    }

    // ==================== 核心逻辑：距离假死 ====================

    public static void 更新假死状态(ServerPlayer player, boolean 距离应该假死) {
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();
        boolean 当前假死 = 是否假死中(playerUUID);

        double 血量阈值 = 灵魂宝石配置.获取退出假死血量阈值();
        boolean 空血中 = 空血假死标记.contains(playerUUID) && player.getHealth() < 血量阈值;
        boolean 应该假死 = 距离应该假死 || 空血中;

        if (应该假死 && !当前假死) {
            进入假死状态(player, currentTime, false);
        } else if (!应该假死 && 当前假死) {
            退出假死状态(player);
        } else if (当前假死) {
            处理假死中(player, currentTime);
        }
    }

    // ==================== 内部逻辑 ====================

    private static void 处理假死中(ServerPlayer player, long currentTime) {
        UUID playerUUID = player.getUUID();
        long startTime = 假死开始时间.getOrDefault(playerUUID, currentTime);

        if (currentTime - startTime > 灵魂宝石配置.获取假死超时Tick()) {
            触发超时死亡(player);
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);宝石登记信息 info = worldData.获取登记信息(playerUUID).orElse(null);

        var result = 灵魂宝石距离计算.计算(player, info, server);
        if (result.原因() ==灵魂宝石距离计算.失败原因.位置未知) {
            处理位置未知(player, currentTime);
        } else {
            位置未知开始时间.remove(playerUUID);
        }

        double 血量阈值 = 灵魂宝石配置.获取退出假死血量阈值();
        if (空血假死标记.contains(playerUUID) && player.getHealth() >= 血量阈值) {LOGGER.debug("玩家 {} 血量恢复至{}，移除空血标记", player.getName().getString(), player.getHealth());
            空血假死标记.remove(playerUUID);
        }
    }

    private static void 处理位置未知(ServerPlayer player, long currentTime) {
        UUID playerUUID = player.getUUID();

        if (!位置未知开始时间.containsKey(playerUUID)) {
            位置未知开始时间.put(playerUUID, currentTime);
            LOGGER.debug("玩家 {} 灵魂宝石位置未知，开始计时", player.getName().getString());
        }

        long unknownStart = 位置未知开始时间.get(playerUUID);

        if (currentTime - unknownStart > 灵魂宝石配置.获取位置未知重生成延迟Tick()) {
            LOGGER.info("玩家 {} 灵魂宝石位置未知超时，重新生成", player.getName().getString());
            boolean success = 灵魂宝石管理器.重新生成灵魂宝石(player);
            if (success) {
                位置未知开始时间.remove(playerUUID);
            }
        }
    }

    // ==================== 进入/退出假死 ====================

    private static void 进入假死状态(ServerPlayer player, long currentTime, boolean 是空血触发) {
        UUID playerUUID = player.getUUID();

        假死开始时间.put(playerUUID, currentTime);
        假死位置.put(playerUUID, player.position());

        player.setDeltaMovement(Vec3.ZERO);

        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        // 同步给自己
        网络工具.发送给玩家(player, new 假死状态同步包(true));

        // 广播给追踪者
        广播假死状态(player, true);

        LOGGER.info("玩家 {} 进入假死状态（{}）",
                player.getName().getString(), 是空血触发 ? "空血" : "距离");

        String messageKey = 是空血触发
                ? "message.puellamagi.feign_death.enter_empty_health"
                : "message.puellamagi.feign_death.enter";

        player.displayClientMessage(
                Component.translatable(messageKey).withStyle(ChatFormatting.RED),
                false
        );
    }

    private static void 退出假死状态(ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        清除玩家状态(playerUUID);

        // 同步给自己
        网络工具.发送给玩家(player, new 假死状态同步包(false));

        // 广播给追踪者
        广播假死状态(player, false);

        LOGGER.info("玩家 {} 退出假死状态", player.getName().getString());

        player.displayClientMessage(
                Component.translatable("message.puellamagi.feign_death.exit").withStyle(ChatFormatting.GREEN),
                false
        );
    }

    public static void 强制退出(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        if (!是否假死中(playerUUID)) return;

        清除玩家状态(playerUUID);
        网络工具.发送给玩家(player, new 假死状态同步包(false));
        广播假死状态(player, false);

        LOGGER.info("玩家 {} 强制退出假死状态", player.getName().getString());
    }

    public static void 尝试恢复(ServerPlayer player) {
        if (!是否假死中(player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(server);
        宝石登记信息 info = worldData.获取登记信息(player.getUUID()).orElse(null);

        var result = 灵魂宝石距离计算.计算(player, info, server);

        double 血量阈值 = 灵魂宝石配置.获取退出假死血量阈值();
        boolean 空血中 = 空血假死标记.contains(player.getUUID())
                && player.getHealth()< 血量阈值;

        if (!result.应该假死() && !空血中) {
            退出假死状态(player);
        }
    }

    private static void 触发超时死亡(ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        清除玩家状态(playerUUID);
        网络工具.发送给玩家(player, new 假死状态同步包(false));
        广播假死状态(player, false);

        LOGGER.warn("玩家 {} 假死超时，判定死亡", player.getName().getString());

        player.displayClientMessage(
                Component.translatable("message.puellamagi.feign_death.timeout")
                        .withStyle(ChatFormatting.DARK_RED),
                false
        );

        player.kill();
    }

    /**
     * 广播假死状态给能看到这个玩家的客户端
     */
    private static void 广播假死状态(ServerPlayer 假死玩家, boolean 是否假死) {
        假死状态同步包 packet = new 假死状态同步包(是否假死, 假死玩家.getUUID());
        网络工具.发送给追踪者(假死玩家, packet);
    }

    // ==================== Tick处理 ====================

    public static void onPlayerTick(ServerPlayer player) {
        if (!是否假死中(player)) return;

        player.setDeltaMovement(Vec3.ZERO);

        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    // ==================== 生命周期 ====================

    public static void onPlayerLogin(ServerPlayer player) {
        boolean 是否假死 = 是否假死中(player);
        网络工具.发送给玩家(player, new 假死状态同步包(是否假死));

        if (是否假死) {
            LOGGER.info("玩家 {} 登录时处于假死状态，已同步", player.getName().getString());
            广播假死状态(player, true);
        }

        // 同步当前所有假死玩家给新登录的玩家
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (UUID 假死玩家UUID : 假死开始时间.keySet()) {
                if (假死玩家UUID.equals(player.getUUID())) continue;

                ServerPlayer 假死玩家 = server.getPlayerList().getPlayer(假死玩家UUID);
                if (假死玩家 != null) {
                    网络工具.发送给玩家(player, new 假死状态同步包(true,假死玩家UUID));
                }
            }
        }
    }

    public static void onPlayerLogout(UUID playerUUID) {
        // 登出不清除假死状态（重连后仍然假死）
    }

    // ==================== 清理 ====================

    public static void 清除玩家状态(UUID playerUUID) {
        假死开始时间.remove(playerUUID);
        位置未知开始时间.remove(playerUUID);
        假死位置.remove(playerUUID);
        空血假死标记.remove(playerUUID);
        致命伤害标记.remove(playerUUID);
    }

    public static void clearAll() {
        假死开始时间.clear();
        位置未知开始时间.clear();
        假死位置.clear();
        空血假死标记.clear();
        致命伤害标记.clear();
    }
}
