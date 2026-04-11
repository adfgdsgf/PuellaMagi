package com.v2t.puellamagi.system.ability.epitaph;

import com.v2t.puellamagi.util.recording.世界快照;
import com.v2t.puellamagi.util.recording.实体帧数据;
import com.v2t.puellamagi.util.recording.玩家快照;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 合并录制数据
 *
 * 录制组关闭后，将所有录制段的数据合并到统一时间线上的产物。
 * 复刻引擎基于此数据驱动回放，替代原来直接使用单个录制会话。
 *
 * 设计目标：
 * - 对复刻引擎提供和原来录制会话完全兼容的访问接口
 * - 单人场景下退化为原始录制会话的等价封装（无额外开销）
 * - 多人场景下提供统一的帧索引、方块变化、快照等合并视图
 *
 * 数据来源：
 * - 帧数据：多个录制段的帧数据按帧偏移合并到统一帧列表
 * - 方块变化：所有段的方块变化帧调整tick序号后按时间排序
 * - 世界快照：最早录制段的快照为基础，后续段的新区域补充
 * - 玩家快照/输入帧/初始状态：按录制者UUID索引
 */
public class 合并录制数据 {

    // ==================== 帧数据（统一时间线） ====================

    /**
     * 统一帧列表
     * 索引 = 全局帧号（0 = 全局起始时间对应的帧）
     * 值 = 该帧所有实体的状态（多个录制段的数据合并）
     */
    private final List<Map<UUID, 实体帧数据>> 帧列表;

    /** 总帧数 */
    private final int 总帧数;

    // ==================== 方块变化（按全局帧号排序） ====================

    /** 合并后的方块变化列表（tick序号已调整为全局帧号） */
    private final List<方块变化帧> 方块变化列表;

    /** 合并后的方块实体变化列表（tick序号已调整为全局帧号） */
    private final List<方块实体变化帧> 方块实体变化列表;

    // ==================== 快照 ====================

    /** 合并后的世界快照（最早录制起点的状态 + 后续段补充的新区域） */
    private final 世界快照 合并快照;

    /** 所有录制者在录制开始时的玩家快照 */
    private final Map<UUID, 玩家快照> 玩家快照表;

    /** 所有录制者在录制结束时的玩家快照（结果驱动恢复用） */
    private final Map<UUID, 玩家快照> 结束快照表;

    // ==================== 输入帧 ====================

    /** 所有录制者的输入帧：录制者UUID → 输入帧列表（全局帧号对齐） */
    private final Map<UUID, List<玩家输入帧>> 玩家输入表;

    /** 所有录制者的鼠标样本 */
    private final Map<UUID, List<float[]>> 鼠标样本表;

    // ==================== 初始状态 ====================

    /** 每个录制者的录制初始状态 */
    private final Map<UUID, 录制初始状态> 初始状态表;

    /** 每个录制者的初始按键列表 */
    private final Map<UUID, List<String>> 玩家初始按键表;

    // ==================== NBT缓存 ====================

    /** 合并后的上次状态NBT缓存（NBT校验用） */
    private final Map<UUID, CompoundTag> 上次状态NBT缓存;

    // ==================== 影响追踪 ====================

    /** 合并后的影响标记表（时间删除用） */
    private final 影响标记表 标记表;

    /** 合并后的影响记录（时间删除用） */
    private final 影响记录 影响;

    // ==================== 参与者信息 ====================

    /** 所有录制者UUID */
    private final Set<UUID> 录制者集合;

    /** 所有被录制到的实体UUID */
    private final Set<UUID> 被录制实体集合;

    /**
     * 每个录制者在统一时间线中的有效帧区间
     * UUID → [起始帧, 结束帧)（左闭右开）
     */
    private final Map<UUID, int[]> 使用者帧范围;

    /** 录制中心点（用于方块操作范围判断） */
    private final net.minecraft.world.phys.Vec3 录制中心;

    /** 维度 */
    private final ServerLevel 维度;

    // ==================== 构造 ====================

