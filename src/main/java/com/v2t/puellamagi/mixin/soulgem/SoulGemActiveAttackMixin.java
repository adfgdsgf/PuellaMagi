package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.core.network.packets.c2s.灵魂宝石损坏请求包;
import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.soulgem.damage.active.主动损坏触发类型;
import com.v2t.puellamagi.system.soulgem.effect.假死状态处理器;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * 灵魂宝石主动攻击检测（客户端）
 *
 * 拦截左键，射线检测是否瞄准灵魂宝石掉落物
 */
@Mixin(Minecraft.class)
public class SoulGemActiveAttackMixin {

    @Unique
    private static final double puellamagi$检测距离 = 4.5;

    @Unique
    private static final double puellamagi$碰撞箱扩展 = 0.3;

    /**
     * 在 startAttack 开始时检测灵魂宝石掉落物
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void puellamagi$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;

        if (player == null || player.level() == null) return;

        // 假死状态不处理
        if (假死状态处理器.客户端是否假死()) {
            return;
        }

        // 射线检测灵魂宝石掉落物
        Optional<ItemEntity> target = puellamagi$检测灵魂宝石掉落物(player);

        if (target.isPresent()) {
            // 发送攻击请求
            网络工具.发送到服务端(new 灵魂宝石损坏请求包(
                    主动损坏触发类型.左键攻击,
                    target.get().getId()
            ));

            // 播放挥动动画
            player.swing(player.getUsedItemHand());

            // 取消默认攻击流程，返回 true 表示已处理
            cir.setReturnValue(true);
        }
    }

    /**
     * 射线检测玩家视线内的灵魂宝石掉落物
     */
    @Unique
    private Optional<ItemEntity> puellamagi$检测灵魂宝石掉落物(LocalPlayer player) {
        Vec3 眼睛位置 = player.getEyePosition();
        Vec3 视线方向 = player.getViewVector(1.0F);
        Vec3 终点 = 眼睛位置.add(视线方向.scale(puellamagi$检测距离));

        //搜索范围
        AABB 搜索范围 = player.getBoundingBox()
                .expandTowards(视线方向.scale(puellamagi$检测距离))
                .inflate(1.0);

        // 获取范围内所有灵魂宝石掉落物
        List<ItemEntity> 掉落物列表 = player.level().getEntitiesOfClass(
                ItemEntity.class,
                搜索范围,
                entity -> {
                    ItemStack stack = entity.getItem();
                    return !stack.isEmpty() && stack.is(ModItems.SOUL_GEM.get());
                }
        );

        // 找到视线最接近的
        ItemEntity 最近目标 = null;
        double 最近距离 = puellamagi$检测距离;

        for (ItemEntity item : 掉落物列表) {
            //稍微放大碰撞箱便于命中
            AABB 碰撞箱 = item.getBoundingBox().inflate(puellamagi$碰撞箱扩展);
            Optional<Vec3> 命中点 = 碰撞箱.clip(眼睛位置, 终点);
            if (命中点.isPresent()) {
                double 距离 = 眼睛位置.distanceTo(命中点.get());
                if (距离 < 最近距离) {
                    最近距离 = 距离;
                    最近目标 = item;
                }
            }
        }

        return Optional.ofNullable(最近目标);
    }
}
