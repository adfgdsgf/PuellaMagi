// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/队伍界面.java

package com.v2t.puellamagi.client.gui;

import com.v2t.puellamagi.client.客户端队伍缓存;
import com.v2t.puellamagi.client.gui.component.*;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.core.network.packets.c2s.队伍操作请求包;
import com.v2t.puellamagi.core.network.packets.c2s.队伍邀请请求包;
import com.v2t.puellamagi.core.network.packets.c2s.队伍邀请响应包;
import com.v2t.puellamagi.core.network.packets.c2s.队伍配置更新包;
import com.v2t.puellamagi.system.team.队伍个人配置;
import com.v2t.puellamagi.system.team.队伍成员数据;
import com.v2t.puellamagi.system.team.队伍数据;
import com.v2t.puellamagi.system.team.队伍权限;
import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 队伍管理界面
 *
 * 使用通用组件构建：
 * - GUI面板：左侧成员列表、右侧配置面板
 * - GUI按钮/GUI确认按钮：底部操作区（自适应宽度）
 * - GUI开关：配置项切换
 * - GUI玩家头像：成员行头像
 * - GUI覆盖层：邀请弹窗
 */
public class 队伍界面 extends Screen {

    // ==================== 尺寸常量 ====================

    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 200;
    private static final int TITLE_HEIGHT = 22;
    private static final int CONTENT_Y_OFFSET = TITLE_HEIGHT + 2;
    private static final int BOTTOM_HEIGHT = 30;
    private static final int CONTENT_HEIGHT = GUI_HEIGHT - CONTENT_Y_OFFSET - BOTTOM_HEIGHT;

    // 面板
    private static final int LEFT_PANEL_X = 6;
    private static final int LEFT_PANEL_WIDTH = 130;
    private static final int RIGHT_PANEL_X = 142;
    private static final int RIGHT_PANEL_WIDTH = 132;

    // 行高
    private static final int MEMBER_ROW_HEIGHT = 20;
    private static final int CONFIG_ROW_HEIGHT = 22;

    // 成员行内部布局
    private static final int HEAD_SIZE = 14;
    private static final int HEAD_MARGIN = 3;
    private static final int ROLE_TAG_MARGIN = 3;

    // 按钮
    private static final int BTN_HEIGHT = 16;
    private static final int BTN_GAP = 4;
    private static final int TOGGLE_WIDTH = 28;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int BOTTOM_AVAILABLE_WIDTH = GUI_WIDTH - 16;

    // 邀请面板
    private static final int INVITE_WIDTH = 200;
    private static final int INVITE_HEIGHT = 170;
    private static final int PLAYER_ROW_HEIGHT = 18;
    private static final int INVITE_LIST_ROWS = 6;
    private static final int SCROLLBAR_WIDTH = 6;

    // ==================== 颜色常量 ====================

    private static final int 背景色 = 0xF0181818;
    private static final int 标题栏色 = 0xFF222233;
    private static final int 行偶数色 = 0xFF252525;
    private static final int 行奇数色 = 0xFF1E1E1E;
    private static final int 行悬停色 = 0xFF353535;
    private static final int 行选中色 = 0xFF2A3A4A;
    private static final int 邀请面板背景色 = 0xFF222222;

    private static final int 文字白= 0xFFFFFF;
    private static final int 文字灰 = 0xAAAAAA;
    private static final int 文字暗灰 = 0x777777;
    private static final int 文字金 = 0xFFD700;
    private static final int 文字红 = 0xFF5555;
    private static final int 文字绿 = 0x55FF55;
    private static final int 文字蓝 = 0x5599FF;

    // ==================== 组件实例 ====================

    @Nullable
    private GUI面板 成员面板;
    @Nullable
    private GUI面板 配置面板;
    @Nullable
    private GUI覆盖层 邀请覆盖层;

    //确认按钮
    private final GUI确认按钮 离开按钮 = new GUI确认按钮(
            Component.translatable("gui.puellamagi.team.button.leave"),
            Component.translatable("gui.puellamagi.team.button.confirm_leave"), true);
    private final GUI确认按钮 解散按钮 = new GUI确认按钮(
            Component.translatable("gui.puellamagi.team.button.disband"),
            Component.translatable("gui.puellamagi.team.button.confirm_disband"), true);
    private final GUI确认按钮 踢出按钮 = new GUI确认按钮(
            Component.translatable("gui.puellamagi.team.button.kick"),
            Component.translatable("gui.puellamagi.team.button.confirm_kick"), true);
    private final GUI确认按钮 转移按钮 = new GUI确认按钮(
            Component.translatable("gui.puellamagi.team.button.transfer"),
            Component.translatable("gui.puellamagi.team.button.confirm_transfer"), false);

    // 滚动条
    @Nullable
    private GUI滚动条 成员滚动条;
    @Nullable
    private GUI滚动条 配置滚动条;
    @Nullable
    private GUI滚动条 在线玩家滚动条;

    // ==================== 状态变量 ====================

    @Nullable
    private final Screen 父界面;
    private int guiLeft, guiTop;

    // 成员选中
    @Nullable
    private UUID 选中的成员UUID = null;

