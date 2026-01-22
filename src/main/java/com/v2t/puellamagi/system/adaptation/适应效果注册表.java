// 文件路径: src/main/java/com/v2t/puellamagi/system/adaptation/适应效果注册表.java

package com.v2t.puellamagi.system.adaptation;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.api.adaptation.I适应效果;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 适应效果注册表
 *
 * 管理所有适应效果的注册和查询
 */
public final class 适应效果注册表 {

    private static final Map<ResourceLocation, I适应效果> 注册表 = new HashMap<>();

    private 适应效果注册表() {}

    /**
     * 注册效果
     */
    public static void 注册(I适应效果 effect) {
        if (effect == null) return;

        ResourceLocation id = effect.获取ID();
        if (注册表.containsKey(id)) {
            PuellaMagi.LOGGER.warn("适应效果 {} 已存在，将被覆盖", id);
        }

        注册表.put(id, effect);
        PuellaMagi.LOGGER.debug("注册适应效果: {}", id);
    }

    /**
     * 获取效果
     */
    public static Optional<I适应效果> 获取(ResourceLocation id) {
        return Optional.ofNullable(注册表.get(id));
    }

    /**
     * 是否已注册
     */
    public static boolean 是否已注册(ResourceLocation id) {
        return 注册表.containsKey(id);
    }

    /**
     * 清空（仅用于测试）
     */
    public static void 清空() {
        注册表.clear();
    }
}
