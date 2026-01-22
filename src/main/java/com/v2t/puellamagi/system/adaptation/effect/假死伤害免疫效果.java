// 文件路径: src/main/java/com/v2t/puellamagi/system/adaptation/effect/假死伤害免疫效果.java

package com.v2t.puellamagi.system.adaptation.effect;

import com.v2t.puellamagi.api.adaptation.I适应效果;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * 假死伤害免疫效果
 *
 * 空血假死时触发的临时伤害免疫
 * 防止环境伤害导致无限死亡循环
 */
public class 假死伤害免疫效果 implements I适应效果 {

    public static final ResourceLocation ID = 资源工具.本mod("feign_death_damage_immunity");
    public static final 假死伤害免疫效果 INSTANCE = new 假死伤害免疫效果();

    /** 基础免疫时长（tick） */
    private static final long 基础免疫时长 = 20 * 2;// 2秒

    /** 连续判定时间（tick） */
    @Override
    public long 获取连续判定时间() {
        return 20 * 30;  // 30秒
    }

    private 假死伤害免疫效果() {}

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public String 获取名称() {
        return "假死伤害免疫";
    }

    @Override
    public boolean 是否免疫伤害(Player player, DamageSource source, long 免疫结束时间, long 当前时间) {
        return 当前时间 < 免疫结束时间;
    }

    @Override
    public long 计算免疫时长(int 连续触发次数) {
        // 公式：基础 × 2^(n-1)，无上限
        // 第1次：2秒
        // 第2次：4秒
        // 第3次：8秒
        // ...
        int n = Math.max(1, 连续触发次数);
        return 基础免疫时长 * (1L << (n - 1));
    }
}
