package com.v2t.puellamagi.system.transformation;

import com.v2t.puellamagi.util.NBT工具;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 变身状态的纯数据类
 * 存储玩家的变身相关信息
 */
public class 变身数据 {

    // ==================== NBT键名 ====================
    private static final String KEY_TRANSFORMED = "Transformed";
    private static final String KEY_GIRL_TYPE = "GirlType";
    private static final String KEY_MODEL = "Model";
    private static final String KEY_ABILITY_DATA = "AbilityData";
    private static final String KEY_SERIES = "Series";
    private static final String KEY_GROWTH_STAGE = "GrowthStage";
    private static final String KEY_GROWTH_DATA = "GrowthData";

    // ==================== 基础变身数据 ====================
    private boolean 已变身 = false;
    private ResourceLocation 少女类型 = null;
    private ResourceLocation 当前模型 = null;
    private CompoundTag 能力数据 = new CompoundTag();  // 固有能力专属存储

    // ==================== 系列与成长数据 ====================
    private ResourceLocation 所属系列 = null;          // 如puellamagi:heart_seed
    private int 当前阶段索引 = 0;                       // 成长阶段：0=种, 1=芽, 2=叶, 3=蕾, 4=花
    private CompoundTag 成长数据 = new CompoundTag();  // 系列专属成长数据

    // ==================== Getter ====================

    public boolean 是否已变身() {
        return 已变身;
    }

    @Nullable
    public ResourceLocation 获取少女类型() {
        return 少女类型;
    }

    @Nullable
    public ResourceLocation 获取模型() {
        return 当前模型;
    }

    public CompoundTag 获取能力数据() {
        return 能力数据;
    }

    @Nullable
    public ResourceLocation 获取所属系列() {
        return 所属系列;
    }

    public int 获取当前阶段索引() {
        return 当前阶段索引;
    }

    public CompoundTag 获取成长数据() {
        return 成长数据;
    }

    // ==================== Setter ====================

    public void 设置变身状态(boolean transformed) {
        this.已变身 = transformed;
    }

    public void 设置少女类型(@Nullable ResourceLocation typeId) {
        this.少女类型 = typeId;
    }

    public void 设置模型(@Nullable ResourceLocation modelId) {
        this.当前模型 = modelId;
    }

    public void 设置能力数据(CompoundTag data) {
        this.能力数据 = data != null ? data : new CompoundTag();
    }

    public void 设置所属系列(@Nullable ResourceLocation seriesId) {
        this.所属系列 = seriesId;
    }

    public void 设置当前阶段索引(int index) {
        this.当前阶段索引 = Math.max(0, index);
    }

    public void 设置成长数据(CompoundTag data) {
        this.成长数据 = data != null ? data : new CompoundTag();
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        // 基础数据
        tag.putBoolean(KEY_TRANSFORMED, 已变身);
        NBT工具.写入资源路径(tag, KEY_GIRL_TYPE, 少女类型);
        NBT工具.写入资源路径(tag, KEY_MODEL, 当前模型);
        tag.put(KEY_ABILITY_DATA, 能力数据.copy());
        // 系列与成长数据
        NBT工具.写入资源路径(tag, KEY_SERIES, 所属系列);
        tag.putInt(KEY_GROWTH_STAGE, 当前阶段索引);
        tag.put(KEY_GROWTH_DATA, 成长数据.copy());
        return tag;
    }

    public void 从NBT读取(CompoundTag tag) {
        // 基础数据
        this.已变身 = NBT工具.获取Boolean(tag, KEY_TRANSFORMED, false);
        this.少女类型 = NBT工具.获取资源路径(tag, KEY_GIRL_TYPE);
        this.当前模型 = NBT工具.获取资源路径(tag, KEY_MODEL);
        this.能力数据 = tag.contains(KEY_ABILITY_DATA)? tag.getCompound(KEY_ABILITY_DATA).copy()
                : new CompoundTag();
        // 系列与成长数据
        this.所属系列 = NBT工具.获取资源路径(tag, KEY_SERIES);
        this.当前阶段索引 = NBT工具.获取Int(tag, KEY_GROWTH_STAGE, 0);
        this.成长数据 = tag.contains(KEY_GROWTH_DATA)
                ? tag.getCompound(KEY_GROWTH_DATA).copy()
                : new CompoundTag();
    }

    // ==================== 工具方法 ====================

    /**
     * 重置变身状态（解除变身时调用）
     * 保留类型、模型、系列、成长进度
     */
    public void 重置() {
        this.已变身 = false;}

    /**
     * 完全清空（切换角色类型时调用）
     */
    public void 清空() {
        this.已变身 = false;
        this.少女类型 = null;
        this.当前模型 = null;
        this.能力数据 = new CompoundTag();
        this.所属系列 = null;
        this.当前阶段索引 = 0;
        this.成长数据 = new CompoundTag();
    }

    /**
     * 复制数据（用于Clone事件）
     */
    public void 复制自(变身数据 other) {
        this.从NBT读取(other.写入NBT());
    }

    /**
     * 尝试进阶到下一阶段
     * @return 是否成功进阶
     */
    public boolean 尝试进阶() {
        // 实际进阶逻辑由成长机制处理，这里只更新索引
        this.当前阶段索引++;
        return true;
    }
}
