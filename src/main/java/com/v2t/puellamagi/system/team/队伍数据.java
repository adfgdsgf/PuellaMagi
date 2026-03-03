// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍数据.java

package com.v2t.puellamagi.system.team;

import com.v2t.puellamagi.api.team.I队伍;
import com.v2t.puellamagi.api.team.I队伍成员;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 队伍数据
 *
 * 存储一个队伍的完整信息
 * 线程安全：使用ConcurrentHashMap存储成员
 */
public class 队伍数据 implements I队伍 {

    private final UUID 队伍ID;
    private UUID 队长UUID;
    private final long 创建时间;

    /** 成员表：玩家UUID → 成员数据 */
    private final Map<UUID, 队伍成员数据> 成员表 = new ConcurrentHashMap<>();

    //==================== 构造 ====================

    /**
     * 创建新队伍
     *
     * @param creatorUUID 创建者UUID（自动成为队长和第一个成员）
     * @param gameTime 当前游戏时间
     */
    public 队伍数据(UUID creatorUUID, long gameTime) {
        this.队伍ID = UUID.randomUUID();
        this.队长UUID = creatorUUID;
        this.创建时间 = gameTime;

        // 创建者自动加入，职位为队长
        成员表.put(creatorUUID, new 队伍成员数据(creatorUUID, gameTime, 队伍职位.队长));
    }

    /**
     * 从已有数据构造（反序列化用）
     */
    private 队伍数据(UUID teamId, UUID leaderUUID, long createTime) {
        this.队伍ID = teamId;
        this.队长UUID = leaderUUID;
        this.创建时间 = createTime;}

    // ==================== I队伍 接口实现 ====================

    @Override
    public UUID 获取队伍ID() {
        return 队伍ID;
    }

    @Override
    public UUID 获取队长UUID() {
        return 队长UUID;
    }

    @Override
    public void 设置队长(UUID playerUUID) {
        if (!是成员(playerUUID)) return;

        //旧队长降级为队员
        队伍成员数据 oldLeader = 成员表.get(队长UUID);
        if (oldLeader != null) {
            oldLeader.设置职位(队伍职位.队员);}

        // 新队长升级
        队伍成员数据 newLeader = 成员表.get(playerUUID);
        if (newLeader != null) {
            newLeader.设置职位(队伍职位.队长);
        }

        this.队长UUID = playerUUID;
    }

    @Override
    public List<UUID> 获取所有成员UUID() {
        return 成员表.values().stream().sorted(Comparator.comparingInt(
                        (队伍成员数据 m) -> m.获取职位().获取显示优先级())    .thenComparingLong(队伍成员数据::获取加入时间))
                .map(队伍成员数据::获取UUID)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<I队伍成员> 获取成员(UUID playerUUID) {
        return Optional.ofNullable(成员表.get(playerUUID));
    }

    @Override
    public boolean 是成员(UUID playerUUID) {
        return 成员表.containsKey(playerUUID);
    }

    @Override
    public boolean 是队长(UUID playerUUID) {
        return 队长UUID.equals(playerUUID);
    }

    @Override
    public int 获取成员数量() {
        return 成员表.size();
    }

    @Override
    public boolean 是否为空() {
        return 成员表.isEmpty();
    }

    // ==================== 权限查询 ====================

    /**
     * 判断成员是否拥有指定权限
     *通过成员的职位判断，不硬编码职位类型
     *
     * @param playerUUID 成员UUID
     * @param permission 权限
     * @return 是否拥有，非成员返回false
     */
    public boolean 成员有权限(UUID playerUUID, 队伍权限 permission) {
        队伍成员数据 member = 成员表.get(playerUUID);
        if (member == null) return false;
        return member.有权限(permission);
    }

    // ==================== 成员操作 ====================

    /**
     * 添加成员（默认队员职位）
     *
     * @param playerUUID 玩家UUID
     * @param gameTime 当前游戏时间
     * @return 是否成功（已存在则失败）
     */
    public boolean 添加成员(UUID playerUUID, long gameTime) {
        if (是成员(playerUUID)) {
            return false;
        }
        成员表.put(playerUUID, new 队伍成员数据(playerUUID, gameTime));
        return true;
    }

    /**
     * 移除成员
     *
     * @param playerUUID 玩家UUID
     * @return 被移除的成员数据，null表示不存在
     */
    public 队伍成员数据 移除成员(UUID playerUUID) {
        return 成员表.remove(playerUUID);
    }

    /**
     * 获取可修改的成员数据（内部使用）
     */
    public 队伍成员数据 获取成员数据(UUID playerUUID) {
        return 成员表.get(playerUUID);
    }

    /**
     * 转移队长
     *旧队长降为队员，新队长升为队长
     *
     * @param fromUUID 当前队长UUID
     * @param toUUID 新队长UUID
     * @return 是否成功
     */
    public boolean 转移队长(UUID fromUUID, UUID toUUID) {
        // 必须由当前队长发起
        if (!是队长(fromUUID)) return false;
        // 目标必须是成员
        if (!是成员(toUUID)) return false;
        // 不能转给自己
        if (fromUUID.equals(toUUID)) return false;

        设置队长(toUUID);
        return true;
    }

    /**
     * 获取最早加入的成员（用于队长自动转移）
     * 排除指定玩家（通常是正在离开的队长）
     *
     * @param excludeUUID 排除的UUID
     * @return 最早加入的成员UUID，null表示队伍为空
     */
    public UUID 获取最早成员(UUID excludeUUID) {
        return 成员表.values().stream()
                .filter(member -> !member.获取UUID().equals(excludeUUID))
                .min(Comparator.comparingLong(队伍成员数据::获取加入时间))
                .map(队伍成员数据::获取UUID)
                .orElse(null);
    }

    public long 获取创建时间() {
        return 创建时间;
    }

    // ==================== NBT序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("teamId", 队伍ID);
        tag.putUUID("leaderUUID", 队长UUID);
        tag.putLong("createTime", 创建时间);

        ListTag memberList = new ListTag();
        for (队伍成员数据 member : 成员表.values()) {
            memberList.add(member.写入NBT());
        }
        tag.put("members", memberList);

        return tag;
    }

    public static 队伍数据 从NBT读取(CompoundTag tag) {
        UUID teamId = tag.getUUID("teamId");
        UUID leaderUUID = tag.getUUID("leaderUUID");
        long createTime = tag.getLong("createTime");

        队伍数据 team = new 队伍数据(teamId, leaderUUID, createTime);

        ListTag memberList = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < memberList.size(); i++) {
            队伍成员数据 member = 队伍成员数据.从NBT读取(memberList.getCompound(i));
            team.成员表.put(member.获取UUID(), member);
        }

        return team;
    }

    // ==================== 网络包序列化 ====================

    public void 写入Buffer(FriendlyByteBuf buf) {
        buf.writeUUID(队伍ID);
        buf.writeUUID(队长UUID);
        buf.writeLong(创建时间);
        buf.writeInt(成员表.size());
        for (队伍成员数据 member : 成员表.values()) {
            member.写入Buffer(buf);
        }
    }

    public static 队伍数据 从Buffer读取(FriendlyByteBuf buf) {
        UUID teamId = buf.readUUID();
        UUID leaderUUID = buf.readUUID();
        long createTime = buf.readLong();

        队伍数据 team = new 队伍数据(teamId, leaderUUID, createTime);

        int memberCount = buf.readInt();
        for (int i = 0; i < memberCount; i++) {
            队伍成员数据 member = 队伍成员数据.从Buffer读取(buf);
            team.成员表.put(member.获取UUID(), member);
        }

        return team;
    }
}
