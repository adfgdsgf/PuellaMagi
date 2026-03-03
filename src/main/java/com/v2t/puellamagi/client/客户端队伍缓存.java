// 文件路径: src/main/java/com/v2t/puellamagi/client/客户端队伍缓存.java

package com.v2t.puellamagi.client;

import com.v2t.puellamagi.system.team.队伍数据;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 客户端队伍数据缓存
 *
 * 存储从服务端同步过来的队伍信息和邀请通知
 *纯客户端运行时数据，不持久化
 *
 * 数据来源：
 * - 队伍数据同步包 → 更新当前队伍
 * - 队伍邀请通知包 → 添加待处理邀请
 * - 断开连接时 → 清除全部
 */
public final class 客户端队伍缓存 {

    /** 当前玩家所在队伍（从服务端同步），null表示无队伍 */
    @Nullable
    private static 队伍数据 当前队伍 = null;

    /** 待处理的邀请列表 */
    private static final List<邀请信息> 待处理邀请 = new ArrayList<>();

    private 客户端队伍缓存() {}

    //==================== 队伍数据 ====================

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
     *同一邀请者的旧邀请会被替换
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

    // ==================== 生命周期 ====================

    /**
     * 断开连接时清除所有缓存
     * 应在ClientPlayerNetworkEvent.LoggingOut 中调用
     */
    public static void 清除全部() {
        当前队伍 = null;
        待处理邀请.clear();
    }

    // ==================== 数据结构 ====================

    /**
     * 客户端邀请信息
     *
     * @param邀请者UUID 邀请者的UUID
     * @param 邀请者名称 邀请者的显示名称（避免客户端反查）
     * @param 接收时间   客户端收到的时间戳（System.currentTimeMillis，用于UI超时显示）
     */
    public record 邀请信息(UUID 邀请者UUID,
                           String 邀请者名称,
                           long 接收时间
    ) {}
}