    // 邀请面板
    private boolean 邀请面板打开 = false;
    private boolean 强制添加模式 = false;
    @Nullable
    private EditBox 邀请输入框;
    private String 保存的输入文本 = "";
    private final List<PlayerInfo> 过滤后的在线玩家 = new ArrayList<>();

    // 在线状态缓存
    private final Set<UUID> 在线UUID集合 = new HashSet<>();

    // ==================== 底部按钮 ====================

    /**
     * 底部按钮定义（运行时动态生成）
     */
    private record 底部按钮项(Component 文字, int 原始宽度, boolean 危险, Runnable 动作) {}

    // ==================== 构造 ====================

    public 队伍界面(@Nullable Screen parent) {
        super(Component.translatable("gui.puellamagi.title.team"));
        this.父界面 = parent;
    }

    // ==================== 生命周期 ====================

    @Override
    protected void init() {
        super.init();

        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        初始化面板();
        初始化滚动条();
        初始化邀请输入框();
    }

    private void 初始化面板() {
        成员面板 = new GUI面板(guiLeft + LEFT_PANEL_X, guiTop + CONTENT_Y_OFFSET,
                LEFT_PANEL_WIDTH, CONTENT_HEIGHT)
                .预留滚动条(SCROLLBAR_WIDTH + 2);

        配置面板 = new GUI面板(guiLeft + RIGHT_PANEL_X, guiTop + CONTENT_Y_OFFSET,
                RIGHT_PANEL_WIDTH, CONTENT_HEIGHT)
                .预留滚动条(SCROLLBAR_WIDTH + 2);

        邀请覆盖层 = new GUI覆盖层(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
    }

    private void 初始化滚动条() {
        int[] memberScroll = 成员面板.获取滚动条区域();
        成员滚动条 = new GUI滚动条(memberScroll[0], memberScroll[1], memberScroll[2])
                .宽度(SCROLLBAR_WIDTH)
                .滚动范围(0, memberScroll[2] / MEMBER_ROW_HEIGHT);

        int[] configScroll = 配置面板.获取滚动条区域();
        配置滚动条 = new GUI滚动条(configScroll[0], configScroll[1], configScroll[2])
                .宽度(SCROLLBAR_WIDTH)
                .滚动范围(0, configScroll[2] / CONFIG_ROW_HEIGHT);

        int inviteX = guiLeft + (GUI_WIDTH - INVITE_WIDTH) / 2;
        int inviteY = guiTop + (GUI_HEIGHT - INVITE_HEIGHT) / 2;
        int listY = inviteY + 56;
        int listH = INVITE_LIST_ROWS * PLAYER_ROW_HEIGHT;

        在线玩家滚动条 = new GUI滚动条(
                inviteX + INVITE_WIDTH - SCROLLBAR_WIDTH - 8,
                listY, listH
        ).宽度(SCROLLBAR_WIDTH).滚动范围(0, INVITE_LIST_ROWS);
    }

    private void 初始化邀请输入框() {
        int inviteX = guiLeft + (GUI_WIDTH - INVITE_WIDTH) / 2;
        int inviteY = guiTop + (GUI_HEIGHT - INVITE_HEIGHT) / 2;

        邀请输入框 = new EditBox(font, inviteX +10, inviteY + 22, INVITE_WIDTH - 20, 16,
                Component.translatable("gui.puellamagi.team.invite.input_hint"));
        邀请输入框.setMaxLength(16);
        邀请输入框.setVisible(邀请面板打开);
        邀请输入框.setValue(保存的输入文本);
        邀请输入框.setResponder(text -> {
            保存的输入文本 = text;
            刷新过滤后的在线玩家();
        });
        addWidget(邀请输入框);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(父界面);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== 渲染主入口 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        刷新在线状态();
        检查所有确认超时();
        校验选中状态();

        int bottomMouseX = 邀请面板打开 ? -9999 : mouseX;
        int bottomMouseY = 邀请面板打开 ? -9999 : mouseY;

        绘制通用背景(graphics);
        绘制标题栏(graphics);

        if (客户端队伍缓存.有队伍()) {
            更新滚动条数据();
            绘制有队伍状态(graphics, bottomMouseX, bottomMouseY);
        } else {
            绘制无队伍状态(graphics, bottomMouseX, bottomMouseY);
        }

        if (邀请面板打开) {
            绘制邀请面板(graphics, mouseX, mouseY);
        }
    }

    private void 校验选中状态() {
        if (选中的成员UUID == null) return;
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null || !team.是成员(选中的成员UUID)) {
            选中的成员UUID = null;
            重置上下文确认();
        }
    }

    private void 检查所有确认超时() {
        离开按钮.检查超时();
        解散按钮.检查超时();
        踢出按钮.检查超时();
        转移按钮.检查超时();
    }

    // ==================== 通用绘制 ====================

