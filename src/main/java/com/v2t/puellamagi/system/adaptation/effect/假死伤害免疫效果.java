// 文件路径: src/main/java/com/v2t/puellamagi/system/adaptation/effect/假死伤害免疫效果.java

package com.v2t.puellamagi.system.adaptation.effect;

import com.v2t.puellamagi.api.adaptation.I适应效果;
import com.v2t.puellamagi.core.config.假死环境适应配置;
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
    public long 获取连续判定时间() {
        return 假死环境适应配置.获取连续判定窗口();
    }

    @Override
    public boolean 是否免疫伤害(Player player, DamageSource source, long 免疫结束时间, long 当前时间) {
        // 检查是否启用
        if (!假死环境适应配置.是否启用()) {
            return false;
        }
        return 当前时间 < 免疫结束时间;
    }

    @Override
    public long 计算免疫时长(int 连续触发次数) {
        int n = Math.max(1, 连续触发次数);

        long 基础时长 = 假死环境适应配置.获取基础免疫时长();
        long 最大时长 = 假死环境适应配置.获取最大免疫时长();
        double 倍率 = 假死环境适应配置.获取连续免疫倍率();

        // 公式：基础 × 倍率^(n-1)
        // 第1次：基础
        // 第2次：基础 × 倍率
        // 第3次：基础 × 倍率²
        // ...
        long 计算时长 = (long) (基础时长 * Math.pow(倍率, n - 1));

        // 不超过最大值
        return Math.min(计算时长, 最大时长);
    }
}
