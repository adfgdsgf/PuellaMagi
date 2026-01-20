package com.v2t.puellamagi.api.类型定义;

import com.v2t.puellamagi.util.本地化工具;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法少女类型定义
 *
 * 定义一种魔法少女的基本属性：
 * - 所属系列
 * - 绑定的固有能力
 * - 默认模型
 *
 * 变身时通过类型ID查找对应的能力并激活
 */
public class 魔法少女类型 {

    private final ResourceLocation id;           // 类型ID，如puellamagi:time_manipulator
    private final ResourceLocation 所属系列;      // 系列ID，如 puellamagi:soul_gem
    private final ResourceLocation 固有能力ID;    // 绑定的能力ID
    private final ResourceLocation 默认模型;      // 默认模型路径
    private final String 名称键;                  // 本地化键名
    private final String 描述键;                  // 本地化键名

    /**
     * 完整构造器
     */
    public 魔法少女类型(ResourceLocation id,
                        ResourceLocation 所属系列,
                        ResourceLocation 固有能力ID,
                        @Nullable ResourceLocation 默认模型,
                        String 名称键,
                        String 描述键) {
        this.id = id;
        this.所属系列 = 所属系列;
        this.固有能力ID = 固有能力ID;
        this.默认模型 = 默认模型;
        this.名称键 = 名称键;
        this.描述键 = 描述键;
    }

    /**
     * 简化构造器（自动生成本地化键名）
     */
    public 魔法少女类型(
            ResourceLocation id,
            ResourceLocation 所属系列,
            ResourceLocation 固有能力ID,
            @Nullable ResourceLocation 默认模型) {
        this(id, 所属系列, 固有能力ID, 默认模型,"girl_type." + id.getNamespace() + "." + id.getPath(),
                "girl_type." + id.getNamespace() + "." + id.getPath() + ".desc");
    }

    //==================== Getter ====================

    public ResourceLocation 获取ID() {
        return id;
    }

    public ResourceLocation 获取所属系列() {
        return 所属系列;
    }

    public ResourceLocation 获取固有能力ID() {
        return 固有能力ID;
    }

    @Nullable
    public ResourceLocation 获取默认模型() {
        return 默认模型;
    }

    public Component 获取名称() {
        return Component.translatable(名称键);
    }

    public Component 获取描述() {
        return Component.translatable(描述键);
    }

    public String 获取名称键() {
        return 名称键;
    }

    public String 获取描述键() {
        return 描述键;
    }

    // ==================== 工具方法 ====================

    @Override
    public String toString() {
        return "魔法少女类型{" +
                "id=" + id +
                ", 系列=" + 所属系列 +
                ", 能力=" + 固有能力ID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        魔法少女类型 that = (魔法少女类型) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
