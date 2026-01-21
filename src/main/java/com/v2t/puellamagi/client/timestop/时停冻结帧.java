// 文件路径: src/main/java/com/v2t/puellamagi/client/timestop/时停冻结帧.java

package com.v2t.puellamagi.client.timestop;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.mixin.access.GuiAccessor;
import com.v2t.puellamagi.system.ability.timestop.时停管理器;
import com.v2t.puellamagi.system.ability.timestop.时停豁免系统;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 时停冻结帧管理器
 *
 * 核心逻辑：
 * 1. 截图在Gui.render之后、Screen之前（世界+HUD，不含Screen）
 * 2. 渲染时：冻结帧 + 聊天组件（聊天实时更新）
 * 3. Screen正常渲染在上面
 */
@OnlyIn(Dist.CLIENT)
public class 时停冻结帧 {

    private static RenderTarget frozenFrame = null;
    private static boolean isFrozen = false;
    private static boolean needCapture = false;
    private static boolean wasCreative = false;

    //==================== 公开查询 ====================

    public static boolean isFrozen() {
        return isFrozen;
    }

    public static boolean 处于冻结状态() {
        return isFrozen;
    }

    public static boolean hasFrozenFrame() {
        return frozenFrame != null;
    }

    public static boolean needCapture() {
        return needCapture;
    }

    public static boolean shouldSkipRender() {
        return isFrozen && frozenFrame != null && !needCapture;
    }

    /**
     * 判断是否是白名单界面
     */
    public static boolean isWhitelistedScreen(Screen screen) {
        if (screen == null) {
            return false;
        }

        // ESC菜单
        if (screen instanceof PauseScreen) {
            return true;
        }

        // 聊天框
        if (screen instanceof ChatScreen) {
            return true;
        }

        // 通过类名检测
        String name = screen.getClass().getName().toLowerCase();
        return name.contains("option")
                || name.contains("setting")
                || name.contains("controls")
                || name.contains("language")
                || name.contains("sound")
                || name.contains("resource")
                || name.contains("gamemode");// F3+F4的GameModeSwitcherScreen
    }

    // ==================== 公开控制 ====================

    public static void reset() {
        isFrozen = false;
        needCapture = false;}

    public static void cleanup() {
        if (frozenFrame != null) {
            frozenFrame.destroyBuffers();
            frozenFrame = null;
        }
        reset();
        wasCreative = false;
    }

    // ==================== 每帧调用 ====================

    public static void onFrameStart() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        TimeStop timeStop = (TimeStop) mc.level;
        boolean isCreative = mc.player.isCreative();

        // 使用豁免系统判断是否需要冻结画面
        boolean shouldFreeze = timeStop.puellamagi$hasActiveTimeStop()
                && 时停豁免系统.应该冻结画面(mc.player);

        if (shouldFreeze && !isFrozen) {
            isFrozen = true;
            needCapture = true;
            mc.getToasts().clear();
            PuellaMagi.LOGGER.info("[冻结帧] 进入冻结状态");
        } else if (shouldFreeze && wasCreative && !isCreative) {
            needCapture = true;
            mc.getToasts().clear();
            PuellaMagi.LOGGER.info("[冻结帧] 创造切生存，重新截图");
        } else if (!shouldFreeze && isFrozen) {
            reset();
            PuellaMagi.LOGGER.info("[冻结帧] 退出冻结状态");
        }

        wasCreative = isCreative;
    }

    /**
     * 检查当前玩家是否拥有时停能力
     */
    private static boolean 拥有时停能力() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return 时停管理器.拥有时停能力(mc.player);
    }

    /**
     * 在Gui.render之后调用 - 截图
     * 此时帧缓冲有世界+HUD，没有Screen
     */
    public static void captureAfterGui() {
        if (!needCapture) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        captureFrame(mc);
        needCapture = false;

        PuellaMagi.LOGGER.info("[冻结帧] 截图完成");
    }

    /**
     *渲染冻结帧
     */
    public static void renderFrozenFrame() {
        if (frozenFrame == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frozenFrame.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainTarget.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
                0, 0, frozenFrame.width, frozenFrame.height,
                0, 0, mainTarget.width, mainTarget.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
        );

        mainTarget.bindWrite(false);
    }

    /**
     * 渲染聊天组件（覆盖在冻结帧上）
     */
    public static void renderChat(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.gui == null) {
            return;
        }

        GuiAccessor accessor = (GuiAccessor) mc.gui;
        int tickCount = accessor.puellamagi$getTickCount();

        // 1.20.1 的 ChatComponent.render 签名是(GuiGraphics, int, int, int)
        // 参数: guiGraphics, tickCount, mouseX, mouseY
        accessor.puellamagi$getChat().render(guiGraphics, tickCount, 0, 0);
    }

    // ==================== 内部方法 ====================

    private static boolean isTimeStopper() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return 时停管理器.是否时停者(mc.player);
    }

    private static void captureFrame(Minecraft mc) {
        RenderTarget mainTarget = mc.getMainRenderTarget();

        if (frozenFrame == null
                || frozenFrame.width != mainTarget.width
                || frozenFrame.height != mainTarget.height) {

            if (frozenFrame != null) {
                frozenFrame.destroyBuffers();
            }
            frozenFrame = new TextureTarget(
                    mainTarget.width,
                    mainTarget.height,
                    true,
                    Minecraft.ON_OSX);
        }

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frozenFrame.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
                0, 0, mainTarget.width, mainTarget.height,
                0, 0, frozenFrame.width, frozenFrame.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
        );

        mainTarget.bindWrite(false);
    }
}
