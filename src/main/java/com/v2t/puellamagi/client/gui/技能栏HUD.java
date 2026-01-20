// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/技能栏HUD.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.client.客户端状态管理;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.system.skill.技能槽位数据;
import com.v2t.puellamagi.system.skill.技能预设;
import com.v2t.puellamagi.system.skill.布局配置;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.资源工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 技能栏HUD覆盖层
 *
 * 支持编辑模式：直接拖动调整位置
 */
public class 技能栏HUD implements IGuiOverlay {

    public static final 技能栏HUD INSTANCE = new 技能栏HUD();

    private static final int 槽位大小 = 常量.默认槽位大小;
    private static final int 槽位间距 = 2;

    // 折叠动画
    private float 折叠进度 = 0f;
    private static final float 动画速度 = 0.25f;

    // 预设名称显示
    private int 当前预设索引缓存 = -1;
    private long 预设切换时间戳 = 0;
    private static final long 预设名称显示毫秒 = 3000;
    private static final long 预设名称淡出毫秒 = 500;

    // ===== 编辑模式 =====
    private boolean 编辑模式 = false;
    private 布局配置 编辑中布局 = null;

    // 当前渲染的技能栏位置和尺寸
    private int 当前X, 当前Y, 当前宽度, 当前高度;

    private 技能栏HUD() {}

    // ==================== 编辑模式控制 ====================

