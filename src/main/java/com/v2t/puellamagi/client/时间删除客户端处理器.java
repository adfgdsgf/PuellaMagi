package com.v2t.puellamagi.client;

import com.v2t.puellamagi.mixin.access.KeyMappingAccessor;
import com.v2t.puellamagi.mixin.access.MouseHandlerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

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
 */
@OnlyIn(Dist.CLIENT)
public final class 时间删除客户端处理器 {

    private 时间删除客户端处理器() {}

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
