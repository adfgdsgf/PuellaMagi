// 文件路径: src/main/java/com/v2t/puellamagi/core/event/模组事件.java

package com.v2t.puellamagi.core.event;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.api.I可变身;
import com.v2t.puellamagi.api.contract.I契约;
import com.v2t.puellamagi.api.soulgem.I污浊度;
import com.v2t.puellamagi.api.类型定义.魔法少女类型;
import com.v2t.puellamagi.core.network.ModNetwork;
import com.v2t.puellamagi.system.ability.能力注册表;
import com.v2t.puellamagi.system.ability.impl.测试能力;
import com.v2t.puellamagi.system.ability.impl.时间停止能力;
import com.v2t.puellamagi.system.adaptation.适应管理器;
import com.v2t.puellamagi.system.interaction.搜身槽位注册表;
import com.v2t.puellamagi.system.interaction.搜身管理器;
import com.v2t.puellamagi.system.interaction.impl.原版槽位提供者;
import com.v2t.puellamagi.system.interaction.impl.假死搜身来源;
import com.v2t.puellamagi.system.interaction.impl.时停搜身来源;
import com.v2t.puellamagi.system.restriction.行动限制管理器;
import com.v2t.puellamagi.system.series.系列注册表;
import com.v2t.puellamagi.system.series.impl.灵魂宝石系列;
import com.v2t.puellamagi.system.series.impl.心之种系列;
import com.v2t.puellamagi.system.skill.技能注册表;
import com.v2t.puellamagi.system.skill.技能能力;
import com.v2t.puellamagi.system.skill.impl.测试技能;
import com.v2t.puellamagi.system.skill.impl.时间停止技能;
import com.v2t.puellamagi.system.soulgem.damage.active.主动损坏注册表;
import com.v2t.puellamagi.system.transformation.魔法少女类型注册表;
import com.v2t.puellamagi.util.资源工具;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 模组总线事件处理
 *
 * 职责：所有模组级初始化逻辑集中在此
 * - Capability注册
 * - 网络注册
 * - 系统初始化（适应、损坏、行动限制、搜身等）
 * - 内容注册（系列、能力、技能、类型）
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
        event.register(I契约.class);
        event.register(I污浊度.class);

        PuellaMagi.LOGGER.info("Capability注册完成");
    }

    /**
     * 通用初始化
     */
    @SubscribeEvent
    public static void 通用初始化(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // ==================== 基础设施初始化 ====================
            初始化基础设施();

            // ==================== 内容注册 ====================
            // 注册顺序很重要：系列 → 能力 → 技能 → 类型
            注册所有系列();
            注册所有能力();
            注册所有技能();
            注册所有魔法少女类型();

            PuellaMagi.LOGGER.info("Puella Magi 通用初始化完成");
        });
    }

    /**
     * 初始化基础设施
     */
    private static void 初始化基础设施() {
        // 网络注册
        ModNetwork.register();
        PuellaMagi.LOGGER.info("网络注册完成");

        // 适应系统初始化
        适应管理器.初始化();
        PuellaMagi.LOGGER.info("适应系统初始化完成");

        //灵魂宝石损坏系统初始化
        主动损坏注册表.初始化();
        PuellaMagi.LOGGER.info("灵魂宝石损坏系统初始化完成");

        // 行动限制系统初始化
        行动限制管理器.初始化();
        PuellaMagi.LOGGER.info("行动限制系统初始化完成");

        // 搜身系统初始化
        初始化搜身系统();
        PuellaMagi.LOGGER.info("搜身系统初始化完成");
    }

    /**
     * 初始化搜身系统
     */
    private static void 初始化搜身系统() {
        // 注册搜身来源
        搜身管理器.注册来源(假死搜身来源.INSTANCE);
        搜身管理器.注册来源(时停搜身来源.INSTANCE);

        // 注册槽位提供者
        搜身槽位注册表.注册(原版槽位提供者.INSTANCE);

        // 未来：饰品mod适配
        // if (ModList.get().isLoaded("curios")) {
        //     搜身槽位注册表.注册(饰品槽位提供者.INSTANCE);
        // }
    }

    /**
     * 注册所有系列
     */
    private static void 注册所有系列() {
        PuellaMagi.LOGGER.debug("开始注册魔法少女系列...");

        系列注册表.注册(灵魂宝石系列.INSTANCE);
        系列注册表.注册(心之种系列.INSTANCE);

        PuellaMagi.LOGGER.info("系列注册完成，共 {} 个", 系列注册表.获取系列数量());
    }

    /**
     * 注册所有能力
     */
    private static void 注册所有能力() {
        PuellaMagi.LOGGER.debug("开始注册能力...");

        能力注册表.注册(资源工具.本mod("test"), 测试能力::new);
        能力注册表.注册(资源工具.本mod("time_control"), 时间停止能力::new);

        PuellaMagi.LOGGER.info("能力注册完成，共 {} 个", 能力注册表.获取能力数量());
    }

    /**
     * 注册所有技能
     */
    private static void 注册所有技能() {
        PuellaMagi.LOGGER.debug("开始注册技能...");

        技能注册表.注册(资源工具.本mod("test_skill"), 测试技能::new);
        技能注册表.注册(资源工具.本mod("time_stop"), 时间停止技能::new);

        PuellaMagi.LOGGER.info("技能注册完成，共 {} 个", 技能注册表.获取技能数量());
    }

    /**
     * 注册所有魔法少女类型
     */
    private static void 注册所有魔法少女类型() {
        PuellaMagi.LOGGER.debug("开始注册魔法少女类型...");

        // 测试类型
        魔法少女类型注册表.注册(new 魔法少女类型(资源工具.本mod("test"),
                资源工具.本mod("test_series"),
                资源工具.本mod("test"),
                null
        ));

        // 时间操控者类型（灵魂宝石系）
        魔法少女类型 时间操控者 = new 魔法少女类型(
                资源工具.本mod("time_manipulator"),
                灵魂宝石系列.ID,
                资源工具.本mod("time_control"),
                null
        );魔法少女类型注册表.注册(时间操控者);灵魂宝石系列.INSTANCE.添加可用类型(时间操控者.获取ID());

        PuellaMagi.LOGGER.info("魔法少女类型注册完成，共 {} 个", 魔法少女类型注册表.获取类型数量());
    }
}
