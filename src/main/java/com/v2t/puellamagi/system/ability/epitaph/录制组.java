package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.util.recording.世界快照;
import com.v2t.puellamagi.util.recording.玩家快照;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 录制组
 *
 * 代表一次完整的"多人录制-合并-回放-结算"流程的载体。
 *
 * 核心理念：录制/回放/时删不属于某个玩家，而是一个全局行为。
 * 多个使用者的录制自动合并为一个录制组，支持并行录制和首尾续接。
 *
 * 生命周期：
 * 1. 首个录制者开始录制 → 创建录制组
 * 2. 后续录制者加入 → 加入同一录制组
 * 3. 所有人结束录制（或续接窗口过期） → 录制组关闭 → 合并数据 → 统一回放
 * 4. 回放结束 → 录制组销毁
 *
 * 使用场景：
 * - 单人预知 = 录制组中只有一个录制段（退化为原有逻辑的等价封装）
 * - 多人预知 = 录制组中多个录制段（帧数据/方块变化/快照按时间线合并）
 */
public class 录制组 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/RecordingGroup");

    // ==================== 标识 ====================

    /** 录制组唯一标识 */
    private final UUID 组ID;

    // ==================== 时间基准 ====================

    /** 全局起始时间：最早录制者开始录制时的gameTime */
    private long 全局起始时间;

    /** 全局结束时间：最晚录制者结束录制时的gameTime（关闭后才确定） */
    private long 全局结束时间 = -1;

    // ==================== 录制段 ====================

    /** 所有录制段：录制者UUID → 录制段 */
    private final Map<UUID, 录制段> 录制段表 = new LinkedHashMap<>();

    /** 当前仍在录制的人数 */
    private int 活跃录制数 = 0;

    // ==================== 参与者追踪 ====================

    /** 所有使用预知技能的录制者 */
    private final Set<UUID> 录制者集合 = new LinkedHashSet<>();

    /** 所有被录制到的实体UUID（范围内的所有实体） */
    private final Set<UUID> 被录制实体集合 = new HashSet<>();

    // ==================== 状态 ====================

    /** 是否已关闭（所有人录完且续接窗口过期） */
    private boolean 已关闭 = false;

    /** 维度引用（同一录制组的所有录制者必须在同一维度） */
    private ServerLevel 维度;

    /** 续接窗口剩余tick（最后一个录制者结束后的grace period） */
    private int 续接窗口剩余 = 0;

    /** 续接窗口长度（tick） */
    private static final int 续接窗口时长 = 2;

    // ==================== 合并后数据 ====================

    /** 合并后的录制数据（关闭后由录制组管理器生成） */
    @Nullable
    private 合并录制数据 合并数据 = null;

    // ==================== 构造 ====================

    /**
     * 创建录制组
     *
     * @param firstUserUUID 首个录制者UUID
     * @param gameTime      创建时的游戏时间
     * @param level         维度
     */
    public 录制组(UUID firstUserUUID, long gameTime, ServerLevel level) {
        this.组ID = UUID.randomUUID();
        this.全局起始时间 = gameTime;
        this.维度 = level;

        LOGGER.info("创建录制组 {}（首个录制者: {}）", 组ID, firstUserUUID);
    }

    // ==================== 录制段管理 ====================

    /**
     * 添加录制段（录制者加入录制组）
     *
     * @param userUUID  录制者UUID
     * @param session   该录制者的录制会话
     * @param gameTime  加入时的游戏时间
     */
    public void 添加录制段(UUID userUUID, 录制管理器.录制会话 session, long gameTime) {
        int 帧偏移 = (int) (gameTime - 全局起始时间);
        录制段 segment = new 录制段(userUUID, session, gameTime, 帧偏移);

        录制段表.put(userUUID, segment);
        录制者集合.add(userUUID);
        被录制实体集合.addAll(session.被录制实体);
        活跃录制数++;

        // 如果在续接窗口期间有新录制者加入，重置续接窗口
        续接窗口剩余 = 0;

        LOGGER.info("录制者 {} 加入录制组 {}（帧偏移={}, 活跃数={}）",
                userUUID, 组ID, 帧偏移, 活跃录制数);
    }

    /**
     * 标记录制段结束
     *
     * @param userUUID 录制者UUID
     * @param gameTime 结束时的游戏时间
     * @return true = 录制组应该启动续接窗口或立即关闭
     */
    public boolean 标记录制段结束(UUID userUUID, long gameTime) {
        录制段 segment = 录制段表.get(userUUID);
        if (segment == null || segment.已结束) return false;

        segment.结束时间 = gameTime;
        segment.已结束 = true;
        活跃录制数--;

        // 更新全局结束时间
        if (全局结束时间 < 0 || gameTime > 全局结束时间) {
            全局结束时间 = gameTime;
        }

        LOGGER.info("录制者 {} 结束录制（活跃数={}）", userUUID, 活跃录制数);

        if (活跃录制数 <= 0) {
            // 所有人都录完了，启动续接窗口
            续接窗口剩余 = 续接窗口时长;
            return true;
        }

        return false;
    }

    /**
     * 移除录制段（取消录制）
     *
     * @param userUUID 录制者UUID
     */
    public void 移除录制段(UUID userUUID) {
        录制段 removed = 录制段表.remove(userUUID);
        if (removed != null) {
            录制者集合.remove(userUUID);
            if (!removed.已结束) {
                活跃录制数--;
            }
            LOGGER.info("录制者 {} 取消录制，从录制组 {} 移除", userUUID, 组ID);
        }
    }

    /**
     * tick续接窗口
     * 每个服务端tick调用一次
     *
     * @return true = 续接窗口过期，录制组应该关闭
     */
    public boolean tick续接窗口() {
        if (续接窗口剩余 <= 0) return false;
        if (活跃录制数 > 0) {
            // 有新录制者加入，重置窗口
            续接窗口剩余 = 0;
            return false;
        }

        续接窗口剩余--;
        if (续接窗口剩余 <= 0) {
            LOGGER.info("录制组 {} 续接窗口过期，准备关闭", 组ID);
            return true;
        }
        return false;
    }

    // ==================== 关闭 ====================

    /**
     * 关闭录制组
     * 关闭后不再接受新的录制段
     */
    public void 关闭() {
        已关闭 = true;
        LOGGER.info("录制组 {} 已关闭（共 {} 个录制段）", 组ID, 录制段表.size());
    }

    // ==================== 查询 ====================

    public UUID 获取组ID() { return 组ID; }

    public long 获取全局起始时间() { return 全局起始时间; }

    public long 获取全局结束时间() { return 全局结束时间; }

    public boolean 是否已关闭() { return 已关闭; }

    public boolean 是否为空() { return 录制段表.isEmpty(); }

    public int 获取活跃录制数() { return 活跃录制数; }

    public ServerLevel 获取维度() { return 维度; }

    public Set<UUID> 获取录制者集合() {
        return Collections.unmodifiableSet(录制者集合);
    }

    public Set<UUID> 获取被录制实体集合() {
        return Collections.unmodifiableSet(被录制实体集合);
    }

    public Collection<录制段> 获取所有录制段() {
        return Collections.unmodifiableCollection(录制段表.values());
    }

    @Nullable
    public 录制段 获取录制段(UUID userUUID) {
        return 录制段表.get(userUUID);
    }

    @Nullable
    public 合并录制数据 获取合并数据() { return 合并数据; }

    public void 设置合并数据(合并录制数据 data) { this.合并数据 = data; }

    /**
     * 计算统一时间线的总帧数
     * 基于录制段的实际帧数据（帧偏移 + 帧数）取最大值
     * 不用gameTime差值，避免和实际帧数据不一致
     */
    public int 计算总帧数() {
        int maxFrame = 0;
        for (录制段 seg : 录制段表.values()) {
            int endFrame = seg.获取全局结束帧();
            if (endFrame > maxFrame) {
                maxFrame = endFrame;
            }
        }
        return maxFrame;
    }

    /**
     * 是否正在续接窗口中
     */
    public boolean 续接窗口中() {
        return 续接窗口剩余 > 0 && 活跃录制数 <= 0;
    }

    // ==================== 录制段内部类 ====================

    /**
     * 单个录制者的录制段
     *
     * 记录该录制者的独立录制会话在录制组统一时间线中的位置。
     * 帧偏移 = 该录制者的开始时间 - 录制组的全局起始时间
     * 合并时：全局帧索引 = 本地帧索引 + 帧偏移
     */
    public static class 录制段 {

        /** 录制者UUID */
        public final UUID 使用者UUID;

        /** 该录制者的独立录制会话 */
        public final 录制管理器.录制会话 会话;

        /** 开始时间（gameTime） */
        public final long 开始时间;

        /** 结束时间（gameTime，结束后才有值） */
        public long 结束时间 = -1;

        /**
         * 帧偏移：该录制段在统一时间线中的起始帧位置
         * = (开始时间 - 录制组全局起始时间)
         */
        public final int 帧偏移;

        /** 是否已结束录制 */
        public boolean 已结束 = false;

        public 录制段(UUID userUUID, 录制管理器.录制会话 session, long startTime, int frameOffset) {
            this.使用者UUID = userUUID;
            this.会话 = session;
            this.开始时间 = startTime;
            this.帧偏移 = frameOffset;
        }

        /**
         * 获取该录制段的帧数
         */
        public int 获取帧数() {
            return 会话.帧数据.获取总帧数();
        }

        /**
         * 获取该录制段在统一时间线中的结束帧位置（不含）
         */
        public int 获取全局结束帧() {
            return 帧偏移 + 获取帧数();
        }
    }
}
