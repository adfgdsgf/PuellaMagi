// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/技能栏HUD.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.gui.hud.HUD布局数据;
import com.v2t.puellamagi.client.gui.hud.I可编辑HUD;
import com.v2t.puellamagi.client.蓄力状态管理;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.client.客户端状态管理;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.system.skill.技能槽位数据;
import com.v2t.puellamagi.system.skill.技能预设;
import com.v2t.puellamagi.util.本地化工具;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 技能栏HUD覆盖层
 *
 * 使用HUD布局数据统一管理位置、方向、缩放
 * 位置使用绝对坐标，与污浊度HUD逻辑一致
 */
public class 技能栏HUD implements IGuiOverlay, I可编辑HUD {

    public static final 技能栏HUD INSTANCE = new 技能栏HUD();

    private static final String HUD_ID = "skill_bar";
    private static final int 槽位大小 = 常量.默认槽位大小;
    private static final int 槽位间距 = 2;

    // 使用动态默认位置计算器：屏幕底部居中
    private final HUD布局数据 布局 = new HUD布局数据(HUD_ID,
            (screenW, screenH) -> {
                int[] size = 计算基础尺寸();
                int defaultX = (screenW - size[0]) / 2;
                int defaultY = screenH - size[1] - 40;
                return new int[]{defaultX, defaultY};
            },
            true, true,
            0.5f, 2.0f
    );

    // 折叠动画
    private float 折叠进度 = 0f;
    private static final float 动画速度 = 0.25f;

    // 预设名称显示
    private int 当前预设索引缓存 = -1;
    private long 预设切换时间戳 = 0;
    private static final long 预设名称显示毫秒 = 3000;
    private static final long 预设名称淡出毫秒 = 500;

    // 编辑模式
    private boolean 编辑模式 = false;

    // 当前渲染位置和尺寸
    private int 渲染X, 渲染Y, 渲染宽度, 渲染高度;

    private 技能栏HUD() {}

    // ==================== 静态工具方法 ====================

    /**
     * 计算基础尺寸（默认缩放，用于默认位置计算）
     */
    private static int[] 计算基础尺寸() {
        int slotCount = 常量.默认技能槽位数;
        int w = slotCount * 槽位大小 + (slotCount - 1) * 槽位间距;
        int h = 槽位大小;
        return new int[]{w, h};
    }

    // ==================== I可编辑HUD 实现 ====================

    @Override
    public String 获取HUD标识() {
        return HUD_ID;
    }

    @Override
    public Component 获取显示名称() {
        return 本地化工具.GUI("hud.skill_bar");
    }

