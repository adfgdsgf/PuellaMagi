// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/技能栏编辑界面.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.客户端状态管理;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.core.network.packets.c2s.布局更新请求包;
import com.v2t.puellamagi.system.skill.布局配置;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.网络工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 技能栏编辑界面
 *
 * 支持：
 * - 拖动技能栏调整位置
 * -拖动控制面板避免遮挡
 * - 面板位置持久化
 */
public class 技能栏编辑界面 extends Screen {

    @Nullable
    private final Screen 父界面;

    // 控制面板尺寸
    private static final int 面板宽度 = 110;
    private static final int 面板高度 = 100;
    private static final int 按钮高度 = 20;
    private static final int 面板边距 = 5;

    // 面板位置（含边距的完整区域）
    private int 面板X, 面板Y;

    // 拖动状态
    private enum 拖动目标 { 无, 技能栏, 面板 }
    private 拖动目标 当前拖动 = 拖动目标.无;
    private int 拖动起始鼠标X, 拖动起始鼠标Y;
    private int 拖动起始偏移X, 拖动起始偏移Y;

    public 技能栏编辑界面(@Nullable Screen parent) {
        super(Component.empty());
        this.父界面 = parent;
    }

    public 技能栏编辑界面() {
        this(null);
    }

    @Override
    protected void init() {
        super.init();
        技能栏HUD.INSTANCE.进入编辑模式();
        // 加载面板位置（或使用默认）
        int savedX = 客户端状态管理.获取编辑面板X();
        int savedY = 客户端状态管理.获取编辑面板Y();

        if (savedX >= 0 && savedY >= 0) {
            // 使用保存的位置，但确保在屏幕内
            面板X = Math.min(savedX, this.width - 面板宽度 - 面板边距 * 2);
            面板Y = Math.min(savedY, this.height - 面板高度 - 面板边距 * 2);
            面板X = Math.max(0, 面板X);
            面板Y = Math.max(0, 面板Y);
        } else {
            // 默认右上角
            面板X = this.width - 面板宽度 - 面板边距 * 2 - 10;
            面板Y = 10;
        }

        创建控制面板();
    }

