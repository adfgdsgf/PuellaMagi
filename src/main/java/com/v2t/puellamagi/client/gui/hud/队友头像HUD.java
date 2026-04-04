package com.v2t.puellamagi.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.v2t.puellamagi.client.客户端队伍缓存;
import com.v2t.puellamagi.client.gui.component.GUI玩家头像;
import com.v2t.puellamagi.util.投影工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Map;
import java.util.UUID;

/**
 * 队友头像HUD
 *
 * 显示规则：
 * -渲染范围内 + 无实心方块遮挡 → 不显示（能直接看到玩家模型）
 * - 渲染范围内 + 被实心方块遮挡 → 显示（模型被挡住）
 * - 渲染范围外 → 始终显示
 * - 视野外 → 钉在屏幕边缘
 * - 跨维度/离线 → 不显示
 * - 距离<8格 → 不显示
 *
 * 数据来源：客户端队伍缓存.队友位置（服务端每0.5秒同步）
 */
public class 队友头像HUD implements IGuiOverlay {

    public static final 队友头像HUD INSTANCE = new 队友头像HUD();

    //==================== 距离阈值 ====================

    private static final double 最近距离 = 8.0;
    private static final double 近距离 = 16.0;
    private static final double 远距离 = 64.0;
    private static final double 极远距离 = 256.0;
    private static final double 超远距离 = 512.0;

    // ==================== 尺寸 ====================

    private static final int 最大尺寸 = 10;
    private static final int 基础尺寸 = 6;
    private static final int 最小尺寸 = 4;
    private static final int 边缘尺寸 = 8;

    // ==================== 透明度 ====================

    private static final float 近距离透明度 = 0.7f;
    private static final float 正常透明度 = 1.0f;
    private static final float 远距离最低透明度 = 0.5f;
    private static final float 超远最低透明度 = 0.2f;

    // ==================== 边距 ====================

    private static final int 屏幕边距 = 20;

    // ==================== 高亮 ====================

    private static final int 高亮边框色 = 0xFFFFD700;

    private 队友头像HUD() {}

    // ==================== 渲染 ====================

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;
        if (mc.level == null) return;
        if (!客户端队伍缓存.有队伍()) return;
        if (mc.options.hideGui) return;
        if (!自己是否显示头像()) return;
        if (!投影工具.矩阵是否有效()) return;

        ResourceLocation 当前维度 = mc.player.level().dimension().location();
        double 渲染距离格 = mc.options.renderDistance().get() * 16.0;

        Map<UUID, 客户端队伍缓存.队友位置> positions = 客户端队伍缓存.获取所有队友位置();