    @Override
    public boolean 当前是否显示(Player player) {
        return 能力工具.是否已变身(player) || 编辑模式;
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
    public boolean 坐标在HUD上(double x, double y) {
        return x >= 渲染X && x <= 渲染X + 渲染宽度
                && y >= 渲染Y && y <= 渲染Y + 渲染高度;
    }

    @Override
    public void 绘制编辑边框(GuiGraphics graphics, boolean selected) {
        int color = selected ? 渲染工具.颜色_金色 : 0xFFFFFF00;
        渲染工具.绘制边框矩形(graphics, 渲染X - 3, 渲染Y - 3,渲染宽度 + 6, 渲染高度 + 6, color,0, 2);
    }

    // ==================== 旧接口兼容 ====================

    public void 触发预设显示() {
        预设切换时间戳 = System.currentTimeMillis();}

    // ==================== 渲染 ====================

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;
        if (mc.options.hideGui && !编辑模式) return;
        if (!编辑模式 && !能力工具.是否已变身(player)) return;

        更新折叠动画();
        if (折叠进度 > 0.99f && !编辑模式) return;

        // 获取预设信息
        int slotCount;
        技能预设 currentPreset = null;

        if (编辑模式) {
            slotCount = 常量.默认技能槽位数;
        } else {
            var capOpt = 能力工具.获取技能能力(player);
            if (capOpt.isEmpty()) return;

            var cap = capOpt.get();
            currentPreset = cap.获取当前预设();
            slotCount = currentPreset.获取槽位数量();

            int currentIndex = cap.获取当前预设索引();
            if (currentIndex !=当前预设索引缓存) {
                当前预设索引缓存 = currentIndex;
                预设切换时间戳 = System.currentTimeMillis();
            }
        }

        // 计算尺寸
        float scale = 布局.获取缩放();
        int scaledSlotSize = (int) (槽位大小 * scale);
        int scaledGap = (int) (槽位间距 * scale);
        boolean horizontal = 布局.获取方向() == HUD方向.横向;

        渲染宽度 = horizontal
                ? slotCount * scaledSlotSize + (slotCount - 1) * scaledGap
                : scaledSlotSize;
        渲染高度 = horizontal
                ? scaledSlotSize
                : slotCount * scaledSlotSize + (slotCount - 1) * scaledGap;

        // 使用布局位置，限制在屏幕内
        int[] clamped = 布局.限制在屏幕内(布局.获取X(), 布局.获取Y(), 渲染宽度, 渲染高度);
        渲染X = clamped[0];
        渲染Y = clamped[1];

        float alpha = 编辑模式 ? 1f : (1f - 折叠进度);

        // 绘制技能栏
        绘制技能栏(graphics, mc.font, player, currentPreset,渲染X, 渲染Y, slotCount, scaledSlotSize, scaledGap,
                horizontal, scale, alpha, screenWidth);
    }

    private void 更新折叠动画() {
        if (编辑模式) {
            折叠进度 = 0f;
            return;
        }
        boolean shouldCollapse = 客户端状态管理.技能栏是否折叠();
        float target = shouldCollapse ? 1f : 0f;
        折叠进度 = 渲染工具.动画插值(折叠进度, target, 动画速度);
    }

    private void 绘制技能栏(GuiGraphics graphics, Font font, Player player,
                            技能预设 preset, int startX, int startY, int slotCount,
                            int slotSize, int gap, boolean horizontal,
                            float scale, float alpha, int screenWidth) {

        for (int i = 0; i < slotCount; i++) {
            int slotX = horizontal ? startX + i * (slotSize + gap) : startX;
            int slotY = horizontal ? startY : startY + i * (slotSize + gap);

            技能槽位数据 slotData = null;
            if (preset != null && i < preset.获取槽位数量()) {
                slotData = preset.获取槽位(i);
            }

            绘制槽位(graphics, font, player, slotData, slotX, slotY,
                    slotSize, i, scale, alpha);
        }

        if (!编辑模式) {
            绘制预设名称(graphics, font, startX, startY, slotCount,
                    slotSize, gap, horizontal, scale, alpha, screenWidth);
        }
    }

    private void 绘制预设名称(GuiGraphics graphics, Font font,int startX, int startY, int slotCount,
                              int slotSize, int gap, boolean horizontal,
                              float scale, float baseAlpha, int screenWidth) {
        long elapsed = System.currentTimeMillis() - 预设切换时间戳;
        if (elapsed > 预设名称显示毫秒 + 预设名称淡出毫秒) return;

        float fadeAlpha = 1f;
        if (elapsed > 预设名称显示毫秒) {
            long fadeElapsed = elapsed - 预设名称显示毫秒;
            fadeAlpha = 1f - (float) fadeElapsed / 预设名称淡出毫秒;
        }

        float finalAlpha = baseAlpha * fadeAlpha;
        if (finalAlpha <= 0.01f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String presetName = 能力工具.获取技能能力(mc.player)
                .map(cap -> cap.获取当前预设().获取名称())
                .orElse("预设");

        int barWidth = horizontal
                ? slotCount * slotSize + (slotCount - 1) * gap
                : slotSize;

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale,1.0f);

        int scaledTextX = (int) ((horizontal
                ? startX + barWidth / 2
                : startX + slotSize / 2) / scale);
        int scaledTextY = (int) ((startY - (int) (font.lineHeight * scale) - 4) / scale);

        int textWidth = font.width(presetName);
        int maxTextWidth = (int) (Math.max(barWidth, 80) / scale);

        if (textWidth > maxTextWidth) {
            String truncated = presetName;
            while (font.width(truncated + "...") > maxTextWidth && truncated.length() > 1) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            presetName = truncated + "...";
            textWidth = font.width(presetName);
        }

        int textColor = 渲染工具.调整透明度(渲染工具.颜色_白色, finalAlpha);
        int shadowColor = 渲染工具.调整透明度(渲染工具.颜色_文字阴影, finalAlpha);

        int drawX = scaledTextX - textWidth / 2;
        graphics.drawString(font, presetName, drawX + 1, scaledTextY + 1, shadowColor, false);
        graphics.drawString(font, presetName, drawX, scaledTextY, textColor, false);

        graphics.pose().popPose();
    }

