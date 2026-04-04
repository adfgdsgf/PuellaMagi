package com.v2t.puellamagi.system.ability.epitaph;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 影响标记表
 *
 * 时间删除用：追踪使用者产生的所有物品/方块的来源
 * 传染式标记：A放的方块 → B破坏 → 掉落物标记A → B捡起 → 物品标记A
 * 结算时：标记=A的全部清除
 *
 * 三种标记：
 * 1. 方块标记：Map<BlockPos, UUID> → 帧方块保底放方块时写入
 * 2. 掉落物标记：Map<Integer, UUID> → 方块被破坏时掉落物继承
 * 3. 物品标记：ItemStack的NBT隐藏tag → 跟着物品走不丢失
 */
public class 影响标记表 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/ImpactMarker");

    /** NBT隐藏tag名*/
    public static final String TAG_KEY = "puellamagi:time_deletion_source";

    //==================== 方块标记 ====================

    private final Map<BlockPos, UUID> 方块来源 = new HashMap<>();

    public void 标记方块(BlockPos pos, UUID sourceUUID) {
        方块来源.put(pos.immutable(), sourceUUID);
    }

    public void 清除方块标记(BlockPos pos) {
        方块来源.remove(pos);
    }

    @Nullable
    public UUID 获取方块来源(BlockPos pos) {
        return 方块来源.get(pos);
    }

    public boolean 方块有标记(BlockPos pos) {
        return 方块来源.containsKey(pos);
    }

    /**
     * 获取指定来源的所有方块位置（结算用）
     */
    public List<BlockPos> 获取来源方块(UUID sourceUUID) {
        List<BlockPos> result = new ArrayList<>();
        for (Map.Entry<BlockPos, UUID> entry : 方块来源.entrySet()) {
            if (entry.getValue().equals(sourceUUID)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ==================== 掉落物标记 ====================

    private final Map<Integer, UUID> 掉落物来源 = new HashMap<>();

    public void 标记掉落物(int entityId, UUID sourceUUID) {
        掉落物来源.put(entityId, sourceUUID);
    }

    public void 清除掉落物标记(int entityId) {
        掉落物来源.remove(entityId);
    }

    @Nullable
    public UUID 获取掉落物来源(int entityId) {
        return 掉落物来源.get(entityId);
    }

    public boolean 掉落物有标记(int entityId) {
        return 掉落物来源.containsKey(entityId);
    }

    // ==================== 物品NBT标记（静态工具方法） ====================

    /**
     * 给物品打上来源标记
     */
    public static void 标记物品(ItemStack stack, UUID sourceUUID) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_KEY, sourceUUID.toString());
    }

    /**
     * 获取物品的来源标记
     */
    @Nullable
    public static UUID 获取物品来源(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_KEY)) return null;
        try {
            return UUID.fromString(tag.getString(TAG_KEY));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 物品是否有来源标记
     */
    public static boolean 物品有标记(ItemStack stack) {
        return 获取物品来源(stack) != null;
    }

    /**
     * 物品是否来自指定来源
     */
    public static boolean 物品来自(ItemStack stack, UUID sourceUUID) {
        UUID source = 获取物品来源(stack);
        return source != null && source.equals(sourceUUID);
    }

    /**
     * 移除物品的来源标记
     */
    public static void 移除物品标记(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_KEY);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    // ==================== 清理====================

    public void 清除全部() {
        方块来源.clear();
        掉落物来源.clear();
    }

    public int 获取方块标记数() { return 方块来源.size(); }
    public int 获取掉落物标记数() { return 掉落物来源.size(); }
}
