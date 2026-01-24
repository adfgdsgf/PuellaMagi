package com.v2t.puellamagi.system.soulgem.data;

import com.v2t.puellamagi.core.data.绑定物品登记信息;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 灵魂宝石登记信息
 *
 * 继承通用绑定物品登记信息，添加灵魂宝石特有字段：
 * - 宝石状态（正常/龟裂/损坏）
 * - 上次更新时间（用于调试）
 */
public class 宝石登记信息 extends 绑定物品登记信息 {

    // ===灵魂宝石特有字段 ===
    private 灵魂宝石状态 当前状态 = 灵魂宝石状态.NORMAL;
    private long 上次更新现实时间;
    private long 上次更新游戏时间;

    // === NBT键名（特有字段）===
    private static final String KEY_STATE = "State";
    private static final String KEY_LAST_UPDATE = "LastUpdate";
    private static final String KEY_LAST_GAME_TIME = "LastGameTime";

    /**
     * 创建新的登记信息
     */
    public 宝石登记信息(long 时间戳) {
        super(时间戳);
        this.上次更新现实时间 = System.currentTimeMillis();
        this.上次更新游戏时间 = 0;
    }

    /**
     * 从NBT创建（反序列化用）
     */
    private 宝石登记信息() {
        super();}

    // === 特有字段 Getter ===

    public 灵魂宝石状态 获取状态() {
        return 当前状态;
    }

    public long 获取上次更新时间() {
        return 上次更新现实时间;
    }

    public long 获取上次更新现实时间() {
        return 上次更新现实时间;
    }

    public long 获取上次更新游戏时间() {
        return 上次更新游戏时间;
    }

    // === 便捷方法（包装父类方法）===

    public ResourceKey<Level> 获取维度() {
        return 获取所在维度();
    }

    @Nullable
    public BlockPos 获取方块坐标() {
        Vec3 pos = 获取坐标();
        return pos != null ? BlockPos.containing(pos) : null;
    }

    public 存储类型 获取存储类型枚举() {
        String typeStr = super.获取存储类型();
        return 存储类型.从序列化名(typeStr);
    }

    @Nullable
    public UUID 获取当前持有者UUID() {
        return 获取持有者UUID();
    }

    // === Setter ===

    public void 设置状态(灵魂宝石状态 状态) {
        this.当前状态 = 状态;
        this.上次更新现实时间 = System.currentTimeMillis();
    }

    /**
     * 更新位置（带游戏时间）
     */
    public void 更新位置(ResourceKey<Level> 维度, Vec3 坐标,存储类型 存储类型, @Nullable UUID 持有者UUID, long 游戏时间) {
        super.更新位置(维度, 坐标, 存储类型.获取序列化名(), 持有者UUID);
        this.上次更新现实时间 = System.currentTimeMillis();
        this.上次更新游戏时间 = 游戏时间;
    }

    /**
     * @deprecated 请使用带游戏时间参数的版本
     */
    @Deprecated
    public void 更新位置(ResourceKey<Level> 维度, Vec3 坐标, 存储类型 存储类型, @Nullable UUID 持有者UUID) {
        更新位置(维度, 坐标, 存储类型, 持有者UUID, this.上次更新游戏时间);
    }

    @Override
    public void 更新时间戳(long 新时间戳) {
        super.更新时间戳(新时间戳);
        this.上次更新现实时间 = System.currentTimeMillis();
    }

    // === 序列化 ===

    @Override
    public CompoundTag 写入NBT() {
        CompoundTag tag = super.写入NBT();
        tag.putString(KEY_STATE, 当前状态.name());
        tag.putLong(KEY_LAST_UPDATE, 上次更新现实时间);
        tag.putLong(KEY_LAST_GAME_TIME, 上次更新游戏时间);
        return tag;
    }

    public static 宝石登记信息 从NBT读取(CompoundTag tag) {
        宝石登记信息 info = new 宝石登记信息();
        info.从NBT读取基础字段(tag);

        if (tag.contains(KEY_STATE)) {
            try {
                info.当前状态 = 灵魂宝石状态.valueOf(tag.getString(KEY_STATE));
            } catch (IllegalArgumentException e) {
                info.当前状态 = 灵魂宝石状态.NORMAL;
            }
        }

        info.上次更新现实时间 = tag.getLong(KEY_LAST_UPDATE);
        info.上次更新游戏时间 = tag.getLong(KEY_LAST_GAME_TIME);

        return info;
    }

    @Override
    public String toString() {
        return String.format("宝石登记[时间戳=%d, 状态=%s, 维度=%s, 坐标=%s, 存储=%s, 持有者=%s]",
                获取有效时间戳(), 当前状态, 获取所在维度(), 获取坐标(), 获取存储类型(),
                获取持有者UUID() != null ? 获取持有者UUID().toString().substring(0, 8) : "无");
    }
}