    private void 绘制槽位(GuiGraphics graphics, Font font, Player player,技能槽位数据 slot, int x, int y, int size,
                          int index, float scale, float alpha) {

        //绘制槽位背景
        if (alpha > 0.1f) {
            渲染工具.绘制纹理(graphics, 资源工具.技能管理_槽位, x, y, size, size, alpha);
        } else {
            int bgColor = 渲染工具.调整透明度(渲染工具.颜色_槽位背景, alpha);
            int borderColor = 渲染工具.调整透明度(渲染工具.颜色_槽位边框, alpha);
            graphics.fill(x, y, x + size, y + size, bgColor);渲染工具.绘制边框矩形(graphics, x, y, size, size, borderColor);
        }

        // 绘制技能图标
        if (alpha > 0.1f && slot != null && !slot.是否为空()) {
            ResourceLocation skillId = slot.获取技能ID();
            if (skillId != null) {
                String initial = skillId.getPath().substring(0, 1).toUpperCase();
                int iconColor = 渲染工具.调整透明度(渲染工具.颜色_白色, alpha);

                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 100);
                graphics.pose().scale(scale, scale, 1.0f);

                int scaledX = (int) (x / scale);
                int scaledY = (int) (y / scale);
                int scaledSize = (int) (size / scale);
                int textWidth = font.width(initial);
                int textX = scaledX + (scaledSize - textWidth) / 2;
                int textY = scaledY + (scaledSize - font.lineHeight) / 2;

                graphics.drawString(font, initial, textX, textY, iconColor, false);
                graphics.pose().popPose();绘制冷却遮罩(graphics, font, player, skillId, x, y, size, scale, alpha);
                绘制蓄力进度(graphics, x, y, size, index, alpha);
                绘制开启状态边框(graphics, player, skillId, x, y, size, index, alpha);
            }
        }

        // 绘制快捷键提示
        if (alpha > 0.5f && index< 按键绑定.技能键.length) {
            String keyName = 按键绑定.技能键[index].getTranslatedKeyMessage().getString();
            if (keyName.length() > 1) {
                keyName = keyName.substring(0, 1).toUpperCase();
            }

            int textColor = 渲染工具.调整透明度(渲染工具.颜色_浅灰, alpha);
            int shadowColor = 渲染工具.调整透明度(渲染工具.颜色_文字阴影, alpha);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100);
            graphics.pose().scale(scale, scale, 1.0f);

            int scaledX = (int) (x / scale);
            int scaledY = (int) (y / scale);
            int scaledSize = (int) (size / scale);

            int textW = font.width(keyName);
            int textX = scaledX + scaledSize - textW - (int) (2 / scale);
            int textY = scaledY + scaledSize - font.lineHeight;

            graphics.drawString(font, keyName, textX + 1, textY + 1, shadowColor, false);
            graphics.drawString(font, keyName, textX, textY, textColor, false);

