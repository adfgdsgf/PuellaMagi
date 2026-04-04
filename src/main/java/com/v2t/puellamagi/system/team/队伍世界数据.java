// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍世界数据.java

package com.v2t.puellamagi.system.team;

import com.v2t.puellamagi.core.data.ModWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队伍世界数据
 *
 * 持久化存储所有队伍信息到存档
 * 继承ModWorldData，使用SavedData机制
 *
 * 维护两套索引：
 * - 队伍ID → 队伍数据（主索引）
 * - 玩家UUID → 队伍ID（反向索引，快速查询玩家所在队伍）
 */
public class 队伍世界数据 extends ModWorldData {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/TeamData");
    private static final String DATA_NAME = "puellamagi_teams";

    /** 队伍ID → 队伍数据 */
    private final Map<UUID, 队伍数据> 队伍表 = new ConcurrentHashMap<>();

    /** 玩家UUID → 队伍ID（反向索引） */
    private final Map<UUID, UUID> 玩家队伍索引 = new ConcurrentHashMap<>();

    //==================== 构造与获取 ====================

    public 队伍世界数据() {
        super();
    }

    public static 队伍世界数据 获取(MinecraftServer server) {
        return 获取数据(server,
                DATA_NAME,
                队伍世界数据::new,
                tag -> {
                    队伍世界数据 data = new 队伍世界数据();
                    data.从NBT加载(tag);
                    return data;
                }
        );
    }

    // ==================== 队伍操作 ====================

    /**
     * 创建队伍
     *
     * @param creatorUUID 创建者UUID
     * @param gameTime 当前游戏时间
     * @return 新队伍数据，null表示玩家已有队伍
     */
    public 队伍数据 创建队伍(UUID creatorUUID, long gameTime) {
        if (玩家队伍索引.containsKey(creatorUUID)) {
            LOGGER.debug("玩家 {} 已有队伍，无法创建新队伍", creatorUUID);
            return null;
        }

        队伍数据 team = new 队伍数据(creatorUUID, gameTime);
        队伍表.put(team.获取队伍ID(), team);
        玩家队伍索引.put(creatorUUID, team.获取队伍ID());

        标记已修改();
        LOGGER.info("创建队伍 {}, 队长: {}", team.获取队伍ID(), creatorUUID);
        return team;
    }

    /**
     * 加入队伍
     *
     * @param playerUUID 玩家UUID
     * @param teamId 目标队伍ID
     * @param gameTime 当前游戏时间
     * @return 是否成功
     */
    public boolean 加入队伍(UUID playerUUID, UUID teamId, long gameTime) {
        if (玩家队伍索引.containsKey(playerUUID)) {
            LOGGER.debug("玩家 {} 已有队伍，无法加入", playerUUID);
            return false;
        }

        队伍数据 team = 队伍表.get(teamId);
        if (team == null) {
            LOGGER.debug("队伍 {} 不存在", teamId);
            return false;
        }

        if (!team.添加成员(playerUUID, gameTime)) {
            return false;
        }

        玩家队伍索引.put(playerUUID, teamId);
        标记已修改();
        LOGGER.info("玩家 {} 加入队伍 {}", playerUUID, teamId);
        return true;
    }

    /**
     * 离开队伍
     *
     * @param playerUUID 玩家UUID
     * @return 是否成功
     */
    public boolean 离开队伍(UUID playerUUID) {
        UUID teamId = 玩家队伍索引.get(playerUUID);
        if (teamId == null) {
            return false;
        }

        队伍数据 team = 队伍表.get(teamId);
        if (team == null) {
            玩家队伍索引.remove(playerUUID);
            标记已修改();
            return false;
        }

        // 如果是队长离开，转移队长
        if (team.是队长(playerUUID)) {
            UUID newLeader = team.获取最早成员(playerUUID);
            if (newLeader != null) {
                team.设置队长(newLeader);
                LOGGER.info("队长转移: {} → {}", playerUUID, newLeader);
            }
        }

        team.移除成员(playerUUID);
        玩家队伍索引.remove(playerUUID);

        // 队伍空了则删除
        if (team.是否为空()) {
            队伍表.remove(teamId);
            LOGGER.info("队伍 {} 已空，自动删除", teamId);
        }

        标记已修改();
        LOGGER.info("玩家 {} 离开队伍 {}", playerUUID, teamId);
        return true;
    }

