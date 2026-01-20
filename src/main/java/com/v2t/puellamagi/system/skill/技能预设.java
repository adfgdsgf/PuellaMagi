package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.常量;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能预设
 * 包含多个技能槽位和布局配置
 */
public class 技能预设 {

    private static final String KEY_NAME = "Name";
    private static final String KEY_SLOTS = "Slots";
    private static final String KEY_LAYOUT = "Layout";

    private String 名称;
    private List<技能槽位数据> 槽位列表;
    private 布局配置 布局;

    /**
     * 创建默认预设
     */
    public 技能预设(String 名称) {
        this.名称 = 名称;
        this.槽位列表 = new ArrayList<>();
        this.布局 = new 布局配置();

        // 创建默认槽位
        for (int i = 0; i < 常量.默认技能槽位数; i++) {
            槽位列表.add(new 技能槽位数据(i));
        }
    }

    /**
     * 完整构造器
     */
    public 技能预设(String 名称, List<技能槽位数据> 槽位列表, 布局配置 布局) {
        this.名称 = 名称;
        this.槽位列表 = 槽位列表 != null ? 槽位列表 : new ArrayList<>();
        this.布局 = 布局 != null ? 布局 : new 布局配置();
    }

    // ==================== Getter ====================

    public String 获取名称() {
        return 名称;
    }

    public List<技能槽位数据> 获取槽位列表() {
        return 槽位列表;
    }

    public 布局配置 获取布局() {
        return 布局;
    }

    public int 获取槽位数量() {
        return 槽位列表.size();
    }

    /**
     * 获取指定索引的槽位
     */
    @Nullable
    public 技能槽位数据 获取槽位(int index) {
        if (index < 0 || index >= 槽位列表.size()) {
            return null;
        }
        return 槽位列表.get(index);
    }

    // ==================== Setter ====================

    public void 设置名称(String 名称) {
        this.名称 = 名称;
    }

    public void 设置布局(布局配置 布局) {
        this.布局 = 布局;
    }

    /**
     * 设置槽位技能
     */
    public void 设置槽位技能(int slotIndex, @Nullable ResourceLocation skillId) {
        技能槽位数据 slot = 获取槽位(slotIndex);
        if (slot != null) {
            slot.设置技能ID(skillId);
        }
    }

    /**
     * 添加新槽位
     */
    public void 添加槽位() {
        int newIndex = 槽位列表.size();
        槽位列表.add(new 技能槽位数据(newIndex));
    }

    /**
     * 移除最后一个槽位
     */
    public void 移除最后槽位() {
        if (!槽位列表.isEmpty()) {
            槽位列表.remove(槽位列表.size() - 1);
        }
    }

    /**
     * 清空所有槽位的技能
     */
    public void 清空所有技能() {
        for (技能槽位数据 slot : 槽位列表) {
            slot.清空();
        }
    }

    // ==================== 序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_NAME, 名称);

        // 保存槽位列表
        ListTag slotsTag = new ListTag();
        for (技能槽位数据 slot : 槽位列表) {
            slotsTag.add(slot.写入NBT());
        }
        tag.put(KEY_SLOTS, slotsTag);

        // 保存布局
        tag.put(KEY_LAYOUT, 布局.写入NBT());

        return tag;
    }

    public static 技能预设 从NBT读取(CompoundTag tag) {
        String name = tag.getString(KEY_NAME);

        // 读取槽位列表
        List<技能槽位数据> slots = new ArrayList<>();
        ListTag slotsTag = tag.getList(KEY_SLOTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < slotsTag.size(); i++) {
            slots.add(技能槽位数据.从NBT读取(slotsTag.getCompound(i)));
        }

        // 读取布局
        布局配置 layout = 布局配置.从NBT读取(tag.getCompound(KEY_LAYOUT));

        return new 技能预设(name, slots, layout);
    }

    // ==================== 复制 ====================

    public 技能预设 复制() {
        List<技能槽位数据> slotsCopy = new ArrayList<>();
        for (技能槽位数据 slot : 槽位列表) {
            slotsCopy.add(slot.复制());
        }
        return new 技能预设(名称, slotsCopy, 布局.复制());
    }
}
