// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/文本输入对话框.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.util.渲染工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 通用文本输入对话框
 *
 * 用法：
 * 文本输入对话框.显示("请输入名称", "默认值", result -> {
 *     if (result != null) {
 *         // 用户确认，result是输入的文本
 *     } else {
 *         // 用户取消
 *     }
 * });
 */
public class 文本输入对话框 extends Screen {

    //==================== 配置 ====================

    private static final int 对话框宽度 = 200;
    private static final int 对话框高度 = 90;
    private static final int 边距 = 10;
    private static final int 按钮宽度 = 60;
    private static final int 按钮高度 = 20;
    private static final int 输入框高度 = 20;

    //==================== 状态 ====================

    @Nullable
    private final Screen 父界面;
    private final String 默认值;
    private final Consumer<String> 回调;  // null表示取消
    private final int 最大长度;
    @Nullable
    private Predicate<String> 验证器 = null;

    private EditBox 输入框;
    private Button 确认按钮;
    private Button 取消按钮;

    private int 对话框X, 对话框Y;

    // ==================== 构造器 ====================

    private 文本输入对话框(Component title, @Nullable Screen parent,String defaultValue, int maxLength, Consumer<String> callback) {
        super(title);
        this.父界面 = parent;
        this.默认值 = defaultValue != null ? defaultValue : "";
        this.最大长度 = maxLength;
        this.回调 = callback;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 显示输入对话框
     * @param title 标题
     * @param defaultValue 默认值
     * @param callback 回调，参数为输入结果（null表示取消）
     */
    public static void 显示(String title, String defaultValue, Consumer<String> callback) {
        显示(Component.literal(title), defaultValue, 32, callback);
    }

    /**
     * 显示输入对话框（带最大长度）
     */
    public static void 显示(Component title, String defaultValue, int maxLength, Consumer<String> callback) {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        文本输入对话框 dialog = new 文本输入对话框(title, parent, defaultValue, maxLength, callback);
        mc.setScreen(dialog);
    }

    /**
     * 显示输入对话框（带验证器）
     */
    public static void 显示(String title, String defaultValue, int maxLength, Predicate<String> validator, Consumer<String> callback) {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        文本输入对话框 dialog = new 文本输入对话框(Component.literal(title), parent, defaultValue, maxLength, callback);
        dialog.验证器 = validator;
        mc.setScreen(dialog);
    }

    // ==================== 初始化 ====================

    @Override
    protected void init() {
        super.init();

        对话框X = (this.width - 对话框宽度) / 2;
        对话框Y = (this.height - 对话框高度) / 2;

        int contentX = 对话框X + 边距;
        int contentWidth = 对话框宽度 - 边距 * 2;

        // 输入框
        输入框 = new EditBox(this.font,
                contentX,
                对话框Y + 30,
                contentWidth,
                输入框高度,
                Component.empty()
        );
        输入框.setMaxLength(最大长度);
        输入框.setValue(默认值);
        输入框.setFocused(true);
        addRenderableWidget(输入框);

        // 按钮区域
        int buttonY = 对话框Y + 对话框高度 - 边距 - 按钮高度;
        int buttonSpacing = 10;
        int totalButtonWidth = 按钮宽度 * 2 + buttonSpacing;
        int buttonStartX = 对话框X + (对话框宽度 - totalButtonWidth) / 2;

        // 确认按钮
        确认按钮 = Button.builder(本地化工具.GUI("button.confirm"), btn -> 确认())
                .bounds(buttonStartX, buttonY, 按钮宽度, 按钮高度)
                .build();
        addRenderableWidget(确认按钮);

        // 取消按钮
        取消按钮 = Button.builder(本地化工具.GUI("button.cancel"), btn -> 取消())
                .bounds(buttonStartX + 按钮宽度 + buttonSpacing, buttonY, 按钮宽度, 按钮高度)
                .build();
        addRenderableWidget(取消按钮);

        // 设置焦点到输入框
        setInitialFocus(输入框);
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制半透明背景遮罩
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // 绘制对话框背景
        graphics.fill(对话框X, 对话框Y, 对话框X + 对话框宽度, 对话框Y + 对话框高度, 0xE0222222);
        渲染工具.绘制边框矩形(graphics, 对话框X, 对话框Y, 对话框宽度, 对话框高度,渲染工具.颜色_槽位边框);

        // 绘制标题
        graphics.drawCenteredString(this.font, this.title,
                对话框X + 对话框宽度/ 2, 对话框Y + 边距, 渲染工具.颜色_白色);

        // 验证状态提示
        if (验证器 != null && !输入框.getValue().isEmpty()) {
            boolean valid = 验证器.test(输入框.getValue());
            if (!valid) {
                graphics.drawString(this.font, "§c名称无效",
                        对话框X + 边距, 对话框Y + 55, 0xFF5555);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ==================== 交互 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter 确认
        if (keyCode == 257) { // GLFW.GLFW_KEY_ENTER
            确认();
            return true;
        }
        // Escape 取消
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            取消();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void 确认() {
        String value = 输入框.getValue().trim();

        // 验证
        if (value.isEmpty()) {
            return;
        }
        if (验证器 != null && !验证器.test(value)) {
            return;
        }

        关闭并回调(value);
    }

    private void 取消() {
        关闭并回调(null);
    }

    private void 关闭并回调(@Nullable String result) {
        Minecraft.getInstance().setScreen(父界面);
        if (回调 != null) {
            回调.accept(result);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
