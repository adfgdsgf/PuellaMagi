package com.v2t.puellamagi.util.recording;

import com.v2t.puellamagi.core.network.packets.s2c.方块批量更新包;
import com.v2t.puellamagi.core.network.packets.s2c.背包同步包;
import com.v2t.puellamagi.system.ability.epitaph.方块变化帧;
import com.v2t.puellamagi.util.实体工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 回放校验器
 *
 * 回放结束时的"对账"工具
 *
 * 纯输入回放让MC自己处理一切（动画/音效/背包消耗全正确）
 * 但浮点位置微偏可能导致最终结果有差异：
 * - 方块：该破坏的没破坏、该放的放到了旁边
 * - 背包：方块偏差导致物品消耗不一致
 * - 玩家状态：血量/饥饿值因为行为偏差而不同
 *
 * 此工具以录制时的记录为基准，强制修正偏差
 * 不是补丁——是保障机制，和银行转账对账一个道理
 *
 * 复用场景：
 * - 预知回放结束
 * - 时间倒流结束
 * - 任何"世界状态应该和某个记录一致"的场景
 */
public final class 回放校验器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/ReplayVerifier");

    private 回放校验器() {}

    /**
     * 校验结果汇总
     */
    public record 校验结果(int 方块修正数, int 玩家修正数, int 总检查数) {
        public boolean 有修正() { return 方块修正数 > 0|| 玩家修正数 > 0; }
    }

    // ==================== 完整校验 ====================

    /**
     * 执行完整校验（方块 + 玩家）
     *
     * @param level     目标维度
     * @param 变化列表   录制时的方块变化记录
     * @param玩家快照表录制结束时的玩家状态快照（录制期间最后一帧的状态）
     * @return 校验结果汇总
     */
    public static 校验结果 完整校验(ServerLevel level,List<方块变化帧> 变化列表,
                                    Map<UUID, 玩家快照> 玩家快照表) {
        int 方块修正 = 校验方块(level, 变化列表);
        int 玩家修正 = 校验玩家(level, 玩家快照表);

        int 总检查 = (变化列表 != null ? 变化列表.size() : 0)+ (玩家快照表 != null ? 玩家快照表.size() : 0);

        校验结果 result = new 校验结果(方块修正, 玩家修正, 总检查);

        if (result.有修正()) {
            LOGGER.info("回放校验完成：方块修正 {}，玩家修正 {}",
                    方块修正, 玩家修正);
        } else {
            LOGGER.debug("回放校验完成：无需修正");
        }

        return result;
    }

    // ==================== 方块校验 ====================

    /**
     * 校验方块状态
     *
     * 以录制时的方块变化记录为准
     * 同一位置多次变化取最后一次的新状态
     * 不一致的强制修正 + 同步客户端
     *
     * @return 修正数量
     */
    public static int 校验方块(ServerLevel level, List<方块变化帧> 变化列表) {
        if (变化列表 == null || 变化列表.isEmpty()) return 0;

        // 计算每个位置的最终期望状态
        Map<BlockPos, BlockState> 期望状态 = new HashMap<>();
        for (方块变化帧 change : 变化列表) {
            期望状态.put(change.获取位置(), change.获取新状态());
        }

        // 对比并修正
        List<BlockPos> 修正位置 = new ArrayList<>();
        List<BlockState> 修正状态 = new ArrayList<>();

        for (Map.Entry<BlockPos, BlockState> entry : 期望状态.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState expected = entry.getValue();
            BlockState actual = level.getBlockState(pos);

            if (!actual.equals(expected)) {
                level.setBlockAndUpdate(pos, expected);
                修正位置.add(pos);
                修正状态.add(expected);
            }
        }

        // 同步客户端
        if (!修正位置.isEmpty()) {
            方块批量更新包 packet = 方块批量更新包.从配对构建(修正位置, 修正状态);
            for (ServerPlayer player : level.players()) {
                网络工具.发送给玩家(player, packet);
            }
            LOGGER.info("方块校验：检查 {} 个位置，修正 {} 个",
                    期望状态.size(), 修正位置.size());
        }

        return 修正位置.size();
    }

    /**
     * 校验方块快照（世界回滚是否完整）
     *
     * 和变化帧校验不同——这个是"世界应该回到快照的样子"
     *
     * @return 修正数量
     */
    public static int 校验快照(ServerLevel level, List<方块快照> 快照列表) {
        if (快照列表 == null || 快照列表.isEmpty()) return 0;

        List<BlockPos> 修正位置 = new ArrayList<>();
        List<BlockState> 修正状态 = new ArrayList<>();

        for (方块快照 snapshot : 快照列表) {
            BlockPos pos = snapshot.获取位置();
            BlockState expected = snapshot.获取方块状态();
            BlockState actual = level.getBlockState(pos);

            if (!actual.equals(expected)) {
                snapshot.恢复到(level);
                修正位置.add(pos);
                修正状态.add(expected);
            }
        }

        if (!修正位置.isEmpty()) {
            方块批量更新包 packet = 方块批量更新包.从配对构建(修正位置, 修正状态);
            for (ServerPlayer player : level.players()) {
                网络工具.发送给玩家(player, packet);
            }
            LOGGER.info("快照校验：检查 {} 个位置，修正 {} 个",
                    快照列表.size(), 修正位置.size());
        }

        return 修正位置.size();
    }

    // ==================== 玩家校验 ====================

    /**
     * 校验玩家状态（背包/血量/经验等）
     *
     * 回放结束时玩家的状态应该和"录制结束时"一致
     * 但因为方块偏差，背包消耗可能不同
     *
     * 以录制结束时拍的快照为准，强制恢复
     *
     * @param玩家快照表  key=玩家UUID，value=录制结束时的快照
     * @return 修正数量
     */
    public static int 校验玩家(ServerLevel level, Map<UUID, 玩家快照> 玩家快照表) {
        if (玩家快照表 == null || 玩家快照表.isEmpty()) return 0;

        int 修正数量 = 0;

        for (Map.Entry<UUID, 玩家快照> entry : 玩家快照表.entrySet()) {
            UUID playerUUID = entry.getKey();
            玩家快照 snapshot = entry.getValue();

            Entity entity = 实体工具.按UUID查找实体(level, playerUUID);
            if (!(entity instanceof ServerPlayer sp)) continue;

            // 直接恢复快照（背包/血量/经验/药水效果）
            snapshot.恢复到(sp);

            // 强制同步背包到客户端
            网络工具.发送给玩家(sp, 背包同步包.从玩家构建(sp));

            修正数量++;
            LOGGER.debug("玩家校验：{} 状态已恢复", sp.getName().getString());
        }

        return 修正数量;
    }

}
