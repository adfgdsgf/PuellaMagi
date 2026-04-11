package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.api.restriction.I限制来源;
import com.v2t.puellamagi.api.restriction.限制类型;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Set;

/**
 * 预知限制来源
 *
 * Phase 2（复刻中）— 战斗/交互限制：
 *   移动和视角由客户端输入回放控制（KeyboardInput/Mouse Mixin替换输入）
 *   MC自己计算物理/动画→ 需要允许移动和视角
 *   攻击/交互/使用物品等仍然禁止（命运锁定的非移动部分）
 *
 * Phase 3（时间删除）使用者— 部分限制：
 *   可以移动和转动视角，不能战斗/交互
 *
 * 设计变更历史：
 * 旧方案：Phase 2全部锁定（服务端帧数据驱动位置）→ 手部抖动/平移问题
 * 新方案：Phase 2允许移动（客户端输入回放驱动）→ MC自己算动画，完美复刻
 */
public class 预知限制来源 implements I限制来源 {

    /**
     * Phase 2: 复刻中 —纯输入回放，不限制任何动作
     *
     * 旧方案：限制攻击/交互/使用物品（帧驱动方块变化）
     * 新方案：全部放行（输入回放 → MC自己处理一切）
     *
     * 客户端按键被回放数据替换（真实键盘被清空）
     * →玩家无法手动操作 → 不需要服务端再限制
     * → 服务端正常处理C2S包 → 动画/音效/背包消耗全正确
     */
    private static final Set<限制类型> 复刻限制 = EnumSet.noneOf(限制类型.class);

    /**
     * Phase 3: 时间删除（使用者自由行动）
     *
     * 自己对自己的操作允许（吃东西/喝药水/丢物品）
     * 自己对外部的操作禁止（放方块/破坏/交互/攻击）
     */
    private static final Set<限制类型> 时间删除限制 = EnumSet.of(
            限制类型.攻击,
            限制类型.释放技能,
            限制类型.交互方块,
            限制类型.交互实体,
            限制类型.破坏方块
    );

    private static final Set<限制类型> 无限制 = EnumSet.noneOf(限制类型.class);

    //==================== 接口实现 ====================

    @Override
    public String 获取来源名称() {
        return "epitaph";
    }

    @Override
    public Set<限制类型> 获取限制(Player player) {
        预知状态管理.阶段 phase = 预知状态管理.获取阶段(player.getUUID());

        return switch (phase) {
            case 待机 -> 无限制;
            case 录制中 -> 无限制;
            case 等待回放 -> 无限制;
            case 复刻中 -> 复刻限制;
            case 时间删除 -> 时间删除限制;
        };
    }
}