    public void 进入编辑模式() {
        编辑模式 = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            能力工具.获取技能能力(mc.player).ifPresent(cap -> {
                编辑中布局 = cap.获取当前预设().获取布局().复制();
            });
        }
        if (编辑中布局 == null) {
            编辑中布局 = new 布局配置();
        }
    }

    public void 退出编辑模式(boolean save) {
        if (save && 编辑中布局 != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                能力工具.获取技能能力(mc.player).ifPresent(cap -> {
                    cap.获取当前预设().设置布局(编辑中布局);
                });
            }
        }
        编辑模式 = false;
        编辑中布局 = null;
    }

    public boolean 是否编辑模式() {
        return 编辑模式;
    }

    public 布局配置 获取编辑中布局() {
        return 编辑中布局;
    }

    public boolean 坐标在技能栏上(double x, double y) {
        return x >= 当前X && x <= 当前X + 当前宽度
                && y >= 当前Y && y <= 当前Y + 当前高度;
    }

    public void 设置位置偏移(int offsetX, int offsetY) {
        if (编辑中布局 != null) {
            编辑中布局.设置偏移(offsetX, offsetY);
        }
    }

    public void 设置编辑中布局(布局配置 layout) {
        if (编辑模式 && layout != null) {
            this.编辑中布局 = layout;
        }
    }

    public int[] 获取当前偏移() {
        if (编辑中布局 != null) {
            return new int[]{编辑中布局.获取偏移X(), 编辑中布局.获取偏移Y()};
        }
        return new int[]{0, 0};
    }

    // ==================== 渲染 ====================

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) {
            return;
        }
        if (mc.options.hideGui && !编辑模式) {
            return;
        }
        if (!编辑模式 && !能力工具.是否已变身(player)) {
            return;
        }

        更新折叠动画();

        if (折叠进度 > 0.99f && !编辑模式) {
            return;
        }

        布局配置 layout;
        int slotCount;
        技能预设 currentPreset = null;

        if (编辑模式 && 编辑中布局 != null) {
            layout = 编辑中布局;
            slotCount = 常量.默认技能槽位数;} else {
            var capOpt = 能力工具.获取技能能力(player);
            if (capOpt.isEmpty()) {
                return;
            }
            var cap = capOpt.get();
            currentPreset = cap.获取当前预设();
            layout = currentPreset.获取布局();
            slotCount = currentPreset.获取槽位数量();

            int currentIndex = cap.获取当前预设索引();
            if (currentIndex !=当前预设索引缓存) {
                当前预设索引缓存 = currentIndex;
                预设切换时间戳 = System.currentTimeMillis();
            }
        }

        float scale = layout.获取缩放比例();
        int[] pos = 计算技能栏位置(layout, slotCount, screenWidth, screenHeight);
        当前X = pos[0];
        当前Y = pos[1];

        int scaledSlotSize = (int) (槽位大小 * scale);
        int scaledGap = (int) (槽位间距 * scale);
        boolean horizontal = layout.获取排列方向() == 布局配置.方向.横向;

        当前宽度 = horizontal
                ? slotCount * scaledSlotSize + (slotCount - 1) * scaledGap
                : scaledSlotSize;
        当前高度 = horizontal
                ? scaledSlotSize
                : slotCount * scaledSlotSize + (slotCount - 1) * scaledGap;

        float alpha = 编辑模式 ? 1f : (1f - 折叠进度);

        if (编辑模式) {
            渲染工具.绘制边框矩形(graphics, 当前X - 3, 当前Y - 3,
                    当前宽度 + 6, 当前高度 + 6, 渲染工具.颜色_金色,0, 2);
        }

        绘制技能栏(graphics, mc.font, player, layout, currentPreset, 当前X, 当前Y, slotCount, scale, alpha, screenWidth);
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

    private int[] 计算技能栏位置(布局配置 layout, int slotCount, int screenW, int screenH) {
        float scale = layout.获取缩放比例();
        int scaledSlotSize = (int) (槽位大小 * scale);
        int scaledGap = (int) (槽位间距 * scale);

        int totalW, totalH;
        if (layout.获取排列方向() == 布局配置.方向.横向) {
            totalW = slotCount * scaledSlotSize + (slotCount - 1) * scaledGap;
            totalH = scaledSlotSize;
        } else {
            totalW = scaledSlotSize;
            totalH = slotCount * scaledSlotSize + (slotCount - 1) * scaledGap;
        }

        int[] anchor = layout.计算锚点坐标(screenW, screenH);
        int x = anchor[0];
        int y = anchor[1];

        布局配置.锚点 anchorType = layout.获取屏幕锚点();

        if (anchorType == 布局配置.锚点.上中 || anchorType == 布局配置.锚点.正中 || anchorType == 布局配置.锚点.下中) {
            x -= totalW / 2;
        } else if (anchorType == 布局配置.锚点.右上|| anchorType == 布局配置.锚点.右中 || anchorType == 布局配置.锚点.右下) {
            x -= totalW;
        }

        if (anchorType == 布局配置.锚点.左中 || anchorType == 布局配置.锚点.正中 || anchorType == 布局配置.锚点.右中) {
            y -= totalH / 2;
        } else if (anchorType == 布局配置.锚点.左下 || anchorType == 布局配置.锚点.下中 || anchorType == 布局配置.锚点.右下) {
            y -= totalH;
        }

        return new int[]{x, y};
    }

    private void 绘制技能栏(GuiGraphics graphics, Font font, Player player,布局配置 layout, 技能预设 preset, int startX, int startY,
                            int slotCount, float scale, float alpha, int screenWidth) {

        int scaledSlotSize = (int) (槽位大小 * scale);
        int scaledGap = (int) (槽位间距 * scale);
        boolean horizontal = layout.获取排列方向() == 布局配置.方向.横向;

        for (int i = 0; i < slotCount; i++) {
            int slotX = horizontal ? startX + i * (scaledSlotSize + scaledGap) : startX;
            int slotY = horizontal ? startY : startY + i * (scaledSlotSize + scaledGap);

            技能槽位数据 slotData = null;
            if (preset != null && i < preset.获取槽位数量()) {
                slotData = preset.获取槽位(i);
            }

            绘制槽位(graphics, font, player, slotData, slotX, slotY, scaledSlotSize, i, scale, alpha);
        }

        if (!编辑模式) {
            绘制预设名称(graphics, font, startX, startY, slotCount, scaledSlotSize, scaledGap, horizontal, scale, alpha, screenWidth);
        }
    }

    private void 绘制预设名称(GuiGraphics graphics, Font font, int startX, int startY, int slotCount,int slotSize, int gap, boolean horizontal,
                              float scale, float baseAlpha, int screenWidth) {
        long elapsed = System.currentTimeMillis() - 预设切换时间戳;
        if (elapsed > 预设名称显示毫秒 + 预设名称淡出毫秒) {
            return;
        }

        float fadeAlpha = 1f;
        if (elapsed > 预设名称显示毫秒) {
            long fadeElapsed = elapsed - 预设名称显示毫秒;
            fadeAlpha = 1f - (float) fadeElapsed / 预设名称淡出毫秒;
        }

        float finalAlpha = baseAlpha * fadeAlpha;
        if (finalAlpha <= 0.01f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        String presetName = 能力工具.获取技能能力(mc.player)
                .map(cap -> cap.获取当前预设().获取名称())
                .orElse("预设");

        int barWidth = horizontal ? slotCount * slotSize + (slotCount - 1) * gap : slotSize;

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale,1.0f);

        int scaledTextX = (int) ((horizontal ? startX + barWidth / 2 : startX + slotSize / 2) / scale);
        int scaledTextY = (int) ((startY - (int)(font.lineHeight * scale) - 4) / scale);

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

    private void 绘制槽位(GuiGraphics graphics, Font font, Player player,技能槽位数据 slot, int x, int y, int size, int index,
                          float scale, float alpha) {

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
                graphics.pose().popPose();

                // 绘制冷却遮罩
                绘制冷却遮罩(graphics, font, player, skillId, x, y, size, scale, alpha);
            }
        }

        // 绘制快捷键
        if (alpha > 0.5f && index < 按键绑定.技能键.length) {
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

            int textWidth = font.width(keyName);
            int textX = scaledX + scaledSize - textWidth - (int)(2 / scale);
            int textY = scaledY + scaledSize - font.lineHeight;

            graphics.drawString(font, keyName, textX + 1, textY + 1, shadowColor, false);
            graphics.drawString(font, keyName, textX, textY, textColor, false);

            graphics.pose().popPose();
        }
    }

    /**
     * 绘制冷却遮罩和剩余秒数
     */
    private void 绘制冷却遮罩(GuiGraphics graphics, Font font, Player player,
                              ResourceLocation skillId, int x, int y, int size,
                              float scale, float alpha) {
        var capOpt = 能力工具.获取技能能力(player);
        if (capOpt.isEmpty()) {
            return;
        }

        var cap = capOpt.get();
        int remainingTicks = cap.获取剩余冷却(skillId);
        if (remainingTicks <= 0) {
            return;
        }

        // 获取技能总冷却时间
        var skillOpt = com.v2t.puellamagi.system.skill.技能注册表.创建实例(skillId);
        if (skillOpt.isEmpty()) {
            return;
        }

        int totalTicks = skillOpt.get().获取冷却时间();
        if (totalTicks <= 0) {
            return;
        }

        float progress = (float) remainingTicks / totalTicks;

        // 绘制从上往下的遮罩
        int maskHeight = (int) (size * progress);
        int maskColor = 渲染工具.调整透明度(0x80000000, alpha);
        graphics.fill(x, y, x + size, y + maskHeight, maskColor);

        // 绘制剩余秒数
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

    public void 触发预设显示() {
        预设切换时间戳 = System.currentTimeMillis();}
}
