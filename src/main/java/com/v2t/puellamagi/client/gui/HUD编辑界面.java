// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/HUD编辑界面.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.gui.hud.I可编辑HUD;
import com.v2t.puellamagi.client.gui.hud.可编辑HUD注册表;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.core.network.packets.c2s.布局更新请求包;
import com.v2t.puellamagi.util.本地化工具;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 通用HUD编辑界面
 */
public class HUD编辑界面 extends Screen {

    @Nullable
    private final Screen 父界面;

    private List<I可编辑HUD> 可编辑列表;

    @Nullable
    private I可编辑HUD 选中的HUD = null;

    @Nullable
    private I可编辑HUD 当前拖动HUD = null;
    private int 拖动起始鼠标X, 拖动起始鼠标Y;
    private int 拖动起始X, 拖动起始Y;

    // 控制面板 - 动态高度
    private static final int 面板宽度 = 140;
    private static final int 按钮高度 = 20;
    private static final int 面板边距 = 8;
    private static final int 按钮间距 = 5;

    private int 面板X, 面板Y;
    private int 面板高度;

    private boolean 正在拖动面板 = false;
    private int 面板拖动起始X, 面板拖动起始Y;
    private int 面板拖动起始鼠标X, 面板拖动起始鼠标Y;

    public HUD编辑界面(@Nullable Screen parent) {
        super(本地化工具.GUI("title.hud_edit"));
        this.父界面 = parent;
    }

    public HUD编辑界面() {
        this(null);
    }

    @Override
    protected void init() {
        super.init();Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            可编辑列表 = 可编辑HUD注册表.获取玩家可见HUD(mc.player);
        } else {
            可编辑列表 = List.of();
        }

        for (I可编辑HUD hud : 可编辑列表) {
            hud.进入编辑模式();
        }

        if (!可编辑列表.isEmpty()) {
            选中的HUD = 可编辑列表.get(0);
        }

        // 计算面板高度并定位
        计算面板高度();
        面板X = this.width - 面板宽度 - 20;
        面板Y = (this.height - 面板高度) / 2;

