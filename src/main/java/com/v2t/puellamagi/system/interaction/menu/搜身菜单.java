// 文件路径: src/main/java/com/v2t/puellamagi/system/interaction/menu/搜身菜单.java

package com.v2t.puellamagi.system.interaction.menu;

import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.区域类型;
import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.搜身容器信息;
import com.v2t.puellamagi.api.interaction.I搜身槽位提供者.槽位限制;
import com.v2t.puellamagi.core.registry.ModMenuTypes;
import com.v2t.puellamagi.mixin.access.AbstractContainerMenuAccessor;
import com.v2t.puellamagi.system.interaction.搜身槽位注册表;
import com.v2t.puellamagi.system.interaction.搜身管理器;
import com.v2t.puellamagi.util.交互工具;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static com.v2t.puellamagi.system.interaction.menu.搜身布局常量.*;

/**
 * 搜身菜单
 *
 * 解耦设计：
 * - 主区域管理器：管理背包等大容量槽位（在主面板内）
 * - 侧边区域管理器：管理装备、副手、饰品等（在独立侧边面板内）
 * - 各区域独立分页，互不影响
 */
public class 搜身菜单 extends AbstractContainerMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger("PuellaMagi/SearchMenu");

    // ==================== 玩家引用 ====================

    private final Player 目标玩家;
    private final Player 搜身者;
    private final Inventory 搜身者背包;

    // ==================== 区域管理器 ====================

    private final 搜身区域管理器 主区域;
    private final 搜身区域管理器 侧边区域;

    // ==================== 布局状态 ====================

    private int 主区域目标槽位数 = 0;
    private int 侧边区域目标槽位数 = 0;
    private int 搜身者背包Y = 0;
    private int 计算的GUI高度 = 0;

    // ==================== 构造器 ====================

    /**
     * 服务端构造
     */
    public 搜身菜单(int containerId, Inventory 搜身者背包, Player 目标玩家) {
        super(ModMenuTypes.搜身菜单类型.get(), containerId);
        this.搜身者 = 搜身者背包.player;
        this.搜身者背包 = 搜身者背包;
        this.目标玩家 = 目标玩家;

        // 创建区域管理器
        this.主区域 = new 搜身区域管理器(区域类型.主区域, 最大目标区域高度, 主区域左边距);
        this.侧边区域 = new 搜身区域管理器(区域类型.侧边区域, 最大目标区域高度, 计算侧边槽位起始X());

        // 初始化
        收集容器();
        计算分页();
        构建当前页槽位();
    }

    /**
     * 客户端构造
     */
    public static 搜身菜单 createClientMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        UUID targetUUID = buf.readUUID();
        Player target = playerInv.player.level().getPlayerByUUID(targetUUID);

        if (target == null) {
            LOGGER.error("客户端无法找到目标玩家: {}", targetUUID);
            target = playerInv.player;
        }

        return new 搜身菜单(containerId, playerInv, target);
    }

    // ==================== 初始化 ====================

    private void 收集容器() {
        List<搜身容器信息> 所有容器 = 搜身槽位注册表.获取所有容器(目标玩家);

        for (搜身容器信息 容器 : 所有容器) {
            if (容器.区域() == 区域类型.主区域) {
                主区域.添加容器(容器);
            } else {
                侧边区域.添加容器(容器);
            }
        }
    }

    private void 计算分页() {
        主区域.计算分页();
        侧边区域.计算分页();
    }

    private void 构建当前页槽位() {
        // 清空槽位
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        this.slots.clear();
        accessor.puellamagi$getLastSlots().clear();
        accessor.puellamagi$getRemoteSlots().clear();

        // 构建各区域槽位
        主区域.构建当前页(顶部边距);
        侧边区域.构建当前页(计算侧边内容起始Y());

        // 添加主区域槽位
        for (搜身区域管理器.槽位信息 info : 主区域.获取当前页槽位()) {
            this.addSlot(创建槽位(info, 目标玩家));
        }
        主区域目标槽位数 = 主区域.获取当前页槽位数();

        // 添加侧边区域槽位
        for (搜身区域管理器.槽位信息 info : 侧边区域.获取当前页槽位()) {
            this.addSlot(创建槽位(info, 目标玩家));
        }
        侧边区域目标槽位数 = 侧边区域.获取当前页槽位数();

        // 计算搜身者背包Y（仅基于主区域高度）
        int 主区域高度 = 主区域.获取当前页内容高度();
        搜身者背包Y = 顶部边距 + 主区域高度 + 分隔线高度;

        // 添加搜身者背包槽位
        添加搜身者槽位();

        // 计算GUI高度
        计算的GUI高度 = 搜身者背包Y + 搜身者背包高度;
    }

    /**
     * 根据槽位信息创建正确类型的 Slot
     */
    private Slot 创建槽位(搜身区域管理器.槽位信息 info, Player target) {
        槽位限制 限制 = info.限制();

        // 有限制且不是无限制类型时，使用限制槽位
        if (限制 != null && !(限制 instanceof 槽位限制.无限制)) {
            return new 搜身限制槽位(
                    info.容器(),
                    info.容器槽位索引(),
                    info.x(),
                    info.y(),
                    限制,
                    target
            );
        } else {
            return new Slot(info.容器(), info.容器槽位索引(), info.x(), info.y());
        }
    }

    private void 添加搜身者槽位() {
        int y = 搜身者背包Y + 标签高度;

        // 主背包 (3行9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                int x = 主区域左边距 + col * 槽位尺寸 + 1;
                int slotY = y + row * 槽位尺寸 + 1;
                this.addSlot(new Slot(搜身者背包, slotIndex, x, slotY));
            }
        }

        // 快捷栏
        int hotbarY = y + 3 * 槽位尺寸 + 4;
        for (int col = 0; col < 9; col++) {
            int x = 主区域左边距 + col * 槽位尺寸 + 1;
            this.addSlot(new Slot(搜身者背包, col, x, hotbarY + 1));
        }
    }

    // ==================== 翻页 API（主区域）====================

    public void 主区域上一页() {
        主区域.上一页();
        构建当前页槽位();
    }

    public void 主区域下一页() {
        主区域.下一页();
        构建当前页槽位();
    }

    public void 主区域跳转到页(int page) {
        主区域.跳转到页(page);
        构建当前页槽位();
    }

    // ==================== 翻页 API（侧边区域）====================

    public void 侧边区域上一页() {
        侧边区域.上一页();
        构建当前页槽位();
    }

    public void 侧边区域下一页() {
        侧边区域.下一页();
        构建当前页槽位();
    }

    public void 侧边区域跳转到页(int page) {
        侧边区域.跳转到页(page);
        构建当前页槽位();
    }

    // ==================== Container方法 ====================

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        int 目标槽位结束 = 主区域目标槽位数 + 侧边区域目标槽位数;
        int 搜身者背包起始 = 目标槽位结束;
        int 搜身者背包结束 = 搜身者背包起始 + 36;

        if (slotIndex < 目标槽位结束) {
            // 从目标槽位移到搜身者背包
            if (!this.moveItemStackTo(slotStack, 搜身者背包起始, 搜身者背包结束, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从搜身者背包移到目标槽位（优先主区域）
            if (!this.moveItemStackTo(slotStack, 0, 目标槽位结束, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (目标玩家 == null || !目标玩家.isAlive()) {
            return false;
        }

        // 服务端：通过会话管理器验证
        if (player instanceof ServerPlayer serverPlayer && 目标玩家 instanceof ServerPlayer targetServer) {
            return 搜身管理器.会话是否有效(serverPlayer, targetServer);
        }

        // 客户端：使用玩家的实体交互距离
        return 交互工具.在实体交互范围内(player, 目标玩家);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        搜身管理器.结束搜身(player.getUUID());
    }

    // ==================== Getter ====================

    public Player 获取目标玩家() {
        return 目标玩家;
    }

    public 搜身区域管理器 获取主区域() {
        return 主区域;
    }

    public 搜身区域管理器 获取侧边区域() {
        return 侧边区域;
    }

    public int 获取搜身者背包Y() {
        return 搜身者背包Y;
    }

    public int 获取GUI高度() {
        return 计算的GUI高度;
    }

    public int 获取目标槽位总数() {
        return 主区域目标槽位数 + 侧边区域目标槽位数;
    }
}
