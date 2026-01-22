package com.v2t.puellamagi.core.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用世界数据基类
 *
 * 使用方式：
 * 1. 继承此类，实现 从NBT加载() 和 写入NBT()
 * 2. 提供静态获取方法，调用 获取数据()
 *
 * 存储位置：save/data/{dataId}.dat
 */
public abstract class ModWorldData extends SavedData {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ModWorldData.class);

    /**
     * 从NBT加载数据
     * 子类实现具体的反序列化逻辑
     */
    protected abstract void 从NBT加载(CompoundTag tag);

    /**
     * 写入NBT
     * 子类实现具体的序列化逻辑
     */
    protected abstract CompoundTag 写入NBT();

    @Override
    public CompoundTag save(CompoundTag tag) {
        return 写入NBT();
    }

    /**
     * 标记数据已修改，需要保存
     * 对外暴露的方法，子类修改数据后调用
     */
    public void 标记已修改() {
        setDirty();
    }

    /**
     * 通用获取方法
     *
     * @param server 服务器实例
     * @param dataId 数据ID，如 "puellamagi_soulgem"
     * @param factory 工厂函数，用于创建新实例
     * @param loader 加载函数，用于从NBT创建实例
     * @return 世界数据实例
     */
    protected static <T extends ModWorldData> T 获取数据(MinecraftServer server,
                                                         String dataId,
                                                         java.util.function.Supplier<T> factory,
                                                         java.util.function.Function<CompoundTag, T> loader) {

        return server.overworld().getDataStorage().computeIfAbsent(tag -> {
                    T instance = factory.get();
                    instance.从NBT加载(tag);
                    return instance;
                },
                factory,
                dataId
        );
    }
}