    /**
     * 踢出成员（仅队长可操作）
     *
     * @param leaderUUID 操作者UUID（必须是队长）
     * @param targetUUID 被踢出的玩家UUID
     * @return 是否成功
     */
    public boolean 踢出成员(UUID leaderUUID, UUID targetUUID) {
        UUID teamId = 玩家队伍索引.get(leaderUUID);
        if (teamId == null) return false;

        队伍数据 team = 队伍表.get(teamId);
        if (team == null) return false;

        if (!team.是队长(leaderUUID)) {
            LOGGER.debug("玩家 {} 不是队长，无法踢人", leaderUUID);
            return false;
        }

        if (leaderUUID.equals(targetUUID)) {
            return false;
        }

        if (!team.是成员(targetUUID)) {
            return false;
        }

        team.移除成员(targetUUID);
        玩家队伍索引.remove(targetUUID);

        标记已修改();
        LOGGER.info("队长 {} 踢出成员 {}（队伍 {}）", leaderUUID, targetUUID, teamId);
        return true;
    }

    /**
     * 解散队伍（仅队长可操作）
     *
     * @param leaderUUID 操作者UUID（必须是队长）
     * @return 被解散队伍的所有成员UUID，null表示失败
     */
    public List<UUID> 解散队伍(UUID leaderUUID) {
        UUID teamId = 玩家队伍索引.get(leaderUUID);
        if (teamId == null) return null;

        队伍数据 team = 队伍表.get(teamId);
        if (team == null) return null;

        if (!team.是队长(leaderUUID)) {
            LOGGER.debug("玩家 {} 不是队长，无法解散", leaderUUID);
            return null;
        }

        List<UUID> members = team.获取所有成员UUID();

        for (UUID memberUUID : members) {
            玩家队伍索引.remove(memberUUID);
        }

        队伍表.remove(teamId);

        标记已修改();
        LOGGER.info("队伍 {} 被队长 {} 解散，共{} 名成员", teamId, leaderUUID, members.size());
        return members;
    }

    // ==================== 查询 ====================

    /**
     * 获取玩家所在队伍
     */
    public Optional<队伍数据> 获取玩家队伍(UUID playerUUID) {
        UUID teamId = 玩家队伍索引.get(playerUUID);
        if (teamId == null) return Optional.empty();
        return Optional.ofNullable(队伍表.get(teamId));
    }

    /**
     * 获取玩家所在队伍ID
     */
    public UUID 获取玩家队伍ID(UUID playerUUID) {
        return 玩家队伍索引.get(playerUUID);
    }

    /**
     * 玩家是否在队伍中
     */
    public boolean 玩家有队伍(UUID playerUUID) {
        return 玩家队伍索引.containsKey(playerUUID);
    }

    /**
     * 判断两个玩家是否在同一队伍
     */
    public boolean 是否同队(UUID playerA, UUID playerB) {
        if (playerA == null || playerB == null) return false;
        if (playerA.equals(playerB)) return true;

        UUID teamA = 玩家队伍索引.get(playerA);
        UUID teamB = 玩家队伍索引.get(playerB);

        if (teamA == null || teamB == null) return false;
        return teamA.equals(teamB);
    }

    /**
     * 根据队伍ID获取队伍
     */
    public Optional<队伍数据> 获取队伍(UUID teamId) {
        return Optional.ofNullable(队伍表.get(teamId));
    }

    /**
     * 获取所有队伍
     */
    public Collection<队伍数据> 获取所有队伍() {
        return Collections.unmodifiableCollection(队伍表.values());
    }

    // ==================== 底层数据操作（供管理器使用） ====================

    /**
     * 移除玩家的队伍映射（反向索引）
     * 仅操作索引，不操作队伍数据本身
     *
     * @param playerUUID 玩家UUID
     */
    public void 移除玩家队伍映射(UUID playerUUID) {
        玩家队伍索引.remove(playerUUID);
    }

    /**
     * 从队伍表中移除队伍
     * 仅操作主索引，调用前需先清理成员的反向索引
     *
     * @param teamId 队伍ID
     */
    public void 移除队伍(UUID teamId) {
        队伍表.remove(teamId);
    }

    // ==================== NBT序列化（实现基类抽象方法）====================

    @Override
    protected CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();

        ListTag teamList = new ListTag();
        for (队伍数据 team : 队伍表.values()) {
            teamList.add(team.写入NBT());
        }
        tag.put("teams", teamList);

        LOGGER.debug("保存 {} 个队伍", 队伍表.size());
        return tag;
    }

    @Override
    protected void 从NBT加载(CompoundTag tag) {
        队伍表.clear();
        玩家队伍索引.clear();

        if (!tag.contains("teams")) return;

        ListTag teamList = tag.getList("teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teamList.size(); i++) {
            队伍数据 team = 队伍数据.从NBT读取(teamList.getCompound(i));
            队伍表.put(team.获取队伍ID(), team);

            // 重建反向索引
            for (UUID memberUUID : team.获取所有成员UUID()) {
                玩家队伍索引.put(memberUUID, team.获取队伍ID());
            }
        }

        LOGGER.info("加载 {} 个队伍, {} 名成员", 队伍表.size(), 玩家队伍索引.size());
    }
}
