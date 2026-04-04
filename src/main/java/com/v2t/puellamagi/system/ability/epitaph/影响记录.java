package com.v2t.puellamagi.system.ability.epitaph;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 影响记录
 *
 * 时间删除用：记录使用者造成/受到的影响
 * 结算时按来源+tick过滤并撤销
 *
 * 不用枚举区分类型：
 * →伤害记录和效果记录分开存
 * → 各自有专门的字段
 * → 撤销逻辑各自处理
 */
public class 影响记录 {

    // ==================== 伤害记录 ====================

    /**
     * 单条伤害记录
     */
    public static class 伤害条目 {
        public final int tick序号;
        public final UUID 攻击者;
        public final UUID 被攻击者;
        public final float 伤害量;

        public 伤害条目(int tick, UUID attacker, UUID target, float damage) {
            this.tick序号 = tick;
            this.攻击者 = attacker;
            this.被攻击者 = target;
            this.伤害量 = damage;
        }
    }

    private final List<伤害条目> 伤害列表 = new ArrayList<>();

    public void 记录伤害(int tick, UUID 攻击者, UUID 被攻击者, float 伤害量) {
        伤害列表.add(new 伤害条目(tick, 攻击者, 被攻击者, 伤害量));
    }

    /**
     * 获取指定来源在指定tick之后造成的所有伤害（结算用）
     */
    public List<伤害条目> 获取来源伤害(UUID 来源UUID, int 起始tick) {
        List<伤害条目> result = new ArrayList<>();
        for (伤害条目 entry : 伤害列表) {
            if (entry.攻击者.equals(来源UUID) && entry.tick序号 >=起始tick) {
                result.add(entry);
            }
        }
        return result;
    }

    // ==================== 效果记录 ====================

    /**
     * 单条效果记录
     */
    public static class 效果条目 {
        public final int tick序号;
        public final UUID 来源;
        public final UUID 目标;
        public final String 效果ID;
        /** 施加效果时目标的血量（用于debuff持续伤害回滚） */
        public final float 施加时血量;

        public 效果条目(int tick, UUID source, UUID target, String effectId, float healthAtApply) {
            this.tick序号 = tick;
            this.来源 = source;
            this.目标 = target;
            this.效果ID = effectId;
            this.施加时血量 = healthAtApply;
        }
    }

    private final List<效果条目> 效果列表 = new ArrayList<>();

    public void 记录效果(int tick, UUID 来源, UUID 目标, String 效果ID, float 当前血量) {
        效果列表.add(new 效果条目(tick, 来源, 目标, 效果ID, 当前血量));
    }

    /**
     * 获取指定来源在指定tick之后施加的所有效果（结算用）
     */
    public List<效果条目> 获取来源效果(UUID 来源UUID, int 起始tick) {
        List<效果条目> result = new ArrayList<>();
        for (效果条目 entry : 效果列表) {
            if (entry.来源.equals(来源UUID) && entry.tick序号 >= 起始tick) {
                result.add(entry);
            }
        }
        return result;
    }

    // ==================== 击杀记录 ====================

    /**
     * 单条击杀记录
     */
    public static class 击杀条目 {
        public final int tick序号;
        public final UUID 击杀者;
        public final UUID 被杀者;

        public 击杀条目(int tick, UUID killer, UUID victim) {
            this.tick序号 = tick;
            this.击杀者 = killer;
            this.被杀者 = victim;
        }
    }

    private final List<击杀条目> 击杀列表 = new ArrayList<>();

    public void 记录击杀(int tick, UUID 击杀者, UUID 被杀者) {
        击杀列表.add(new 击杀条目(tick, 击杀者, 被杀者));
    }

    /**
     * 获取指定来源在指定tick之后的所有击杀（结算用）
     */
    public List<击杀条目> 获取来源击杀(UUID 来源UUID, int 起始tick) {
        List<击杀条目> result = new ArrayList<>();
        for (击杀条目 entry : 击杀列表) {
            if (entry.击杀者.equals(来源UUID) && entry.tick序号 >= 起始tick) {
                result.add(entry);
            }
        }
        return result;
    }

    // ==================== 清理 ====================

    public void 清除全部() {
        伤害列表.clear();
        效果列表.clear();
        击杀列表.clear();
    }

    public int 获取总记录数() {
        return 伤害列表.size() + 效果列表.size() + 击杀列表.size();
    }
}
