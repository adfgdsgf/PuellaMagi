// 文件路径: src/main/java/com/v2t/puellamagi/util/资源工具.java

package com.v2t.puellamagi.util;

import com.v2t.puellamagi.常量;
import net.minecraft.resources.ResourceLocation;

/**
 * ResourceLocation构建工具
 * 统一管理资源路径的创建，避免硬编码MOD_ID
 */
public final class 资源工具 {
    private 资源工具() {}

    //==================== 纹理信息记录 ====================

    /**
     * 纹理信息（包含路径和原始尺寸）
     *绘制时会自动使用正确的纹理尺寸
     */
    public record 纹理信息(ResourceLocation 路径, int 宽度, int 高度) {
        /**
         * 正方形纹理的便捷构造
         */
        public 纹理信息(ResourceLocation 路径, int 尺寸) {
            this(路径, 尺寸, 尺寸);
        }
    }

    // ==================== 预定义纹理 ====================

    // 技能管理界面
    public static final 纹理信息 技能管理_背景 = new 纹理信息(技能管理GUI("background"), 256, 176);
    public static final 纹理信息 技能管理_槽位 = new 纹理信息(技能管理GUI("slot"), 32);
    public static final 纹理信息 技能管理_按钮普通 = new 纹理信息(技能管理GUI("button_normal"), 80, 20);
    public static final 纹理信息 技能管理_按钮悬停 = new 纹理信息(技能管理GUI("button_hover"), 80, 20);

    // ==================== 路径构建方法 ====================

    /**
     * 创建本mod的资源路径
     */
    public static ResourceLocation 本mod(String path) {
        return new ResourceLocation(常量.MOD_ID, path);
    }

    /**
     * 纹理路径 (textures/xxx.png)
     */
    public static ResourceLocation 纹理(String name) {
        return 本mod("textures/" + name + ".png");
    }

    /**
     * GUI纹理路径 (textures/gui/xxx.png)
     */
    public static ResourceLocation GUI纹理(String name) {
        return 本mod("textures/gui/" + name + ".png");
    }

    /**
     * 技能图标路径 (textures/skill/xxx.png)
     */
    public static ResourceLocation 技能图标(String name) {
        return 本mod("textures/skill/" + name + ".png");
    }

    /**
     * 技能管理界面纹理路径 (textures/gui/skill_manager/xxx.png)
     */
    public static ResourceLocation 技能管理GUI(String name) {
        return 本mod("textures/gui/skill_manager/" + name + ".png");
    }

    /**
     * 音效路径
     */
    public static ResourceLocation 音效(String name) {
        return 本mod(name);
    }
}
