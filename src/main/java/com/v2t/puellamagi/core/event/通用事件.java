package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.core.command.命令注册器;
import com.v2t.puellamagi.core.network.packets.s2c.队友位置同步包;
import com.v2t.puellamagi.system.ability.epitaph.*;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.skill.impl.预知技能;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.system.soulgem.污浊度管理器;
import com.v2t.puellamagi.system.soulgem.污浊度能力;
import com.v2t.puellamagi.system.team.队伍同步工具;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.system.team.队伍数据;
import com.v2t.puellamagi.system.team.队伍邀请管理器;
import com.v2t.puellamagi.util.network.存在屏蔽器;
import com.v2t.puellamagi.util.network.输入接管器;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.core.network.packets.s2c.技能能力同步包;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import com.v2t.puellamagi.system.ability.能力管理器;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import com.v2t.puellamagi.system.contract.契约能力;
import com.v2t.puellamagi.system.skill.技能管理器;
import com.v2t.puellamagi.system.skill.技能能力;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.system.transformation.变身能力;
import com.v2t.puellamagi.util.绑定物品工具;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 通用事件处理
 *
 * 职责：监听Forge事件并转发给对应的管理器/系列
 * 不包含业务逻辑，只做转发
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class 通用事件 {

    private static final ResourceLocation 变身能力ID = new ResourceLocation(常量.MOD_ID, "transformation");
    private static final ResourceLocation 技能能力ID = new ResourceLocation(常量.MOD_ID, "skill");
    private static final ResourceLocation 契约能力ID = new ResourceLocation(常量.MOD_ID, "contract");
    private static final ResourceLocation 污浊度能力ID = new ResourceLocation(常量.MOD_ID, "corruption");

    /**
     * 清理计数器
     * 用于定期执行缓存清理，防止内存泄漏
     * 每200tick（10秒）执行一次
     */
    private static int 清理计数器 = 0;

    @SubscribeEvent
    public static void 注册命令(RegisterCommandsEvent event) {
        命令注册器.register(event.getDispatcher());
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
            if (!event.getObject().getCapability(ModCapabilities.契约能力).isPresent()) {
                event.addCapability(契约能力ID, new 契约能力());
            }
            if (!event.getObject().getCapability(ModCapabilities.污浊度能力).isPresent()) {
                event.addCapability(污浊度能力ID, new 污浊度能力());
            }
        }
    }

    @SubscribeEvent
    public static void 玩家克隆(PlayerEvent.Clone event) {
        Player 原玩家 = event.getOriginal();
        Player 新玩家 = event.getEntity();

        原玩家.reviveCaps();

        try {
            // ==================== 能力复制 ====================

            // 复制变身能力
            能力工具.获取变身能力完整(原玩家).ifPresent(旧能力 -> {
                能力工具.获取变身能力完整(新玩家).ifPresent(新能力 -> {
                    新能力.复制自(旧能力);
                    if (event.isWasDeath()) {
                        新能力.设置变身状态(false);
                        能力管理器.失效能力(原玩家);
                        时停管理器.玩家下线(原玩家);
                        PuellaMagi.LOGGER.debug("玩家 {} 死亡重生，已解除变身状态",
                                新玩家.getName().getString());
                    }
                });
            });

            // 复制技能能力
            能力工具.获取技能能力(原玩家).ifPresent(旧能力 -> {
                能力工具.获取技能能力(新玩家).ifPresent(新能力 -> {
                    新能力.复制自(旧能力);
                    if (event.isWasDeath()) {
                        新能力.清除所有冷却();
                    }
                });
            });

            // 复制契约能力（契约状态在死亡后保留）
            能力工具.获取契约能力完整(原玩家).ifPresent(旧能力 -> {
                能力工具.获取契约能力完整(新玩家).ifPresent(新能力 -> {
                    新能力.复制自(旧能力);
                });
            });

            // 复制污浊度能力
            能力工具.获取污浊度能力完整(原玩家).ifPresent(旧能力 -> {
                能力工具.获取污浊度能力完整(新玩家).ifPresent(新能力 -> {
                    新能力.复制自(旧能力);
                    if (event.isWasDeath()) {
                        污浊度管理器.玩家死亡(新玩家, 新能力);
                    }
                });
            });

            // ==================== 死亡专属处理 ====================

            if (event.isWasDeath()) {
                // 清除假死状态
                假死状态处理器.清除玩家状态(新玩家.getUUID());

                // 强制终止预知能力
                预知技能.玩家下线(新玩家.getUUID());

                // 同步死亡保留的绑定物品
                绑定物品工具.同步到新玩家(原玩家, 新玩家);
            }

        } finally {
            原玩家.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void 玩家登录(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 通用同步
            契约管理器.同步契约状态(serverPlayer);
            变身管理器.同步变身数据(serverPlayer);
            同步技能能力(serverPlayer);
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
            同步技能能力(serverPlayer);
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

            // 通用同步
            契约管理器.同步契约状态(serverPlayer);
            变身管理器.同步变身数据(serverPlayer);
            同步技能能力(serverPlayer);
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

    @SubscribeEvent
    public static void 服务器关闭(ServerStoppingEvent event) {
        // 通用清理
        假死状态处理器.clearAll();
        绑定物品工具.清理所有缓存();

        // 预知系统清理
        录制管理器.清除全部();
        复刻引擎.清除全部();
        预知状态管理.清除全部();
        输入接管器.清除全部();
        存在屏蔽器.清除全部();

        // 队伍邀请清理（非持久化数据）
        队伍邀请管理器.clearAll();

        PuellaMagi.LOGGER.info("服务器关闭，已清理所有缓存");
    }

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

        // ==================== 队友位置同步（每20tick = 1秒） ====================
        if (serverPlayer.tickCount % 5 == 0) {
            同步队友位置(serverPlayer);
        }
    }

    /**
     * 同步技能能力到客户端
     */
    public static void 同步技能能力(ServerPlayer player) {
        能力工具.获取技能能力(player).ifPresent(cap -> {
            技能能力同步包 packet = new 技能能力同步包(player.getUUID(), cap.写入NBT());
            网络工具.发送给玩家(player, packet);
        });
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

    /**
     * 友伤控制
     *
     * 使用LivingAttackEvent在hurt()最早阶段拦截
     * cancel后完全不触发：伤害、受击动画、击退、音效
     *
     * 检查攻击者的个人配置：
     * - A开友伤、B关友伤 → A打B有伤害，B打A无伤害
     */
    @SubscribeEvent
    public static void 攻击事件(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (attacker.equals(target)) return;

        MinecraftServer server = attacker.getServer();
        if (server == null) return;

        if (!队伍管理器.是否同队(server, attacker.getUUID(), target.getUUID())) return;

        if (!队伍管理器.获取个人配置(server, attacker.getUUID(), "friendlyFire")) {
            event.setCanceled(true);
        }
    }

    /**
     * 时间删除期间：拦截对使用者的新外部伤害
     *
     * 只拦截来源是实体/玩家的伤害
     * 环境伤害（火/溺水/窒息/摔落/虚空）不拦截
     * 已有效果的持续伤害不拦截
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void 时删伤害拦截(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer target)) return;

        UUID targetUUID = target.getUUID();

        // 检查目标是否在时间删除中
        if (!预知状态管理.是否时间删除中(targetUUID)) return;

        // 判断伤害来源
        net.minecraft.world.damagesource.DamageSource source = event.getSource();

        // 有实体来源的伤害 → 拦截（怪/玩家攻击、投射物等）
        if (source.getEntity() != null || source.getDirectEntity() != null) {
            event.setCanceled(true);
            return;
        }

        // 环境伤害不拦截 → 正常受伤
        // 火/溺水/窒息/摔落/虚空/饥饿等全部放行
    }

    /**
     * 时间删除期间：拦截对使用者施加的新效果
     *
     * 已有的效果正常tick
     * 只拦截新施加的（时删后来的）
     */
    @SubscribeEvent
    public static void 时删效果拦截(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer target)) return;

        UUID targetUUID = target.getUUID();
        if (!预知状态管理.是否时间删除中(targetUUID)) return;

        // 检查这个效果是不是已有的（时删前就有的）
        net.minecraft.world.effect.MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null) return;

        // 如果玩家身上已经有这个效果 → 放行（已有的正常tick）
        if (target.hasEffect(incoming.getEffect())) return;

        // 新效果 → 拦截
        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    /**
     * 回放期间：记录伤害到影响记录（时间删除结算用）
     *
     * 记录所有实体间的伤害（不只是使用者的）
     * 结算时按来源筛选
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void 记录伤害(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.isCanceled()) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        影响记录工具.尝试记录伤害(attacker, event.getEntity(), event.getAmount());
    }

    /**
     * 回放期间：记录击杀到影响记录（时间删除结算用）
     */
    @SubscribeEvent
    public static void 记录击杀(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity killer = event.getSource().getEntity();
        if (killer == null) return;

        影响记录工具.尝试记录击杀(killer, event.getEntity());
    }

    /**
     * 方块变化事件 — 录制方块变化
     * 预知能力录制期间追踪方块变化
     */
    @SubscribeEvent
    public static void 方块放置(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        // 获取触发者UUID（放置方块的玩家）
        UUID triggerUUID = null;
        if (event.getEntity() instanceof Player player) {
            triggerUUID = player.getUUID();
        }

        录制管理器.记录方块变化(serverLevel, event.getPos(),
                event.getBlockSnapshot().getReplacedBlock(), event.getPlacedBlock(), triggerUUID);
    }

    @SubscribeEvent
    public static void 方块破坏(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        // 获取触发者UUID（破坏方块的玩家）
        UUID triggerUUID = event.getPlayer() != null ? event.getPlayer().getUUID() : null;

        录制管理器.记录方块变化(serverLevel, event.getPos(),
                event.getState(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), triggerUUID);
    }

    /**
     * 服务端Tick — 驱动录制和复刻
     *
     * 录制：END阶段采集（Level.tick执行完毕，所有实体状态已更新）
     * 复刻：START阶段驱动（在Level.tick之前设好位置和重放包）
     *
     * 为什么复刻必须在START：
     * Level.tick → player.tick → ServerPlayerGameMode.tick
     * → 检查"玩家还在看着那个方块吗？"
     * → 如果我们还没设好位置/朝向 → 判定"移开了" → 破坏进度中断
     * → 放在START → 位置/朝向已就位 → 破坏进度正常推进
     */
    @SubscribeEvent
    public static void 服务端Tick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (event.phase == TickEvent.Phase.START) {
            // ==================== 复刻引擎驱动（Level.tick之前） ====================
            // 位置/朝向 + 包回放必须在Level.tick之前
            // 否则破坏进度检查时玩家位置不对
            for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
                boolean still = 复刻引擎.tick(userUUID);
                if (!still) {
                    复刻引擎.结束复刻(userUUID);
                    预知状态管理.结束(userUUID);
                    PuellaMagi.LOGGER.info("玩家 {} 复刻自然播放完毕", userUUID);
                }
            }
        }

        if (event.phase == TickEvent.Phase.END) {
            // ==================== 录制帧采集（Level.tick之后） ====================
            for (UUID userUUID : 录制管理器.获取所有活跃使用者()) {
                录制管理器.采集帧(userUUID);
            }

        }
    }

    // ==================== 时间删除 — 传染标记 ====================

    /**
     * 方块破坏时：检查方块是否有来源标记 →掉落物继承标记
     *
     * 场景：A（使用者）放了方块 → B破坏了→ 掉落物应该标记=A
     * 结算时掉落物被清除 → 不刷物品
     */
    @SubscribeEvent
    public static void 方块破坏传染(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();

        // 检查所有活跃复刻会话的标记表
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session == null || !session.时间删除中) continue;

            UUID 方块来源 = session.录制.标记表.获取方块来源(pos);
            if (方块来源 == null) continue;

            // 方块有标记 → 掉落物继承
            // 延迟一tick标记掉落物（掉落物在破坏后才生成）
            final UUID source = 方块来源;
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + 1, () -> {
                //搜索方块位置附近的掉落物
                net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(pos).inflate(2.0);
                for (Entity entity : serverLevel.getEntities(
                        (Entity) null, area,
                        e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) {
                    session.录制.标记表.标记掉落物(entity.getId(), source);
                }
            }));

            // 清除方块标记（方块已经没了）
            session.录制.标记表.清除方块标记(pos);
            break;
        }
    }

    /**
     * 玩家拾取物品时：检查掉落物是否有来源标记 → 物品NBT加tag
     *
     * 场景：B捡起了来源=A的掉落物 → 物品NBT标记A
     * 结算时从B背包清除
     */
    @SubscribeEvent
    public static void 拾取传染(net.minecraftforge.event.entity.player.EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide) return;

        net.minecraft.world.entity.item.ItemEntity itemEntity = event.getItem();
        int entityId = itemEntity.getId();

        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session == null || !session.时间删除中) continue;

            UUID 来源 = session.录制.标记表.获取掉落物来源(entityId);
            if (来源 == null) continue;

            //掉落物有标记 → 物品NBT加tag
            net.minecraft.world.item.ItemStack stack = itemEntity.getItem();
            影响标记表.标记物品(stack, 来源);

            // 清除掉落物标记（已被捡起）
            session.录制.标记表.清除掉落物标记(entityId);
            break;
        }
    }

    /**
     * 合成时：检查原料是否有来源标记 → 产物继承标记 +玩家自己的原料退回
     *
     * 场景：B用A的钻石+自己的木棍合成了东西
     * → 产物标记=A → 结算时删除
     * → 木棍退回B背包 → B不亏
     */
    @SubscribeEvent
    public static void 合成传染(net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 检查是否在复刻的时间删除期间
        boolean 在时删中 = false;
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session != null && session.时间删除中) {
                在时删中 = true;
                break;
            }
        }
        if (!在时删中) return;

        // 检查原料中是否有标记的物品
        net.minecraft.world.Container craftMatrix = event.getInventory();
        UUID 标记来源 = null;
        List<net.minecraft.world.item.ItemStack> 玩家自己的原料 = new ArrayList<>();

        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack ingredient = craftMatrix.getItem(i);
            if (ingredient.isEmpty()) continue;

            UUID 物品来源 = 影响标记表.获取物品来源(ingredient);
            if (物品来源 != null) {
                标记来源 = 物品来源;
            } else {
                // 不是标记物品 → 是玩家自己的原料 → 记录用于退回
                玩家自己的原料.add(ingredient.copy());
            }
        }

        if (标记来源 == null) return;

        // 产物标记来源
        影响标记表.标记物品(event.getCrafting(), 标记来源);

        // 退回玩家自己的原料
        for (net.minecraft.world.item.ItemStack 退回物品 : 玩家自己的原料) {
            if (!player.getInventory().add(退回物品)) {
                // 背包满了就丢地上
                player.drop(退回物品, false);
            }
        }
    }

}
