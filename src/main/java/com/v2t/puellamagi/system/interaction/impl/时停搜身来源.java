// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/impl/时停搜身来源.java

package com.v2t.puellamagi.system.interaction.impl;

import com.v2t.puellamagi.api.interaction.I可被搜身;
import com.v2t.puellamagi.api.timestop.时停豁免级别;
import com.v2t.puellamagi.core.config.搜身配置;
import com.v2t.puellamagi.system.ability.timestop.时停豁免系统;
import net.minecraft.world.entity.player.Player;

/**
 * 时停搜身来源
 *
 * 当目标玩家被时停冻结时，可被搜身
 *
 * 判断逻辑：
 * - 目标的豁免级别 == 无豁免（完全冻结）
 * - 搜身者必须有完全豁免（能行动）
 *
 * 无豁免者无法感知时停中发生的事，因此不需要提示
 * 视觉豁免者虽然能看见但不能动，可选择提示
 */
public class 时停搜身来源 implements I可被搜身 {

    public static final 时停搜身来源 INSTANCE = new 时停搜身来源();

    private 时停搜身来源() {}

    @Override
    public String 获取来源ID() {
        return "puellamagi:time_stop";
    }

    @Override
    public boolean 可被搜身(Player target, Player searcher) {
        // 检查配置是否启用
        if (!搜身配置.是否启用()) {
            return false;
        }
        if (!搜身配置.时停时可被搜身()) {
            return false;
        }

        // 搜身者必须有完全豁免（能行动）
        时停豁免级别 searcherLevel = 时停豁免系统.获取豁免级别(searcher);
        if (searcherLevel != 时停豁免级别.完全豁免) {
            return false;
        }

        // 目标必须被冻结（无豁免或视觉豁免）
        时停豁免级别 targetLevel = 时停豁免系统.获取豁免级别(target);
        return targetLevel.需要冻结();
    }

    @Override
    public boolean 需要提示被搜身者(Player target) {
        // 检查配置是否需要通知
        if (!搜身配置.是否通知被搜身者()) {
            return false;
        }

        // 视觉豁免者能看见但不能动，可以提示
        时停豁免级别 level = 时停豁免系统.获取豁免级别(target);
        return level == 时停豁免级别.视觉豁免;
    }

    @Override
    public String 获取提示消息键() {
        return "message.puellamagi.being_searched";
    }
}
