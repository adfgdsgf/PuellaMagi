// 文件路径: src/main/java/com/v2t/puellamagi/system/adaptation/source/空血假死适应源.java

package com.v2t.puellamagi.system.adaptation.source;

import com.v2t.puellamagi.api.adaptation.I适应源;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * 空血假死适应源
 *
 * 当玩家因某种伤害进入空血假死时触发适应
 */
public class 空血假死适应源 implements I适应源 {

    public static final ResourceLocation ID = 资源工具.本mod("empty_health_feign_death");
    public static final 空血假死适应源 INSTANCE = new 空血假死适应源();

    private 空血假死适应源() {}

    @Override
    public ResourceLocation 获取ID() {
        return ID;
    }

    @Override
    public boolean 应该触发(Player player, DamageSource damageSource, Object context) {
        // 空血假死时一定触发
        // 具体判断逻辑在Mixin中，这里只要被调用就返回true
        return damageSource != null;
    }
}
