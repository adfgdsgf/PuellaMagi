// 文件路径: src/main/java/com/v2t/puellamagi/core/event/战斗事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.system.ability.epitaph.影响记录工具;
import com.v2t.puellamagi.system.ability.epitaph.预知状态管理;
import com.v2t.puellamagi.system.team.队伍管理器;
import com.v2t.puellamagi.常量;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 战斗事件处理
 *
 * 职责：
 * - 友伤控制（队伍系统）
 * - 时间删除期间的伤害/效果拦截
 * - 伤害和击杀的影响记录（预知系统用）
 *
 * 不含业务逻辑，只做判断和转发
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class 战斗事件 {

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
    @SubscribeEvent(priority = EventPriority.HIGHEST)
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
    public static void 时删效果拦截(MobEffectEvent.Applicable event) {
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
        event.setResult(Event.Result.DENY);
    }

    /**
     * 回放期间：记录伤害到影响记录（时间删除结算用）
     *
     * 记录所有实体间的伤害（不只是使用者的）
     * 结算时按来源筛选
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void 记录伤害(LivingHurtEvent event) {
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
    public static void 记录击杀(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity killer = event.getSource().getEntity();
        if (killer == null) return;

        影响记录工具.尝试记录击杀(killer, event.getEntity());
    }
}
