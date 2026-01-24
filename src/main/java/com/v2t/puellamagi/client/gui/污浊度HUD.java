// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/污浊度HUD.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.gui.hud.HUD布局数据;
import com.v2t.puellamagi.client.gui.hud.I可编辑HUD;
import com.v2t.puellamagi.system.contract.契约管理器;
import com.v2t.puellamagi.system.series.impl.灵魂宝石系列;
import com.v2t.puellamagi.util.本地化工具;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.渲染工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 污浊度HUD覆盖层
 *
 * 显示灵魂宝石系玩家的污浊度状态
 * 支持：位置编辑、方向切换、缩放调整
 */
public class 污浊度HUD implements IGuiOverlay, I可编辑HUD {

    public static final 污浊度HUD INSTANCE = new 污浊度HUD();

    // HUD标识
    private static final String HUD_ID = "corruption";

    // 基础尺寸（未缩放）
    private static final int 基础宽度 = 80;
    private static final int 基础高度 = 12;
    private static final int 默认X = 5;
    private static final int 默认Y = 5;

    // 颜色定义
    private static final int 颜色_纯净 = 0xFF8844FF;
    private static final int 颜色_中等 = 0xFF4422AA;
    private static final int 颜色_污浊 = 0xFF111111;
    private static final int 颜色_边框 = 0xFFAAAAAA;
    private static final int 颜色_文字 = 0xFFFFFFFF;
    private static final int 颜色_文字阴影 = 0xFF000000;

    // 布局数据
    private final HUD布局数据 布局 = new HUD布局数据(HUD_ID, 默认X, 默认Y,
            true,  // 支持方向
            true,  // 支持缩放
            0.5f, 2.0f  // 缩放范围
    );

    // 动画相关
    private float 显示进度动画 = 0f;
    private float 当前显示值 = 0f;
    private static final float 动画速度 = 0.1f;
    private long 淡出开始时间 = 0;
    private static final long 防抖时间_毫秒 = 500;

    // 编辑模式
    private boolean 编辑模式 = false;

    // 当前渲染位置（用于坐标检测）
    private int 渲染X, 渲染Y, 渲染宽度, 渲染高度;

    private 污浊度HUD() {}

    //==================== IGuiOverlay 实现 ====================

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;
        if (mc.options.hideGui && !编辑模式) return;

        // 原始判断
        boolean 原始应显示 = 编辑模式 || 是否显示污浊度(player);

        // 防抖逻辑：一旦开始淡出，短时间内忽略"显示"请求
        boolean shouldShow;
        if (!原始应显示) {
            // 开始淡出
            if (淡出开始时间 == 0) {
                淡出开始时间 = System.currentTimeMillis();
            }
            shouldShow = false;
        } else {
            // 原始判断为"显示"，检查是否在防抖期内
            if (淡出开始时间 > 0 && System.currentTimeMillis() - 淡出开始时间 < 防抖时间_毫秒) {
                // 防抖期内，继续隐藏
                shouldShow = false;
            } else {
                // 正常显示
                淡出开始时间 = 0;
                shouldShow = true;
            }
        }

        if (!shouldShow) {
            显示进度动画 = 渲染工具.动画插值(显示进度动画, 0f, 动画速度);
            if (显示进度动画 < 0.01f) {
                淡出开始时间 = 0;  // 完全隐藏后重置
                return;
            }
        } else {
            显示进度动画 = 渲染工具.动画插值(显示进度动画, 1f, 动画速度);
        }

        // 获取污浊度百分比
        float 目标值 = 能力工具.获取污浊度百分比(player);
        当前显示值 = 渲染工具.动画插值(当前显示值, 目标值, 动画速度);

        // 计算实际尺寸和位置
        float scale = 布局.获取缩放();
        boolean horizontal = 布局.获取方向() == HUD方向.横向;

        int baseW = horizontal ? 基础宽度 : 基础高度;
        int baseH = horizontal ? 基础高度 : 基础宽度;

        渲染宽度 = (int) (baseW * scale);
        渲染高度 = (int) (baseH * scale);渲染X = 布局.获取X();
        渲染Y = 布局.获取Y();

        // 限制在屏幕内渲染X = Math.max(0, Math.min(渲染X, screenWidth - 渲染宽度));
        渲染Y = Math.max(0, Math.min(渲染Y, screenHeight - 渲染高度));

        float alpha = 显示进度动画;

        // 绘制污浊度条
        绘制污浊度条(graphics, mc.font, 渲染X, 渲染Y, 渲染宽度, 渲染高度,
                当前显示值, alpha, horizontal);

