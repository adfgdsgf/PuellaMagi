// 文件路径: src/main/java/com/v2t/puellamagi/system/adaptation/适应数据.java

package com.v2t.puellamagi.system.adaptation;

import com.v2t.puellamagi.api.adaptation.I适应效果;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 适应数据
 *
 * 存储玩家对某种伤害类型的适应状态
 * 与效果分离：数据存状态，效果定义行为
 */
public class 适应数据 {

    /** 适应的伤害类型 */
    private final ResourceKey<DamageType> 伤害类型;

    /** 使用的效果ID */
    private final ResourceLocation 效果ID;

    /** 免疫结束时间（游戏tick） */
    private long 免疫结束时间;

    /** 连续触发次数 */
    private int 连续触发次数;

    /** 上次触发时间 */
    private long 上次触发时间;

    public 适应数据(ResourceKey<DamageType> 伤害类型, ResourceLocation 效果ID) {
        this.伤害类型 =伤害类型;
        this.效果ID = 效果ID;
        this.免疫结束时间 = 0;
        this.连续触发次数 = 0;
        this.上次触发时间 = 0;
    }

    // ==================== Getter/Setter ====================

    public ResourceKey<DamageType> 获取伤害类型() {
        return 伤害类型;
    }

    public ResourceLocation 获取效果ID() {
        return 效果ID;
    }

    public long 获取免疫结束时间() {
        return 免疫结束时间;
    }

    public void 设置免疫结束时间(long 时间) {
        this.免疫结束时间 = 时间;
    }

    public int 获取连续触发次数() {
        return 连续触发次数;
    }

    public void 设置连续触发次数(int 次数) {
        this.连续触发次数 = 次数;
    }

    public long 获取上次触发时间() {
        return 上次触发时间;
    }

    public void 设置上次触发时间(long 时间) {
        this.上次触发时间 = 时间;
    }

    // ==================== 状态判断 ====================

    /**
     * 是否正在免疫中
     */
    public boolean 是否免疫中(long 当前时间) {
        return 当前时间 < 免疫结束时间;
    }

    /**
     * 获取剩余免疫时间（tick）
     */
    public long 获取剩余时间(long 当前时间) {
        return Math.max(0, 免疫结束时间 - 当前时间);
    }

    // ==================== NBT序列化 ====================

    public CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("damageType", 伤害类型.location().toString());
        tag.putString("effectId", 效果ID.toString());
        tag.putLong("immuneEndTime", 免疫结束时间);
        tag.putInt("consecutiveCount", 连续触发次数);
        tag.putLong("lastTriggerTime", 上次触发时间);
        return tag;
    }

    //静态工厂方法从NBT读取需要Registry，放在管理器中处理
}