        for (Map.Entry<UUID, 客户端队伍缓存.队友位置> entry : positions.entrySet()) {
            UUID teammateUUID = entry.getKey();
            客户端队伍缓存.队友位置 pos = entry.getValue();

            if (teammateUUID.equals(mc.player.getUUID())) continue;
            if (!pos.dimension().equals(当前维度)) continue;

            double dx = pos.x() - mc.player.getX();
            double dy = pos.y() - mc.player.getEyeY();
            double dz = pos.z() - mc.player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // 可见性判断优先：渲染范围内 + 无遮挡 + 距离<8格 → 不显示
            boolean 在渲染范围内 = distance <= 渲染距离格;
            boolean 模型可见 = 在渲染范围内 && 队友模型可见(mc, pos);

            // 能看见模型 → 不显示头像（无论距离）
            if (模型可见) continue;

            // 看不见模型时，距离太近也不跳过（被墙挡住需要显示）

            // 投影到屏幕（头顶上方）
            投影工具.投影结果 result = 投影工具.投影到屏幕(
                    pos.x(), pos.y() + 1.5, pos.z(),
                    screenWidth, screenHeight
            );

            if (result == null) continue;

            int size = 计算尺寸(distance);
            float alpha = 计算透明度(distance);
            boolean 在边缘 = false;

            float drawX = result.screenX();
            float drawY = result.screenY();

            if (!result.在视野内()) {
                float[] clamped = 投影工具.钉到屏幕边缘(
                        result.screenX(), result.screenY(),
                        result.在摄像机前方(),
                        screenWidth, screenHeight,屏幕边距
                );
                drawX = clamped[0];
                drawY = clamped[1];
                size = 边缘尺寸;
                在边缘 = true;
            }

            int headX = (int) (drawX - size / 2.0f);
            int headY = (int) (drawY - size / 2.0f);

            boolean 高亮 = 客户端队伍缓存.是否高亮(teammateUUID);
            绘制队友标记(graphics, teammateUUID, headX, headY, size, alpha, 高亮, distance, 在边缘);
        }
    }

    /**
     * 检查当前玩家是否开启了队友头像显示
     */
    private boolean 自己是否显示头像() {
        var team = 客户端队伍缓存.获取队伍();
        if (team == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        return team.获取成员(mc.player.getUUID())
                .map(member -> ((com.v2t.puellamagi.system.team.队伍成员数据) member).获取配置().显示队友头像())
                .orElse(true);
    }

    // ==================== 可见性判断 ====================

    /**
     * 判断队友模型是否可见（无实心方块遮挡）
     *
     * 从摄像机眼睛位置向队友位置做射线检测
     * 只检测实心方块（花、火把、玻璃等不算遮挡）
     */
    private boolean 队友模型可见(Minecraft mc, 客户端队伍缓存.队友位置 pos) {
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 targetPos = new Vec3(pos.x(), pos.y(), pos.z());

        // 射线检测：只检测遮挡视线的方块
        BlockHitResult hitResult = mc.level.clip(new ClipContext(
                cameraPos,
                targetPos,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        // 如果射线没有命中任何方块，或者命中点超过队友位置 → 可见
        if (hitResult.getType() == HitResult.Type.MISS) {
            return true;
        }

        // 命中了方块：检查命中点是否在队友之前
        double hitDist = hitResult.getLocation().distanceTo(cameraPos);
        double targetDist = targetPos.distanceTo(cameraPos);

        return hitDist >= targetDist;
    }

    // ==================== 参数计算 ====================

    private int 计算尺寸(double distance) {
        if (distance <= 近距离) {
            return 最大尺寸;
        } else if (distance <= 远距离) {
            float t = (float) ((distance - 近距离) / (远距离 - 近距离));
            return Math.round(最大尺寸 + (基础尺寸 - 最大尺寸) * t);
        } else {
            float t = (float) Math.min(1.0, (distance - 远距离) / (极远距离 - 远距离));
            return Math.round(基础尺寸 + (最小尺寸 - 基础尺寸) * t);
        }
    }

    private float 计算透明度(double distance) {
        if (distance <= 近距离) {
            return 近距离透明度;
        } else if (distance <= 远距离) {
            return 正常透明度;
        } else if (distance <= 极远距离) {
            float t = (float) ((distance - 远距离) / (极远距离 - 远距离));
            return 正常透明度 + (远距离最低透明度 - 正常透明度) * t;
        } else if (distance <= 超远距离) {
            float t = (float) ((distance - 极远距离) / (超远距离 - 极远距离));
            return 远距离最低透明度 + (超远最低透明度 - 远距离最低透明度) * t;
        } else {
            return 超远最低透明度;
        }
    }

    // ==================== 绘制 ====================

    /**
     * 绘制单个队友标记
     *布局：[头像][距离m]距离显示在头像右侧
     */
    private void 绘制队友标记(GuiGraphics graphics, UUID uuid,int x, int y, int size, float alpha,
                              boolean 高亮, double distance, boolean 在边缘) {
        Minecraft mc = Minecraft.getInstance();

        // 应用透明度
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // 绘制头像
        if (高亮) {
            int borderAlpha = (int) (alpha * 255) << 24;
            int color = (高亮边框色 & 0x00FFFFFF) | borderAlpha;
            GUI玩家头像.绘制带边框(graphics, uuid, x, y, size, color);
        } else {
            int borderAlpha = (int) (alpha * 180) << 24;
            GUI玩家头像.绘制带边框(graphics, uuid, x, y, size, borderAlpha);
        }

        // 恢复颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        // 距离文字（头像右侧，垂直居中）
        String distText = Math.round(distance) + "m";
        int textX = x + size + 2;
        int textY = y + (size - mc.font.lineHeight) / 2;

        int textAlpha = (int) (alpha * 255);
        int textColor = (textAlpha << 24) | 0xCCCCCC;

        graphics.drawString(mc.font, distText, textX, textY, textColor, true);
    }
}
