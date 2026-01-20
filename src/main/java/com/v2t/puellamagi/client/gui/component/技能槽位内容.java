// 文件路径: src/main/java/com/v2t/puellamagi/client/gui/component/技能槽位内容.java

package com.v2t.puellamagi.client.gui.component;

import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 技能的槽位内容实现
 */
public class 技能槽位内容 implements I槽位内容 {

    private final ResourceLocation 技能ID;

    // 缓存的技能实例
    @Nullable
    private I技能 缓存技能 = null;
    private boolean 已加载 = false;

    public 技能槽位内容(ResourceLocation skillId) {
        this.技能ID = skillId;
    }

    private I技能 获取技能() {
        if (!已加载) {
            缓存技能 = 技能注册表.创建实例(技能ID).orElse(null);
            已加载 = true;
        }
        return 缓存技能;
    }

    @Override
    public ResourceLocation 获取ID() {
        return 技能ID;
    }

    @Override
    public Component 获取名称() {
        I技能 skill = 获取技能();
        return skill != null ? skill.获取名称() : Component.literal(技能ID.getPath());
    }

    @Override
    @Nullable
    public ResourceLocation 获取图标() {
        I技能 skill = 获取技能();
        return skill != null ? skill.获取图标() : null;
    }

    @Override
    public void 绘制(GuiGraphics graphics, int x, int y, int size) {
        I技能 skill = 获取技能();

        //尝试绘制图标
        ResourceLocation icon = 获取图标();
        if (icon != null) {
            // TODO: 实际绘制图标纹理
            // graphics.blit(icon, x +2, y + 2, 0, 0, size - 4, size - 4, size - 4, size - 4);
        }

        // 暂时用首字母表示
        String initial = 技能ID.getPath().substring(0, 1).toUpperCase();
        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                initial,
                x + size / 2,
                y + (size - 8) / 2,
                0xFFFFFF
        );
    }

    @Override
    public List<Component> 获取详情() {
        List<Component> details = new ArrayList<>();

        I技能 skill = 获取技能();
        if (skill == null) {
            details.add(Component.literal(技能ID.toString()));
            return details;
        }

        // 名称
        details.add(skill.获取名称());

        // 类型
        details.add(Component.literal("§7类型: " + skill.获取按键类型().name()));

        // 冷却
        int cd = skill.获取冷却时间();
        details.add(Component.literal("§7冷却: " + String.format("%.1f", cd / 20f) + "秒"));

        // 描述
        Component desc = skill.获取描述();
        if (desc != null && !desc.getString().isEmpty()) {
            details.add(Component.empty());
            details.add(desc);
        }

        return details;
    }

    // ==================== 工具方法 ====================

    /**
     * 从技能ID创建槽位内容
     */
    public static 技能槽位内容 从ID创建(ResourceLocation skillId) {
        return new 技能槽位内容(skillId);
    }

    /**
     * 获取技能类型（用于类型判断）
     */
    @Nullable
    public I技能.按键类型 获取按键类型() {
        I技能 skill = 获取技能();
        return skill != null ? skill.获取按键类型() : null;
    }
}
