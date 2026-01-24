// 文件路径: src/main/java/com/v2t/puellamagi/mixin/client/CreativeModeDeleteMixin.java

package com.v2t.puellamagi.mixin.client;

import com.v2t.puellamagi.api.item.I绑定物品;
import com.v2t.puellamagi.core.network.packets.c2s.创造模式删除绑定物品包;
import com.v2t.puellamagi.util.绑定物品快照;
import com.v2t.puellamagi.util.网络工具;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 客户端检测创造模式删除绑定物品
 *
 * 覆盖场景：
 * -拖入创造物品栏
 * - 点击垃圾桶按钮
 * - 用其他物品替换槽位
 * - Shift+左键删除
 * - 数字键替换
 *
 * 原理：在操作前记录绑定物品，延迟一帧后检测是否消失
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeDeleteMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/CreativeDelete");

    @Unique
    private final List<绑定物品快照> puellamagi$操作前物品列表 = new ArrayList<>();

    @Unique
    private boolean puellamagi$需要下帧检测 = false;

    //==================== slotClicked 处理 ====================

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClickedHead(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        //丢弃到背包外会生成掉落物，不是删除
        if (slotId < 0) {
            return;
        }

        puellamagi$记录所有绑定物品();
        if (!puellamagi$操作前物品列表.isEmpty()) {
            puellamagi$需要下帧检测 = true;
        }
    }

    // ==================== keyPressed 处理（数字键替换） ====================

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressedHead(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        puellamagi$记录所有绑定物品();
        if (!puellamagi$操作前物品列表.isEmpty()) {
            puellamagi$需要下帧检测 = true;
        }
    }

    // ==================== containerTick 延迟检测 ====================

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void onContainerTick(CallbackInfo ci) {
        if (!puellamagi$需要下帧检测) return;
        puellamagi$需要下帧检测 = false;

        puellamagi$检查删除并通知();
    }

    // ==================== 通用逻辑 ====================

    @Unique
    private void puellamagi$记录所有绑定物品() {
        puellamagi$操作前物品列表.clear();

        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        Player player = screen.getMinecraft().player;
        if (player == null) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            puellamagi$尝试记录绑定物品(stack);
        }

        ItemStack carried = screen.getMenu().getCarried();
        puellamagi$尝试记录绑定物品(carried);
    }

    @Unique
    private void puellamagi$检查删除并通知() {
        if (puellamagi$操作前物品列表.isEmpty()) return;

        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        Player player = screen.getMinecraft().player;
        if (player == null) {
            puellamagi$操作前物品列表.clear();
            return;
        }

        for (绑定物品快照 快照 : puellamagi$操作前物品列表) {
            if (!puellamagi$物品仍存在(player, screen, 快照)) {
                LOGGER.debug("检测到绑定物品被删除: 类型={},所有者={}",
                        快照.物品ID(), 快照.所有者UUID());
                网络工具.发送到服务端(new 创造模式删除绑定物品包(
                        快照.物品ID(),
                        快照.所有者UUID(),
                        快照.时间戳()
                ));
            }
        }

        puellamagi$操作前物品列表.clear();
    }

    @Unique
    private void puellamagi$尝试记录绑定物品(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof I绑定物品 绑定物品)) return;

        UUID 所有者 = 绑定物品.获取所有者UUID(stack);
        if (所有者 == null) return;

        puellamagi$操作前物品列表.add(new 绑定物品快照(
                ForgeRegistries.ITEMS.getKey(stack.getItem()),
                所有者,
                绑定物品.获取时间戳(stack)
        ));
    }

    @Unique
    private boolean puellamagi$物品仍存在(Player player, CreativeModeInventoryScreen screen, 绑定物品快照 目标快照) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            绑定物品快照 当前快照 = puellamagi$创建快照(stack);
            if (目标快照.匹配(当前快照)) {
                return true;
            }
        }

        ItemStack carried = screen.getMenu().getCarried();
        绑定物品快照 鼠标快照 = puellamagi$创建快照(carried);
        return 目标快照.匹配(鼠标快照);
    }

    @Unique
    private 绑定物品快照 puellamagi$创建快照(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof I绑定物品 绑定物品)) return null;

        UUID 所有者 = 绑定物品.获取所有者UUID(stack);
        if (所有者 == null) return null;

        return new 绑定物品快照(
                ForgeRegistries.ITEMS.getKey(stack.getItem()),
                所有者,
                绑定物品.获取时间戳(stack)
        );
    }
}
