package com.v2t.puellamagi.client;

import com.v2t.puellamagi.mixin.access.KeyMappingAccessor;
import com.v2t.puellamagi.mixin.access.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 时间删除客户端处理器
 *
 * 统一管理进入时间删除时客户端需要做的所有清理工作
 * 各子系统的清理逻辑集中在此，不散落在客户端复刻管理器中
 *
 * 职责：
 * 1. 恢复鼠标光标到真实位置（防止视角暴转）
 * 2. 同步朝向旧值（防止插值跳变）
 * 3. 重置方块破坏状态（防止破坏动画卡住）
 * 4. 清空按键状态（防止回放残留按键继续生效）
 * 5. 管理方块忽略列表（时删期间A不看到自己触发的方块变化）
 */
@OnlyIn(Dist.CLIENT)
public final class 时间删除客户端处理器 {

    private 时间删除客户端处理器() {}

    // ==================== 方块忽略列表 ====================

    /**
     * 被忽略的方块位置集合
     *
     * 时删期间，服务端帧方块修正执行A的操作后，
     * 通过S2C包通知A的客户端将这些方块位置加入此列表。
     * 当MC原版方块更新包到达时，Mixin检查此列表：
     * - 在列表中 → 不执行方块变化（A看到方块还在原来的状态）
     * - 不在列表中 → 正常执行
     *
     * 时删结束时通过清空通知包清空此列表。
     */
    private static final Set<BlockPos> 忽略方块集合 = new HashSet<>();

    /**
     * 添加方块位置到忽略列表
     * 由时删方块忽略包的IGNORE操作调用
     *
     * @param positions 需要忽略的方块位置列表
     */
    public static void 添加忽略方块(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            忽略方块集合.add(pos.immutable());
        }
    }

    /**
     * 清空忽略列表
     * 由时删方块忽略包的CLEAR操作调用（时删结束时）
     */
    public static void 清空忽略方块() {
        忽略方块集合.clear();
    }

    /**
     * 检查是否有任何忽略方块（快速短路判断）
     *
     * @return true = 忽略列表非空
     */
    public static boolean 有忽略方块() {
        return !忽略方块集合.isEmpty();
    }

    /**
     * 检查指定位置是否在忽略列表中
     *
     * @param pos 方块位置
     * @return true = 该位置的方块变化应被A忽略
     */
    public static boolean 是否忽略方块(BlockPos pos) {
        return 忽略方块集合.contains(pos);
    }

    /**
     * 检查区段方块更新包中是否包含需要忽略的方块
     *
     * 遍历包中的所有方块更新，检查位置是否在忽略列表中
     *
     * @param packet 区段方块更新包
     * @return true = 包中至少有一个方块需要忽略
     */
    public static boolean 区段包含忽略方块(ClientboundSectionBlocksUpdatePacket packet) {
        // 用boolean数组作为闭包捕获
        boolean[] 结果 = {false};
        packet.runUpdates((pos, state) -> {
            if (忽略方块集合.contains(pos)) {
                结果[0] = true;
            }
        });
        return 结果[0];
    }

    /**
     * 过滤区段方块更新包：跳过忽略列表中的方块，其余正常应用
     *
     * 当区段包中混合了忽略和非忽略方块时：
     * - cancel原包（Mixin已做）
     * - 手动遍历，只应用不在忽略列表中的方块变化
     *
     * @param packet 被cancel的区段方块更新包
     */
    public static void 过滤并应用区段方块更新(ClientboundSectionBlocksUpdatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        packet.runUpdates((pos, state) -> {
            // 在忽略列表中 → 跳过（A看不到这个变化）
            if (忽略方块集合.contains(pos)) return;

            // 不在忽略列表中 → 正常应用
            level.setBlock(pos, state, 19);
        });
    }

    // ==================== 进入时删处理 ====================

    /**
     * 进入时间删除时的客户端处理
     * 由客户端复刻管理器.进入时间删除()调用
     */
    public static void 处理进入时删() {
        Minecraft mc = Minecraft.getInstance();
        恢复鼠标光标(mc);
        同步朝向旧值(mc);
        重置方块破坏(mc);
        清空按键状态();
    }

    /**
     * 恢复鼠标光标到真实位置
     *
     * 回放期间xpos/ypos被设为录制值，和真实鼠标位置差异很大
     * 不恢复的话第一次鼠标移动会产生巨大delta → 视角暴转
     */
    private static void 恢复鼠标光标(Minecraft mc) {
        if (mc.getWindow() == null) return;

        double[] realX = new double[1];
        double[] realY = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), realX, realY);

        MouseHandlerAccessor mouseAccessor = (MouseHandlerAccessor) mc.mouseHandler;
        mouseAccessor.puellamagi$setXPos(realX[0]);
        mouseAccessor.puellamagi$setYPos(realY[0]);
    }

    /**
     * 同步朝向旧值
     *
     * 防止yRotO和yRot差异导致第一帧插值跳变
     */
    private static void 同步朝向旧值(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        player.yHeadRotO = player.getYHeadRot();
    }

    /**
     * 重置方块破坏状态
     *
     * 时删时可能正在破坏方块 → 破坏动画卡住
     */
    private static void 重置方块破坏(Minecraft mc) {
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }

    /**
     * 清空所有按键状态
     *
     * 回放期间注入了按键事件 → KeyMapping残留isDown=true
     * 不清的话时删后残留按键继续生效（如一直往前走）
     */
    private static void 清空按键状态() {
        Map<String, net.minecraft.client.KeyMapping> allKeys =
                KeyMappingAccessor.puellamagi$getAll();
        for (net.minecraft.client.KeyMapping key : allKeys.values()) {
            key.setDown(false);
            ((KeyMappingAccessor) key).puellamagi$setClickCount(0);
        }
    }
}
