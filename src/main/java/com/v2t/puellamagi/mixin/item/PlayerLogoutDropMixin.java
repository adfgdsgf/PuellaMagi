package com.v2t.puellamagi.mixin.item;

import com.v2t.puellamagi.api.item.I绑定物品;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 玩家下线时掉落非自己的绑定物品
 *
 * 防止玩家拿着别人的灵魂宝石下线导致对方假死超时死亡
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerLogoutDropMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/LogoutDrop");

    /**
     * 玩家断开连接时检查背包
     */
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void puellamagi$dropOthersBoundItems(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        UUID selfUUID = self.getUUID();

        // 遍历背包
        for (int i = 0; i < self.getInventory().getContainerSize(); i++) {
            ItemStack stack = self.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof I绑定物品 item) {
                // 检查是否启用下线掉落
                if (!item.是否下线掉落非己()) continue;

                // 检查所有者
                UUID ownerUUID = item.获取所有者UUID(stack);
                if (ownerUUID == null) continue;

                // 不是自己的物品
                if (!ownerUUID.equals(selfUUID)) {
                    // 从背包移除
                    self.getInventory().setItem(i, ItemStack.EMPTY);

                    // 生成掉落物
                    ItemEntity drop = new ItemEntity(
                            self.level(),
                            self.getX(), self.getY() + 0.5, self.getZ(),
                            stack.copy()
                    );
                    drop.setPickUpDelay(40); // 2秒拾取延迟
                    self.level().addFreshEntity(drop);LOGGER.info("玩家 {} 下线，掉落非己绑定物品: {} (所有者: {})",
                            self.getName().getString(),
                            item.获取绑定类型(),
                            ownerUUID.toString().substring(0, 8));
                }
            }
        }
    }
}
