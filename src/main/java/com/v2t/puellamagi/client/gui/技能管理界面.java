// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/技能管理界面.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.gui.component.GUI槽位;
import com.v2t.puellamagi.client.gui.component.GUI滚动条;
import com.v2t.puellamagi.client.gui.component.文本输入对话框;
import com.v2t.puellamagi.client.gui.component.拖拽上下文;
import com.v2t.puellamagi.client.gui.component.技能槽位内容;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.core.network.packets.c2s.槽位配置请求包;
import com.v2t.puellamagi.core.network.packets.c2s.预设切换请求包;
import com.v2t.puellamagi.core.network.packets.c2s.预设管理请求包;
import com.v2t.puellamagi.system.skill.技能槽位数据;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.skill.技能预设;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.网络工具;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.资源工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能管理界面
 *
 * 预设标签交互：
 * - 左键单击：切换预设
 * - 左键双击：重命名预设
 * - 右键单击：删除预设（需确认）
 * - [+] 按钮：新建预设
 */
public class 技能管理界面 extends Screen {

    // ==================== 尺寸常量 ====================
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 180;

    // 技能网格区域（左侧）
    private static final int GRID_X = 10;
    private static final int GRID_Y = 30;
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 4;
    private static final int GRID_CELL_SIZE = 22;
    private static final int GRID_GAP = 3;

    // 技能详情区域（右侧）
    private static final int DETAIL_X = 115;
    private static final int DETAIL_Y = 30;
    private static final int DETAIL_WIDTH = 155;
    private static final int DETAIL_HEIGHT = 95;

    // 技能槽位区域（底部）
    private static final int SLOTS_X = 10;
    private static final int SLOTS_Y = 145;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 6;

    // 顶部标签
    private static final int TAB_Y = 6;
    private static final int TAB_WIDTH = 50;
    private static final int TAB_HEIGHT = 18;
    private static final int ADD_BTN_SIZE = 18;
    private static final int MAX_VISIBLE_TABS = 4;

    // 外部布局按钮
    private static final int LAYOUT_BTN_WIDTH = 40;
    private static final int LAYOUT_BTN_HEIGHT = 16;

    // 双击检测
    private static final long 双击间隔毫秒 = 400;

    // ==================== 状态变量 ====================
    private int guiLeft, guiTop;

    // 技能网格槽位
    private final List<GUI槽位> 网格槽位列表 = new ArrayList<>();

    // 底部技能槽位
    private final List<GUI槽位> 装备槽位列表 = new ArrayList<>();

    // 可用技能列表
    private final List<ResourceLocation> 可用技能列表 = new ArrayList<>();

    // 滚动条
    private GUI滚动条 滚动条;

    // 拖拽上下文
    private final 拖拽上下文<技能槽位内容> 拖拽 = new 拖拽上下文<>();

    // 当前拖拽的内容
    @Nullable
    private 技能槽位内容 当前拖拽内容 = null;

    // 拖拽来源追踪
    private enum 拖拽来源类型 { 无, 网格, 装备槽位 }
    private 拖拽来源类型 当前拖拽来源 = 拖拽来源类型.无;
    private int 拖拽来源索引 = -1;

    // 悬停详情显示
    @Nullable
    private 技能槽位内容 悬停的技能 = null;

    // 双击检测
    private long 上次点击时间 = 0;
    private int 上次点击的标签索引 = -1;

    // 删除确认状态
    private int 待删除的预设索引 = -1;
    private long 删除确认开始时间 = 0;
    private static final long 删除确认超时毫秒 = 3000;

    public 技能管理界面() {
        super(Component.empty());
    }

