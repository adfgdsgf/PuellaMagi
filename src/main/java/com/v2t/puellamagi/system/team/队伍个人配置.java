// 文件路径: src/main/java/com/v2t/puellamagi/system/team/队伍个人配置.java

package com.v2t.puellamagi.system.team;

import com.v2t.puellamagi.api.team.I队伍成员;
import net.minecraft.nbt.CompoundTag;

/**
 * 队伍个人配置实现
 *
 * 每个成员独立控制自己的行为表现
 * 配置影响的是"自己的行为"，不影响队友
 *
 * 示例：
 * - A开友伤、B关友伤 → A打B有伤害，B打A无伤害
 * - A开觉醒、B关觉醒 → A会被队友唤醒，B不会
 */
public class 队伍个人配置 implements I队伍成员.I队伍个人配置 {

    //==================== 配置项 ====================

    private boolean 友伤 = false;
    private boolean 时停觉醒 = true;
    private boolean 锁定跳过队友 = true;
    private boolean 允许队友搜身 = false;

    // ==================== 接口实现 ====================

    @Override
    public boolean 友伤开启() {
        return 友伤;
    }

    @Override
    public boolean 时停觉醒() {
        return 时停觉醒;
    }

    @Override
    public boolean 锁定跳过队友() {
        return 锁定跳过队友;
    }

    @Override
    public boolean 允许队友搜身() {
        return 允许队友搜身;
    }

    // ==================== 设置方法 ====================

    public void 设置友伤(boolean value) {
        this.友伤 = value;
    }

    public void 设置时停觉醒(boolean value) {
        this.时停觉醒 = value;
    }

    public void 设置锁定跳过队友(boolean value) {
        this.锁定跳过队友 = value;
    }

    public void 设置允许队友搜身(boolean value) {
        this.允许队友搜身 = value;
    }

    /**
     * 通过配置键名设置值
     *用于网络包和UI的通用更新
     *
     * @param key 配置键名
     * @param value 值
     * @return 是否成功（键名是否有效）
     */
    public boolean 设置配置(String key, boolean value) {
        return switch (key) {
            case "friendlyFire" -> { 友伤 = value; yield true; }
            case "timestopAwakening" -> { 时停觉醒 = value; yield true; }
            case "lockOnSkipTeammate" -> { 锁定跳过队友 = value; yield true; }
            case "allowTeammateSearch" -> { 允许队友搜身 = value; yield true; }
            default -> false;
        };
    }

    /**
     * 通过配置键名获取值
     */
    public boolean 获取配置(String key) {
        return switch (key) {
            case "friendlyFire" ->友伤;
            case "timestopAwakening" -> 时停觉醒;
            case "lockOnSkipTeammate" -> 锁定跳过队友;
            case "allowTeammateSearch" -> 允许队友搜身;
            default -> false;
        };
    }

    /**
     * 获取所有配置键名（用于UI渲染和网络同步）
     */
    public static String[] 获取所有配置键() {
        return new String[]{
                "friendlyFire",
                "timestopAwakening",
                "lockOnSkipTeammate",
                "allowTeammateSearch"
        };
    }

    /**
     * 获取配置项的翻译键
     */
    public static String 获取翻译键(String key) {
        return "gui.puellamagi.team.config." + key;
    }

    // ==================== NBT序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("friendlyFire", 友伤);
        tag.putBoolean("timestopAwakening", 时停觉醒);
        tag.putBoolean("lockOnSkipTeammate", 锁定跳过队友);
        tag.putBoolean("allowTeammateSearch", 允许队友搜身);
        return tag;
    }

    public void 从NBT读取(CompoundTag tag) {
        if (tag == null) return;

        if (tag.contains("friendlyFire")) {
            友伤 = tag.getBoolean("friendlyFire");
        }
        if (tag.contains("timestopAwakening")) {
            时停觉醒 = tag.getBoolean("timestopAwakening");
        }
        if (tag.contains("lockOnSkipTeammate")) {
            锁定跳过队友 = tag.getBoolean("lockOnSkipTeammate");
        }
        if (tag.contains("allowTeammateSearch")) {
            允许队友搜身 = tag.getBoolean("allowTeammateSearch");
        }
    }

    // ==================== 网络包序列化 ====================

    public void 写入Buffer(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBoolean(友伤);
        buf.writeBoolean(时停觉醒);
        buf.writeBoolean(锁定跳过队友);
        buf.writeBoolean(允许队友搜身);
    }

    public static 队伍个人配置 从Buffer读取(net.minecraft.network.FriendlyByteBuf buf) {
        队伍个人配置 config = new 队伍个人配置();
        config.友伤 = buf.readBoolean();
        config.时停觉醒 = buf.readBoolean();
        config.锁定跳过队友 = buf.readBoolean();
        config.允许队友搜身 = buf.readBoolean();
        return config;
    }
}
