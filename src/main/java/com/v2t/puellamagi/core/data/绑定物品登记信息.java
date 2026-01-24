package com.v2t.puellamagi.core.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 绑定物品登记信息基类
 *
 * 存储绑定物品在世界数据中的通用信息：
 * - 有效时间戳（唯一性校验）
 * - 当前位置（维度+坐标）
 * - 存储类型（背包/掉落物/容器）
 * - 当前持有者
 *
 * 子类可扩展额外字段（如灵魂宝石的状态）
 */
public class 绑定物品登记信息 {

    // NBT键名
    protected static final String KEY_TIMESTAMP = "Timestamp";
    protected static final String KEY_DIMENSION = "Dimension";
    protected static final String KEY_POS_X = "PosX";
    protected static final String KEY_POS_Y = "PosY";
    protected static final String KEY_POS_Z = "PosZ";
    protected static final String KEY_STORAGE_TYPE = "StorageType";
    protected static final String KEY_HOLDER_UUID = "HolderUUID";

    //唯一性校验
    protected long 有效时间戳;

    // 位置追踪
    @Nullable
    protected ResourceKey<Level> 所在维度;
    @Nullable
    protected Vec3 坐标;
    protected String 存储类型名称 = "unknown";  // ← 改名：存储类型 → 存储类型名称
    @Nullable
    protected UUID 持有者UUID;

    public 绑定物品登记信息(long 时间戳) {
        this.有效时间戳 = 时间戳;
    }

    protected 绑定物品登记信息() {
        // 用于反序列化
    }

    // ==================== Getter ====================

    public long 获取有效时间戳() {
        return 有效时间戳;
    }

    @Nullable
    public ResourceKey<Level> 获取所在维度() {
        return 所在维度;
    }

    @Nullable
    public Vec3 获取坐标() {
        return 坐标;
    }

    public String 获取存储类型() {
        return 存储类型名称;  // ← 对应修改
    }

    @Nullable
    public UUID 获取持有者UUID() {
        return 持有者UUID;
    }

    /**
     * 检查位置是否已知
     */
    public boolean 位置已知() {
        return 所在维度 != null && 坐标 != null;
    }

    // ==================== Setter ====================

    /**
     * 更新位置信息
     */
    public void 更新位置(ResourceKey<Level> 维度, Vec3 坐标, String 存储类型, @Nullable UUID 持有者UUID) {
        this.所在维度 = 维度;
        this.坐标 = 坐标;
        this.存储类型名称 = 存储类型;  // ← 对应修改
        this.持有者UUID = 持有者UUID;
    }

    /**
     * 更新时间戳（重新生成物品时使用）
     */
    public void 更新时间戳(long 新时间戳) {
        this.有效时间戳 = 新时间戳;
    }

    /**
     * 清除位置信息（位置未知时）
     */
    public void 清除位置() {
        this.所在维度 = null;
        this.坐标 = null;
        this.存储类型名称 = "unknown";// ← 对应修改
        this.持有者UUID = null;
    }

    // ==================== 序列化 ====================

    /**
     * 写入NBT（子类可重写以添加额外字段）
     */
    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(KEY_TIMESTAMP, 有效时间戳);

        if (所在维度 != null) {
            tag.putString(KEY_DIMENSION, 所在维度.location().toString());
        }
        if (坐标 != null) {
            tag.putDouble(KEY_POS_X, 坐标.x);
            tag.putDouble(KEY_POS_Y, 坐标.y);
            tag.putDouble(KEY_POS_Z, 坐标.z);
        }
        tag.putString(KEY_STORAGE_TYPE, 存储类型名称);  // ← 对应修改
        if (持有者UUID != null) {
            tag.putUUID(KEY_HOLDER_UUID, 持有者UUID);
        }

        return tag;
    }

    /**
     * 从NBT读取基础字段（子类调用后再读取自己的字段）
     */
    protected void 从NBT读取基础字段(CompoundTag tag) {
        this.有效时间戳 = tag.getLong(KEY_TIMESTAMP);

        if (tag.contains(KEY_DIMENSION)) {
            ResourceLocation dimLoc = new ResourceLocation(tag.getString(KEY_DIMENSION));
            this.所在维度 = ResourceKey.create(Registries.DIMENSION, dimLoc);
        }
        if (tag.contains(KEY_POS_X)) {
            this.坐标 = new Vec3(
                    tag.getDouble(KEY_POS_X),
                    tag.getDouble(KEY_POS_Y),
                    tag.getDouble(KEY_POS_Z)
            );
        }
        this.存储类型名称 = tag.getString(KEY_STORAGE_TYPE);  // ← 对应修改
        if (tag.hasUUID(KEY_HOLDER_UUID)) {
            this.持有者UUID = tag.getUUID(KEY_HOLDER_UUID);
        }
    }

    /**
     * 从NBT创建实例（子类需要重写此方法）
     */
    public static 绑定物品登记信息 从NBT读取(CompoundTag tag) {
        绑定物品登记信息 info = new 绑定物品登记信息();
        info.从NBT读取基础字段(tag);
        return info;
    }
}
