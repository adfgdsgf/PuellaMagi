// 文件路径: src/main/java/com/v2t/puellamagi/core/event/模组事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.api.I可变身;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import com.v2t.puellamagi.system.ability.能力注册表;
import com.v2t.puellamagi.system.ability.impl.测试能力;
import com.v2t.puellamagi.system.ability.impl.时间停止能力;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.skill.技能能力;
import com.v2t.puellamagi.system.skill.impl.测试技能;
import com.v2t.puellamagi.system.skill.impl.时间停止技能;
import com.v2t.puellamagi.system.transformation.魔法少女类型注册表;
import com.v2t.puellamagi.util.资源工具;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 模组总线事件处理
 * 处理Capability注册、能力注册、技能注册、类型注册等模组级事件
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class 模组事件 {

    /**
     * 注册Capability接口
     */
    @SubscribeEvent
    public static void 注册能力(RegisterCapabilitiesEvent event) {
        event.register(I可变身.class);
        event.register(技能能力.class);
    }

    /**
     * 通用初始化
     */
    @SubscribeEvent
    public static void 通用初始化(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            注册所有能力();
            注册所有技能();
            注册所有魔法少女类型();
        });
    }

    /**
     * 注册所有能力到注册表
     */
    private static void 注册所有能力() {
        PuellaMagi.LOGGER.info("开始注册能力...");

        // 测试能力（解锁所有技能）
        能力注册表.注册(资源工具.本mod("test"), 测试能力::new);

        // 时间停止能力
        能力注册表.注册(资源工具.本mod("time_control"), 时间停止能力::new);

        PuellaMagi.LOGGER.info("能力注册完成，共 {} 个", 能力注册表.获取能力数量());
    }

    /**
     * 注册所有技能到注册表
     */
    private static void 注册所有技能() {
        PuellaMagi.LOGGER.info("开始注册技能...");

        // 测试技能
        技能注册表.注册(资源工具.本mod("test_skill"), 测试技能::new);

        // 时间停止技能
        技能注册表.注册(资源工具.本mod("time_stop"), 时间停止技能::new);

        PuellaMagi.LOGGER.info("技能注册完成，共 {} 个", 技能注册表.获取技能数量());
    }

    /**
     * 注册所有魔法少女类型
     */
    private static void 注册所有魔法少女类型() {
        PuellaMagi.LOGGER.info("开始注册魔法少女类型...");

        // 测试类型（绑定测试能力，可解锁所有技能）
        魔法少女类型注册表.注册(new 魔法少女类型(资源工具.本mod("test"),// 类型ID
                资源工具.本mod("test_series"),       // 所属系列
                资源工具.本mod("test"),              // 绑定能力ID
                null// 默认模型（暂无）
        ));

        // 时间操控者类型
        魔法少女类型注册表.注册(new 魔法少女类型(
                资源工具.本mod("time_manipulator"),  // 类型ID
                资源工具.本mod("soul_gem"),          // 所属系列：灵魂宝石系
                资源工具.本mod("time_control"),      // 绑定能力ID
                null                                  // 默认模型（暂无）
        ));

        PuellaMagi.LOGGER.info("魔法少女类型注册完成，共 {} 个", 魔法少女类型注册表.获取类型数量());
    }
}
