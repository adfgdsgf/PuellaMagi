// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/搜身界面.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.gui.component.GUI翻页;
import com.v2t.puellamagi.core.network.ModNetwork;
import com.v2t.puellamagi.core.network.packets.c2s.搜身翻页请求包;
import com.v2t.puellamagi.system.interaction.menu.搜身区域管理器;
import com.v2t.puellamagi.system.interaction.menu.搜身菜单;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.v2t.puellamagi.system.interaction.menu.搜身布局常量.*;

/**
 * 搜身界面
 *
 * 双面板布局：
 * - 主面板：背包等大容量槽位
 * - 侧边独立面板：装备、饰品等
 */
@OnlyIn(Dist.CLIENT)
public class 搜身界面 extends AbstractContainerScreen<搜身菜单> {

    // ==================== 翻页组件 ====================

    private GUI翻页 主区域翻页;
    private GUI翻页 侧边区域翻页;

    // ==================== 构造器 ====================

    public 搜身界面(搜身菜单 menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        更新尺寸();
    }

    private void 更新尺寸() {
        this.imageWidth = 主面板宽度;
        this.imageHeight = menu.获取GUI高度();
    }

    // ==================== 侧边面板计算 ====================

    private int 获取侧边面板X() {
        return leftPos + 主面板宽度 + 侧边面板间隔;
    }

    private int 获取侧边面板Y() {
        return topPos + 侧边面板顶部偏移;
    }

    private int 获取侧边面板宽度() {
        搜身区域管理器 侧边区域 = menu.获取侧边区域();
        if (!侧边区域.是否有内容()) return 0;

        int 最大列数 = 1;
        for (搜身区域管理器.区域显示信息 info : 侧边区域.获取当前页区域()) {
            最大列数 = Math.max(最大列数, info.列数());
        }
        return 侧边面板内边距 * 2 + 最大列数 * 槽位尺寸;
    }

    private int 获取侧边面板高度() {
        搜身区域管理器 侧边区域 = menu.获取侧边区域();
        if (!侧边区域.是否有内容()) return 0;

        int 内容高度 = 侧边区域.获取当前页内容高度();
        int 高度 = 侧边面板内边距 * 2 + 内容高度;

        if (侧边区域.需要翻页()) {
            高度 += 16;
        }
        return 高度;
    }

    // ==================== 初始化 ====================

    @Override
    protected void init() {
        super.init();

        this.titleLabelY = 6;
        this.inventoryLabelY = menu.获取搜身者背包Y() + 2;

        // 主区域翻页组件
        搜身区域管理器 主区域 = menu.获取主区域();
        if (主区域.需要翻页()) {
            int y = topPos + 顶部边距 + 主区域.获取当前页内容高度() - 14;
            主区域翻页 = new GUI翻页(leftPos + 主区域左边距, y)
                    .总数量(主区域.获取总页数())
                    .每页容量(1)
                    .翻页回调(this::主区域翻页回调);
            主区域翻页.设置当前页(主区域.获取当前页());
        } else {
            主区域翻页 = null;
        }

        // 侧边区域翻页组件
        搜身区域管理器 侧边区域 = menu.获取侧边区域();
        if (侧边区域.需要翻页()) {
            int panelX = 获取侧边面板X();
            int panelH = 获取侧边面板高度();
            int x = panelX + 侧边面板内边距;
            int y = 获取侧边面板Y() + panelH -侧边面板内边距 - 12;
            侧边区域翻页 = new GUI翻页(x, y)
                    .总数量(侧边区域.获取总页数())
                    .每页容量(1)
                    .翻页回调(this::侧边区域翻页回调);
            侧边区域翻页.设置当前页(侧边区域.获取当前页());
        } else {
            侧边区域翻页 = null;
        }
    }

    private void 主区域翻页回调(int page) {
        ModNetwork.getChannel().sendToServer(
                new 搜身翻页请求包(搜身翻页请求包.区域.主区域, page)
        );
        menu.主区域跳转到页(page);刷新界面();
    }

    private void 侧边区域翻页回调(int page) {
        ModNetwork.getChannel().sendToServer(
                new 搜身翻页请求包(搜身翻页请求包.区域.侧边区域, page)
        );
        menu.侧边区域跳转到页(page);
        刷新界面();
    }

    private void 刷新界面() {
        更新尺寸();
        rebuildWidgets();
    }

    // ==================== 渲染 ====================

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        //===== 主面板 =====
        绘制面板(gui, leftPos, topPos, imageWidth, imageHeight);

