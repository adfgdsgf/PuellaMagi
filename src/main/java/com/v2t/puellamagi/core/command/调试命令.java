package com.v2t.puellamagi.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.v2t.puellamagi.mixin.access.ChunkMapInvoker;
import com.v2t.puellamagi.system.soulgem.data.宝石登记信息;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 开发调试用命令 - ChunkMap 追踪测试
 */
public class 调试命令 {

    private static final int OP_LEVEL = 2;

    // 记录原位置
    private static final Map<UUID, 原位置记录> 原位置表 = new HashMap<>();

    private record 原位置记录(ResourceKey<Level> 维度, Vec3 坐标) {}

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("debug")
                .requires(source -> source.hasPermission(OP_LEVEL))

                .then(Commands.literal("gemview")
                        .then(Commands.literal("test")
                                .executes(ctx -> 测试跨维度追踪(ctx.getSource()))
                        )
                        .then(Commands.literal("restore")
                                .executes(ctx -> 恢复视角(ctx.getSource()))
                        )
                )

                .then(Commands.literal("chunk")
                        .then(Commands.literal("info")
                                .executes(ctx -> 查看区块信息(ctx.getSource()))
                        )
                )
        );
    }

    /**
     * 测试跨维度追踪方案
     */
    private static int 测试跨维度追踪(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (!能力工具.是灵魂宝石系(player)) {
            source.sendFailure(Component.literal("你不是灵魂宝石系，无法测试"));
            return 0;
        }

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(player.getServer());
        Optional<宝石登记信息> infoOpt = worldData.获取登记信息(player.getUUID());

        if (infoOpt.isEmpty()) {
            source.sendFailure(Component.literal("灵魂宝石未登记"));
            return 0;
        }

        宝石登记信息 info = infoOpt.get();
        if (info.获取维度() == null || info.获取坐标() == null) {
            source.sendFailure(Component.literal("灵魂宝石位置未知"));
            return 0;
        }

        ResourceKey<Level> targetDim = info.获取维度();
        ServerLevel targetLevel = player.getServer().getLevel(targetDim);
        ServerLevel currentLevel = player.serverLevel();

        if (targetLevel == null) {
            source.sendFailure(Component.literal("目标维度不存在"));
            return 0;
        }

        Vec3 targetPos = info.获取坐标();
        BlockPos targetBlockPos = BlockPos.containing(targetPos);
        boolean 跨维度 = !currentLevel.dimension().equals(targetDim);

        // 记录原位置
        原位置表.put(player.getUUID(), new 原位置记录(currentLevel.dimension(), player.position()));

        try {
            if (跨维度) {
                // ===跨维度处理 ===

                // 1. 发送维度切换包（客户端切换）
                player.connection.send(new ClientboundRespawnPacket(
                        targetLevel.dimensionTypeId(),
                        targetLevel.dimension(),
                        BiomeManager.obfuscateSeed(targetLevel.getSeed()),
                        player.gameMode.getGameModeForPlayer(),
                        player.gameMode.getPreviousGameModeForPlayer(),
                        targetLevel.isDebug(),
                        targetLevel.isFlat(),
                        (byte) 3,
                        player.getLastDeathLocation(),
                        player.getPortalCooldown()
                ));

                // 2. 尝试把玩家加入目标维度的 ChunkMap 追踪
                ChunkMap targetChunkMap = targetLevel.getChunkSource().chunkMap;

                // 调用 updatePlayerStatus 尝试添加追踪
                // 注意：这可能不会生效，因为 player.level() 还是原维度
                ((ChunkMapInvoker) targetChunkMap).puellamagi$updatePlayerStatus(player, true);

                // 3. 发送区块缓存中心
                int chunkX = targetBlockPos.getX() >> 4;
                int chunkZ = targetBlockPos.getZ() >> 4;
                player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkX, chunkZ));

                // 4. 手动发送区块（保底）
                发送周围区块(player, targetLevel, targetBlockPos, 4);

                // 5. 发送位置
                player.connection.send(new ClientboundPlayerPositionPacket(
                        targetPos.x, targetPos.y + 1, targetPos.z,
                        player.getYRot(), player.getXRot(),
                        Set.of(), 0
                ));

                source.sendSuccess(() -> Component.literal(
                        "=== 跨维度追踪测试 ===\n" +
                                "目标: " + targetDim.location() + "\n" +
                                "坐标: " + targetBlockPos.toShortString() + "\n" +
                                "---\n" +
                                "尝试了ChunkMap.updatePlayerStatus()\n" +
                                "观察区块是否持续加载\n" +
                                "使用 restore 恢复"
                ), true);} else {
                // === 同维度处理 ===
                // 只需要发送位置同步
                player.connection.send(new ClientboundSetChunkCacheCenterPacket(
                        targetBlockPos.getX() >> 4,
                        targetBlockPos.getZ() >> 4
                ));
                player.connection.send(new ClientboundPlayerPositionPacket(
                        targetPos.x, targetPos.y + 1, targetPos.z,
                        player.getYRot(), player.getXRot(),
                        Set.of(), 0
                ));

                source.sendSuccess(() -> Component.literal(
                        "=== 同维度测试 ===\n" +
                                "坐标: " + targetBlockPos.toShortString()
                ), true);
            }

        } catch (Exception e) {
            source.sendFailure(Component.literal("发生错误: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }

        return 1;
    }

    /**
     * 恢复视角
     */
    private static int 恢复视角(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        原位置记录 记录 = 原位置表.remove(player.getUUID());
        if (记录 == null) {
            source.sendFailure(Component.literal("没有记录原位置"));
            return 0;
        }

        ServerLevel originalLevel = player.getServer().getLevel(记录.维度());
        if (originalLevel == null) {
            source.sendFailure(Component.literal("原维度不存在"));
            return 0;
        }

        BlockPos originalBlockPos = BlockPos.containing(记录.坐标());

        // 1. 发送维度切换包
        player.connection.send(new ClientboundRespawnPacket(
                originalLevel.dimensionTypeId(),
                originalLevel.dimension(),
                BiomeManager.obfuscateSeed(originalLevel.getSeed()),
                player.gameMode.getGameModeForPlayer(),
                player.gameMode.getPreviousGameModeForPlayer(),
                originalLevel.isDebug(),
                originalLevel.isFlat(),
                (byte) 3,
                player.getLastDeathLocation(),
                player.getPortalCooldown()
        ));

        // 2. 发送区块中心
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(
                originalBlockPos.getX() >> 4,
                originalBlockPos.getZ() >> 4
        ));

        // 3. 发送区块
        发送周围区块(player, originalLevel, originalBlockPos, 4);

        // 4. 发送位置
        player.connection.send(new ClientboundPlayerPositionPacket(
                记录.坐标().x, 记录.坐标().y, 记录.坐标().z,
                player.getYRot(), player.getXRot(),
                Set.of(), 0
        ));

        source.sendSuccess(() -> Component.literal(
                "已恢复到: " + 记录.维度().location() + "\n" +
                        "坐标: " + originalBlockPos.toShortString()
        ), true);

        return 1;
    }

    /**
     * 发送周围区块
     */
    private static void 发送周围区块(ServerPlayer player, ServerLevel level, BlockPos center, int radius) {
        int centerX = center.getX() >> 4;
        int centerZ = center.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                player.connection.send(new ClientboundLevelChunkWithLightPacket(
                        chunk,
                        level.getLightEngine(),
                        null, null
                ));
            }
        }
    }

    /**
     * 查看区块信息
     */
    private static int 查看区块信息(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        StringBuilder sb = new StringBuilder("=== 区块信息 ===\n");
        sb.append("服务端维度: ").append(level.dimension().location()).append("\n");
        sb.append("服务端坐标: ").append(pos.toShortString()).append("\n");
        sb.append("服务端区块: [").append(pos.getX() >> 4).append(", ").append(pos.getZ() >> 4).append("]\n");

        // ChunkMap 状态
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        sb.append("视距: ").append(chunkMap.getDistanceManager().getNaturalSpawnChunkCount()).append("\n");

        if (能力工具.是灵魂宝石系(player)) {
            灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(player.getServer());
            worldData.获取登记信息(player.getUUID()).ifPresent(info -> {
                if (info.获取维度() != null && info.获取坐标() != null) {
                    sb.append("---\n");
                    sb.append("宝石维度: ").append(info.获取维度().location()).append("\n");
                    sb.append("宝石坐标: ").append(String.format("%.1f, %.1f, %.1f",
                            info.获取坐标().x, info.获取坐标().y, info.获取坐标().z)).append("\n");
                }
            });
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
