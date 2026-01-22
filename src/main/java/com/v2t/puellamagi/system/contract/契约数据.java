// 文件路径: src/main/java/com/v2t/puellamagi/system/contract/契约数据.java

package com.v2t.puellamagi.system.contract;

import com.v2t.puellamagi.util.NBT工具;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 契约状态的纯数据类
 * 存储玩家的契约相关信息
 */
public class 契约数据 {

    // ==================== NBT键名 ====================

    private static final String KEY_CONTRACTED = "Contracted";
    private static final String KEY_SERIES = "Series";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_CONTRACT_TIME = "ContractTime";
    private static final String KEY_EXTRA_DATA = "ExtraData";

    // 灵魂宝石相关
    private static final String KEY_SOUL_GEM_ISSUED = "SoulGemIssued";
    private static final String KEY_SOUL_GEM_UUID = "SoulGemUUID";
    private static final String KEY_SOUL_GEM_TIMESTAMP = "SoulGemTimestamp";

    // ==================== 数据字段 ====================

    private boolean 已契约 = false;
    private ResourceLocation 系列ID = null;
    private ResourceLocation 类型ID = null;
    private long 契约时间 = 0;
    private CompoundTag 额外数据 = new CompoundTag();

    // 灵魂宝石相关
    private boolean 已发放灵魂宝石 = false;
    private UUID 灵魂宝石UUID = null;
    private long 灵魂宝石时间戳 = 0;

    // ==================== 基础 Getter ====================

    public boolean 是否已契约() {
        return 已契约;
    }

    @Nullable
    public ResourceLocation 获取系列ID() {
        return 系列ID;
    }

    @Nullable
    public ResourceLocation 获取类型ID() {
        return 类型ID;
    }

    public long 获取契约时间() {
        return 契约时间;
    }

    public CompoundTag 获取额外数据() {
        return 额外数据;
    }

    // ==================== 灵魂宝石 Getter ====================

    public boolean 是否已发放灵魂宝石() {
        return 已发放灵魂宝石;
    }

    @Nullable
    public UUID 获取灵魂宝石UUID() {
        return 灵魂宝石UUID;
    }

    public long 获取灵魂宝石时间戳() {
        return 灵魂宝石时间戳;
    }

    // ==================== 基础 Setter ====================

    public void 设置契约状态(boolean contracted) {
        this.已契约 = contracted;
    }

    public void 设置系列ID(@Nullable ResourceLocation seriesId) {
        this.系列ID = seriesId;
    }

    public void 设置类型ID(@Nullable ResourceLocation typeId) {
        this.类型ID = typeId;
    }

    public void 设置契约时间(long time) {
        this.契约时间 = time;
    }

    public void 设置额外数据(CompoundTag data) {
        this.额外数据 = data != null ? data : new CompoundTag();
    }

    // ==================== 灵魂宝石 Setter ====================

    public void 设置已发放灵魂宝石(boolean issued) {
        this.已发放灵魂宝石 = issued;
    }

    public void 设置灵魂宝石UUID(@Nullable UUID uuid) {
        this.灵魂宝石UUID = uuid;
    }

    public void 设置灵魂宝石时间戳(long timestamp) {
        this.灵魂宝石时间戳 = timestamp;
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();

        // 基础数据
        tag.putBoolean(KEY_CONTRACTED, 已契约);
        NBT工具.写入资源路径(tag, KEY_SERIES, 系列ID);
        NBT工具.写入资源路径(tag, KEY_TYPE, 类型ID);
        tag.putLong(KEY_CONTRACT_TIME, 契约时间);
        tag.put(KEY_EXTRA_DATA, 额外数据.copy());

        // 灵魂宝石数据
        tag.putBoolean(KEY_SOUL_GEM_ISSUED, 已发放灵魂宝石);
        if (灵魂宝石UUID != null) {
            tag.putUUID(KEY_SOUL_GEM_UUID, 灵魂宝石UUID);
        }
        tag.putLong(KEY_SOUL_GEM_TIMESTAMP, 灵魂宝石时间戳);

        return tag;
    }

    public void 从NBT读取(CompoundTag tag) {
        // 基础数据
        this.已契约 = NBT工具.获取Boolean(tag, KEY_CONTRACTED, false);
        this.系列ID = NBT工具.获取资源路径(tag, KEY_SERIES);
        this.类型ID = NBT工具.获取资源路径(tag, KEY_TYPE);
        this.契约时间 = tag.getLong(KEY_CONTRACT_TIME);
        this.额外数据 = tag.contains(KEY_EXTRA_DATA)
                ? tag.getCompound(KEY_EXTRA_DATA).copy()
                : new CompoundTag();

        // 灵魂宝石数据
        this.已发放灵魂宝石 = NBT工具.获取Boolean(tag, KEY_SOUL_GEM_ISSUED, false);
        this.灵魂宝石UUID = tag.hasUUID(KEY_SOUL_GEM_UUID) ? tag.getUUID(KEY_SOUL_GEM_UUID) : null;
        this.灵魂宝石时间戳 = tag.getLong(KEY_SOUL_GEM_TIMESTAMP);
    }

    // ==================== 工具方法 ====================

    /**
     * 签订契约
     */
    public void 签订(ResourceLocation seriesId, ResourceLocation typeId, long gameTime) {
        this.已契约 = true;
        this.系列ID = seriesId;
        this.类型ID = typeId;
        this.契约时间 = gameTime;
        //灵魂宝石状态不在这里设置，由灵魂宝石管理器处理
    }

    /**
     * 解除契约
     * 注意：解除契约不回收灵魂宝石物品，但清除记录
     */
    public void 解除() {
        this.已契约 = false;
        this.系列ID = null;
        this.类型ID = null;
        this.契约时间 = 0;
        this.额外数据 = new CompoundTag();

        // 清除灵魂宝石记录
        this.已发放灵魂宝石 = false;
        this.灵魂宝石UUID = null;
        this.灵魂宝石时间戳 = 0;
    }

    /**
     * 记录灵魂宝石发放
     */
    public void 记录灵魂宝石发放(UUID gemUUID, long timestamp) {
        this.已发放灵魂宝石 = true;
        this.灵魂宝石UUID = gemUUID;
        this.灵魂宝石时间戳 = timestamp;
    }

    /**
     * 更新灵魂宝石时间戳（重新生成时使用）
     */
    public void 更新灵魂宝石时间戳(long newTimestamp) {
        this.灵魂宝石时间戳 = newTimestamp;
    }

    /**
     * 复制数据（用于Clone事件）
     */
    public void 复制自(契约数据 other) {
        this.从NBT读取(other.写入NBT());
    }
}
