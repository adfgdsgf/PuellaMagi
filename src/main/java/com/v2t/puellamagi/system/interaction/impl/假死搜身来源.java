// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/impl/假死搜身来源.java

package com.v2t.puellamagi.system.interaction.impl;

import com.v2t.puellamagi.api.interaction.I可被搜身;
import com.v2t.puellamagi.core.config.搜身配置;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import net.minecraft.world.entity.player.Player;

/**
 * 假死搜身来源
 *
 * 当玩家处于假死状态（距离假死或空血假死）时，可被搜身
 * 假死状态下玩家无法感知，因此不需要提示
 */
public class 假死搜身来源 implements I可被搜身 {

    public static final 假死搜身来源 INSTANCE = new 假死搜身来源();

    private 假死搜身来源() {}

    @Override
    public String 获取来源ID() {
        return "puellamagi:feign_death";
    }

    @Override
    public boolean 可被搜身(Player target, Player searcher) {
        // 检查配置是否启用
        if (!搜身配置.是否启用()) {
            return false;
        }
        if (!搜身配置.假死时可被搜身()) {
            return false;
        }

        return 假死状态处理器.是否假死中(target);
    }

    @Override
    public boolean 需要提示被搜身者(Player target) {
        // 假死状态无法感知，但仍遵循配置
        // 这里返回false因为假死状态下玩家无法感知
        return false;
    }

    @Override
    public String 获取提示消息键() {
        return "message.puellamagi.being_searched";
    }
}