    private void 创建控制面板() {
        // 清空现有按钮
        this.clearWidgets();

        int btnX = 面板X + 面板边距;
        int btnY = 面板Y + 面板边距;
        int btnWidth = 50;

        // 第一行：方向切换
        addRenderableWidget(Button.builder(
                        本地化工具.GUI("label.horizontal"),
                        btn -> 设置方向(布局配置.方向.横向))
                .bounds(btnX, btnY, btnWidth, 按钮高度)
                .build()
        );

        addRenderableWidget(Button.builder(
                        本地化工具.GUI("label.vertical"),
                        btn -> 设置方向(布局配置.方向.纵向))
                .bounds(btnX + btnWidth + 5, btnY, btnWidth, 按钮高度)
                .build()
        );

        // 第二行：缩放调整
        addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        btn -> 调整缩放(-0.1f))
                .bounds(btnX, btnY + 25, 25, 按钮高度)
                .build()
        );

        addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        btn -> 调整缩放(0.1f))
                .bounds(btnX + 面板宽度 - 25, btnY + 25, 25, 按钮高度)
                .build()
        );

        // 第三行：默认按钮（居中）
        addRenderableWidget(Button.builder(
                        本地化工具.GUI("button.reset"),
                        btn -> 恢复默认())
                .bounds(btnX + (面板宽度 - btnWidth) / 2, btnY + 50, btnWidth, 按钮高度)
                .build()
        );

        // 第四行：保存和取消
        addRenderableWidget(Button.builder(
                        本地化工具.GUI("button.save"),
                        btn -> 保存并关闭())
                .bounds(btnX, btnY + 75, btnWidth, 按钮高度)
                .build()
        );

        addRenderableWidget(Button.builder(
                        本地化工具.GUI("button.cancel"),
                        btn -> 取消并关闭())
                .bounds(btnX + btnWidth + 5, btnY + 75, btnWidth, 按钮高度)
                .build()
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制面板背景
        int bgX = 面板X;
        int bgY = 面板Y;
        int bgW = 面板宽度 + 面板边距 * 2;
        int bgH = 面板高度 + 面板边距 * 2;

        graphics.fill(bgX, bgY, bgX + bgW, bgY + bgH, 0xC0000000);

        // 面板边框（拖动时高亮）
        int borderColor = 当前拖动 == 拖动目标.面板 ? 渲染工具.颜色_金色
                : 渲染工具.颜色_槽位边框;
        渲染工具.绘制边框矩形(graphics, bgX, bgY, bgW, bgH, borderColor);

        // 缩放百分比显示
        布局配置 layout = 技能栏HUD.INSTANCE.获取编辑中布局();
        if (layout != null) {
            String scaleText = String.format("%.0f%%", layout.获取缩放比例() * 100);
            graphics.drawCenteredString(this.font, scaleText,
                    面板X + 面板边距 + 面板宽度 / 2,
                    面板Y + 面板边距 + 28,
                    渲染工具.颜色_白色);
        }

        // 底部提示
        graphics.drawCenteredString(this.font, "拖动技能栏或面板调整位置",
                this.width / 2, this.height - 15, 渲染工具.颜色_浅灰);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button !=0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 先检查是否点击了按钮（让按钮优先处理）
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 检查是否点击了面板区域（但不在按钮上）
        if (坐标在面板上(mouseX, mouseY)) {
            当前拖动 = 拖动目标.面板;
            拖动起始鼠标X = (int) mouseX;
            拖动起始鼠标Y = (int) mouseY;
            拖动起始偏移X = 面板X;
            拖动起始偏移Y = 面板Y;
            return true;
        }

        // 检查是否点击了技能栏
        if (技能栏HUD.INSTANCE.坐标在技能栏上(mouseX, mouseY)) {
            当前拖动 = 拖动目标.技能栏;
            拖动起始鼠标X = (int) mouseX;
            拖动起始鼠标Y = (int) mouseY;
            int[] offset = 技能栏HUD.INSTANCE.获取当前偏移();
            拖动起始偏移X = offset[0];
            拖动起始偏移Y = offset[1];
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0|| 当前拖动 == 拖动目标.无) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        int deltaX = (int) mouseX - 拖动起始鼠标X;
        int deltaY = (int) mouseY - 拖动起始鼠标Y;

        if (当前拖动 == 拖动目标.技能栏) {
            技能栏HUD.INSTANCE.设置位置偏移(拖动起始偏移X + deltaX, 拖动起始偏移Y + deltaY);
            return true;
        }

        if (当前拖动 == 拖动目标.面板) {
            // 计算新位置，限制在屏幕内
            int newX = 拖动起始偏移X + deltaX;
            int newY = 拖动起始偏移Y + deltaY;

            int bgW = 面板宽度 + 面板边距 * 2;
            int bgH = 面板高度 + 面板边距 * 2;

            面板X = Math.max(0, Math.min(newX, this.width - bgW));
            面板Y = Math.max(0, Math.min(newY, this.height - bgH));

            // 重新创建按钮（更新位置）
            创建控制面板();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && 当前拖动 != 拖动目标.无) {
            // 如果拖动了面板，保存位置
            if (当前拖动 == 拖动目标.面板) {
                客户端状态管理.设置编辑面板位置(面板X, 面板Y);
            }
            当前拖动 = 拖动目标.无;}
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 检查坐标是否在面板区域内
     */
    private boolean 坐标在面板上(double mouseX, double mouseY) {
        int bgW = 面板宽度 + 面板边距 * 2;
        int bgH = 面板高度 + 面板边距 * 2;
        return mouseX >= 面板X && mouseX <= 面板X + bgW
                && mouseY >= 面板Y && mouseY <= 面板Y + bgH;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (按键绑定.技能栏编辑键.matches(keyCode, scanCode)) {
            取消并关闭();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void 设置方向(布局配置.方向 dir) {
        布局配置 layout = 技能栏HUD.INSTANCE.获取编辑中布局();
        if (layout != null) {
            layout.设置排列方向(dir);
        }
    }

    private void 调整缩放(float delta) {
        布局配置 layout = 技能栏HUD.INSTANCE.获取编辑中布局();
        if (layout != null) {
            layout.设置缩放比例(layout.获取缩放比例() + delta);
        }
    }

    /**
     * 恢复默认布局
     */
    private void 恢复默认() {
        布局配置 默认布局 = new 布局配置();
        技能栏HUD.INSTANCE.设置编辑中布局(默认布局);
    }

    private void 保存并关闭() {
        布局配置 layout = 技能栏HUD.INSTANCE.获取编辑中布局();
        if (layout != null) {
            网络工具.发送到服务端(new 布局更新请求包(layout));
        }

        技能栏HUD.INSTANCE.退出编辑模式(true);
        Minecraft.getInstance().setScreen(父界面);
    }

    private void 取消并关闭() {
        技能栏HUD.INSTANCE.退出编辑模式(false);
        Minecraft.getInstance().setScreen(父界面);
    }

    @Override
    public void onClose() {
        技能栏HUD.INSTANCE.退出编辑模式(false);
        Minecraft.getInstance().setScreen(父界面);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