        //主区域槽位背景
        绘制区域槽位背景(gui, menu.获取主区域());

        // 分隔线
        int separatorY = topPos + menu.获取搜身者背包Y() - 3;
        gui.fill(leftPos + 7, separatorY, leftPos + imageWidth - 7, separatorY + 1, 0xFF373737);
        gui.fill(leftPos +7, separatorY + 1, leftPos + imageWidth - 7, separatorY + 2, 0xFFFFFFFF);

        // 搜身者背包槽位背景
        绘制搜身者背包背景(gui);

        // 主区域翻页
        if (主区域翻页 != null) {
            主区域翻页.绘制(gui, mouseX, mouseY);
        }

        // ===== 侧边面板（完全独立） =====
        搜身区域管理器 侧边区域 = menu.获取侧边区域();
        if (侧边区域.是否有内容()) {
            int panelX = 获取侧边面板X();
            int panelY = 获取侧边面板Y();
            int panelW = 获取侧边面板宽度();
            int panelH = 获取侧边面板高度();绘制面板(gui, panelX, panelY, panelW, panelH);
            绘制区域槽位背景(gui, 侧边区域);

            if (侧边区域翻页 != null) {
                侧边区域翻页.绘制(gui, mouseX, mouseY);
            }
        }
    }

    private void 绘制面板(GuiGraphics gui, int x, int y, int w, int h) {
        gui.fill(x, y, x + w, y + h, 0xFFC6C6C6);

        gui.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        gui.fill(x + 1, y + h - 1, x + w, y + h, 0xFF373737);
        gui.fill(x + w - 1, y + 1, x + w, y + h, 0xFF373737);
    }

    private void 绘制区域槽位背景(GuiGraphics gui,搜身区域管理器 区域) {
        for (搜身区域管理器.区域显示信息 info : 区域.获取当前页区域()) {
            int baseX = leftPos + info.相对X();
            int baseY = topPos + info.相对Y() + 标签高度;
            int columns = info.列数();

            for (int i = 0; i < info.槽位数量(); i++) {
                int row = i / columns;
                int col = i % columns;
                绘制槽位背景(gui, baseX + col * 槽位尺寸, baseY + row * 槽位尺寸);
            }
        }
    }

    private void 绘制搜身者背包背景(GuiGraphics gui) {
        int baseX = leftPos + 主区域左边距;
        int baseY = topPos + menu.获取搜身者背包Y() + 标签高度;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                绘制槽位背景(gui, baseX + col * 槽位尺寸, baseY + row * 槽位尺寸);
            }
        }

        int hotbarY = baseY + 3 * 槽位尺寸 + 4;
        for (int col = 0; col < 9; col++) {
            绘制槽位背景(gui, baseX + col * 槽位尺寸, hotbarY);
        }
    }

    private void 绘制槽位背景(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 槽位尺寸, y + 1, 0xFF373737);
        gui.fill(x, y, x + 1, y + 槽位尺寸, 0xFF373737);
        gui.fill(x + 槽位尺寸 - 1, y, x +槽位尺寸, y + 槽位尺寸, 0xFFFFFFFF);
        gui.fill(x, y + 槽位尺寸 - 1, x + 槽位尺寸, y + 槽位尺寸, 0xFFFFFFFF);gui.fill(x + 1, y + 1, x + 槽位尺寸 - 1, y + 槽位尺寸 - 1, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        Component targetName = Component.translatable("gui.puellamagi.search.target",
                menu.获取目标玩家().getDisplayName());
        gui.drawString(font, targetName, titleLabelX, titleLabelY, 0xFF404040, false);

        for (搜身区域管理器.区域显示信息 info : menu.获取主区域().获取当前页区域()) {
            Component label = Component.translatable(info.显示名称键());
            gui.drawString(font, label, info.相对X(), info.相对Y() + 2, 0xFF404040, false);
        }

        for (搜身区域管理器.区域显示信息 info : menu.获取侧边区域().获取当前页区域()) {
            Component label = Component.translatable(info.显示名称键());
            gui.drawString(font, label, info.相对X(), info.相对Y() + 2, 0xFF404040, false);
        }

        Component yourInv = Component.translatable("gui.puellamagi.search.your_inventory");
        gui.drawString(font, yourInv, 主区域左边距, inventoryLabelY, 0xFF404040, false);
    }

    // ==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (主区域翻页 != null && 主区域翻页.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (侧边区域翻页 != null && 侧边区域翻页.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (主区域翻页 != null && 主区域翻页.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (侧边区域翻页 != null && 侧边区域翻页.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
