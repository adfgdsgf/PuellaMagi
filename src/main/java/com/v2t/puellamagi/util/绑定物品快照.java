// 文件路径: src/main/java/com/v2t/puellamagi/util/绑定物品快照.java

package com.v2t.puellamagi.util;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 绑定物品快照
 *
 * 用于创造模式删除检测，记录物品的关键信息
 */
public record 绑定物品快照(
        ResourceLocation 物品ID,
        UUID 所有者UUID,
        long 时间戳
) {
    /**
     * 检查是否与另一个快照匹配（同一个绑定物品）
     */
    public boolean 匹配(绑定物品快照 other) {
        if (other == null) return false;
        return this.物品ID.equals(other.物品ID)
                && this.所有者UUID.equals(other.所有者UUID)
                && this.时间戳 == other.时间戳;
    }
}
