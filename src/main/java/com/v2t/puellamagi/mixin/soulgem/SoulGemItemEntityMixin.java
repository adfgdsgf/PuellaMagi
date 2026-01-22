// 文件路径: src/main/java/com/v2t/puellamagi/mixin/soulgem/SoulGemItemEntityMixin.java

package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 灵魂宝石掉落物处理
 *
 * 职责：
 * 1. 验证有效性（时间戳与世界数据比对）
 * 2. 汇报位置到世界数据（持有者为null）
 */
@Mixin(ItemEntity.class)
public abstract class SoulGemItemEntityMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (self.level().isClientSide) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        ItemStack stack = getItem();

        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return;
        }

        if (self.tickCount % 20 != 0) return;

        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null) return;

        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(serverLevel.getServer());

        long gemTimestamp = 灵魂宝石数据.获取时间戳(stack);
        if (!worldData.验证时间戳(ownerUUID, gemTimestamp)) {
            self.discard();
            return;
        }

        //掉落物没有持有者，传null
        worldData.更新位置(
                ownerUUID,
                serverLevel.dimension(),
                self.position(),
                存储类型.掉落物,
                null  // 没有持有者
        );
    }
}
