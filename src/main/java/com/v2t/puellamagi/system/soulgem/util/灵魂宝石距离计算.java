package com.v2t.puellamagi.system.soulgem.util;

import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import com.v2t.puellamagi.system.soulgem.effect.持有状态;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 灵魂宝石距离计算工具
 *
 * 职责：统一计算玩家与灵魂宝石的距离
 *
 * 设计原则：
 * - 单一职责：只负责距离计算
 * - 统一入口：所有需要距离判断的地方都调用这里
 * - 避免重复：距离效果处理器和假死状态处理器共用此逻辑
 */
public final class 灵魂宝石距离计算 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SoulGemDistance");

    private 灵魂宝石距离计算() {}

    // ==================== 计算结果 ====================

    /**
     * 距离计算结果
     *
     * 使用record实现不可变数据对象
     */
    public record 计算结果(
            double 距离,
            boolean 持有者在线,
            失败原因 原因
    ) {
        /**
         * 是否计算成功（距离有效）
         */
        public boolean 有效() {
            return 距离 >= 0;
        }

        /**
         * 获取对应的持有状态（用于debuff）
         *
         * 计算失败时返回正常状态，不应用debuff
         */
        public 持有状态 获取持有状态() {
            if (!有效()) {
                return 持有状态.正常;
            }
            return 持有状态.fromDistance(距离);
        }

        /**
         * 是否应该进入假死
         */
        public boolean 应该假死() {
            // 情况1：距离有效且超出范围
            if (有效() && 距离 >= 持有状态.超出范围.获取最小距离()) {
                return true;
            }

            // 情况2：跨维度
            if (原因 == 失败原因.跨维度) {
                return true;
            }

            // 情况3：持有者离线
            if (原因 == 失败原因.持有者离线) {
                return true;
            }

            // 其他情况（无登记、位置未知等）不触发假死
            // 这些情况由位置未知超时机制处理（5分钟后重生成）
            return false;
        }

        /**
         * 是否可以退出假死
         */
        public boolean 可以退出假死() {
            return 有效() && 距离 < 持有状态.超出范围.获取最小距离();
        }
    }

    /**
     * 失败原因枚举
     */
    public enum 失败原因 {
        无("无"),
        无登记信息("无登记信息"),
        位置未知("位置未知"),
        持有者UUID为空("持有者UUID为空"),
        持有者离线("持有者离线"),跨维度("跨维度");

        private final String 描述;

        失败原因(String 描述) {
            this.描述 = 描述;
        }

        public String 获取描述() {
            return 描述;
        }
    }

    // ==================== 工厂方法 ====================

    private static 计算结果 成功(double 距离) {
        return new 计算结果(距离, true,失败原因.无);
    }

    private static 计算结果 失败(失败原因 原因) {
        return new 计算结果(-1, false, 原因);
    }

    private static 计算结果 持有者离线() {
        return new 计算结果(-1, false, 失败原因.持有者离线);
    }

    // ==================== 核心计算方法 ====================

    /**
     * 计算玩家与灵魂宝石的距离
     *
     * 这是唯一的计算入口，所有距离判断都应该调用此方法
     *
     * @param owner 宝石所有者
     * @param info 宝石登记信息（可为null）
     * @param server 服务器实例
     * @return 计算结果
     */
    public static 计算结果 计算(ServerPlayer owner, @Nullable 宝石登记信息 info, MinecraftServer server) {
        // 1. 无登记信息
        if (info == null) {
            LOGGER.trace("玩家 {} 无宝石登记信息", owner.getName().getString());
            return 失败(失败原因.无登记信息);
        }

        // 2. 位置未知
        if (info.获取维度() == null || info.获取坐标() == null) {
            LOGGER.trace("玩家 {}宝石位置未知", owner.getName().getString());
            return 失败(失败原因.位置未知);
        }

        // 3. 持有者在线检查（仅当宝石在玩家背包中时）
        if (info.获取存储类型枚举() == 存储类型.玩家背包) {
            UUID 持有者UUID = info.获取当前持有者UUID();

            if (持有者UUID == null) {
                LOGGER.warn("玩家 {} 宝石在玩家背包但持有者UUID为null", owner.getName().getString());
                return 失败(失败原因.持有者UUID为空);
            }

            // 不是自己持有，检查持有者是否在线
            if (!持有者UUID.equals(owner.getUUID())) {
                ServerPlayer 持有者 = server.getPlayerList().getPlayer(持有者UUID);
                if (持有者 == null) {
                    LOGGER.debug("玩家 {} 宝石持有者 {} 已离线",
                            owner.getName().getString(),
                            持有者UUID.toString().substring(0, 8));
                    return 持有者离线();
                }
            }
        }

        // 4. 跨维度检查
        if (!owner.level().dimension().equals(info.获取维度())) {
            LOGGER.trace("玩家 {} 与宝石跨维度", owner.getName().getString());
            return 失败(失败原因.跨维度);
        }

        // 5. 计算欧几里得距离
        double distance = owner.position().distanceTo(info.获取坐标());
        LOGGER.trace("玩家 {} 与宝石距离: {}", owner.getName().getString(), distance);
        return 成功(distance);
    }

    // ==================== 便捷方法 ====================

    /**
     * 快速判断是否应该进入假死
     */
    public static boolean 应该假死(ServerPlayer owner, @Nullable 宝石登记信息 info, MinecraftServer server) {
        return 计算(owner, info, server).应该假死();
    }

    /**
     * 快速判断是否可以退出假死
     */
    public static boolean 可以退出假死(ServerPlayer owner, @Nullable 宝石登记信息 info, MinecraftServer server) {
        return 计算(owner, info, server).可以退出假死();
    }

    /**
     * 快速获取持有状态
     */
    public static 持有状态 获取持有状态(ServerPlayer owner, @Nullable 宝石登记信息 info, MinecraftServer server) {
        return 计算(owner, info, server).获取持有状态();
    }
}
