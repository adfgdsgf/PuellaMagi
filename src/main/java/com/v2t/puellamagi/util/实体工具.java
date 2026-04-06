package com.v2t.puellamagi.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 实体工具
 *
 * 提供服务端实体查找的通用方法
 * 消除多个文件中重复定义的实体查找逻辑
 *
 * 使用场景：
 * - 复刻引擎：按UUID查找被控制实体/被锁定玩家
 * - 录制管理器：按UUID查找被录制实体
 * - 回放校验器：按UUID查找玩家
 * - 世界快照：按UUID查找实体ID
 */
public final class 实体工具 {

    private 实体工具() {}

    /**
     * 按UUID查找实体
     *
     * 遍历维度中所有实体，按UUID匹配
     * 适用于服务端（ServerLevel）
     *
     * @param level 服务端维度
     * @param uuid  目标实体的UUID
     * @return 找到的实体，未找到返回null
     */
    @Nullable
    public static Entity 按UUID查找实体(ServerLevel level, UUID uuid) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 按UUID查找实体ID
     *
     * 返回实体的数值ID而非实体引用
     * 用于世界快照等需要ID而非引用的场景
     *
     * @param level 服务端维度
     * @param uuid  目标实体的UUID
     * @return 找到的实体ID，未找到返回-1
     */
    public static int 按UUID查找实体ID(ServerLevel level, UUID uuid) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getUUID().equals(uuid)) {
                return entity.getId();
            }
        }
        return -1;
    }
}