        刷新控制面板();
    }

    private void 计算面板高度() {
        // 基础高度：标题+ 底部按钮区域
        int height = 面板边距 + 20; // 标题区域

        if (选中的HUD != null) {
            if (选中的HUD.支持方向切换()) {
                height += 按钮高度 + 按钮间距;
            }if (选中的HUD.支持缩放()) {
                height += 按钮高度 + 按钮间距;
            }// 重置当前按钮
            height += 按钮高度 + 按钮间距 + 10;
        }

        // 底部：重置全部 + 保存/取消
        height += 按钮高度 + 按钮间距; // 重置全部
        height += 按钮高度 + 面板边距;// 保存/取消

        面板高度 = height;
    }

    private void 刷新控制面板() {
        this.clearWidgets();
        计算面板高度();

        int btnX = 面板X + 面板边距;
        int btnY = 面板Y + 面板边距 + 20;
        int btnWidth = 面板宽度 - 面板边距 * 2;
        int halfBtnWidth = (btnWidth - 5) / 2;

        if (选中的HUD != null) {
            // 方向切换
            if (选中的HUD.支持方向切换()) {
                addRenderableWidget(Button.builder(本地化工具.GUI("label.horizontal"),
                                btn -> 设置方向(I可编辑HUD.HUD方向.横向))
                        .bounds(btnX, btnY, halfBtnWidth, 按钮高度)
                        .build()
                );
                addRenderableWidget(Button.builder(
                                本地化工具.GUI("label.vertical"),
                                btn -> 设置方向(I可编辑HUD.HUD方向.纵向))
                        .bounds(btnX + halfBtnWidth + 5, btnY, halfBtnWidth, 按钮高度)
                        .build()
                );
                btnY += 按钮高度 + 按钮间距;
            }

            // 缩放调整
            if (选中的HUD.支持缩放()) {
                addRenderableWidget(Button.builder(
                                Component.literal("-"),
                                btn -> 调整缩放(-0.1f))
                        .bounds(btnX, btnY,30, 按钮高度)
                        .build()
                );
                addRenderableWidget(Button.builder(
                                Component.literal("+"),
                                btn -> 调整缩放(0.1f))
                        .bounds(btnX + btnWidth - 30, btnY, 30, 按钮高度)
                        .build()
                );
                btnY += 按钮高度 + 按钮间距;
            }

            // 重置当前HUD按钮
            addRenderableWidget(Button.builder(
                            本地化工具.GUI("button.reset"),
                            btn -> 重置当前HUD())
                    .bounds(btnX, btnY, btnWidth, 按钮高度)
                    .build());
            btnY += 按钮高度 + 10; // 额外间距分隔
        }

        // 重置全部按钮
        addRenderableWidget(Button.builder(
                        本地化工具.GUI("button.reset_all"),
                        btn -> 重置所有HUD())
                .bounds(btnX, btnY, btnWidth, 按钮高度)
                .build()
        );
        btnY += 按钮高度 + 按钮间距;

        // 保存和取消
        addRenderableWidget(Button.builder(
                        本地化工具.GUI("button.save"),
                        btn -> 保存并关闭())
                .bounds(btnX, btnY, halfBtnWidth, 按钮高度)
                .build()
        );
        addRenderableWidget(Button.builder(
                        本地化工具.GUI("button.cancel"),
                        btn -> 取消并关闭())
                .bounds(btnX + halfBtnWidth + 5, btnY, halfBtnWidth, 按钮高度)
                .build()
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x40000000);

        for (I可编辑HUD hud : 可编辑列表) {
            boolean isSelected = (hud == 选中的HUD);
            boolean isHovering = hud.坐标在HUD上(mouseX, mouseY);

            if (isSelected || isHovering) {
                hud.绘制编辑边框(graphics, isSelected);
            }绘制HUD标签(graphics, hud, isSelected, isHovering);
        }绘制控制面板(graphics, mouseX, mouseY);

        if (可编辑列表.isEmpty()) {
            graphics.drawCenteredString(this.font, "当前没有可编辑的HUD",
                    this.width / 2, this.height / 2, 渲染工具.颜色_浅灰);
        } else {
            graphics.drawCenteredString(this.font, "点击选中HUD，拖动调整位置",
                    this.width / 2, 10, 渲染工具.颜色_浅灰);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void 绘制HUD标签(GuiGraphics graphics, I可编辑HUD hud, boolean selected, boolean hovering) {
        int[] pos = hud.获取当前位置();
        int[] size = hud.获取尺寸();

        Component name = hud.获取显示名称();
        int nameWidth = this.font.width(name);
        int nameX = pos[0] + (size[0] - nameWidth) / 2;
        int nameY = pos[1] + size[1] + 4;

        nameX = Math.max(2, Math.min(nameX, this.width - nameWidth - 2));
        nameY = Math.min(nameY, this.height - this.font.lineHeight - 2);

        int bgColor = selected ? 0xE0000000 : (hovering ? 0xC0000000 : 0x80000000);
        graphics.fill(nameX - 3, nameY - 2, nameX + nameWidth + 3, nameY + this.font.lineHeight + 2, bgColor);

        int textColor = selected ? 渲染工具.颜色_金色 : (hovering ? 0xFFFFFF88 : 渲染工具.颜色_白色);
        graphics.drawString(this.font, name, nameX, nameY, textColor);
    }

    private void 绘制控制面板(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(面板X,面板Y, 面板X + 面板宽度, 面板Y + 面板高度, 0xE0222222);

        boolean panelHover = 坐标在面板上(mouseX, mouseY);
        int borderColor = 正在拖动面板 ?渲染工具.颜色_金色
                : (panelHover ? 渲染工具.颜色_浅灰 : 渲染工具.颜色_槽位边框);
        渲染工具.绘制边框矩形(graphics, 面板X, 面板Y, 面板宽度, 面板高度, borderColor);

        String title = 选中的HUD != null ? 选中的HUD.获取显示名称().getString() : "HUD编辑";
        graphics.drawCenteredString(this.font, title,
                面板X + 面板宽度 / 2, 面板Y + 面板边距, 渲染工具.颜色_白色);

        //缩放百分比显示
        if (选中的HUD != null && 选中的HUD.支持缩放()) {
            String scaleText = String.format("%.0f%%", 选中的HUD.获取缩放() * 100);
            int scaleY = 面板Y + 面板边距 + 20;
            if (选中的HUD.支持方向切换()) {
                scaleY += 按钮高度 + 按钮间距;
            }
            graphics.drawCenteredString(this.font, scaleText,
                    面板X + 面板宽度 / 2, scaleY + 5, 渲染工具.颜色_白色);
        }
    }

    private boolean 坐标在面板上(double mouseX, double mouseY) {
        return mouseX >= 面板X && mouseX <= 面板X + 面板宽度
                && mouseY >= 面板Y && mouseY <= 面板Y + 面板高度;
    }

    private void 设置方向(I可编辑HUD.HUD方向 dir) {
        if (选中的HUD != null && 选中的HUD.支持方向切换()) {
            选中的HUD.设置方向(dir);
        }
    }

    private void 调整缩放(float delta) {
        if (选中的HUD != null && 选中的HUD.支持缩放()) {
            float current = 选中的HUD.获取缩放();
            float newScale = Mth.clamp(current + delta,
                    选中的HUD.获取最小缩放(), 选中的HUD.获取最大缩放());
            选中的HUD.设置缩放(newScale);
        }
    }

    private void 重置当前HUD() {
        if (选中的HUD != null) {
            选中的HUD.重置为默认();
        }
    }

    private void 重置所有HUD() {
        for (I可编辑HUD hud : 可编辑列表) {
            hud.重置为默认();
        }
    }

    private void 保存并关闭() {
        for (I可编辑HUD hud : 可编辑列表) {
            hud.退出编辑模式(true);
        }

        if (可编辑列表.stream().anyMatch(hud -> "skill_bar".equals(hud.获取HUD标识()))) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                com.v2t.puellamagi.util.能力工具.获取技能能力(mc.player).ifPresent(cap -> {
                    网络工具.发送到服务端(new 布局更新请求包(cap.获取当前预设().获取布局()));
                });
            }
        }Minecraft.getInstance().setScreen(父界面);
    }

    private void 取消并关闭() {
        for (I可编辑HUD hud : 可编辑列表) {
            hud.退出编辑模式(false);
        }
        Minecraft.getInstance().setScreen(父界面);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (坐标在面板上(mouseX, mouseY)) {
            正在拖动面板 = true;
            面板拖动起始X =面板X;
            面板拖动起始Y = 面板Y;
            面板拖动起始鼠标X = (int) mouseX;
            面板拖动起始鼠标Y = (int) mouseY;
            return true;
        }

        for (I可编辑HUD hud : 可编辑列表) {
            if (hud.坐标在HUD上(mouseX, mouseY)) {
                选中的HUD = hud;
                当前拖动HUD = hud;
                拖动起始鼠标X = (int) mouseX;
                拖动起始鼠标Y = (int) mouseY;
                int[] pos = hud.获取当前位置();
                拖动起始X = pos[0];
                拖动起始Y = pos[1];刷新控制面板();
                return true;
            }
        }

        选中的HUD = null;刷新控制面板();
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        if (正在拖动面板) {
            int deltaX = (int) mouseX - 面板拖动起始鼠标X;
            int deltaY = (int) mouseY - 面板拖动起始鼠标Y;
            面板X = Mth.clamp(面板拖动起始X + deltaX, 0, this.width - 面板宽度);
            面板Y = Mth.clamp(面板拖动起始Y + deltaY, 0, this.height - 面板高度);
            刷新控制面板();
            return true;
        }

        if (当前拖动HUD != null) {
            int deltaX = (int) mouseX - 拖动起始鼠标X;
            int deltaY = (int) mouseY - 拖动起始鼠标Y;

            int newX = 拖动起始X + deltaX;
            int newY = 拖动起始Y + deltaY;

            int[] size = 当前拖动HUD.获取尺寸();
            newX = Mth.clamp(newX, 0, this.width - size[0]);
            newY = Mth.clamp(newY, 0, this.height - size[1]);

            当前拖动HUD.设置位置(newX, newY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            正在拖动面板 = false;
            当前拖动HUD = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256||按键绑定.技能栏编辑键.matches(keyCode, scanCode)) {
            取消并关闭();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        取消并关闭();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