    /**
     * 构造合并录制数据
     * 由录制组管理器在录制组关闭后调用合并算法生成
     *
     * @param 帧列表           统一时间线的帧数据
     * @param 总帧数           总帧数
     * @param 方块变化列表     合并后的方块变化
     * @param 方块实体变化列表 合并后的方块实体变化
     * @param 合并快照         合并后的世界快照
     * @param 玩家快照表       录制开始时的玩家快照
     * @param 结束快照表       录制结束时的玩家快照
     * @param 玩家输入表       输入帧
     * @param 鼠标样本表       鼠标样本
     * @param 初始状态表       初始状态
     * @param 玩家初始按键表   初始按键
     * @param 上次状态NBT缓存  NBT缓存
     * @param 标记表           影响标记表
     * @param 影响             影响记录
     * @param 录制者集合       所有录制者
     * @param 被录制实体集合   所有被录制到的实体
     * @param 使用者帧范围     每个录制者的帧区间
     * @param 录制中心         录制中心点
     * @param 维度             维度
     */
    public 合并录制数据(
            List<Map<UUID, 实体帧数据>> 帧列表,
            int 总帧数,
            List<方块变化帧> 方块变化列表,
            List<方块实体变化帧> 方块实体变化列表,
            世界快照 合并快照,
            Map<UUID, 玩家快照> 玩家快照表,
            Map<UUID, 玩家快照> 结束快照表,
            Map<UUID, List<玩家输入帧>> 玩家输入表,
            Map<UUID, List<float[]>> 鼠标样本表,
            Map<UUID, 录制初始状态> 初始状态表,
            Map<UUID, List<String>> 玩家初始按键表,
            Map<UUID, CompoundTag> 上次状态NBT缓存,
            影响标记表 标记表,
            影响记录 影响,
            Set<UUID> 录制者集合,
            Set<UUID> 被录制实体集合,
            Map<UUID, int[]> 使用者帧范围,
            net.minecraft.world.phys.Vec3 录制中心,
            ServerLevel 维度
    ) {
        this.帧列表 = 帧列表;
        this.总帧数 = 总帧数;
        this.方块变化列表 = 方块变化列表;
        this.方块实体变化列表 = 方块实体变化列表;
        this.合并快照 = 合并快照;
        this.玩家快照表 = 玩家快照表;
        this.结束快照表 = 结束快照表;
        this.玩家输入表 = 玩家输入表;
        this.鼠标样本表 = 鼠标样本表;
        this.初始状态表 = 初始状态表;
        this.玩家初始按键表 = 玩家初始按键表;
        this.上次状态NBT缓存 = 上次状态NBT缓存;
        this.标记表 = 标记表;
        this.影响 = 影响;
        this.录制者集合 = 录制者集合;
        this.被录制实体集合 = 被录制实体集合;
        this.使用者帧范围 = 使用者帧范围;
        this.录制中心 = 录制中心;
        this.维度 = 维度;
    }

    // ==================== 帧数据访问（兼容复刻引擎现有接口） ====================

    /**
     * 获取指定帧的所有实体数据
     * 兼容原 录制数据.获取帧() 接口
     */
    @Nullable
    public Map<UUID, 实体帧数据> 获取帧(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= 帧列表.size()) {
            return null;
        }
        return 帧列表.get(frameIndex);
    }

    /**
     * 获取指定帧中指定实体的数据
     * 兼容原 录制数据.获取实体帧() 接口
     */
    @Nullable
    public 实体帧数据 获取实体帧(int frameIndex, UUID entityUUID) {
        Map<UUID, 实体帧数据> frame = 获取帧(frameIndex);
        return frame != null ? frame.get(entityUUID) : null;
    }

    /**
     * 获取总帧数
     * 兼容原 录制数据.获取总帧数() 接口
     */
    public int 获取总帧数() { return 总帧数; }

    // ==================== 方块变化访问 ====================

    public List<方块变化帧> 获取方块变化列表() { return 方块变化列表; }

    public List<方块实体变化帧> 获取方块实体变化列表() { return 方块实体变化列表; }

    // ==================== 快照访问 ====================

    public 世界快照 获取起点快照() { return 合并快照; }

    public Map<UUID, 玩家快照> 获取玩家快照表() { return 玩家快照表; }

    public Map<UUID, 玩家快照> 获取结束快照表() { return 结束快照表; }

    // ==================== 输入帧访问 ====================

    public Map<UUID, List<玩家输入帧>> 获取玩家输入表() { return 玩家输入表; }

    public Map<UUID, List<float[]>> 获取鼠标样本表() { return 鼠标样本表; }

    // ==================== 初始状态访问 ====================

    public Map<UUID, 录制初始状态> 获取初始状态表() { return 初始状态表; }

    public Map<UUID, List<String>> 获取玩家初始按键表() { return 玩家初始按键表; }

    /**
     * 获取指定录制者的初始状态（兼容单人模式下的访问方式）
     */
    @Nullable
    public 录制初始状态 获取初始状态(UUID userUUID) {
        return 初始状态表.get(userUUID);
    }

    /**
     * 获取指定录制者的初始按键
     */
    @Nullable
    public List<String> 获取初始按键(UUID userUUID) {
        return 玩家初始按键表.get(userUUID);
    }

    // ==================== NBT缓存访问 ====================

    public Map<UUID, CompoundTag> 获取上次状态NBT缓存() { return 上次状态NBT缓存; }

    // ==================== 影响追踪访问 ====================

    public 影响标记表 获取标记表() { return 标记表; }

    public 影响记录 获取影响() { return 影响; }

    // ==================== 参与者信息 ====================

    public Set<UUID> 获取录制者集合() { return 录制者集合; }

    public Set<UUID> 获取被录制实体集合() { return 被录制实体集合; }

    public Map<UUID, int[]> 获取使用者帧范围() { return 使用者帧范围; }

    /**
     * 检查指定录制者在指定帧是否在有效范围内
     *
     * @param userUUID   录制者UUID
     * @param frameIndex 全局帧索引
     * @return true = 该帧在该录制者的录制区间内
     */
    public boolean 帧在录制范围内(UUID userUUID, int frameIndex) {
        int[] range = 使用者帧范围.get(userUUID);
        if (range == null) return false;
        return frameIndex >= range[0] && frameIndex < range[1];
    }

    // ==================== 位置和维度 ====================

    public net.minecraft.world.phys.Vec3 获取录制中心() { return 录制中心; }

    public ServerLevel 获取维度() { return 维度; }
}
