// 文件路径: src/main/java/com/v2t/puellamagi/system/skill/抽象基类技能.java

package com.v2t.puellamagi.system.skill;

import com.v2t.puellamagi.api.I技能;
import com.v2t.puellamagi.util.资源工具;
import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 技能抽象基类
 *
 * 架构层次：I技能（接口） → 抽象基类技能（本类） → 具体技能实现
 *
 * 职责：
 * - 减少样板代码（ID、名称、图标的标准实现）
 * - 为方案C（组件组合）升级预留接缝
 *
 * 使用方式：
 *   继承本类，传入技能的注册名（如 "time_stop"）
 *   自动生成ID、名称、图标的标准路径
 *
 * 注意：
 *   复杂技能（如预知的4阶段状态机）可以直接 implements I技能，
 *   不强制使用本基类
 */
public abstract class 抽象基类技能 implements I技能 {

    private final ResourceLocation id;
    private final ResourceLocation 图标;
    private final String 注册名;

    /**
     * @param 注册名 技能的注册名，如 "time_stop"
     *               自动生成 ID: puellamagi:time_stop
     *               自动生成 图标: puellamagi:textures/gui/skill/time_stop.png
     *               自动生成 名称翻译键: skill.puellamagi.time_stop
     */
    protected 抽象基类技能(String 注册名) {
        this.注册名 = 注册名;
        this.id = 资源工具.本mod(注册名);
        this.图标 = 资源工具.技能图标(注册名);
    }

    @Override
    public ResourceLocation 获取ID() {
        return id;
    }

    @Override
    public Component 获取名称() {
        return 本地化工具.技能名(注册名);
    }

    @Override
    public Component 获取描述() {
        return 本地化工具.提示("skill." + 注册名 + ".desc");
    }

    @Override
    public ResourceLocation 获取图标() {
        return 图标;
    }
}
