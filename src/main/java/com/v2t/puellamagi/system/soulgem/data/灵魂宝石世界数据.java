// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/data/灵魂宝石世界数据.java

package com.v2t.puellamagi.system.soulgem.data;

import com.v2t.puellamagi.core.data.ModWorldData;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石状态;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 灵魂宝石世界数据
 *
 * 作为所有灵魂宝石的"唯一真实数据源"
 * 存储位置：save/data/puellamagi_soulgem.dat
 *
 * 核心职责：
 * 1. 登记玩家的灵魂宝石信息
 * 2. 追踪宝石位置（由物品主动汇报）
 * 3. 提供唯一性校验（时间戳机制）
 */
public class 灵魂宝石世界数据 extends ModWorldData {

    private static final String DATA_ID = "puellamagi_soulgem";
    private static final String KEY_ENTRIES = "Entries";

    /**
     * 玩家UUID -> 宝石登记信息
     */
    private final Map<UUID, 宝石登记信息> 登记表 = new HashMap<>();

    /**
     * 获取世界数据实例
     */
    public static 灵魂宝石世界数据 获取(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                tag -> {灵魂宝石世界数据 data = new 灵魂宝石世界数据();
                    data.从NBT加载(tag);
                    return data;
                },
                灵魂宝石世界数据::new,
                DATA_ID
        );
    }

    // === 核心API===

    /**
     * 登记新的灵魂宝石
     *
     * @param 玩家UUID 所有者UUID
     * @param 时间戳 创建时间戳，用于唯一性校验
     * @return 创建的登记信息
     */
    public 宝石登记信息 登记宝石(UUID 玩家UUID, long 时间戳) {
        宝石登记信息 info = new 宝石登记信息(时间戳);
        登记表.put(玩家UUID, info);
        标记已修改();LOGGER.debug("登记灵魂宝石: 玩家={}, 时间戳={}", 玩家UUID, 时间戳);
        return info;
    }

    /**
     * 获取玩家的宝石登记信息
     */
    public Optional<宝石登记信息> 获取登记信息(UUID 玩家UUID) {
        return Optional.ofNullable(登记表.get(玩家UUID));
    }

    /**
     * 检查玩家是否有登记的灵魂宝石
     */
    public boolean 存在登记(UUID 玩家UUID) {
        return 登记表.containsKey(玩家UUID);
    }

    /**
     * 更新宝石位置（带持有者）
     *
     * @param 玩家UUID 宝石所有者UUID
     * @param 维度 所在维度
     * @param 坐标 所在坐标
     * @param 存储类型 存储类型
     * @param 持有者UUID 当前持有者UUID，掉落物/容器时为null
     */
    public void 更新位置(UUID 玩家UUID, ResourceKey<Level> 维度, Vec3 坐标, 存储类型 存储类型, @Nullable UUID 持有者UUID) {宝石登记信息 info = 登记表.get(玩家UUID);
        if (info != null) {
            info.更新位置(维度, 坐标, 存储类型,持有者UUID);
            标记已修改();
        }
    }

    /**
     * 更新宝石状态
     */
    public void 更新状态(UUID 玩家UUID, 灵魂宝石状态 状态) {
        宝石登记信息 info = 登记表.get(玩家UUID);
        if (info != null) {
            info.设置状态(状态);
            标记已修改();LOGGER.debug("更新灵魂宝石状态: 玩家={}, 状态={}", 玩家UUID, 状态);
        }
    }

    /**
     * 更新时间戳（重新生成宝石时使用）
     */
    public void 更新时间戳(UUID 玩家UUID, long 新时间戳) {
        宝石登记信息 info = 登记表.get(玩家UUID);
        if (info != null) {
            info.更新时间戳(新时间戳);
            标记已修改();
            LOGGER.debug("更新灵魂宝石时间戳: 玩家={}, 新时间戳={}", 玩家UUID, 新时间戳);
        }
    }

    /**
     * 移除登记（玩家死亡/解除契约时）
     */
    public void 移除登记(UUID 玩家UUID) {
        if (登记表.remove(玩家UUID) != null) {
            标记已修改();
            LOGGER.debug("移除灵魂宝石登记: 玩家={}", 玩家UUID);
        }
    }

    /**
     * 验证时间戳是否有效
     * 用于判断物品是否为当前有效的灵魂宝石
     */
    public boolean 验证时间戳(UUID 玩家UUID, long 物品时间戳) {
        宝石登记信息 info = 登记表.get(玩家UUID);
        if (info == null) {
            return false;
        }
        return info.获取有效时间戳() == 物品时间戳;
    }

    // === 序列化 ===

    @Override
    protected void 从NBT加载(CompoundTag tag) {
        登记表.clear();

        if (tag.contains(KEY_ENTRIES)) {
            CompoundTag entries = tag.getCompound(KEY_ENTRIES);
            for (String uuidStr : entries.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    宝石登记信息 info = 宝石登记信息.从NBT读取(entries.getCompound(uuidStr));
                    登记表.put(uuid, info);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("无法解析UUID: {}", uuidStr);
                }
            }
        }LOGGER.info("加载灵魂宝石数据: {} 条记录", 登记表.size());
    }

    @Override
    protected CompoundTag 写入NBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag entries = new CompoundTag();

        for (Map.Entry<UUID, 宝石登记信息> entry : 登记表.entrySet()) {
            entries.put(entry.getKey().toString(), entry.getValue().写入NBT());
        }

        tag.put(KEY_ENTRIES, entries);
        return tag;
    }
}
