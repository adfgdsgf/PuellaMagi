// 文件路径: src/main/java/com/v2t/puellamagi/core/event/client/客户端交互事件.java

package com.v2t.puellamagi.core.event.client;

import com.v2t.puellamagi.api.access.IProjectileAccess;
import com.v2t.puellamagi.api.timestop.TimeStop;
import com.v2t.puellamagi.client.keybind.按键绑定;
import com.v2t.puellamagi.core.config.时停配置;
import com.v2t.puellamagi.core.network.packets.c2s.掉落物拾取请求包;
import com.v2t.puellamagi.core.network.packets.c2s.投射物拾取请求包;
import com.v2t.puellamagi.core.network.packets.c2s.搜身请求包;
import com.v2t.puellamagi.util.网络工具;
import com.v2t.puellamagi.常量;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端交互事件
 *
 * 处理：
 * - 时停中右键拾取掉落物和投射物
 * - 搜身（修饰键+右键点击玩家）
 */
@Mod.EventBusSubscriber(modid = 常量.MOD_ID, value = Dist.CLIENT)
public class 客户端交互事件 {

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        // 只处理右键按下
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        // 优先检测搜身（修饰键+右键+目标是玩家）
        if (尝试搜身(mc)) {
            event.setCanceled(true);
            return;
        }

        // 时停中的拾取逻辑
        处理时停拾取(mc, event);
    }

    /**
     * 尝试搜身
     * @return 是否成功触发搜身（需要取消原事件）
     */
    private static boolean 尝试搜身(Minecraft mc) {
        // 检查修饰键是否按住
        if (!按键绑定.搜身修饰键.isDown()) {
            return false;
        }

        // 检测准心指向的玩家
        Player target = 检测准心玩家(mc);
        if (target == null) {
            return false;
        }

        // 不能搜自己
        if (target.equals(mc.player)) {
            return false;
        }

        // 发送搜身请求
        网络工具.发送到服务端(new 搜身请求包(target.getUUID()));
        return true;
    }

    /**
     * 检测准心指向的玩家
     */
    private static Player 检测准心玩家(Minecraft mc) {
        if (mc.player == null) return null;

        double reach = mc.player.getAttributeValue(
                net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(reach));

        AABB searchBox = mc.player.getBoundingBox()
                .expandTowards(lookVec.scale(reach))
                .inflate(1.0);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                mc.player,
                eyePos,
                endPos,
                searchBox,
                entity -> entity instanceof Player && !entity.isSpectator() && entity != mc.player,
                reach * reach
        );

        if (result != null && result.getEntity() instanceof Player player) {
            return player;
        }

        return null;
    }

    /**
     * 处理时停中的拾取
     */
    private static void 处理时停拾取(Minecraft mc, InputEvent.MouseButton.Pre event) {
        TimeStop timeStop = (TimeStop) mc.level;

        // 只有时停者需要右键拾取
        if (!timeStop.puellamagi$isTimeStopper(mc.player)) return;
        if (!timeStop.puellamagi$hasActiveTimeStop()) return;

        // 检测准心指向的实体
        Entity target = 检测准心物品或投射物(mc);
        if (target instanceof ItemEntity) {
            网络工具.发送到服务端(new 掉落物拾取请求包(target.getId()));
            event.setCanceled(true);} else if (target instanceof AbstractArrow arrow) {
            IProjectileAccess access = (IProjectileAccess) arrow;
            double stopThreshold = 时停配置.获取静止阈值();

            if (access.puellamagi$getSpeedMultiplier() <= stopThreshold
                    || !access.puellamagi$isTimeStopCreated()) {
                网络工具.发送到服务端(new 投射物拾取请求包(target.getId()));
                event.setCanceled(true);
            }
        }
    }

    /**
     * 检测准心指向的掉落物或投射物
     */
    private static Entity 检测准心物品或投射物(Minecraft mc) {
        if (mc.player == null) return null;

        double reach = mc.player.getAttributeValue(
                net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        reach = Math.max(reach, 3.0);

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(reach + 1.0));

        AABB searchBox = mc.player.getBoundingBox()
                .expandTowards(lookVec.scale(reach + 1.0))
                .inflate(1.5);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                mc.player,
                eyePos,
                endPos,
                searchBox,
                entity -> (entity instanceof ItemEntity || entity instanceof AbstractArrow)
                        && !entity.isSpectator(),
                reach * reach
        );

        if (result != null) {
            return result.getEntity();
        }

        return null;
    }
}