        // 编辑模式边框
        if (编辑模式) {
            绘制编辑边框(graphics, false);
        }
    }

    private boolean 是否显示污浊度(Player player) {
        if (!能力工具.是否已契约(player)) {
            return false;
        }

        ResourceLocation seriesId = 契约管理器.获取契约(player)
                .map(contract -> contract.获取系列ID())
                .orElse(null);

        return 灵魂宝石系列.ID.equals(seriesId);
    }

    private void 绘制污浊度条(GuiGraphics graphics, Font font, int x, int y,int width, int height, float percent, float alpha, boolean horizontal) {
        if (alpha< 0.01f) return;

        // 外边框
        int borderColor = 渲染工具.调整透明度(颜色_边框, alpha);渲染工具.绘制边框矩形(graphics, x, y, width, height, borderColor, 0, 1);

        // 进度条
        if (percent > 0) {
            int progressColor = 计算渐变颜色(percent);
            progressColor = 渲染工具.调整透明度(progressColor, alpha);

            if (horizontal) {
                int progressWidth = (int) ((width - 2) * percent);
                graphics.fill(x + 1, y + 1, x + 1 + progressWidth, y + height - 1, progressColor);
            } else {
                //纵向：从下往上填充
                int progressHeight = (int) ((height - 2) * percent);
                int progressY = y + height - 1 - progressHeight;
                graphics.fill(x + 1, progressY, x + width - 1, y + height - 1, progressColor);
            }
        }

        // 危险闪烁
        if (percent > 0.8f) {
            long time = System.currentTimeMillis();
            float flash = 0.5f + 0.5f * (float) Math.sin(time / 200.0);
            int flashColor = 渲染工具.调整透明度(0xFFFF0000, alpha * flash * 0.3f);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, flashColor);
        }

        // 百分比文字
        String percentText = String.format("%.0f%%", percent * 100);
        int textWidth = font.width(percentText);

        int textX, textY;
        if (horizontal) {
            textX = x + (width - textWidth) / 2;
            textY = y + (height - font.lineHeight) / 2 + 1;
        } else {
            textX = x + (width - textWidth) / 2;
            textY = y + (height - font.lineHeight) / 2;
        }

        int shadowColor = 渲染工具.调整透明度(颜色_文字阴影, alpha);
        int textColor = 渲染工具.调整透明度(颜色_文字, alpha);

        graphics.drawString(font, percentText, textX + 1, textY + 1, shadowColor, false);
        graphics.drawString(font, percentText, textX, textY, textColor, false);
    }

    private int 计算渐变颜色(float percent) {
        if (percent <= 0.5f) {
            float t = percent * 2f;
            return 颜色插值(颜色_纯净, 颜色_中等, t);
        } else {
            float t = (percent - 0.5f) * 2f;
            return 颜色插值(颜色_中等, 颜色_污浊, t);
        }
    }

    private int 颜色插值(int colorA, int colorB, float t) {
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int a = (int) (aA + (aB - aA) * t);
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================== I可编辑HUD 实现 ====================

    @Override
    public String 获取HUD标识() {
        return HUD_ID;
    }

    @Override
    public Component 获取显示名称() {
        return 本地化工具.GUI("hud.corruption");
    }

    @Override
    public boolean 当前是否显示(Player player) {
        return 是否显示污浊度(player);
    }

    @Override
    public int[] 获取当前位置() {
        return new int[]{布局.获取X(), 布局.获取Y()};
    }

    @Override
    public void 设置位置(int x, int y) {
        布局.设置位置(x, y);
    }

    @Override
    public int[] 获取默认位置() {
        return 布局.获取默认位置();
    }

    @Override
    public int[] 获取尺寸() {
        return new int[]{渲染宽度, 渲染高度};
    }

    @Override
    public boolean 支持方向切换() {
        return true;
    }

    @Override
    public HUD方向 获取方向() {
        return 布局.获取方向();
    }

    @Override
    public void 设置方向(HUD方向 direction) {
        布局.设置方向(direction);
    }

    @Override
    public boolean 支持缩放() {
        return true;
    }

    @Override
    public float 获取缩放() {
        return 布局.获取缩放();
    }

    @Override
    public void 设置缩放(float scale) {
        布局.设置缩放(scale);
    }

    @Override
    public float 获取最小缩放() {
        return 布局.获取最小缩放();
    }

    @Override
    public float 获取最大缩放() {
        return 布局.获取最大缩放();
    }

    @Override
    public void 进入编辑模式() {
        编辑模式 = true;
        布局.开始编辑();
    }

    @Override
    public void 退出编辑模式(boolean save) {
        if (save) {
            布局.保存编辑();
        } else {
            布局.取消编辑();
        }
        编辑模式 = false;
    }

    @Override
    public boolean 是否编辑模式() {
        return 编辑模式;
    }

    @Override
    public void 重置为默认() {
        布局.重置为默认();}

    @Override
    public boolean 坐标在HUD上(double mouseX, double mouseY) {
        return mouseX >= 渲染X && mouseX <= 渲染X + 渲染宽度
                && mouseY >= 渲染Y && mouseY <= 渲染Y + 渲染高度;
    }

    @Override
    public void 绘制编辑边框(GuiGraphics graphics, boolean selected) {
        int color = selected ? 渲染工具.颜色_金色 : 0xFFFFFF00;
        渲染工具.绘制边框矩形(graphics, 渲染X - 2, 渲染Y - 2,渲染宽度 + 4, 渲染高度 + 4, color, 0, 2);
    }
}
