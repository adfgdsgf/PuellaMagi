// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍成员数据.java

package com.v2t.puellamagi.system.team;

import com.v2t.puellamagi.api.team.I队伍成员;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 队伍成员数据
 *
 * 存储单个成员的信息：职位、个人配置
 */
public class 队伍成员数据 implements I队伍成员 {

    private final UUID 玩家UUID;
    private final long 加入时间;
    private 队伍职位 职位;
    private final 队伍个人配置 个人配置;

    /**
     * 创建新成员（默认队员职位）
     */
    public 队伍成员数据(UUID playerUUID, long joinTime) {
        this(playerUUID, joinTime, 队伍职位.队员, new 队伍个人配置());
    }

    /**
     * 创建新成员（指定职位）
     */
    public 队伍成员数据(UUID playerUUID, long joinTime, 队伍职位 role) {
        this(playerUUID, joinTime, role, new 队伍个人配置());
    }

    private 队伍成员数据(UUID playerUUID, long joinTime, 队伍职位 role, 队伍个人配置 config) {
        this.玩家UUID = playerUUID;
        this.加入时间 = joinTime;
        this.职位 = role;
        this.个人配置 = config;
    }

    // ==================== 接口实现 ====================

    @Override
    public UUID 获取UUID() {
        return 玩家UUID;
    }

    @Override
    public long 获取加入时间() {
        return 加入时间;
    }

    @Override
    public I队伍个人配置 获取个人配置() {
        return 个人配置;
    }

    @Override
    public String 获取职位ID() {
        return 职位.获取ID();
    }

    // ==================== 职位 ====================

    public 队伍职位 获取职位() {
        return 职位;
    }

    public void 设置职位(队伍职位 role) {
        this.职位 = role;
    }

    /**
     * 判断该成员是否拥有指定权限
     * 直接委托给职位判断
     */
    public boolean 有权限(队伍权限 permission) {
        return 职位.有权限(permission);
    }

    // ==================== 配置 ====================

    /**
     * 获取可修改的个人配置（内部使用）
     */
    public 队伍个人配置 获取配置() {
        return 个人配置;
    }

    // ==================== NBT序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", 玩家UUID);
        tag.putLong("joinTime", 加入时间);
        tag.putString("role", 职位.获取ID());
        tag.put("config", 个人配置.写入NBT());
        return tag;
    }

    public static 队伍成员数据 从NBT读取(CompoundTag tag) {
        UUID uuid = tag.getUUID("uuid");
        long joinTime = tag.getLong("joinTime");

        队伍职位 role = 队伍职位.队员;
        if (tag.contains("role")) {
            role = 队伍职位.从ID获取(tag.getString("role"));
        }

        队伍个人配置 config = new 队伍个人配置();
        if (tag.contains("config")) {
            config.从NBT读取(tag.getCompound("config"));
        }

        return new 队伍成员数据(uuid, joinTime, role, config);
    }

    // ==================== 网络包序列化 ====================

    public void 写入Buffer(FriendlyByteBuf buf) {
        buf.writeUUID(玩家UUID);
        buf.writeLong(加入时间);
        buf.writeUtf(职位.获取ID());
        个人配置.写入Buffer(buf);
    }

    public static 队伍成员数据 从Buffer读取(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        long joinTime = buf.readLong();
        队伍职位 role = 队伍职位.从ID获取(buf.readUtf());
        队伍个人配置 config = 队伍个人配置.从Buffer读取(buf);
        return new 队伍成员数据(uuid, joinTime, role, config);
    }
}