    private void 绘制通用背景(GuiGraphics graphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 背景色);
        渲染工具.绘制边框矩形(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, GUI面板.默认边框色);
    }

    private void 绘制标题栏(GuiGraphics graphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + TITLE_HEIGHT, 标题栏色);
        渲染工具.绘制居中文本(graphics, font,
                Component.translatable("gui.puellamagi.title.team"),
                guiLeft + GUI_WIDTH / 2, guiTop + 7, 文字白);
    }

    // ==================== 无队伍状态 ====================

    private void 绘制无队伍状态(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = guiLeft + GUI_WIDTH / 2;
        int contentY = guiTop + CONTENT_Y_OFFSET;
        渲染工具.绘制居中文本(graphics, font,
                Component.translatable("gui.puellamagi.team.no_team_hint"),
                centerX, contentY + 15, 文字灰);

        int createBtnW = 80;
        int createBtnX = centerX - createBtnW / 2;
        int createBtnY = contentY + 35;
        GUI按钮.绘制(graphics, font, createBtnX, createBtnY, createBtnW, BTN_HEIGHT + 2,
                Component.translatable("gui.puellamagi.team.button.create"), mouseX, mouseY);

        var invites = 客户端队伍缓存.获取待处理邀请();
        int inviteAreaY = contentY + 65;

        Component inviteTitle = invites.isEmpty()
                ? Component.translatable("gui.puellamagi.team.no_invites")
                : Component.translatable("gui.puellamagi.team.pending_invites", invites.size());
        graphics.drawString(font, inviteTitle, guiLeft + 10, inviteAreaY, 文字灰);

        int inviteY = inviteAreaY + 14;
        for (int i = 0; i < invites.size() && i < 4; i++) {
            绘制邀请条目(graphics, guiLeft + 10, inviteY, GUI_WIDTH - 20,
                    invites.get(i), mouseX, mouseY);
            inviteY += 22;
        }
    }

    private void 绘制邀请条目(GuiGraphics graphics, int x, int y, int w,客户端队伍缓存.邀请信息 invite, int mouseX, int mouseY) {
        graphics.fill(x, y, x + w, y + 20, 行偶数色);
        graphics.drawString(font, invite.邀请者名称(), x + 4, y + 6, 文字白);

        GUI按钮.绘制小型(graphics, font, x + w - 80, y + 3, 35, 14,
                Component.translatable("gui.puellamagi.team.button.accept"),
                mouseX, mouseY, 文字绿, false);

        GUI按钮.绘制小型(graphics, font, x + w - 40, y + 3, 35, 14,
                Component.translatable("gui.puellamagi.team.button.reject"),
                mouseX, mouseY, 文字红, false);
    }

    // ==================== 有队伍状态 ====================

    private void 更新滚动条数据() {
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null) return;

        int memberVisibleRows = CONTENT_HEIGHT / MEMBER_ROW_HEIGHT;
        if (成员滚动条 != null) {
            成员滚动条.滚动范围(team.获取成员数量(), memberVisibleRows);
        }

        int configVisibleRows = CONTENT_HEIGHT / CONFIG_ROW_HEIGHT;
        if (配置滚动条 != null) {
            配置滚动条.滚动范围(队伍个人配置.获取所有配置键().length, configVisibleRows);
        }
    }

    private void 绘制有队伍状态(GuiGraphics graphics, int mouseX, int mouseY) {绘制成员列表(graphics, mouseX, mouseY);
        绘制配置列表(graphics, mouseX, mouseY);
        绘制底部按钮(graphics, mouseX, mouseY);
    }

    private void 绘制成员列表(GuiGraphics graphics, int mouseX, int mouseY) {
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null ||成员面板 == null || 成员滚动条 == null) return;

        成员面板.标题(Component.translatable("gui.puellamagi.team.label.members", team.获取成员数量()));
        成员面板.绘制(graphics, font);

        int[] content = 成员面板.获取内容区域();
        int listX = content[0];
        int listY = content[1];
        int listW = content[2];
        int startRow = 成员滚动条.获取当前行();

        UUID localUUID = Objects.requireNonNull(minecraft).player != null
                ? minecraft.player.getUUID() : null;
        List<UUID> members = team.获取所有成员UUID();
        int visibleRows = content[3] / MEMBER_ROW_HEIGHT;

        for (int i = 0; i < visibleRows && (startRow + i) < members.size(); i++) {
            UUID memberUUID = members.get(startRow + i);
            int rowY = listY + i * MEMBER_ROW_HEIGHT;

            if (rowY + MEMBER_ROW_HEIGHT >成员面板.获取Y() + 成员面板.获取高度()) break;

            绘制成员行(graphics, listX, rowY, listW,
                    team, memberUUID, localUUID, i, mouseX, mouseY);
        }

        成员滚动条.绘制(graphics, mouseX, mouseY);
    }

    private void 绘制成员行(GuiGraphics graphics, int x, int y, int w,队伍数据 team, UUID memberUUID, UUID localUUID,
                            int rowIndex, int mouseX, int mouseY) {
        boolean isHover = 渲染工具.鼠标在区域内(mouseX, mouseY, x, y, w, MEMBER_ROW_HEIGHT);
        boolean isSelected = memberUUID.equals(选中的成员UUID);
        boolean isOnline = 在线UUID集合.contains(memberUUID);
        boolean isSelf = memberUUID.equals(localUUID);

        队伍成员数据 memberData = (队伍成员数据) team.获取成员(memberUUID).orElse(null);
        if (memberData == null) return;

        // 行背景
        int bgColor;
        if (isSelected) {
            bgColor = 行选中色;
        } else if (isHover) {
            bgColor = 行悬停色;
        } else {
            bgColor = rowIndex % 2 == 0 ? 行偶数色 : 行奇数色;
        }
        graphics.fill(x, y, x + w, y + MEMBER_ROW_HEIGHT, bgColor);

        if (isSelected) {
            渲染工具.绘制边框矩形(graphics, x, y, w, MEMBER_ROW_HEIGHT, 文字蓝);
        }

        int drawX = x + HEAD_MARGIN;

        // 在线状态点
        int dotY = y + (MEMBER_ROW_HEIGHT - 4) / 2;
        int dotColor = isOnline ? 文字绿 : 文字暗灰;
        graphics.fill(drawX, dotY, drawX + 4, dotY + 4, 0xFF000000 | dotColor);
        drawX += 6;

        // 职位标签
        Component roleTag = memberData.获取职位().获取显示名称();
        int roleTagWidth = font.width(roleTag);
        int roleTagColor = memberData.有权限(队伍权限.解散队伍) ? 文字金 : 文字灰;
        graphics.drawString(font, roleTag, drawX, y + 6, roleTagColor, false);
        drawX += roleTagWidth + ROLE_TAG_MARGIN;

        // 玩家头像
        GUI玩家头像.绘制(graphics, memberUUID, drawX, y + (MEMBER_ROW_HEIGHT - HEAD_SIZE) / 2, HEAD_SIZE);
        drawX += HEAD_SIZE + HEAD_MARGIN;

        // 名字（截断防溢出）
        String name =获取玩家名称(memberUUID);
        int nameColor = isSelf ? 文字金 : 文字白;
        int maxNameWidth = x + w - drawX - 2;
        if (font.width(name) > maxNameWidth) {
            while (font.width(name + "..") > maxNameWidth && name.length() > 1) {
                name = name.substring(0, name.length() - 1);
            }
            name = name + "..";
        }
        graphics.drawString(font, name, drawX, y + 6, nameColor, false);
    }

    private void 绘制配置列表(GuiGraphics graphics, int mouseX, int mouseY) {
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null || 配置面板 == null || 配置滚动条 == null) return;

        UUID localUUID = Objects.requireNonNull(minecraft).player != null
                ? minecraft.player.getUUID() : null;

        配置面板.标题(Component.translatable("gui.puellamagi.team.label.config"));
        配置面板.绘制(graphics, font);

        int[] content = 配置面板.获取内容区域();
        int listX = content[0];
        int listY = content[1];
        int listW = content[2];
        int startRow = 配置滚动条.获取当前行();
        int visibleRows = content[3] / CONFIG_ROW_HEIGHT;

        队伍成员数据 myData = localUUID != null
                ? (队伍成员数据) team.获取成员(localUUID).orElse(null) : null;

        String[] keys = 队伍个人配置.获取所有配置键();
        for (int i = 0; i < visibleRows && (startRow + i) < keys.length; i++) {
            String key = keys[startRow + i];
            int rowY = listY + i * CONFIG_ROW_HEIGHT;

            if (rowY + CONFIG_ROW_HEIGHT > 配置面板.获取Y() + 配置面板.获取高度()) break;

            boolean currentValue = myData != null && myData.获取配置().获取配置(key);

            graphics.fill(listX, rowY, listX + listW, rowY + CONFIG_ROW_HEIGHT,
                    i % 2 == 0 ? 行偶数色 : 行奇数色);

            Component label = Component.translatable(队伍个人配置.获取翻译键(key));
            graphics.drawString(font, label, listX + 4, rowY + 7, 文字白, false);

            int toggleX = listX + listW - TOGGLE_WIDTH - 4;
            int toggleY = rowY + (CONFIG_ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
            GUI开关.绘制(graphics, font, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT,
                    currentValue, mouseX, mouseY);
            }

        配置滚动条.绘制(graphics, mouseX, mouseY);
    }

    // ==================== 底部按钮（自适应） ====================

    /**
     * 根据当前状态生成底部按钮列表
     *绘制和点击共用，保证一致
     */
    private List<底部按钮项> 生成底部按钮列表() {
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null) return List.of();

        UUID localUUID = minecraft != null && minecraft.player != null
                ? minecraft.player.getUUID() : null;
        if (localUUID == null) return List.of();

        队伍成员数据 myData = (队伍成员数据) team.获取成员(localUUID).orElse(null);
        if (myData == null) return List.of();

        boolean isOP = minecraft.player.hasPermissions(2);
        List<底部按钮项> buttons = new ArrayList<>();

        if (myData.有权限(队伍权限.邀请成员)) {
            buttons.add(new 底部按钮项(
                    Component.translatable("gui.puellamagi.team.button.invite"),
                    40, false, () -> 打开邀请面板(false)));
        }

        buttons.add(new 底部按钮项(
                离开按钮.是否等待确认()
                        ? Component.translatable("gui.puellamagi.team.button.confirm_leave")
                        : Component.translatable("gui.puellamagi.team.button.leave"),
                40, true, () -> {if (离开按钮.点击()) {
            网络工具.发送到服务端(new 队伍操作请求包(队伍操作请求包.操作类型.离开));
        } else {
            重置其他确认(离开按钮);
        }
        }));

        if (myData.有权限(队伍权限.解散队伍)) {
            buttons.add(new 底部按钮项(
                    解散按钮.是否等待确认()
                            ? Component.translatable("gui.puellamagi.team.button.confirm_disband")
                            : Component.translatable("gui.puellamagi.team.button.disband"),
                    40, true, () -> {
                if (解散按钮.点击()) {
                    网络工具.发送到服务端(new 队伍操作请求包(队伍操作请求包.操作类型.解散));
                } else {
                    重置其他确认(解散按钮);
                }
            }));
        }

        if (选中的成员UUID != null && !选中的成员UUID.equals(localUUID)) {
            if (myData.有权限(队伍权限.踢出成员)) {
                buttons.add(new 底部按钮项(
                        踢出按钮.是否等待确认()
                                ? Component.translatable("gui.puellamagi.team.button.confirm_kick")
                                : Component.translatable("gui.puellamagi.team.button.kick"),
                        40, true, () -> {
                    if (踢出按钮.点击()) {
                        网络工具.发送到服务端(new 队伍操作请求包(
                                队伍操作请求包.操作类型.踢出, 选中的成员UUID));
                        选中的成员UUID = null;
                    } else {
                        重置其他确认(踢出按钮);
                    }
                }));
            }

            if (myData.有权限(队伍权限.转移队长)) {
                buttons.add(new 底部按钮项(
                        转移按钮.是否等待确认()
                                ? Component.translatable("gui.puellamagi.team.button.confirm_transfer")
                                : Component.translatable("gui.puellamagi.team.button.transfer"),
                        55, false, () -> {
                    if (转移按钮.点击()) {
                        网络工具.发送到服务端(new 队伍操作请求包(
                                队伍操作请求包.操作类型.转移队长, 选中的成员UUID));
                        选中的成员UUID = null;
                    } else {
                        重置其他确认(转移按钮);
                    }
                }));
            }
        }

        if (isOP) {
            buttons.add(new 底部按钮项(
                    Component.translatable("gui.puellamagi.team.button.force_add"),
                    55, false, () -> 打开邀请面板(true)));
        }

        return buttons;
    }

    private void 绘制底部按钮(GuiGraphics graphics, int mouseX, int mouseY) {
        List<底部按钮项> buttons = 生成底部按钮列表();
        if (buttons.isEmpty()) return;

        int[] originals = buttons.stream().mapToInt(底部按钮项::原始宽度).toArray();
        int[] widths = GUI按钮.计算自适应宽度(originals, BOTTOM_AVAILABLE_WIDTH, BTN_GAP);

        int btnY = guiTop + GUI_HEIGHT - BOTTOM_HEIGHT + 7;
        int currentX = guiLeft + 8;

        for (int i = 0; i < buttons.size(); i++) {
            GUI按钮.绘制(graphics, font, currentX, btnY, widths[i], BTN_HEIGHT,
                    buttons.get(i).文字(), mouseX, mouseY, buttons.get(i).危险(), true);
            currentX += widths[i] + BTN_GAP;
        }
    }

    // ==================== 邀请面板 ====================

    private void 绘制邀请面板(GuiGraphics graphics, int mouseX, int mouseY) {
        if (邀请覆盖层 == null) return;

        邀请覆盖层.渲染(graphics, () -> {
            int[] pos = 邀请覆盖层.计算居中坐标(INVITE_WIDTH, INVITE_HEIGHT);
            int panelX = pos[0];
            int panelY = pos[1];

            graphics.fill(panelX, panelY, panelX + INVITE_WIDTH, panelY + INVITE_HEIGHT, 邀请面板背景色);
            渲染工具.绘制边框矩形(graphics, panelX, panelY, INVITE_WIDTH, INVITE_HEIGHT, GUI面板.默认边框色);

            Component title = 强制添加模式
                    ? Component.translatable("gui.puellamagi.team.invite.title_force")
                    : Component.translatable("gui.puellamagi.team.invite.title");
            渲染工具.绘制居中文本(graphics, font, title,
                    panelX + INVITE_WIDTH / 2, panelY + 6, 文字白);

            if (邀请输入框 != null) {邀请输入框.render(graphics, mouseX, mouseY, 0);
            }

            int labelY = panelY + 44;
            graphics.drawString(font,
                    Component.translatable("gui.puellamagi.team.invite.online_players"),
                    panelX + 8, labelY, 文字灰);

            int listY = labelY + 12;
            int listW = INVITE_WIDTH - 16 - SCROLLBAR_WIDTH;

            for (int i = 0; i< INVITE_LIST_ROWS; i++) {
                int playerIndex = (在线玩家滚动条 != null ? 在线玩家滚动条.获取当前行() : 0) + i;
                int rowY = listY + i * PLAYER_ROW_HEIGHT;

                if (rowY + PLAYER_ROW_HEIGHT > panelY + INVITE_HEIGHT - 26) break;

                if (playerIndex < 过滤后的在线玩家.size()) {
                    PlayerInfo info = 过滤后的在线玩家.get(playerIndex);
                    String name = info.getProfile().getName();
                    boolean rowHover = 渲染工具.鼠标在区域内(mouseX, mouseY,
                            panelX + 8, rowY, listW, PLAYER_ROW_HEIGHT);

                    int rowBg = rowHover ? 行悬停色 : (i % 2 == 0 ? 行偶数色 : 行奇数色);
                    graphics.fill(panelX + 8, rowY, panelX + 8 + listW, rowY + PLAYER_ROW_HEIGHT, rowBg);
                    graphics.drawString(font, name, panelX + 12, rowY + 5, 文字白, false);
                }
            }

            if (在线玩家滚动条 != null) {
                在线玩家滚动条.位置(panelX + INVITE_WIDTH - SCROLLBAR_WIDTH - 8, listY);
                在线玩家滚动条.绘制(graphics, mouseX, mouseY);
            }

            int btnAreaY = panelY + INVITE_HEIGHT - 24;
            Component sendText = 强制添加模式
                    ? Component.translatable("gui.puellamagi.team.button.force_add")
                    : Component.translatable("gui.puellamagi.team.button.send_invite");
            GUI按钮.绘制(graphics, font, panelX +10, btnAreaY, 60, BTN_HEIGHT,
                    sendText, mouseX, mouseY);
            GUI按钮.绘制(graphics, font, panelX + INVITE_WIDTH - 60, btnAreaY, 50, BTN_HEIGHT,
                    Component.translatable("gui.puellamagi.button.cancel"), mouseX, mouseY);
        });
    }
    //==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (邀请面板打开) {
            return 处理邀请面板点击(mx, my) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (客户端队伍缓存.有队伍()) {
            return 处理有队伍点击(mx, my) || super.mouseClicked(mouseX, mouseY, button);
        } else {
            return 处理无队伍点击(mx, my) || super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private boolean 处理无队伍点击(int mx, int my) {
        int centerX = guiLeft + GUI_WIDTH / 2;
        int contentY = guiTop + CONTENT_Y_OFFSET;

        int createBtnW = 80;
        int createBtnX = centerX - createBtnW / 2;
        int createBtnY = contentY + 35;
        if (GUI按钮.命中检测(mx, my, createBtnX, createBtnY, createBtnW, BTN_HEIGHT + 2)) {
            网络工具.发送到服务端(new 队伍操作请求包(队伍操作请求包.操作类型.创建));
            return true;
        }

        var invites = 客户端队伍缓存.获取待处理邀请();
        int inviteY = contentY + 65+ 14;
        int inviteX = guiLeft + 10;
        int inviteW = GUI_WIDTH - 20;

        for (int i = 0; i < invites.size() && i < 4; i++) {
            var invite = invites.get(i);

            if (GUI按钮.命中检测(mx, my, inviteX + inviteW - 80, inviteY + 3, 35, 14)) {
                网络工具.发送到服务端(new 队伍邀请响应包(invite.邀请者UUID(), true));
                客户端队伍缓存.移除邀请(invite.邀请者UUID());
                return true;
            }

            if (GUI按钮.命中检测(mx, my, inviteX + inviteW - 40, inviteY + 3, 35, 14)) {
                网络工具.发送到服务端(new 队伍邀请响应包(invite.邀请者UUID(), false));
                客户端队伍缓存.移除邀请(invite.邀请者UUID());
                return true;
            }

            inviteY += 22;
        }

        return false;
    }

    private boolean 处理有队伍点击(int mx, int my) {
        队伍数据 team = 客户端队伍缓存.获取队伍();
        if (team == null) return false;

        UUID localUUID = minecraft != null && minecraft.player != null
                ? minecraft.player.getUUID() : null;

        if (成员滚动条 != null && 成员滚动条.mouseClicked(mx, my, 0)) return true;
        if (配置滚动条 != null && 配置滚动条.mouseClicked(mx, my, 0)) return true;

        if (处理成员列表点击(mx, my, team, localUUID)) return true;
        if (处理配置开关点击(mx, my, team, localUUID)) return true;

        return 处理底部按钮点击(mx, my);
    }

    private boolean 处理成员列表点击(int mx, int my, 队伍数据 team, UUID localUUID) {
        if (成员面板 == null || 成员滚动条 == null) return false;

        int[] content = 成员面板.获取内容区域();
        int listX = content[0];
        int listY = content[1];
        int listW = content[2];
        int startRow = 成员滚动条.获取当前行();

        List<UUID> members = team.获取所有成员UUID();
        int visibleRows = content[3] / MEMBER_ROW_HEIGHT;

        for (int i = 0; i < visibleRows && (startRow + i) < members.size(); i++) {
            UUID memberUUID = members.get(startRow + i);
            int rowY = listY + i * MEMBER_ROW_HEIGHT;

            if (渲染工具.鼠标在区域内(mx, my, listX, rowY, listW, MEMBER_ROW_HEIGHT)) {
                if (memberUUID.equals(localUUID) || memberUUID.equals(选中的成员UUID)) {
                    选中的成员UUID = null;
                } else {
                    选中的成员UUID = memberUUID;
                }
                重置上下文确认();
                return true;
            }
        }

        return false;
    }

    private boolean 处理配置开关点击(int mx, int my, 队伍数据 team, UUID localUUID) {
        if (配置面板 == null || 配置滚动条 == null) return false;

        队伍成员数据 myData = localUUID != null
                ? (队伍成员数据) team.获取成员(localUUID).orElse(null) : null;
        if (myData == null) return false;

        int[] content = 配置面板.获取内容区域();
        int listX = content[0];
        int listW = content[2];
        int listY = content[1];
        int startRow = 配置滚动条.获取当前行();
        int visibleRows = content[3] / CONFIG_ROW_HEIGHT;

        String[] keys = 队伍个人配置.获取所有配置键();

        for (int i = 0; i < visibleRows && (startRow + i) < keys.length; i++) {
            String key = keys[startRow + i];
            int rowY = listY + i * CONFIG_ROW_HEIGHT;

            int toggleX = listX + listW - TOGGLE_WIDTH - 4;
            int toggleY = rowY + (CONFIG_ROW_HEIGHT - TOGGLE_HEIGHT) / 2;

            if (GUI开关.命中检测(mx, my, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
                boolean newValue = !myData.获取配置().获取配置(key);
                网络工具.发送到服务端(new 队伍配置更新包(key, newValue));
                myData.获取配置().设置配置(key, newValue);
                return true;
            }
        }
        return false;
    }

    private boolean 处理底部按钮点击(int mx, int my) {
        List<底部按钮项> buttons = 生成底部按钮列表();
        if (buttons.isEmpty()) return false;

        int[] originals = buttons.stream().mapToInt(底部按钮项::原始宽度).toArray();
        int[] widths = GUI按钮.计算自适应宽度(originals, BOTTOM_AVAILABLE_WIDTH, BTN_GAP);

        int btnY = guiTop + GUI_HEIGHT - BOTTOM_HEIGHT +7;
        int currentX = guiLeft + 8;

        for (int i = 0; i < buttons.size(); i++) {
            if (GUI按钮.命中检测(mx, my, currentX, btnY, widths[i], BTN_HEIGHT)) {
                buttons.get(i).动作().run();
                return true;
            }
            currentX += widths[i] + BTN_GAP;
        }

        return false;
    }

    private boolean 处理邀请面板点击(int mx, int my) {
        if (邀请覆盖层 == null) return false;

        int[] pos = 邀请覆盖层.计算居中坐标(INVITE_WIDTH, INVITE_HEIGHT);
        int panelX = pos[0];
        int panelY = pos[1];

        if (!渲染工具.鼠标在区域内(mx, my, panelX, panelY, INVITE_WIDTH, INVITE_HEIGHT)) {
            关闭邀请面板();
            return true;
        }

        if (邀请输入框 != null) {
            boolean clickedEditBox = 邀请输入框.mouseClicked(mx, my, 0);
            if (clickedEditBox) {
                this.setFocused(邀请输入框);
            } else {
                邀请输入框.setFocused(false);
                this.setFocused(null);
            }
        }

        if (在线玩家滚动条 != null && 在线玩家滚动条.mouseClicked(mx, my, 0)) return true;

        int labelY = panelY + 44;
        int listY = labelY + 12;
        int listW = INVITE_WIDTH - 16 - SCROLLBAR_WIDTH;
        int startRow = 在线玩家滚动条 != null ? 在线玩家滚动条.获取当前行() : 0;

        for (int i = 0; i < INVITE_LIST_ROWS; i++) {
            int playerIndex = startRow + i;
            int rowY = listY + i * PLAYER_ROW_HEIGHT;

            if (rowY + PLAYER_ROW_HEIGHT > panelY + INVITE_HEIGHT - 26) break;

            if (playerIndex < 过滤后的在线玩家.size()) {
                if (渲染工具.鼠标在区域内(mx, my, panelX + 8, rowY, listW, PLAYER_ROW_HEIGHT)) {
                    String name = 过滤后的在线玩家.get(playerIndex).getProfile().getName();
                    if (邀请输入框 != null) {
                        邀请输入框.setValue(name);
                    }return true;
                }
            }
        }

        int btnAreaY = panelY + INVITE_HEIGHT - 24;
        if (GUI按钮.命中检测(mx, my, panelX + 10, btnAreaY, 60, BTN_HEIGHT)) {
            发送邀请();
            return true;
        }
        if (GUI按钮.命中检测(mx, my, panelX + INVITE_WIDTH - 60, btnAreaY, 50, BTN_HEIGHT)) {
            关闭邀请面板();
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        if (邀请面板打开) {
            if (在线玩家滚动条 != null && 在线玩家滚动条.mouseDragged(mouseX, mouseY, button, dragX, dragY))
                return true;
        } else if (客户端队伍缓存.有队伍()) {
            if (成员滚动条 != null && 成员滚动条.mouseDragged(mouseX, mouseY, button, dragX, dragY))
                return true;
            if (配置滚动条 != null && 配置滚动条.mouseDragged(mouseX, mouseY, button, dragX, dragY))
                return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (成员滚动条 != null)成员滚动条.mouseReleased(mouseX, mouseY, button);
        if (配置滚动条 != null) 配置滚动条.mouseReleased(mouseX, mouseY, button);
        if (在线玩家滚动条 != null) 在线玩家滚动条.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (邀请面板打开) {
            if (邀请覆盖层 != null) {
                int[] pos = 邀请覆盖层.计算居中坐标(INVITE_WIDTH, INVITE_HEIGHT);
                int listY = pos[1] + 56;
                int listH = INVITE_LIST_ROWS * PLAYER_ROW_HEIGHT;
                if (在线玩家滚动条 != null && 在线玩家滚动条.坐标在滚动区域内(
                        mouseX, mouseY, pos[0] + 8, listY, INVITE_WIDTH - 16, listH)) {
                    return 在线玩家滚动条.mouseScrolled(delta);
                }
            }return false;
        } else if (客户端队伍缓存.有队伍()) {
            if (成员面板 != null && 成员滚动条 != null && 成员面板.包含坐标(mouseX, mouseY)) {
                return 成员滚动条.mouseScrolled(delta);
            }
            if (配置面板 != null && 配置滚动条 != null && 配置面板.包含坐标(mouseX, mouseY)) {
                return 配置滚动条.mouseScrolled(delta);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    //==================== 键盘交互 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (邀请面板打开) {
            if (keyCode == 256) {
                关闭邀请面板();
                return true;
            }if (keyCode == 257 || keyCode == 335) {
                发送邀请();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (按键绑定.技能栏编辑键.matches(keyCode, scanCode)) {
            if (minecraft != null) minecraft.setScreen(null);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== 动作方法 ====================

    private void 打开邀请面板(boolean forceMode) {
        邀请面板打开 = true;
        强制添加模式 = forceMode;
        if (邀请输入框 != null) {
            邀请输入框.setVisible(true);
            邀请输入框.setFocused(true);
            邀请输入框.setValue("");
        }
        刷新过滤后的在线玩家();
    }

    private void 关闭邀请面板() {
        邀请面板打开 = false;
        强制添加模式 = false;
        if (邀请输入框 != null) {
            邀请输入框.setVisible(false);
            邀请输入框.setFocused(false);
        }
        保存的输入文本 = "";
    }

    private void 发送邀请() {
        if (邀请输入框 == null) return;
        String inputName = 邀请输入框.getValue().trim();
        if (inputName.isEmpty()) return;

        UUID targetUUID = 根据名称获取UUID(inputName);
        if (targetUUID == null) return;

        if (强制添加模式) {
            网络工具.发送到服务端(new 队伍操作请求包(队伍操作请求包.操作类型.强制拉人, targetUUID));
        } else {
            网络工具.发送到服务端(new 队伍邀请请求包(targetUUID));
        }

        关闭邀请面板();
    }

    // ==================== 确认状态管理 ====================

    private void 重置其他确认(GUI确认按钮 keep) {
        if (keep !=离开按钮) 离开按钮.重置();
        if (keep != 解散按钮) 解散按钮.重置();
        if (keep != 踢出按钮) 踢出按钮.重置();
        if (keep != 转移按钮) 转移按钮.重置();
    }

    private void 重置上下文确认() {踢出按钮.重置();
        转移按钮.重置();
    }

    // ==================== 数据辅助 ====================

    private void 刷新在线状态() {
        在线UUID集合.clear();
        if (minecraft == null || minecraft.getConnection() == null) return;
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            在线UUID集合.add(info.getProfile().getId());
        }
    }

    private void 刷新过滤后的在线玩家() {
        过滤后的在线玩家.clear();
        if (minecraft == null || minecraft.getConnection() == null) return;

        String filter = 邀请输入框 != null ? 邀请输入框.getValue().trim().toLowerCase() : "";
        UUID localUUID = minecraft.player != null ? minecraft.player.getUUID() : null;
        队伍数据 team = 客户端队伍缓存.获取队伍();

        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            UUID uuid = info.getProfile().getId();
            if (uuid.equals(localUUID)) continue;
            if (team != null && team.是成员(uuid)) continue;
            String name = info.getProfile().getName();
            if (!filter.isEmpty() && !name.toLowerCase().contains(filter)) continue;
            过滤后的在线玩家.add(info);
        }

        过滤后的在线玩家.sort(Comparator.comparing(a -> a.getProfile().getName()));
        if (在线玩家滚动条 != null) {
            在线玩家滚动条.设置总行数(过滤后的在线玩家.size());
        }
    }

    @Nullable
    private UUID 根据名称获取UUID(String name) {
        if (minecraft == null || minecraft.getConnection() == null) return null;
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            if (info.getProfile().getName().equalsIgnoreCase(name)) {
                return info.getProfile().getId();
            }
        }
        return null;
    }

    private String 获取玩家名称(UUID uuid) {
        if (minecraft == null || minecraft.getConnection() == null) {
            return uuid.toString().substring(0, 8);
        }
        PlayerInfo info = minecraft.getConnection().getPlayerInfo(uuid);
        if (info != null) {
            return info.getProfile().getName();
        }
        return uuid.toString().substring(0, 8);
    }
}
