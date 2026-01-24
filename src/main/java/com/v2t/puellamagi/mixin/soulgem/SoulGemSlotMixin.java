package com.v2t.puellamagi.mixin.soulgem;

import com.v2t.puellamagi.core.registry.ModItems;
import com.v2t.puellamagi.system.soulgem.data.灵魂宝石世界数据;
import com.v2t.puellamagi.system.soulgem.data.存储类型;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石数据;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 灵魂宝石容器槽位处理
 *
 * 当灵魂宝石通过GUI被放入容器槽位时，汇报位置到世界数据（持有者为null）
 */
@Mixin(Slot.class)
public abstract class SoulGemSlotMixin {

    @Shadow
    @Final
    public Container container;

    @Inject(method = "set", at = @At("HEAD"))
    private void onSet(ItemStack stack, CallbackInfo ci) {
        // 只处理灵魂宝石
        if (stack.isEmpty() || !stack.is(ModItems.SOUL_GEM.get())) {
            return;
        }

        // 检查容器是否为方块实体（有位置信息）
        if (!(container instanceof BlockEntity blockEntity)) {
            return;
        }

        // 只在服务端处理
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) {
            return;
        }

        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 获取所有者
        UUID ownerUUID = 灵魂宝石数据.获取所有者UUID(stack);
        if (ownerUUID == null) return;

        // 获取世界数据
        灵魂宝石世界数据 worldData = 灵魂宝石世界数据.获取(serverLevel.getServer());

        // 验证有效性
        long gemTimestamp = 灵魂宝石数据.获取时间戳(stack);
        if (!worldData.验证时间戳(ownerUUID, gemTimestamp)) {
            //旧宝石，清空（会在下一次GUI交互时生效）
            stack.setCount(0);
            return;
        }

        // 汇报位置（容器没有持有者，传null）
        BlockPos pos = blockEntity.getBlockPos();
        worldData.更新位置(
                ownerUUID,
                serverLevel.dimension(),
                Vec3.atCenterOf(pos),
                存储类型.容器,
                null,
                serverLevel.getGameTime()  // 新增：游戏时间
        );
    }
}
