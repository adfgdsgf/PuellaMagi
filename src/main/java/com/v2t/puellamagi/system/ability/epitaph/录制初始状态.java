package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 录制初始状态
 *
 * 录制开始时拍摄的"正在进行中的操作"快照
 *
 * 服务端部分：
 * - 使用物品状态（手+剩余时间）→ 服务端可查
 * - 破坏方块状态 → 服务端可查
 *
 * 客户端部分：
 * - 所有KeyMapping的isDown状态 → 客户端上报
 * - 存在录制会话的玩家初始按键表里
 *
 * 回放开始时：
 * - 服务端恢复使用物品状态
 * - 客户端恢复所有按住的KeyMapping
 */
public class 录制初始状态 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/InitialState");

    //==================== 使用物品状态 ====================

    private boolean 正在使用物品 = false;
    private InteractionHand 使用的手 = null;
    private int 使用剩余时间 = 0;

    // ==================== 破坏方块状态 ====================

    private boolean 正在破坏方块 = false;

    // ==================== 从玩家采集（服务端部分） ====================

    public static 录制初始状态 从玩家采集(ServerPlayer player) {
        录制初始状态 state = new 录制初始状态();

        if (player.isUsingItem()) {
            state.正在使用物品 = true;
            state.使用的手 = player.getUsedItemHand();
            state.使用剩余时间 = player.getUseItemRemainingTicks();
            LOGGER.info("采集初始状态：正在使用物品（手={}, 剩余={}tick）",
                    state.使用的手, state.使用剩余时间);
        }

        com.v2t.puellamagi.mixin.access.ServerPlayerGameModeAccessor gameMode =
                (com.v2t.puellamagi.mixin.access.ServerPlayerGameModeAccessor) player.gameMode;
        if (gameMode.puellamagi$isDestroyingBlock()) {
            state.正在破坏方块 = true;
            LOGGER.info("采集初始状态：正在破坏方块");
        }

        return state;
    }

    // ==================== 查询====================

    public boolean 有进行中的操作() {
        return 正在使用物品 || 正在破坏方块;
    }

    public boolean 是否正在使用物品() { return 正在使用物品; }
    public InteractionHand 获取使用的手() { return 使用的手; }
    public int 获取使用剩余时间() { return 使用剩余时间; }public boolean 是否正在破坏方块() { return 正在破坏方块; }

    // ==================== 服务端恢复 ====================

    public void 恢复到服务端(ServerPlayer player) {
        if (正在使用物品 && 使用的手 != null) {
            player.startUsingItem(使用的手);
            if (player instanceof com.v2t.puellamagi.api.access.ILivingEntityAccess access) {
                access.puellamagi$setUseItemRemaining(使用剩余时间);
            }
            LOGGER.info("服务端恢复使用物品：手={}, 剩余={}tick", 使用的手, 使用剩余时间);
        }
    }
}
