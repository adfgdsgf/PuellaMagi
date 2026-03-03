// 文件路径: src/main/java/com/v2t/puellamagi/system/ability/timestop/时停限制来源.java

package com.v2t.puellamagi.system.ability.timestop;

import com.v2t.puellamagi.api.restriction.I限制来源;
import com.v2t.puellamagi.api.restriction.限制类型;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 时停状态的限制来源
 *
 * 被时停冻结的玩家限制所有行动
 * 通过行动限制管理器统一拦截，与假死共用FeignDeathInputMixin
 */
public class 时停限制来源 implements I限制来源 {

    /** 时停时的限制集合（全部限制） */
    private static final Set<限制类型> 全部限制 = Collections.unmodifiableSet(
            EnumSet.allOf(限制类型.class)
    );

    @Override
    public Set<限制类型> 获取限制(Player player) {
        if (时停豁免系统.应该冻结(player)) {
            return 全部限制;
        }
        return Collections.emptySet();
    }

    @Override
    public String 获取来源名称() {
        return "时停冻结";
    }
}
