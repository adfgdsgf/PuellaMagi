package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.util.recording.世界快照;
import com.v2t.puellamagi.util.recording.实体帧数据;
import com.v2t.puellamagi.util.recording.玩家快照;
import com.v2t.puellamagi.util.recording.方块快照;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 录制组管理器
 *
 * 全局单例，管理所有活跃录制组的生命周期。
 *
 * 职责：
 * - 录制者开始录制时：查找可加入的录制组（同维度且未关闭），没有则创建新组
 * - 录制者结束录制时：标记录制段结束，检查是否所有人都录完
 * - 录制组关闭时：执行合并算法，生成合并录制数据
 * - 续接窗口管理：最后一人录完后等待2tick，期间有新录制者加入则续接
 *
 * 使用场景：
 * - 被录制管理器在开始/停止/取消录制时调用
 * - 被预知录制事件在服务端tick时调用（续接窗口tick）
 */
public final class 录制组管理器 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/RecordingGroupManager");

    // ==================== 活跃录制组 ====================

    /** 所有活跃录制组：组ID → 录制组 */
    private static final Map<UUID, 录制组> 活跃组表 = new ConcurrentHashMap<>();

    /** 录制者到录制组的快速映射：录制者UUID → 组ID */
    private static final Map<UUID, UUID> 使用者组映射 = new ConcurrentHashMap<>();

    private 录制组管理器() {}

    // ==================== 核心操作 ====================

    /**
     * 玩家开始录制时调用：加入已有录制组或创建新组
     *
     * 匹配规则：
     * - 同一维度
     * - 未关闭
     * - 正在录制中或在续接窗口内
     *
     * @param userUUID 录制者UUID
     * @param session  录制会话
     * @param gameTime 开始时间
     * @param level    维度
     * @return 录制组ID
     */
    public static UUID 加入或创建录制组(UUID userUUID, 录制管理器.录制会话 session,
                                           long gameTime, ServerLevel level) {
        // 检查是否已在某个录制组中
        if (使用者组映射.containsKey(userUUID)) {
            LOGGER.warn("录制者 {} 已在录制组中，跳过", userUUID);
            return 使用者组映射.get(userUUID);
        }

        // 查找可加入的录制组
        录制组 target = 查找可加入录制组(level);

        if (target != null) {
            // 加入已有录制组
            target.添加录制段(userUUID, session, gameTime);
            使用者组映射.put(userUUID, target.获取组ID());

            LOGGER.info("录制者 {} 加入已有录制组 {}", userUUID, target.获取组ID());
            return target.获取组ID();
        }

        // 创建新录制组
        录制组 newGroup = new 录制组(userUUID, gameTime, level);
        newGroup.添加录制段(userUUID, session, gameTime);

        活跃组表.put(newGroup.获取组ID(), newGroup);
        使用者组映射.put(userUUID, newGroup.获取组ID());

        LOGGER.info("为录制者 {} 创建新录制组 {}", userUUID, newGroup.获取组ID());
        return newGroup.获取组ID();
    }

    /**
     * 玩家结束录制时调用
     *
     * @param userUUID 录制者UUID
     * @param gameTime 结束时间
     * @return 如果录制组已关闭且合并完成，返回合并后的数据；否则返回null（表示需要等待）
     */
    @Nullable
    public static 合并录制数据 标记录制段结束(UUID userUUID, long gameTime) {
        UUID 组ID = 使用者组映射.get(userUUID);
        if (组ID == null) {
            LOGGER.warn("录制者 {} 不在任何录制组中", userUUID);
            return null;
        }

        录制组 group = 活跃组表.get(组ID);
        if (group == null) return null;

        boolean 应检查关闭 = group.标记录制段结束(userUUID, gameTime);

        // 如果所有人都录完了，检查是否立即关闭（无续接窗口）或等待
        if (应检查关闭 && group.获取活跃录制数() <= 0) {
            // 续接窗口由tick方法管理，这里不立即关闭
            // 如果续接窗口时长为0，则立即关闭
            if (group.续接窗口中()) {
                LOGGER.debug("录制组 {} 进入续接窗口", 组ID);
                return null;
            }
        }

        return null;
    }

    /**
     * 玩家取消录制时调用
     *
     * @param userUUID 录制者UUID
     */
    public static void 取消录制段(UUID userUUID) {
        UUID 组ID = 使用者组映射.remove(userUUID);
        if (组ID == null) return;

        录制组 group = 活跃组表.get(组ID);
        if (group == null) return;

        group.移除录制段(userUUID);

        // 如果录制组为空，移除整个录制组
        if (group.是否为空()) {
            活跃组表.remove(组ID);
            LOGGER.info("录制组 {} 已清空，移除", 组ID);
        }
    }

    // ==================== Tick驱动 ====================

    /**
     * 服务端每tick调用
     * 管理所有录制组的续接窗口
     *
     * @return 本tick关闭的录制组列表（已完成合并，可以开始回放）
     */
    public static List<录制组> tick() {
        List<录制组> 已关闭列表 = new ArrayList<>();

        for (录制组 group : 活跃组表.values()) {
            if (group.是否已关闭()) continue;

            if (group.tick续接窗口()) {
                // 续接窗口过期，关闭录制组并执行合并
                group.关闭();
                合并录制数据 merged = 合并录制组(group);
                group.设置合并数据(merged);
                已关闭列表.add(group);
            }
        }

        return 已关闭列表;
    }

    // ==================== 合并算法 ====================

    /**
     * 将录制组中所有录制段的数据合并为统一的合并录制数据
     *
     * 合并策略：
     * - 帧数据：按帧偏移将每个录制段的帧数据放到统一时间线上，同一实体重叠时后加入的覆盖
     * - 方块变化：调整tick序号(+偏移)后按时间排序
     * - 世界快照：最早录制段的快照为基础，后续段补充新区域
     * - 玩家快照/输入帧/初始状态：按录制者UUID直接汇总
     *
     * @param group 要合并的录制组
     * @return 合并后的录制数据
     */
    public static 合并录制数据 合并录制组(录制组 group) {
        int totalFrames = group.计算总帧数();
        Collection<录制组.录制段> segments = group.获取所有录制段();

        LOGGER.info("开始合并录制组 {}（{} 个录制段，{} 帧）",
                group.获取组ID(), segments.size(), totalFrames);

        // === 1. 合并帧数据 ===
        List<Map<UUID, 实体帧数据>> 合并帧列表 = new ArrayList<>(totalFrames);
        for (int i = 0; i < totalFrames; i++) {
            合并帧列表.add(new HashMap<>());
        }

        for (录制组.录制段 seg : segments) {
            int segFrames = seg.获取帧数();
            for (int localFrame = 0; localFrame < segFrames; localFrame++) {
                int globalFrame = localFrame + seg.帧偏移;
                if (globalFrame < 0 || globalFrame >= totalFrames) continue;

                Map<UUID, 实体帧数据> segFrame = seg.会话.帧数据.获取帧(localFrame);
                if (segFrame != null) {
                    // 合并：后加入的录制段覆盖已有数据（同一实体在同一帧不太可能由多段录制）
                    合并帧列表.get(globalFrame).putAll(segFrame);
                }
            }
        }

        // === 2. 合并方块变化 ===
        List<方块变化帧> 合并方块变化 = new ArrayList<>();
        for (录制组.录制段 seg : segments) {
            for (方块变化帧 change : seg.会话.方块变化列表) {
                // 调整tick序号：本地序号 + 帧偏移 = 全局序号
                int globalTick = change.获取tick序号() + seg.帧偏移;
                合并方块变化.add(new 方块变化帧(
                        change.获取位置(), change.获取旧状态(), change.获取新状态(),
                        globalTick, change.获取触发者UUID()
                ));
            }
        }
        // 按全局tick序号排序
        合并方块变化.sort(Comparator.comparingInt(方块变化帧::获取tick序号));

        // === 3. 合并方块实体变化 ===
        List<方块实体变化帧> 合并方块实体变化 = new ArrayList<>();
        for (录制组.录制段 seg : segments) {
            for (方块实体变化帧 change : seg.会话.方块实体变化列表) {
                int globalTick = change.获取tick序号() + seg.帧偏移;
                合并方块实体变化.add(new 方块实体变化帧(
                        change.获取位置(), change.获取旧NBT(), change.获取新NBT(),
                        globalTick, change.获取触发者UUID()
                ));
            }
        }
        合并方块实体变化.sort(Comparator.comparingInt(方块实体变化帧::获取tick序号));

        // === 4. 合并世界快照 ===
        // 以最早录制段（偏移=0）的快照为基础，后续段补充新区域
        世界快照 合并快照 = null;
        for (录制组.录制段 seg : segments) {
            if (合并快照 == null) {
                // 第一个段的快照直接作为基础
                合并快照 = seg.会话.起点快照;
            } else {
                // 后续段：补充基础快照中没有的方块
                for (方块快照 block : seg.会话.起点快照.获取所有方块快照()) {
                    if (!合并快照.包含方块(block.获取位置())) {
                        合并快照.添加方块(block);
                    }
                }
                // 补充基础快照中没有的实体
                for (com.v2t.puellamagi.util.recording.实体快照 entitySnap :
                        seg.会话.起点快照.获取所有实体快照()) {
                    if (合并快照.获取实体快照(entitySnap.获取UUID()) == null) {
                        合并快照.添加实体(entitySnap);
                    }
                }
            }
        }

        // 兜底：如果没有任何录制段（不应该发生）
        if (合并快照 == null) {
            合并快照 = new 世界快照(group.获取全局起始时间());
        }

        // === 5. 汇总玩家快照 ===
        Map<UUID, 玩家快照> 玩家快照表 = new HashMap<>();
        Map<UUID, 玩家快照> 结束快照表 = new HashMap<>();
        for (录制组.录制段 seg : segments) {
            玩家快照表.putAll(seg.会话.玩家快照表);
            if (seg.会话.结束快照表 != null) {
                结束快照表.putAll(seg.会话.结束快照表);
            }
        }

        // === 6. 合并输入帧（按帧偏移对齐到全局时间线） ===
        // 多人场景：B既是A的被录制实体，又是自己的录制者
        // B的输入帧存在于B自己的录制段中（本地帧0~N）
        // A的录制段中也可能有B的输入帧（A的本地帧0~M）
        // 合并时需要：
        // 1. 用帧偏移将本地帧号映射到全局帧号
        // 2. 优先使用录制者自己录制段中的输入帧（更精确）
        // 3. 全局时间线上没有输入帧的位置填null（边界外）
        Map<UUID, List<玩家输入帧>> 合并输入表 = new HashMap<>();
        Map<UUID, List<float[]>> 合并鼠标样本表 = new HashMap<>();

        // 第一遍：收集所有玩家UUID在各录制段中的输入帧及其帧偏移
        // 数据结构：玩家UUID → [(帧偏移, 输入帧列表, 是否是该玩家自己的录制段)]
        Map<UUID, List<输入帧来源>> 输入帧来源表 = new HashMap<>();

        for (录制组.录制段 seg : segments) {
            for (Map.Entry<UUID, List<玩家输入帧>> entry : seg.会话.玩家输入表.entrySet()) {
                UUID playerUUID = entry.getKey();
                List<玩家输入帧> inputList = entry.getValue();
                // 判断这个输入帧列表是否来自该玩家自己的录制段
                boolean 是自己的录制段 = playerUUID.equals(seg.使用者UUID);

                输入帧来源表.computeIfAbsent(playerUUID, k -> new ArrayList<>())
                        .add(new 输入帧来源(seg.帧偏移, inputList, 是自己的录制段));
            }
        }

        // 第二遍：对每个玩家，构建全局时间线上的输入帧列表
        for (Map.Entry<UUID, List<输入帧来源>> entry : 输入帧来源表.entrySet()) {
            UUID playerUUID = entry.getKey();
            List<输入帧来源> 来源列表 = entry.getValue();

            // 按优先级排序：自己的录制段优先
            来源列表.sort((a, b) -> Boolean.compare(b.是自己的录制段, a.是自己的录制段));

            // 构建全局时间线输入帧列表
            List<玩家输入帧> 全局输入帧 = new ArrayList<>(totalFrames);
            for (int i = 0; i < totalFrames; i++) {
                全局输入帧.add(null);
            }

            // 按优先级从低到高填充（后填充的覆盖先填充的）
            // 反转顺序：先填低优先级，再填高优先级覆盖
            for (int srcIdx = 来源列表.size() - 1; srcIdx >= 0; srcIdx--) {
                输入帧来源 src = 来源列表.get(srcIdx);
                for (int localFrame = 0; localFrame < src.输入帧列表.size(); localFrame++) {
                    int globalFrame = localFrame + src.帧偏移;
                    if (globalFrame < 0 || globalFrame >= totalFrames) continue;

                    玩家输入帧 input = src.输入帧列表.get(localFrame);
                    if (input != null) {
                        全局输入帧.set(globalFrame, input);
                    }
                }
            }

            合并输入表.put(playerUUID, 全局输入帧);
        }

        // 填充录制者在帧偏移之前的空位（用帧数据中的位置/朝向构造最小输入帧）
        // 场景：B的帧偏移>0 → 帧0~偏移-1没有B的输入帧 → 客户端收不到输入帧 → 过渡保护永远不关闭
        // 解决：从已合并的帧列表中提取B在那些帧的朝向数据，构造"静止不动"的输入帧
        // → 客户端收到输入帧 → 过渡保护正常关闭 → 输入回放激活 → B的动画正常
        //
        // 重要：只填充帧偏移之前的区间（帧0 ~ 帧偏移-1）
        // 帧偏移之后的区间本应有真实输入帧，如果缺失（如录制最后一帧未上报）则保持null
        // 避免合成帧覆盖真实输入帧末尾，导致选中槽位归零、视角瞬移等问题
        //
        // 构建录制者→帧偏移映射，用于确定每个录制者的填充范围
        Map<UUID, Integer> 录制者帧偏移表 = new HashMap<>();
        for (录制组.录制段 seg : segments) {
            录制者帧偏移表.put(seg.使用者UUID, seg.帧偏移);
        }

        for (UUID 录制者UUID : group.获取录制者集合()) {
            List<玩家输入帧> 输入帧列表 = 合并输入表.get(录制者UUID);
            if (输入帧列表 == null) continue;

            int 帧偏移 = 录制者帧偏移表.getOrDefault(录制者UUID, 0);

            // 只填充帧偏移之前的空位（该录制者开始录制前的区间）
            // 帧偏移之后的空位保持null（那些帧本应有真实输入帧）
            for (int i = 0; i < 帧偏移 && i < 输入帧列表.size(); i++) {
                if (输入帧列表.get(i) != null) continue;

                // 该帧没有输入帧 → 从帧数据中提取朝向构造合成输入帧
                Map<UUID, 实体帧数据> 帧 = 合并帧列表.get(i);
                实体帧数据 实体帧 = (帧 != null) ? 帧.get(录制者UUID) : null;

                if (实体帧 != null) {
                    // 通过工厂方法创建合成帧，标记为合成帧
                    // 调用方通过是否合成帧()统一判断是否跳过选中槽位等字段
                    输入帧列表.set(i, 玩家输入帧.创建合成帧(
                            实体帧.获取YRot(), 实体帧.获取XRot()
                    ));
                }
            }
        }

        // 鼠标样本：同样按录制段汇总（目前兼容方法，不需要精确对齐）
        for (录制组.录制段 seg : segments) {
            合并鼠标样本表.putAll(seg.会话.鼠标样本表);
        }

        // === 7. 汇总初始状态 ===
        Map<UUID, 录制初始状态> 初始状态表 = new HashMap<>();
        Map<UUID, List<String>> 玩家初始按键表 = new HashMap<>();
        for (录制组.录制段 seg : segments) {
            if (seg.会话.初始状态 != null) {
                初始状态表.put(seg.使用者UUID, seg.会话.初始状态);
            }
            List<String> keys = seg.会话.玩家初始按键.get(seg.使用者UUID);
            if (keys != null) {
                玩家初始按键表.put(seg.使用者UUID, keys);
            }
        }

        // === 8. 合并NBT缓存 ===
        Map<UUID, CompoundTag> 合并NBT缓存 = new HashMap<>();
        for (录制组.录制段 seg : segments) {
            合并NBT缓存.putAll(seg.会话.上次状态NBT缓存);
        }

        // === 9. 合并影响标记表和影响记录 ===
        // 创建新的实例，合并时把所有段的内容聚合
        影响标记表 合并标记表 = new 影响标记表();
        影响记录 合并影响 = new 影响记录();
        // 影响数据在录制期间就已经写入各段的标记表和影响记录中
        // 回放期间会继续追加到合并后的实例
        // 这里只需要把各段已有的数据合并过来
        for (录制组.录制段 seg : segments) {
            合并标记表.合并(seg.会话.标记表);
            合并影响.合并(seg.会话.影响);
        }

        // === 10. 计算使用者帧范围 ===
        Map<UUID, int[]> 使用者帧范围 = new HashMap<>();
        for (录制组.录制段 seg : segments) {
            使用者帧范围.put(seg.使用者UUID,
                    new int[]{seg.帧偏移, seg.获取全局结束帧()});
        }

        // === 11. 计算录制中心（取所有段中心的平均值） ===
        double cx = 0, cy = 0, cz = 0;
        int count = 0;
        for (录制组.录制段 seg : segments) {
            cx += seg.会话.录制中心.x;
            cy += seg.会话.录制中心.y;
            cz += seg.会话.录制中心.z;
            count++;
        }
        Vec3 录制中心 = count > 0
                ? new Vec3(cx / count, cy / count, cz / count)
                : Vec3.ZERO;

        // === 构建合并录制数据 ===
        合并录制数据 result = new 合并录制数据(
                合并帧列表, totalFrames,
                合并方块变化, 合并方块实体变化,
                合并快照,
                玩家快照表, 结束快照表,
                合并输入表, 合并鼠标样本表,
                初始状态表, 玩家初始按键表,
                合并NBT缓存,
                合并标记表, 合并影响,
                group.获取录制者集合(), group.获取被录制实体集合(),
                使用者帧范围,
                录制中心, group.获取维度()
        );

        // 诊断日志：验证帧数据和输入帧的对齐
        for (Map.Entry<UUID, List<玩家输入帧>> inputEntry : 合并输入表.entrySet()) {
            List<玩家输入帧> inputList = inputEntry.getValue();
            int nullCount = 0;
            for (玩家输入帧 f : inputList) {
                if (f == null) nullCount++;
            }
            LOGGER.info("合并诊断 - 玩家 {} 输入帧: 总数={}, null数={}, 帧数据总帧={}",
                    inputEntry.getKey(), inputList.size(), nullCount, totalFrames);
        }
        for (录制组.录制段 seg : segments) {
            LOGGER.info("合并诊断 - 录制段 {} 帧偏移={}, 原始帧数={}, 原始输入帧数={}",
                    seg.使用者UUID, seg.帧偏移, seg.获取帧数(),
                    seg.会话.玩家输入表.values().stream().mapToInt(List::size).sum());
        }

        LOGGER.info("录制组 {} 合并完成：{} 帧，{} 个方块变化，{} 个录制者",
                group.获取组ID(), totalFrames,
                合并方块变化.size(), group.获取录制者集合().size());

        return result;
    }

    // ==================== 查询 ====================

    /**
     * 获取指定玩家所在的录制组
     */
    @Nullable
    public static 录制组 获取玩家所在录制组(UUID userUUID) {
        UUID 组ID = 使用者组映射.get(userUUID);
        if (组ID == null) return null;
        return 活跃组表.get(组ID);
    }

    /**
     * 获取指定组ID的录制组
     */
    @Nullable
    public static 录制组 获取录制组(UUID groupID) {
        return 活跃组表.get(groupID);
    }

    /**
     * 玩家是否在某个录制组中
     */
    public static boolean 玩家在录制组中(UUID userUUID) {
        return 使用者组映射.containsKey(userUUID);
    }

    /**
     * 获取所有正在续接窗口中的录制组
     */
    public static List<录制组> 获取续接窗口中的录制组() {
        List<录制组> result = new ArrayList<>();
        for (录制组 group : 活跃组表.values()) {
            if (group.续接窗口中()) {
                result.add(group);
            }
        }
        return result;
    }

    // ==================== 清理 ====================

    /**
     * 从活跃组表中移除已关闭的录制组
     * 在回放开始后调用（录制组的数据已经被复刻引擎接管）
     *
     * @param groupID 录制组ID
     */
    public static void 移除录制组(UUID groupID) {
        录制组 removed = 活跃组表.remove(groupID);
        if (removed != null) {
            // 清除该组所有录制者的映射
            for (UUID userUUID : removed.获取录制者集合()) {
                使用者组映射.remove(userUUID);
            }
            LOGGER.info("录制组 {} 已从活跃组表中移除", groupID);
        }
    }

    /**
     * 玩家下线清理
     */
    public static void 玩家下线(UUID playerUUID) {
        取消录制段(playerUUID);
    }

    /**
     * 服务器关闭清理
     */
    public static void 清除全部() {
        活跃组表.clear();
        使用者组映射.clear();
        LOGGER.debug("录制组管理器已清空");
    }

    // ==================== 内部方法 ====================

    /**
     * 查找可加入的录制组
     *
     * 条件：
     * - 同一维度
     * - 未关闭
     * - 正在录制中（活跃录制数>0）或正在续接窗口中
     */
    @Nullable
    private static 录制组 查找可加入录制组(ServerLevel level) {
        for (录制组 group : 活跃组表.values()) {
            if (group.是否已关闭()) continue;
            if (group.获取维度() != level) continue;
            if (group.获取活跃录制数() > 0 || group.续接窗口中()) {
                return group;
            }
        }
        return null;
    }

    // ==================== 内部数据类 ====================

    /**
     * 输入帧来源 — 记录某个玩家的输入帧数据及其在全局时间线中的偏移
     *
     * 用于合并算法中对同一玩家的多份输入帧数据进行优先级排序和偏移对齐。
     * 一个玩家可能同时出现在多个录制段的输入帧表中：
     * - 自己的录制段：输入帧最精确（自己客户端上报的）
     * - 别人的录制段：输入帧可能不完整（只在范围内才有）
     */
    private static class 输入帧来源 {
        /** 该录制段在全局时间线中的帧偏移 */
        final int 帧偏移;
        /** 输入帧列表（本地帧号索引） */
        final List<玩家输入帧> 输入帧列表;
        /** 是否是该玩家自己的录制段（优先级更高） */
        final boolean 是自己的录制段;

        输入帧来源(int 帧偏移, List<玩家输入帧> 输入帧列表, boolean 是自己的录制段) {
            this.帧偏移 = 帧偏移;
            this.输入帧列表 = 输入帧列表;
            this.是自己的录制段 = 是自己的录制段;
        }
    }
}
