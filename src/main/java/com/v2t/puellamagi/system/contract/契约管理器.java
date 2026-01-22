// 文件路径: src/main/java/com/v2t/puellamagi/system/contract/契约管理器.java

package com.v2t.puellamagi.system.contract;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.contract.I契约;
import com.v2t.puellamagi.api.series.I系列;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import com.v2t.puellamagi.core.network.packets.s2c.契约能力同步包;
import com.v2t.puellamagi.core.network.packets.s2c.假死状态同步包;
import com.v2t.puellamagi.core.network.packets.s2c.技能能力同步包;
import com.v2t.puellamagi.core.registry.ModCapabilities;
import com.v2t.puellamagi.system.series.impl.灵魂宝石系列;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.skill.技能预设;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.system.soulgem.effect.距离效果处理器;
import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import com.v2t.puellamagi.system.transformation.变身管理器;
import com.v2t.puellamagi.system.transformation.魔法少女类型注册表;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 契约管理器
 */
public final class 契约管理器 {
    private 契约管理器() {}

    //==================== 契约查询 ====================

    public static Optional<I契约> 获取契约(Player player) {
        if (player == null) return Optional.empty();
        return player.getCapability(ModCapabilities.契约能力).resolve();
    }

    public static boolean 是否已契约(Player player) {
        return 获取契约(player)
                .map(I契约::是否已契约)
                .orElse(false);
    }

    public static Optional<I系列> 获取所属系列(Player player) {
        return 获取契约(player)
                .map(I契约::获取系列ID)
                .flatMap(系列注册表::获取);
    }

    public static Optional<魔法少女类型> 获取类型(Player player) {
        return 获取契约(player)
                .map(I契约::获取类型ID)
                .flatMap(魔法少女类型注册表::获取);
    }

    // ==================== 契约操作 ====================

    public static boolean 签订契约(ServerPlayer player, ResourceLocation seriesId, ResourceLocation typeId) {
        Optional<I系列> seriesOpt = 系列注册表.获取(seriesId);
        if (seriesOpt.isEmpty()) {
            PuellaMagi.LOGGER.warn("签订契约失败：系列 {} 不存在", seriesId);
            return false;
        }

        Optional<魔法少女类型> typeOpt = 魔法少女类型注册表.获取(typeId);
        if (typeOpt.isEmpty()) {
            PuellaMagi.LOGGER.warn("签订契约失败：类型 {} 不存在", typeId);
            return false;
        }魔法少女类型 type = typeOpt.get();
        if (!type.获取所属系列().equals(seriesId)) {
            PuellaMagi.LOGGER.warn("签订契约失败：类型 {} 不属于系列 {}", typeId, seriesId);
            return false;
        }

        Optional<I契约> contractOpt = 获取契约(player);
        if (contractOpt.isEmpty()) {
            PuellaMagi.LOGGER.error("签订契约失败：玩家 {} 没有契约能力", player.getName().getString());
            return false;
        }

        I契约 contract = contractOpt.get();

        if (contract.是否已契约()) {
            PuellaMagi.LOGGER.info("玩家 {} 已有契约，将被覆盖", player.getName().getString());
            解除契约内部(player, false);
        }

        long gameTime = player.level().getGameTime();
        contract.签订契约(seriesId, typeId, gameTime);

        能力工具.获取变身能力完整(player).ifPresent(cap -> {
            cap.设置所属系列(seriesId);
            cap.设置少女类型(typeId);});

        I系列 series = seriesOpt.get();
        series.加入系列时(player);

        PuellaMagi.LOGGER.info("玩家 {} 签订契约：系列={}, 类型={}",
                player.getName().getString(), seriesId, typeId);

        // 同步契约状态到客户端
        同步契约状态(player);

        // 如果是灵魂宝石系，自动发放灵魂宝石
        if (灵魂宝石系列.ID.equals(seriesId)) {
            灵魂宝石管理器.尝试发放灵魂宝石(player);
        }

        return true;
    }

    public static boolean 解除契约(ServerPlayer player) {
        return 解除契约内部(player, true);
    }

    private static boolean 解除契约内部(ServerPlayer player, boolean sendSync) {
        Optional<I契约> contractOpt = 获取契约(player);
        if (contractOpt.isEmpty() || !contractOpt.get().是否已契约()) {
            return false;
        }

        I契约 contract = contractOpt.get();
        ResourceLocation seriesId = contract.获取系列ID();

        // 强制退出假死
        if (假死状态处理器.是否假死中(player)) {
            假死状态处理器.强制退出(player);
        }

        // 清除假死相关状态
        假死状态处理器.清除玩家状态(player.getUUID());

        // 清除距离效果状态
        距离效果处理器.onPlayerLogout(player.getUUID());

        // 解除变身
        能力工具.获取变身能力完整(player).ifPresent(cap -> {
            if (cap.是否已变身()) {
                变身管理器.解除变身(player);
            }cap.设置所属系列(null);
            cap.设置少女类型(null);
        });

        // 重置技能
        能力工具.获取技能能力(player).ifPresent(skillCap -> {
            skillCap.获取所有预设().clear();
            skillCap.获取所有预设().add(new 技能预设("预设1"));
            skillCap.切换预设(0);
            skillCap.清除所有冷却();
        });

        // 调用系列离开回调
        if (seriesId != null) {
            系列注册表.获取(seriesId).ifPresent(series -> series.离开系列时(player));
        }

        // 解除契约数据
        contract.解除契约();

        PuellaMagi.LOGGER.info("玩家 {} 解除契约", player.getName().getString());

        // 同步状态到客户端
        if (sendSync) {
            同步契约状态(player);同步技能状态(player);
            // 确保客户端清除假死状态
            网络工具.发送给玩家(player, new 假死状态同步包(false));
        }

        return true;
    }

    // ==================== 同步方法 ====================

    public static void 同步契约状态(ServerPlayer player) {
        能力工具.获取契约能力完整(player).ifPresent(contract -> {
            契约能力同步包 packet = new 契约能力同步包(contract.写入NBT());
            网络工具.发送给玩家(player, packet);
        });
    }

    private static void 同步技能状态(ServerPlayer player) {
        能力工具.获取技能能力(player).ifPresent(skillCap -> {
            技能能力同步包 packet = new 技能能力同步包(player.getUUID(), skillCap.写入NBT());
            网络工具.发送给玩家(player, packet);
        });
    }

    // ==================== 变身检查 ====================

    public static boolean 可以变身(Player player) {
        return 是否已契约(player);
    }
}
