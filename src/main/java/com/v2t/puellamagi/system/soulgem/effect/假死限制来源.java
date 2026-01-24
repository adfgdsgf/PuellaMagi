// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/effect/假死限制来源.java

package com.v2t.puellamagi.system.soulgem.effect;

import com.v2t.puellamagi.api.restriction.I限制来源;
import com.v2t.puellamagi.api.restriction.限制类型;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 假死状态的限制来源
 *
 * 假死时限制所有行动
 */
public class 假死限制来源 implements I限制来源 {

    /** 假死时的限制集合（全部限制） */
    private static final Set<限制类型> 全部限制 = Collections.unmodifiableSet(
            EnumSet.allOf(限制类型.class)
    );

    @Override
    public Set<限制类型> 获取限制(Player player) {
        if (假死状态处理器.是否假死中(player)) {
            return 全部限制;
        }
        return Collections.emptySet();
    }

    @Override
    public String 获取来源名称() {
        return "假死状态";
    }
}