            graphics.pose().popPose();
        }
    }

    private void 绘制冷却遮罩(GuiGraphics graphics, Font font, Player player,
                              ResourceLocation skillId, int x, int y, int size,
                              float scale, float alpha) {
        var capOpt = 能力工具.获取技能能力(player);
        if (capOpt.isEmpty()) return;

        var cap = capOpt.get();
        int remainingTicks = cap.获取剩余冷却(skillId);
        if (remainingTicks <= 0) return;

        var skillOpt = com.v2t.puellamagi.system.skill.技能注册表.创建实例(skillId);
        if (skillOpt.isEmpty()) return;

        int totalTicks = skillOpt.get().获取冷却时间();
        if (totalTicks <= 0) return;

        float progress = (float) remainingTicks / totalTicks;

        // 绘制冷却遮罩
        int maskHeight = (int) (size * progress);
        int maskColor = 渲染工具.调整透明度(0x80000000, alpha);
        graphics.fill(x, y, x + size, y + maskHeight, maskColor);

        // 绘制冷却秒数
        if (alpha > 0.5f) {
            int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
            String cdText = String.valueOf(remainingSeconds);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.pose().scale(scale, scale, 1.0f);

            int scaledX = (int) (x / scale);
            int scaledY = (int) (y / scale);
            int scaledSize = (int) (size / scale);

            int textWidth = font.width(cdText);
            int textX = scaledX + (scaledSize - textWidth) / 2;
            int textY = scaledY + (scaledSize - font.lineHeight) / 2;

            int cdColor = 渲染工具.调整透明度(0xFFFF6666, alpha);
            int shadowColor = 渲染工具.调整透明度(0xFF000000, alpha);

            graphics.drawString(font, cdText, textX + 1, textY + 1, shadowColor, false);
            graphics.drawString(font, cdText, textX, textY, cdColor, false);

            graphics.pose().popPose();
        }
    }

    private void 绘制蓄力进度(GuiGraphics graphics, int x, int y, int size,int slotIndex, float alpha) {
        if (蓄力状态管理.获取蓄力槽位() != slotIndex) return;

        float progress = 蓄力状态管理.获取蓄力进度();
        if (progress <= 0) return;

        int barHeight =3;
        int barY = y + size - barHeight - 1;
        int barWidth = size - 2;
        int barX = x + 1;

        // 背景
        int bgColor = 渲染工具.调整透明度(0x80000000, alpha);
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, bgColor);

        // 进度条颜色
        int progressWidth = (int) (barWidth * progress);
        int progressColor;

        if (progress< 0.5f) {
            progressColor = 0xFF00AAFF;
        } else if (progress < 1.0f) {
            progressColor = 0xFF00FF00;
        } else {
            long time = System.currentTimeMillis();
            boolean flash = (time / 100) % 2 == 0;
            progressColor = flash ? 0xFFFFD700 : 0xFFFFFF00;
        }

        int finalColor = 渲染工具.调整透明度(progressColor, alpha);
        graphics.fill(barX, barY, barX + progressWidth, barY + barHeight, finalColor);

        // 蓄满高亮边框
        if (progress >= 1.0f) {
            int highlightColor = 渲染工具.调整透明度(0xFFFFD700, alpha * 0.8f);
            渲染工具.绘制边框矩形(graphics, x, y, size, size, highlightColor,0, 1);
        }
    }

    private void 绘制开启状态边框(GuiGraphics graphics, Player player,ResourceLocation skillId, int x, int y, int size,
                                  int slotIndex, float alpha) {
        if (!蓄力状态管理.槽位技能是否开启(player, slotIndex)) return;

        long time = System.currentTimeMillis();
        float breathe = 0.7f +0.3f * (float) Math.sin(time / 300.0);
        int highlightColor = 渲染工具.调整透明度(0xFFFFD700, alpha * breathe);渲染工具.绘制边框矩形(graphics, x, y, size, size, highlightColor, 0, 2);
    }
}
