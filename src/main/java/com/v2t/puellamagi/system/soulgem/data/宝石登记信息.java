// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/data/宝石登记信息.java

package com.v2t.puellamagi.system.soulgem.data;

import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 单个灵魂宝石的登记信息
 *
 * 存储在世界数据中，作为"唯一真实数据源"
 * 物品NBT中的数据只是校验用的副本
 */
public class 宝石登记信息 {

    // === 核心唯一性字段 ===
    private long 有效时间戳;

    // === 状态 ===
    private 灵魂宝石状态 当前状态 = 灵魂宝石状态.NORMAL;

    // === 位置信息 ===
    private ResourceKey<Level> 所在维度;
    private Vec3 所在坐标;
    private 存储类型 当前存储类型 = 存储类型.未知;

    // === 持有者信息（新增）===
    @Nullable
    private UUID 当前持有者UUID;

    // === 时间记录 ===
    private long 上次更新时间;

    // === NBT键名 ===
    private static final String KEY_TIMESTAMP = "Timestamp";
    private static final String KEY_STATE = "State";
    private static final String KEY_DIMENSION = "Dimension";
    private static final String KEY_POS_X = "PosX";
    private static final String KEY_POS_Y = "PosY";
    private static final String KEY_POS_Z = "PosZ";
    private static final String KEY_STORAGE_TYPE = "StorageType";
    private static final String KEY_HOLDER_UUID = "HolderUUID";
    private static final String KEY_LAST_UPDATE = "LastUpdate";

    /**
     * 创建新的登记信息
     */
    public 宝石登记信息(long 时间戳) {
        this.有效时间戳 = 时间戳;
        this.上次更新时间 = System.currentTimeMillis();
    }

    /**
     * 从NBT创建（反序列化用）
     */
    private 宝石登记信息() {}

    // === Getter ===

    public long 获取有效时间戳() {
        return 有效时间戳;
    }

    public 灵魂宝石状态 获取状态() {
        return 当前状态;
    }

    public ResourceKey<Level> 获取维度() {
        return 所在维度;
    }

    public Vec3 获取坐标() {
        return 所在坐标;
    }

    public BlockPos 获取方块坐标() {
        return 所在坐标 != null ? BlockPos.containing(所在坐标) : null;
    }

    public 存储类型 获取存储类型() {
        return 当前存储类型;
    }

    /**
     * 获取当前持有者UUID
     *
     * @return 持有者UUID，如果是掉落物/容器则为null
     */
    @Nullable
    public UUID 获取当前持有者UUID() {
        return 当前持有者UUID;
    }

    public long 获取上次更新时间() {
        return 上次更新时间;
    }

    // === Setter（修改后需要调用世界数据的标记已修改）===

    public void 设置状态(灵魂宝石状态 状态) {
        this.当前状态 = 状态;
        this.上次更新时间 = System.currentTimeMillis();
    }

    /**
     * 更新位置（带持有者）
     *
     * @param 维度 所在维度
     * @param 坐标 所在坐标
     * @param 存储类型 存储类型
     * @param 持有者UUID 当前持有者UUID，掉落物/容器时为null
     */
    public void 更新位置(ResourceKey<Level> 维度, Vec3 坐标, 存储类型 存储类型, @Nullable UUID 持有者UUID) {
        this.所在维度 = 维度;
        this.所在坐标 = 坐标;
        this.当前存储类型 = 存储类型;
        this.当前持有者UUID = 持有者UUID;
        this.上次更新时间 = System.currentTimeMillis();
    }

    public void 更新时间戳(long 新时间戳) {
        this.有效时间戳 = 新时间戳;
        this.上次更新时间 = System.currentTimeMillis();
    }

    // === 序列化 ===

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();

        tag.putLong(KEY_TIMESTAMP, 有效时间戳);
        tag.putString(KEY_STATE, 当前状态.name());

        if (所在维度 != null) {
            tag.putString(KEY_DIMENSION, 所在维度.location().toString());
        }

        if (所在坐标 != null) {
            tag.putDouble(KEY_POS_X, 所在坐标.x);
            tag.putDouble(KEY_POS_Y, 所在坐标.y);
            tag.putDouble(KEY_POS_Z, 所在坐标.z);
        }

        tag.putString(KEY_STORAGE_TYPE, 当前存储类型.获取序列化名());

        if (当前持有者UUID != null) {
            tag.putUUID(KEY_HOLDER_UUID, 当前持有者UUID);
        }

        tag.putLong(KEY_LAST_UPDATE, 上次更新时间);

        return tag;
    }

    public static 宝石登记信息 从NBT读取(CompoundTag tag) {
        宝石登记信息 info = new 宝石登记信息();

        info.有效时间戳 = tag.getLong(KEY_TIMESTAMP);

        if (tag.contains(KEY_STATE)) {
            try {
                info.当前状态 = 灵魂宝石状态.valueOf(tag.getString(KEY_STATE));
            } catch (IllegalArgumentException e) {
                info.当前状态 = 灵魂宝石状态.NORMAL;
            }
        }

        if (tag.contains(KEY_DIMENSION)) {
            String dimStr = tag.getString(KEY_DIMENSION);
            ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
            if (dimLoc != null) {
                info.所在维度 = ResourceKey.create(Registries.DIMENSION, dimLoc);
            }
        }

        if (tag.contains(KEY_POS_X)) {
            info.所在坐标 = new Vec3(tag.getDouble(KEY_POS_X),
                    tag.getDouble(KEY_POS_Y),
                    tag.getDouble(KEY_POS_Z)
            );
        }

        info.当前存储类型 = 存储类型.从序列化名(tag.getString(KEY_STORAGE_TYPE));

        if (tag.hasUUID(KEY_HOLDER_UUID)) {
            info.当前持有者UUID = tag.getUUID(KEY_HOLDER_UUID);
        }

        info.上次更新时间 = tag.getLong(KEY_LAST_UPDATE);

        return info;
    }

    @Override
    public String toString() {
        return String.format("宝石登记[时间戳=%d, 状态=%s, 维度=%s, 坐标=%s, 存储=%s, 持有者=%s]",
                有效时间戳, 当前状态, 所在维度, 所在坐标, 当前存储类型,
                当前持有者UUID != null ? 当前持有者UUID.toString().substring(0, 8) : "无");
    }
}
