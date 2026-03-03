package com.v2t.puellamagi.client;

import com.v2t.puellamagi.core.network.packets.s2c.队友位置同步包;
import com.v2t.puellamagi.system.team.队伍数据;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 客户端队伍数据缓存
 *
 * 存储从服务端同步过来的队伍信息、邀请通知、队友位置
 *纯客户端运行时数据，不持久化
 *
 * 数据来源：
 * - 队伍数据同步包 → 更新当前队伍
 * - 队伍邀请通知包 → 添加待处理邀请
 * - 队友位置同步包 → 更新队友位置
 * - 断开连接时 → 清除全部
 */
public final class 客户端队伍缓存 {

    /** 当前玩家所在队伍（从服务端同步），null表示无队伍 */
    @Nullable
    private static 队伍数据 当前队伍 = null;

    /** 待处理的邀请列表 */
    private static final List<邀请信息> 待处理邀请 = new ArrayList<>();

    /** 队友位置缓存：UUID → 位置信息 */
    private static final Map<UUID, 队友位置> 队友位置表 = new HashMap<>();

    /** 需要高亮的队友（技能系统预留） */
    private static final Set<UUID> 高亮队友 = new HashSet<>();

    private 客户端队伍缓存() {}

    // ==================== 队伍数据 ====================

    /**
     * 设置当前队伍（收到同步包时调用）
     */
    public static void 设置队伍(@Nullable 队伍数据 team) {
        当前队伍 = team;
    }

    /**
     * 获取当前队伍
     */
    @Nullable
    public static 队伍数据 获取队伍() {
        return 当前队伍;
    }

    /**
     * 是否有队伍
     */
    public static boolean 有队伍() {
        return 当前队伍 != null;
    }

    /**
     * 清除队伍数据
     */
    public static void 清除队伍() {
        当前队伍 = null;
    }

    // ==================== 邀请管理 ====================

    /**
     * 添加邀请（收到邀请通知包时调用）
     * 同一邀请者的旧邀请会被替换
     */
    public static void 添加邀请(邀请信息 invite) {
        待处理邀请.removeIf(i -> i.邀请者UUID().equals(invite.邀请者UUID()));
        待处理邀请.add(invite);
    }

    /**
     * 获取所有待处理邀请（不可修改视图）
     */
    public static List<邀请信息> 获取待处理邀请() {
        return Collections.unmodifiableList(待处理邀请);
    }

    /**
     * 获取待处理邀请数量
     */
    public static int 获取邀请数量() {
        return 待处理邀请.size();
    }

    /**
     * 移除指定邀请者的邀请（接受/拒绝后调用）
     */
    public static void 移除邀请(UUID inviterUUID) {
        待处理邀请.removeIf(i -> i.邀请者UUID().equals(inviterUUID));
    }

    /**
     * 清除所有邀请
     */
    public static void 清除所有邀请() {
        待处理邀请.clear();
    }

    // ==================== 队友位置 ====================

    /**
     * 更新队友位置（收到位置同步包时调用）
     * 全量替换：包中没有的队友视为不可见
     */
    public static void 更新队友位置(List<队友位置同步包.条目> positions) {
        队友位置表.clear();
        for (队友位置同步包.条目 entry : positions) {
            队友位置表.put(entry.uuid(), new 队友位置(
                    entry.x(), entry.y(), entry.z(),
                    entry.dimension(),
                    System.currentTimeMillis()
            ));
        }
    }

    /**
     * 获取所有队友位置
     */
    public static Map<UUID, 队友位置> 获取所有队友位置() {
        return Collections.unmodifiableMap(队友位置表);
    }

    /**
     * 获取指定队友位置
     */
    @Nullable
    public static 队友位置 获取队友位置(UUID uuid) {
        return 队友位置表.get(uuid);
    }

    // ==================== 高亮系统（技能预留） ====================

    /**
     * 设置队友高亮（技能系统调用）
     */
    public static void 添加高亮(UUID uuid) {
        高亮队友.add(uuid);
    }

    /**
     * 移除队友高亮
     */
    public static void 移除高亮(UUID uuid) {
        高亮队友.remove(uuid);
    }

    /**
     * 清除所有高亮
     */
    public static void 清除所有高亮() {
        高亮队友.clear();
    }

    /**
     * 是否高亮
     */
    public static boolean 是否高亮(UUID uuid) {
        return 高亮队友.contains(uuid);
    }

    // ==================== 生命周期 ====================

    /**
     * 断开连接时清除所有缓存
     */
    public static void 清除全部() {
        当前队伍 = null;
        待处理邀请.clear();
        队友位置表.clear();
        高亮队友.clear();
    }

    // ==================== 数据结构 ====================

    /**
     * 客户端邀请信息
     */
    public record 邀请信息(UUID 邀请者UUID,
                           String 邀请者名称,
                           long 接收时间
    ) {}

    /**
     * 队友位置信息（客户端缓存）
     *
     * @param x 世界坐标X
     * @param y         世界坐标Y（眼睛高度）
     * @param z         世界坐标Z
     * @param dimension 维度ID
     * @param 更新时间  客户端收到的时间戳
     */
    public record 队友位置(double x, double y, double z,ResourceLocation dimension,
                           long 更新时间
    ) {}
}
