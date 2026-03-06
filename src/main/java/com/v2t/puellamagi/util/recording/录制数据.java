package com.v2t.puellamagi.util.recording;

import java.util.*;

/**
 * 录制数据
 *
 * 通用的帧数据容器，按tick索引存储每帧的实体状态
 * 具体技能可以在此基础上附加专用数据（如玩家输入帧）
 *
 * 复用场景：预知录制、分身轨迹、死亡回放
 */
public class 录制数据 {

    /** 录制起始的游戏时间 */
    private final long 起始时间;

    /** 帧列表，索引0= 第一帧（起始时间） */
    private final List<Map<UUID, 实体帧数据>> 帧列表;

    /** 最大帧数限制 */
    private final int 最大帧数;

    // ==================== 构造 ====================

    /**
     * @param startTime 起始游戏时间（tick）
     * @param maxFrames 最大帧数（0=无限制）
     */
    public 录制数据(long startTime, int maxFrames) {
        this.起始时间 = startTime;
        this.最大帧数 = maxFrames;
        this.帧列表 = new ArrayList<>();
    }

    // ==================== 录制操作 ====================

    /**
     * 添加一帧
     *
     * @param 帧数据 该帧所有实体的状态
     * @return 是否成功（可能因为达到上限而失败）
     */
    public boolean 添加帧(Map<UUID, 实体帧数据> 帧数据) {
        if (最大帧数 > 0 && 帧列表.size() >= 最大帧数) {
            return false;
        }
        帧列表.add(帧数据);
        return true;
    }

    // ==================== 查询 ====================

    /**
     * 获取指定帧的所有实体数据
     *
     * @param frameIndex 帧索引（0开始）
     * @return 实体帧数据Map，null表示帧不存在
     */
    @javax.annotation.Nullable
    public Map<UUID, 实体帧数据> 获取帧(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= 帧列表.size()) {
            return null;
        }
        return 帧列表.get(frameIndex);
    }

    /**
     * 获取指定帧中指定实体的数据
     */
    @javax.annotation.Nullable
    public 实体帧数据 获取实体帧(int frameIndex, UUID entityUUID) {
        Map<UUID, 实体帧数据> frame = 获取帧(frameIndex);
        return frame != null ? frame.get(entityUUID) : null;
    }

    /**
     * 获取指定实体在所有帧中的轨迹
     * 用于影子渲染
     *
     * @param entityUUID 实体UUID
     * @return 该实体的帧数据列表（按时间顺序）
     */
    public List<实体帧数据> 获取实体轨迹(UUID entityUUID) {
        List<实体帧数据> trajectory = new ArrayList<>();
        for (Map<UUID, 实体帧数据> frame : 帧列表) {
            实体帧数据 data = frame.get(entityUUID);
            if (data != null) {
                trajectory.add(data);
            }
        }
        return trajectory;
    }

    // ==================== 状态 ====================

    public long 获取起始时间() { return 起始时间; }
    public int 获取总帧数() { return 帧列表.size(); }
    public int 获取最大帧数() { return 最大帧数; }
    public boolean 已满() { return 最大帧数 > 0&& 帧列表.size() >= 最大帧数; }
    public boolean 为空() { return 帧列表.isEmpty(); }

    /**
     * 获取录制时长（tick）
     */
    public int 获取录制时长() { return 帧列表.size(); }

    /**
     * 清空所有帧数据
     */
    public void 清空() { 帧列表.clear(); }
}
