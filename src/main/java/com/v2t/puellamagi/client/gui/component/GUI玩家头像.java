// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/GUI玩家头像.java

package com.v2t.puellamagi.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 玩家头像渲染工具
 *
 * 渲染玩家皮肤的头部区域（底层+帽子层）
 * 支持在线玩家和离线玩家（离线使用默认皮肤）
 *
 * 复用场景：队伍界面成员列表、场外队友头像HUD、搜身界面等
 */
public final class GUI玩家头像 {

    private GUI玩家头像() {}

    //皮肤纹理中头部的UV坐标（64x64纹理）
    private static final float HEAD_U = 8.0f;
    private static final float HEAD_V = 8.0f;
    private static final int HEAD_UV_SIZE = 8;
    private static final float HAT_U = 40.0f;
    private static final float HAT_V = 8.0f;
    private static final int HAT_UV_SIZE = 8;
    private static final int TEXTURE_SIZE = 64;

    /**
     * 绘制玩家头像（头部底层 + 帽子层）
     *
     * @param graphics 绘图上下文
     * @param playerUUID 玩家UUID
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param size 头像尺寸（像素，正方形）
     */
    public static void 绘制(GuiGraphics graphics, UUID playerUUID, int x, int y, int size) {
        绘制(graphics, playerUUID, x, y, size, true);
    }

    /**
     * 绘制玩家头像
     *
     * @param graphics 绘图上下文
     * @param playerUUID 玩家UUID
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param size 头像尺寸（像素，正方形）
     * @param显示帽子层是否渲染帽子/头饰层
     */
    public static void 绘制(GuiGraphics graphics, UUID playerUUID, int x, int y,int size, boolean 显示帽子层) {
        ResourceLocation skinTexture = 获取皮肤纹理(playerUUID);

        // 头部底层
        graphics.blit(skinTexture, x, y, size, size,
                HEAD_U, HEAD_V, HEAD_UV_SIZE, HEAD_UV_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);

        // 帽子层（覆盖在底层上方）
        if (显示帽子层) {
            graphics.blit(skinTexture, x, y, size, size,
                    HAT_U, HAT_V, HAT_UV_SIZE, HAT_UV_SIZE,
                    TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    /**
     *绘制带边框的玩家头像
     *
     * @param graphics 绘图上下文
     * @param playerUUID 玩家UUID
     * @param x 绘制位置X
     * @param y 绘制位置Y
     * @param size 头像尺寸
     * @param borderColor 边框颜色（ARGB），0表示无边框
     */
    public static void 绘制带边框(GuiGraphics graphics, UUID playerUUID, int x, int y,
                                  int size, int borderColor) {
        if (borderColor != 0) {
            // 1像素边框
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, borderColor);
        }
        绘制(graphics, playerUUID, x, y, size);
    }

    /**
     * 获取玩家的皮肤纹理
     * 在线玩家使用实际皮肤，离线/未知玩家使用默认皮肤
     *
     * @param playerUUID 玩家UUID
     * @return 皮肤纹理资源路径
     */
    public static ResourceLocation 获取皮肤纹理(UUID playerUUID) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(playerUUID);
            if (info != null) {
                return info.getSkinLocation();
            }
        }

        return DefaultPlayerSkin.getDefaultSkin(playerUUID);
    }
}
