// 文件路径: src/main/java/com/v2t/puellamagi/core/event/client/客户端事件.java

package com.v2t.puellamagi.core.event.client;

import com.v2t.puellamagi.PuellaMagi;
import com.v2t.puellamagi.client.gui.HUD编辑界面;
import com.v2t.puellamagi.client.gui.污浊度HUD;
import com.v2t.puellamagi.client.gui.hud.可编辑HUD注册表;
import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.client.客户端状态管理;
import com.v2t.puellamagi.client.蓄力状态管理;
import com.v2t.puellamagi.client.gui.技能栏HUD;
import com.v2t.puellamagi.client.gui.技能管理界面;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.core.network.packets.c2s.变身请求包;
import com.v2t.puellamagi.core.network.packets.c2s.技能按下请求包;
import com.v2t.puellamagi.core.network.packets.c2s.技能松开请求包;
import com.v2t.puellamagi.core.network.packets.c2s.预设切换请求包;
import com.v2t.puellamagi.util.能力工具;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端事件处理
 * 处理按键输入、客户端初始化、HUD注册等
 */
public class 客户端事件 {

    //==================== 按键状态追踪 ====================

    private static final boolean[] 技能键按住状态 = new boolean[6];

    /**
     * MOD总线事件（客户端）
     */
    @Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {

        @SubscribeEvent
        public static void 注册按键(RegisterKeyMappingsEvent event) {
            for (var key : 按键绑定.获取所有按键()) {
                event.register(key);
            }
            PuellaMagi.LOGGER.info("Puella Magi 按键绑定注册完成");
        }

        @SubscribeEvent
        public static void 注册HUD(RegisterGuiOverlaysEvent event) {
            // 技能栏HUD
            event.registerAbove(
                    VanillaGuiOverlay.HOTBAR.id(),
                    "skill_bar",
                    技能栏HUD.INSTANCE
            );

            // 污浊度HUD
            event.registerBelow(
                    VanillaGuiOverlay.PLAYER_LIST.id(),
                    "corruption_bar",
                    污浊度HUD.INSTANCE
            );

            PuellaMagi.LOGGER.info("Puella Magi HUD注册完成");
        }

        @SubscribeEvent
        public static void 客户端初始化(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // 初始化客户端状态管理
                客户端状态管理.初始化();

                // 注册可编辑HUD
                注册可编辑HUD();

                PuellaMagi.LOGGER.info("Puella Magi 客户端初始化完成");
            });
        }

        /**
         * 注册所有可编辑的HUD到注册表
         */
        private static void 注册可编辑HUD() {
            // 技能栏HUD
            可编辑HUD注册表.注册(技能栏HUD.INSTANCE);

            // 污浊度HUD
            可编辑HUD注册表.注册(污浊度HUD.INSTANCE);

            // 未来可添加更多：
            // 可编辑HUD注册表.注册(成长度HUD.INSTANCE);

            PuellaMagi.LOGGER.info("可编辑HUD注册完成，共 {} 个",
                    可编辑HUD注册表.获取所有().size());
        }
    }

    /**
     * Forge总线事件（客户端）
     */
    @Mod.EventBusSubscriber(modid = 常量.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBus {

        @SubscribeEvent
        public static void 客户端Tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            Player player = mc.player;

            // 客户端冷却tick
            能力工具.获取技能能力(player).ifPresent(cap -> cap.tick());

            // K键：技能管理界面切换
            while (按键绑定.技能栏编辑键.consumeClick()) {
                处理技能管理界面按键(mc, player);
            }

            // 如果在GUI界面中，释放所有按住的按键
            if (mc.screen != null) {
                释放所有技能键(player);
                return;
            }

            // 以下按键只在游戏中（无GUI）时处理

            // 变身键
            while (按键绑定.变身键.consumeClick()) {
                处理变身按键(player);
            }

            // 技能键
            处理技能键状态(player);

            // 预设切换
            while (按键绑定.下一预设键.consumeClick()) {
                网络工具.发送到服务端(new 预设切换请求包(true));
            }
            while (按键绑定.上一预设键.consumeClick()) {
                网络工具.发送到服务端(new 预设切换请求包(false));
            }

            // 技能栏折叠
            while (按键绑定.技能栏折叠键.consumeClick()) {
                切换技能栏折叠();
            }
        }

        private static void 处理技能管理界面按键(Minecraft mc, Player player) {
            // 如果已经在技能管理界面或HUD编辑界面，关闭它
            if (mc.screen instanceof 技能管理界面 || mc.screen instanceof HUD编辑界面) {
                mc.setScreen(null);
                return;
            }

            // 只有已契约才能打开技能管理界面
            if (!能力工具.是否已契约(player)) {
                return;
            }

            if (mc.screen == null) {
                mc.setScreen(new 技能管理界面());
            }
        }

        private static void 处理技能键状态(Player player) {
            if (!能力工具.是否已变身(player)) {
                释放所有技能键(player);
                return;
            }

            for (int i = 0; i < 按键绑定.技能键.length; i++) {
                boolean 当前按住 = 按键绑定.技能键[i].isDown();
                boolean 之前按住 = 技能键按住状态[i];

                if (当前按住 && !之前按住) {
                    // 检查CD（客户端也要检查，避免无效请求和错误的蓄力动画）
                    if (检查技能是否冷却中(player, i)) {
                        continue;
                    }

                    技能键按住状态[i] = true;
                    网络工具.发送到服务端(new 技能按下请求包(i));
                    蓄力状态管理.尝试开始蓄力(player, i);
                    PuellaMagi.LOGGER.debug("技能键 {} 按下", i);
                } else if (!当前按住 && 之前按住) {
                    技能键按住状态[i] = false;
                    网络工具.发送到服务端(new 技能松开请求包(i));
                    蓄力状态管理.结束槽位蓄力(i);
                    PuellaMagi.LOGGER.debug("技能键 {} 松开", i);
                }
            }
        }

        /**
         * 检查指定槽位的技能是否在冷却中
         */
        private static boolean 检查技能是否冷却中(Player player, int slotIndex) {
            var capOpt = 能力工具.获取技能能力(player);
            if (capOpt.isEmpty()) return false;

            var cap = capOpt.get();
            var preset = cap.获取当前预设();
            if (slotIndex >= preset.获取槽位数量()) return false;

            var slotData = preset.获取槽位(slotIndex);
            if (slotData == null || slotData.是否为空()) return false;

            net.minecraft.resources.ResourceLocation skillId = slotData.获取技能ID();
            return cap.是否冷却中(skillId);
        }

        private static void 释放所有技能键(Player player) {
            for (int i = 0; i < 技能键按住状态.length; i++) {
                if (技能键按住状态[i]) {
                    技能键按住状态[i] = false;
                    网络工具.发送到服务端(new 技能松开请求包(i));
                    PuellaMagi.LOGGER.debug("技能键 {} 强制松开", i);
                }
            }
            蓄力状态管理.清除所有状态();
        }

        private static void 处理变身按键(Player player) {
            if (能力工具.是否已变身(player)) {
                网络工具.发送到服务端(new 变身请求包());} else {
                if (!能力工具.是否已契约(player)) {
                    return;
                }
                网络工具.发送到服务端(new 变身请求包(true));
            }
        }

        private static void 切换技能栏折叠() {
            boolean current = 客户端状态管理.技能栏是否折叠();
            客户端状态管理.设置技能栏折叠(!current);
            PuellaMagi.LOGGER.debug("技能栏折叠状态: {}", !current);
        }
    }
}
