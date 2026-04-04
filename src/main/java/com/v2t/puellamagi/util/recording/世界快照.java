package com.v2t.puellamagi.util.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 世界快照
 *
 * 保存某一时刻的完整世界状态（实体+方块）
 * 用于回滚世界到快照时刻
 *
 * 注意：只保存指定范围内的实体和被记录的方块变化位置
 * 不会保存整个世界（那不现实）
 *
 * 复用场景：预知回滚、时间倒流、存档点恢复
 */
public class 世界快照 {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/WorldSnapshot");

    /**拍摄时的游戏时间 */
    private final long 游戏时间;

    /** 实体快照表：UUID → 快照 */
    private final Map<UUID, 实体快照> 实体表;

    /** 方块快照列表 */
    private final List<方块快照> 方块列表;

    // ==================== 构造 ====================

    public 世界快照(long gameTime) {
        this.游戏时间 = gameTime;
        this.实体表 = new HashMap<>();
        this.方块列表 = new ArrayList<>();
    }

    // ==================== 采集 ====================

    /**
     * 添加实体快照
     */
    public void 添加实体(实体快照 snapshot) {
        实体表.put(snapshot.获取UUID(), snapshot);
    }

    /**
     * 添加方块快照
     */
    public void 添加方块(方块快照 snapshot) {
        方块列表.add(snapshot);
    }

    /**
     * 从世界采集指定范围内的所有实体
     *
     * @param level  世界
     * @param center 中心点
     * @param range  范围（方块）
     */
    public void 采集范围内实体(ServerLevel level, net.minecraft.world.phys.Vec3 center, double range) {
        AABB box = AABB.ofSize(center, range * 2, range * 2, range * 2);

        for (Entity entity : level.getEntities((Entity) null, box, e -> true)) {
            实体表.put(entity.getUUID(), 实体快照.从实体创建(entity));
        }LOGGER.debug("世界快照采集完成：{} 个实体", 实体表.size());
    }

    /**
     * 采集范围内所有方块实体
     *
     * 遍历范围内已加载的区块，保存所有BlockEntity的NBT
     * 用于回滚时恢复容器内容（箱子/漏斗/熔炉等）
     */
    public void 采集范围内方块实体(ServerLevel level, net.minecraft.world.phys.Vec3 center, double range) {
        int minX = (int) Math.floor(center.x - range);
        int maxX = (int) Math.floor(center.x + range);
        int minZ = (int) Math.floor(center.z - range);
        int maxZ = (int) Math.floor(center.z + range);

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        int count = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;

                var chunk = level.getChunk(cx, cz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();

                    // 范围检查
                    double dx = pos.getX() - center.x;
                    double dy = pos.getY() - center.y;
                    double dz = pos.getZ() - center.z;
                    if (dx * dx + dy * dy + dz * dz > range * range) continue;

                    // 已经有了就跳过（可能被方块变化提前记录了）
                    if (包含方块(pos)) continue;

                    net.minecraft.world.level.block.entity.BlockEntity be = entry.getValue();
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                    net.minecraft.nbt.CompoundTag nbt = be.saveWithoutMetadata();

                    添加方块(new 方块快照(pos, state, nbt));
                    count++;
                }
            }
        }

        LOGGER.debug("方块实体快照采集完成：{} 个", count);
    }

    // ==================== 回滚 ====================

    /**
     * 将快照恢复到世界
     *
     * @param level 目标世界
     * @return 恢复的实体数 + 方块数
     */
    public int[] 恢复到(ServerLevel level) {
        int 实体恢复数 = 0;
        int 方块恢复数 = 0;

        //恢复方块（先恢复方块，再恢复实体，避免实体卡方块）
        for (方块快照 block : 方块列表) {
            if (block.恢复到(level)) {
                方块恢复数++;
            }
        }

        // 恢复实体
        for (实体快照 snapshot : 实体表.values()) {
            Entity entity = level.getEntity(findEntityByUUID(level, snapshot.获取UUID()));
            if (entity != null) {
                if (snapshot.恢复到(entity)) {
                    实体恢复数++;
                }
            }
        }

        LOGGER.debug("世界快照恢复完成：{} 个实体, {} 个方块", 实体恢复数, 方块恢复数);
        return new int[]{实体恢复数, 方块恢复数};
    }

    /**
     * 通过UUID查找实体的数字ID
     * MC中 ServerLevel.getEntity(int) 需要数字ID
     */
    private int findEntityByUUID(ServerLevel level, UUID uuid) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getUUID().equals(uuid)) {
                return entity.getId();
            }
        }
        return -1;
    }

    /**
     * 检查是否已包含指定位置的方块快照
     */
    public boolean 包含方块(BlockPos pos) {
        for (方块快照 block : 方块列表) {
            if (block.获取位置().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 查询 ====================

    public long 获取游戏时间() { return 游戏时间; }

    public int 实体数量() { return 实体表.size(); }

    public int 方块数量() { return 方块列表.size(); }

    @Nullable
    public 实体快照 获取实体快照(UUID uuid) {
        return 实体表.get(uuid);
    }

    public Collection<实体快照> 获取所有实体快照() {
        return Collections.unmodifiableCollection(实体表.values());
    }

    public List<方块快照> 获取所有方块快照() {
        return Collections.unmodifiableList(方块列表);
    }

    /**
     * 获取实体的完整NBT（时间删除结算用，复活被杀实体）
     */
    @Nullable
    public net.minecraft.nbt.CompoundTag 获取实体NBT(UUID uuid) {
        实体快照 snapshot = 实体表.get(uuid);
        if (snapshot == null) return null;
        return snapshot.获取NBT();
    }

    /**
     * 检查是否包含指定实体的快照
     */
    public boolean 包含实体(UUID uuid) {
        return 实体表.containsKey(uuid);
    }

    /**
     * 获取所有实体的UUID集合
     * 用于判断"录制前就存在的实体"
     */
    public Set<UUID> 获取所有实体UUID() {
        return Collections.unmodifiableSet(实体表.keySet());
    }
}