    // ==================== 初始化 ====================

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;刷新技能列表();
        初始化滚动条();
        初始化网格槽位();
        初始化装备槽位();}

    private void 刷新技能列表() {
        可用技能列表.clear();
        可用技能列表.addAll(技能注册表.获取所有技能ID());
    }

    private void 初始化滚动条() {
        int gridHeight = GRID_ROWS * (GRID_CELL_SIZE + GRID_GAP) - GRID_GAP;
        int scrollBarX = guiLeft + GRID_X + GRID_COLS * (GRID_CELL_SIZE + GRID_GAP);
        int scrollBarY = guiTop + GRID_Y;

        int 总行数 = (int) Math.ceil((double) 可用技能列表.size() / GRID_COLS);滚动条 = new GUI滚动条(scrollBarX, scrollBarY, gridHeight)
                .宽度(6)
                .滚动范围(总行数, GRID_ROWS)
                .滚动回调(this::更新网格槽位内容);
    }

    private void 初始化网格槽位() {
        网格槽位列表.clear();

        int gridLeft = guiLeft + GRID_X;
        int gridTop = guiTop + GRID_Y;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;
                int x = gridLeft + col * (GRID_CELL_SIZE + GRID_GAP);
                int y = gridTop + row * (GRID_CELL_SIZE + GRID_GAP);

                GUI槽位 slot = new GUI槽位(index)
                        .位置(x, y)
                        .尺寸(GRID_CELL_SIZE)
                        .背景(资源工具.技能管理_槽位);

                网格槽位列表.add(slot);
            }
        }

        更新网格槽位内容();
    }

    private void 更新网格槽位内容() {
        int startIndex = 滚动条.获取当前行() * GRID_COLS;

        for (int i = 0; i < 网格槽位列表.size(); i++) {
            GUI槽位 slot = 网格槽位列表.get(i);
            int skillIndex = startIndex + i;

            if (skillIndex < 可用技能列表.size()) {
                ResourceLocation skillId = 可用技能列表.get(skillIndex);
                slot.设置内容(技能槽位内容.从ID创建(skillId));
                slot.禁用(false);
            } else {
                slot.设置内容(null);
                slot.禁用(true);
            }
        }
    }

    private void 初始化装备槽位() {
        装备槽位列表.clear();

        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
        if (capOpt.isEmpty()) {
            return;
        }

        技能预设 preset = capOpt.get().获取当前预设();
        int slotsLeft = guiLeft + SLOTS_X;
        int slotsTop = guiTop + SLOTS_Y;

        for (int i = 0; i < preset.获取槽位数量(); i++) {
            int x = slotsLeft + i * (SLOT_SIZE + SLOT_GAP);

            String keyHint = null;
            if (i < 按键绑定.技能键.length) {
                String key = 按键绑定.技能键[i].getTranslatedKeyMessage().getString();
                keyHint = key.length() > 1 ? key.substring(0, 1) : key;
            }

            GUI槽位 slot = new GUI槽位(i)
                    .位置(x, slotsTop)
                    .尺寸(SLOT_SIZE)
                    .背景(资源工具.技能管理_槽位)
                    .快捷键(keyHint);

            技能槽位数据 data = preset.获取槽位(i);
            if (data != null && !data.是否为空()) {
                slot.设置内容(技能槽位内容.从ID创建(data.获取技能ID()));
            }

            装备槽位列表.add(slot);
        }
    }

    private void 刷新装备槽位内容() {
        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
        if (capOpt.isEmpty()) {
            return;
        }

        技能预设 preset = capOpt.get().获取当前预设();

        for (int i = 0; i < 装备槽位列表.size() && i < preset.获取槽位数量(); i++) {
            GUI槽位 slot = 装备槽位列表.get(i);
            技能槽位数据 data = preset.获取槽位(i);

            if (data != null && !data.是否为空()) {
                slot.设置内容(技能槽位内容.从ID创建(data.获取技能ID()));
            } else {
                slot.设置内容(null);
            }
        }
    }

    // ==================== 网格区域计算 ====================

    private int 获取网格宽度() {
        return GRID_COLS * (GRID_CELL_SIZE + GRID_GAP);
    }

    private int 获取网格高度() {
        return GRID_ROWS * (GRID_CELL_SIZE + GRID_GAP);
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        更新悬停状态(mouseX, mouseY);

        绘制背景(graphics);
        绘制预设标签(graphics, mouseX, mouseY);
        绘制技能网格(graphics, mouseX, mouseY);
        绘制装备槽位(graphics);
        绘制技能详情(graphics);
        绘制布局按钮(graphics, mouseX, mouseY);

        if (拖拽.是否正在拖拽() && 当前拖拽内容 != null) {
            绘制拖拽技能(graphics, mouseX, mouseY);
        }
    }

    private void 绘制背景(GuiGraphics graphics) {
        渲染工具.绘制纹理(graphics, 资源工具.技能管理_背景, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
    }

    private void 绘制预设标签(GuiGraphics graphics, int mouseX, int mouseY) {
        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
        if (capOpt.isEmpty()) {
            return;
        }

        var cap = capOpt.get();
        int presetCount = cap.获取预设数量();
        int currentIndex = cap.获取当前预设索引();

        int startX = guiLeft + 10;
        int y = guiTop + TAB_Y;

        // 绘制预设标签
        int visibleCount = Math.min(presetCount, MAX_VISIBLE_TABS);
        for (int i = 0; i < visibleCount; i++) {
            int x = startX + i * (TAB_WIDTH + 2);
            boolean isActive = (i == currentIndex);
            boolean isHover = mouseX >= x && mouseX < x + TAB_WIDTH
                    && mouseY >= y && mouseY < y + TAB_HEIGHT;
            boolean isPendingDelete = (i == 待删除的预设索引);

            // 选择纹理
            资源工具.纹理信息 tex;
            if (isPendingDelete) {
                tex = 资源工具.技能管理_按钮悬停;
            } else {
                tex = (isActive || isHover) ? 资源工具.技能管理_按钮悬停 : 资源工具.技能管理_按钮普通;
            }渲染工具.绘制纹理(graphics, tex, x, y, TAB_WIDTH, TAB_HEIGHT);

            // 显示文字
            String displayText;
            int textColor;
            if (isPendingDelete) {
                displayText = "点击删除";
                textColor = 0xFF5555;
            } else {
                String name = cap.获取所有预设().get(i).获取名称();
                if (name.length() > 5) {
                    name = name.substring(0, 5) + "..";
                }
                displayText = name;
                textColor = isActive ? 0xFFFFFF : 0xCCCCCC;
            }
            graphics.drawCenteredString(this.font, displayText, x + TAB_WIDTH / 2, y + 5, textColor);
        }

        // 绘制 [+] 按钮
        if (presetCount < 10) {
            int addBtnX = startX + visibleCount * (TAB_WIDTH + 2);
            boolean isAddHover = mouseX >= addBtnX && mouseX < addBtnX + ADD_BTN_SIZE
                    && mouseY >= y && mouseY < y + ADD_BTN_SIZE;

            资源工具.纹理信息 addTex = isAddHover ? 资源工具.技能管理_按钮悬停 : 资源工具.技能管理_按钮普通;
            渲染工具.绘制纹理(graphics, addTex, addBtnX, y, ADD_BTN_SIZE, ADD_BTN_SIZE);
            graphics.drawCenteredString(this.font, "+", addBtnX + ADD_BTN_SIZE / 2, y + 5, 0xFFFFFF);
        }
    }

    private void 绘制布局按钮(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = guiLeft + GUI_WIDTH +4;
        int y = guiTop;

        boolean isHover = mouseX >= x && mouseX < x + LAYOUT_BTN_WIDTH
                && mouseY >= y && mouseY < y + LAYOUT_BTN_HEIGHT;

        资源工具.纹理信息 tex = isHover ? 资源工具.技能管理_按钮悬停 : 资源工具.技能管理_按钮普通;
        渲染工具.绘制纹理(graphics, tex, x, y, LAYOUT_BTN_WIDTH, LAYOUT_BTN_HEIGHT);
        graphics.drawCenteredString(this.font, "布局", x + LAYOUT_BTN_WIDTH / 2, y + 4, 0xFFFFFF);
    }

    private void 绘制技能网格(GuiGraphics graphics, int mouseX, int mouseY) {
        for (GUI槽位 slot : 网格槽位列表) {
            slot.绘制(graphics);
        }滚动条.绘制(graphics, mouseX, mouseY);
    }

    private void 绘制技能详情(GuiGraphics graphics) {
        if (悬停的技能 == null) {
            return;
        }

        int detailLeft = guiLeft + DETAIL_X;
        int detailTop = guiTop + DETAIL_Y;

        List<Component> details = 悬停的技能.获取详情();
        int y = detailTop;
        int lineSpacing = this.font.lineHeight + 2;

        for (int i = 0; i < details.size() && y < detailTop + DETAIL_HEIGHT - lineSpacing; i++) {
            Component line = details.get(i);
            if (line.getString().isEmpty()) {
                y += 4;
            } else {
                int color = (i == 0) ? 0xFFFF88 : 0xCCCCCC;
                graphics.drawString(this.font, line, detailLeft, y, color);
                y += lineSpacing;
            }
        }
    }

    private void 绘制装备槽位(GuiGraphics graphics) {
        int slotsLeft = guiLeft + SLOTS_X;
        int slotsTop = guiTop + SLOTS_Y;

        graphics.drawString(this.font, "技能槽位", slotsLeft, slotsTop - 12, 0xCCCCCC);

        for (GUI槽位 slot : 装备槽位列表) {
            slot.设置拖拽悬停(拖拽.是否正在拖拽() && slot.是否悬停());
            slot.绘制(graphics);
        }
    }

    private void 绘制拖拽技能(GuiGraphics graphics, int mouseX, int mouseY) {
        if (当前拖拽内容 == null) {
            return;
        }

        int size = GRID_CELL_SIZE;
        int x = mouseX - size / 2;
        int y = mouseY - size / 2;

        渲染工具.绘制纹理(graphics, 资源工具.技能管理_槽位, x, y, size, size, 0.7f);

        当前拖拽内容.绘制(graphics, x +2, y + 2, size - 4);
    }

    // ==================== 悬停状态更新 ====================

    private void 更新悬停状态(int mouseX, int mouseY) {
        悬停的技能 = null;

        for (GUI槽位 slot : 网格槽位列表) {
            if (slot.更新悬停(mouseX, mouseY)) {
                if (slot.获取内容() instanceof 技能槽位内容 content) {
                    悬停的技能 = content;
                }
            }
        }

        for (GUI槽位 slot : 装备槽位列表) {
            if (slot.更新悬停(mouseX, mouseY)) {
                if (slot.获取内容() instanceof 技能槽位内容 content) {
                    if (悬停的技能 == null) {
                        悬停的技能 = content;
                    }
                }
            }
        }

        // 检查删除确认超时
        if (待删除的预设索引 >= 0) {
            if (System.currentTimeMillis() - 删除确认开始时间 > 删除确认超时毫秒) {
                待删除的预设索引 = -1;
            }
        }
    }

    // ==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        //滚动条点击
        if (button == 0 && 滚动条.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 预设标签区域交互
        if (处理预设标签点击(mx, my, button)) {
            return true;
        }

        // 右键清除装备槽位
        if (button == 1) {
            for (GUI槽位 slot : 装备槽位列表) {
                if (slot.包含坐标(mouseX, mouseY) && !slot.是否为空()) {
                    网络工具.发送到服务端(槽位配置请求包.清空(slot.获取索引()));
                    var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
                    capOpt.ifPresent(cap -> {
                        cap.获取当前预设().设置槽位技能(slot.获取索引(), null);
                        刷新装备槽位内容();
                    });
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // 左键处理
        if (button == 0) {
            if (处理布局按钮点击(mx, my)) {
                return true;
            }

            // 点击网格槽位
            for (int i = 0; i < 网格槽位列表.size(); i++) {
                GUI槽位 slot = 网格槽位列表.get(i);
                if (slot.包含坐标(mouseX, mouseY) && slot.获取内容() instanceof 技能槽位内容 content) {
                    拖拽.开始按下(content, i, mouseX, mouseY);
                    当前拖拽内容 = content;
                    当前拖拽来源 = 拖拽来源类型.网格;拖拽来源索引 = i;
                    return true;
                }
            }

            // 点击装备槽位
            for (GUI槽位 slot : 装备槽位列表) {
                if (slot.包含坐标(mouseX, mouseY) && slot.获取内容() instanceof 技能槽位内容 content) {
                    拖拽.开始按下(content, slot.获取索引(), mouseX, mouseY);
                    当前拖拽内容 = content;
                    当前拖拽来源 = 拖拽来源类型.装备槽位;
                    拖拽来源索引 = slot.获取索引();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 处理预设标签区域的点击
     * @return 是否处理了点击
     */
    private boolean 处理预设标签点击(int mx, int my, int button) {
        int startX = guiLeft + 10;
        int y = guiTop + TAB_Y;

        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
        if (capOpt.isEmpty()) {
            return false;
        }

        var cap = capOpt.get();
        int presetCount = cap.获取预设数量();
        int visibleCount = Math.min(presetCount, MAX_VISIBLE_TABS);

        // 检查是否点击了预设标签
        for (int i = 0; i < visibleCount; i++) {
            int tabX = startX + i * (TAB_WIDTH + 2);
            if (mx >= tabX && mx < tabX + TAB_WIDTH && my >= y && my < y + TAB_HEIGHT) {

                // 如果正在等待删除确认
                if (待删除的预设索引 == i) {
                    if (button == 0) {
                        // 左键确认删除
                        网络工具.发送到服务端(预设管理请求包.删除(i));
                        cap.删除预设(i);
                        初始化装备槽位();
                        待删除的预设索引 = -1;
                        return true;
                    } else if (button == 1) {
                        // 右键取消
                        待删除的预设索引 = -1;
                        return true;
                    }
                }

                if (button == 0) {
                    // 左键：切换或双击重命名
                    long currentTime = System.currentTimeMillis();

                    // 检查是否是双击
                    if (上次点击的标签索引 == i && (currentTime - 上次点击时间) < 双击间隔毫秒) {
                        // 双击 - 重命名
                        打开重命名对话框(i);
                        上次点击时间 = 0;
                        上次点击的标签索引 = -1;
                    } else {
                        // 单击 - 切换预设
                        网络工具.发送到服务端(new 预设切换请求包(i));
                        cap.切换预设(i);
                        初始化装备槽位();

                        上次点击时间 = currentTime;
                        上次点击的标签索引 = i;
                    }
                    return true;

                } else if (button == 1) {
                    // 右键：进入删除确认状态
                    if (presetCount <= 1) {
                        return true;
                    }

                    待删除的预设索引 = i;
                    删除确认开始时间 = System.currentTimeMillis();
                    return true;
                }
            }
        }

        // 检查是否点击了 [+] 按钮
        if (button == 0 && presetCount < 10) {
            int addBtnX = startX + visibleCount * (TAB_WIDTH + 2);
            if (mx >= addBtnX && mx < addBtnX + ADD_BTN_SIZE
                    && my >= y && my < y + ADD_BTN_SIZE) {
                打开新建对话框();
                return true;
            }
        }

        // 点击了标签区域以外的地方，取消删除确认
        待删除的预设索引 = -1;

        return false;
    }

    /**
     * 打开新建预设对话框
     */
    private void 打开新建对话框() {
        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
        if (capOpt.isEmpty()) {
            return;
        }

        int nextIndex = capOpt.get().获取预设数量() + 1;
        String defaultName = "预设" + nextIndex;

        文本输入对话框.显示(
                本地化工具.GUI("title.input_preset_name"),
                defaultName,
                16,
                result -> {
                    if (result != null && !result.isBlank()) {
                        网络工具.发送到服务端(预设管理请求包.新建(result));
                        能力工具.获取技能能力(Minecraft.getInstance().player).ifPresent(cap -> {
                            cap.添加预设(new 技能预设(result));
                        });
                    }
                }
        );
    }

    /**
     * 打开重命名对话框
     */
    private void 打开重命名对话框(int presetIndex) {
        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
        if (capOpt.isEmpty()) {
            return;
        }

        String currentName = capOpt.get().获取所有预设().get(presetIndex).获取名称();

        文本输入对话框.显示(
                本地化工具.GUI("title.rename_preset"),
                currentName,
                16,
                result -> {
                    if (result != null && !result.isBlank()) {
                        网络工具.发送到服务端(预设管理请求包.重命名(presetIndex, result));
                        能力工具.获取技能能力(Minecraft.getInstance().player).ifPresent(cap -> {
                            if (presetIndex < cap.获取预设数量()) {
                                cap.获取所有预设().get(presetIndex).设置名称(result);
                            }
                        });
                    }
                }
        );
    }

    private boolean 处理布局按钮点击(int mx, int my) {
        int x = guiLeft + GUI_WIDTH + 4;
        int y = guiTop;

        if (mx >= x && mx < x + LAYOUT_BTN_WIDTH && my >= y && my < y + LAYOUT_BTN_HEIGHT) {
            Minecraft.getInstance().setScreen(new 技能栏编辑界面(this));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 滚动条拖动
        if (button == 0 && 滚动条.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        // 技能拖拽
        if (button == 0) {
            拖拽.更新拖拽(mouseX, mouseY);}
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 滚动条释放
        if (滚动条.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        // 技能拖拽结束
        if (button == 0 && 拖拽.是否鼠标按下中()) {
            技能槽位内容 content = 当前拖拽内容;
            拖拽来源类型 来源 = 当前拖拽来源;
            int 来源索引 = 拖拽来源索引;拖拽上下文.松开结果 result = 拖拽.结束拖拽();

            if (result == 拖拽上下文.松开结果.拖拽完成 && content != null) {
                boolean 放到了槽位 = false;

                for (GUI槽位 slot : 装备槽位列表) {
                    if (slot.包含坐标(mouseX, mouseY)) {
                        int 目标索引 = slot.获取索引();
                        放到了槽位 = true;

                        if (来源 == 拖拽来源类型.装备槽位 && 来源索引 != 目标索引) {
                            网络工具.发送到服务端(槽位配置请求包.移动(来源索引, 目标索引));
                        } else if (来源 == 拖拽来源类型.网格) {
                            网络工具.发送到服务端(槽位配置请求包.设置(目标索引, content.获取ID()));
                        }

                        var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
                        capOpt.ifPresent(cap -> {
                            var preset = cap.获取当前预设();
                            if (来源 == 拖拽来源类型.装备槽位 && 来源索引 != 目标索引) {
                                preset.设置槽位技能(来源索引, null);
                            }
                            preset.设置槽位技能(目标索引, content.获取ID());
                            刷新装备槽位内容();
                        });
                        break;
                    }
                }

                if (来源 == 拖拽来源类型.装备槽位 && !放到了槽位) {
                    网络工具.发送到服务端(槽位配置请求包.清空(来源索引));var capOpt = 能力工具.获取技能能力(Minecraft.getInstance().player);
                    capOpt.ifPresent(cap -> {
                        cap.获取当前预设().设置槽位技能(来源索引, null);
                        刷新装备槽位内容();
                    });
                }
            }

            当前拖拽内容 = null;
            当前拖拽来源 = 拖拽来源类型.无;
            拖拽来源索引 = -1;}
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int gridLeft = guiLeft + GRID_X;
        int gridTop = guiTop + GRID_Y;

        if (滚动条.坐标在滚动区域内(mouseX, mouseY, gridLeft, gridTop,
                获取网格宽度() + 10, 获取网格高度())) {
            return 滚动条.mouseScrolled(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ==================== 按键处理 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (按键绑定.技能栏编辑键.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
