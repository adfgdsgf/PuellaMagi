package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.util.NBT工具;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 技能槽位数据
 * 表示技能栏中的一个槽位
 */
public class 技能槽位数据 {

    private static final String KEY_SLOT_INDEX = "SlotIndex";
    private static final String KEY_SKILL_ID = "SkillId";
    private static final String KEY_X = "X";
    private static final String KEY_Y = "Y";

    private int 槽位索引;
    private ResourceLocation 技能ID;  // null表示空槽
    private int x;  // 相对位置X
    private int y;  // 相对位置Y

    /**
     * 空槽构造器
     */
    public 技能槽位数据(int 槽位索引) {
        this.槽位索引 = 槽位索引;
        this.技能ID = null;
        this.x = 0;
        this.y = 0;
    }

    /**
     * 完整构造器
     */
    public 技能槽位数据(int 槽位索引, @Nullable ResourceLocation 技能ID, int x, int y) {
        this.槽位索引 = 槽位索引;
        this.技能ID = 技能ID;
        this.x = x;
        this.y = y;
    }

    //==================== Getter ====================

    public int 获取槽位索引() {
        return 槽位索引;
    }

    @Nullable
    public ResourceLocation 获取技能ID() {
        return 技能ID;
    }

    public int 获取X() {
        return x;
    }

    public int 获取Y() {
        return y;
    }

    public boolean 是否为空() {
        return 技能ID == null;
    }

    // ==================== Setter ====================

    public void 设置技能ID(@Nullable ResourceLocation skillId) {
        this.技能ID = skillId;
    }

    public void 设置位置(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void 清空() {
        this.技能ID = null;
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_SLOT_INDEX, 槽位索引);
        NBT工具.写入资源路径(tag, KEY_SKILL_ID, 技能ID);
        tag.putInt(KEY_X, x);
        tag.putInt(KEY_Y, y);
        return tag;
    }

    public static 技能槽位数据 从NBT读取(CompoundTag tag) {
        int index = tag.getInt(KEY_SLOT_INDEX);
        ResourceLocation skillId = NBT工具.获取资源路径(tag, KEY_SKILL_ID);
        int x = tag.getInt(KEY_X);
        int y = tag.getInt(KEY_Y);
        return new 技能槽位数据(index, skillId, x, y);
    }

    // ==================== 复制 ====================

    public 技能槽位数据 复制() {
        return new 技能槽位数据(槽位索引, 技能ID, x, y);
    }
}
