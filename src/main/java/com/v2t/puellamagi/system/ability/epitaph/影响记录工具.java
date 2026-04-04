package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * 影响记录工具
 *
 * 统一入口：自动判断当前是录制期间还是复刻期间
 * 通用事件只需调一个方法，不需要自己遍历两个管理器
 *
 * 优先级：复刻期间 > 录制期间
 * 两者不会同时存在（录制结束后才开始复刻）
 */
public final class 影响记录工具 {

    private 影响记录工具() {}

    /**
     * 尝试记录伤害
     *
     * @param attacker 攻击者
     * @param target 被攻击者
     * @param amount 伤害量
     * @return 是否成功记录
     */
    public static boolean 尝试记录伤害(Entity attacker, Entity target, float amount) {
        // 复刻期间
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session == null) continue;

            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
            if (state == null) continue;

            session.录制.影响.记录伤害(state.获取当前复刻帧(),
                    attacker.getUUID(),
                    target.getUUID(),
                    amount
            );
            return true;
        }

        // 录制期间
        for (UUID userUUID : 录制管理器.获取所有活跃使用者()) {
            录制管理器.录制会话 session = 录制管理器.获取会话(userUUID);
            if (session == null) continue;

            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
            if (state == null) continue;

            session.影响.记录伤害(
                    state.获取已录制帧数(),
                    attacker.getUUID(),
                    target.getUUID(),
                    amount
            );
            return true;
        }

        return false;
    }

    /**
     * 尝试记录击杀
     *
     * @param killer 击杀者
     * @param victim 被杀者
     * @return 是否成功记录
     */
    public static boolean 尝试记录击杀(Entity killer, Entity victim) {
        // 复刻期间
        for (UUID userUUID : 复刻引擎.获取所有活跃使用者()) {
            复刻引擎.复刻会话 session = 复刻引擎.获取会话(userUUID);
            if (session == null) continue;

            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
            if (state == null) continue;

            session.录制.影响.记录击杀(
                    state.获取当前复刻帧(),
                    killer.getUUID(),
                    victim.getUUID()
            );
            return true;
        }

        // 录制期间
        for (UUID userUUID : 录制管理器.获取所有活跃使用者()) {
            录制管理器.录制会话 session = 录制管理器.获取会话(userUUID);
            if (session == null) continue;

            预知状态管理.玩家预知状态 state = 预知状态管理.获取状态(userUUID);
            if (state == null) continue;

            session.影响.记录击杀(
                    state.获取已录制帧数(),
                    killer.getUUID(),
                    victim.getUUID()
            );
            return true;
        }

        return false;
    }
}
