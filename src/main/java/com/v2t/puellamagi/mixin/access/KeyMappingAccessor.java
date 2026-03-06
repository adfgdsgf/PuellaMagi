package com.v2t.puellamagi.mixin.access;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * KeyMapping字段访问器
 *
 * clickCount：这个tick内还没被MC消费的点击次数
 *MC每帧调consumeClick()消费一次 → 触发一次攻击/使用
 *             我们需要读取但不消费（消费了MC就不处理了）
 *
 * ALL：所有已注册的KeyMapping
 *      包括MC内置 + 所有mod注册的
 *      遍历这个就能录制所有按键，不需要硬编码
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {

    @Accessor("clickCount")
    int puellamagi$getClickCount();

    @Accessor("clickCount")
    void puellamagi$setClickCount(int count);

    @Accessor("ALL")
    static Map<String, KeyMapping> puellamagi$getAll() {
        throw new AssertionError();
    }
}
