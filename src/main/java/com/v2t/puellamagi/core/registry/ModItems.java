// 文件路径: src/main/java/com/v2t/puellamagi/core/registry/ModItems.java

package com.v2t.puellamagi.core.registry;

import com.v2t.puellamagi.常量;
import com.v2t.puellamagi.system.soulgem.item.灵魂宝石物品;
import com.v2t.puellamagi.system.soulgem.item.悲叹之种物品;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册表
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, 常量.MOD_ID);

    //==================== 灵魂宝石系====================

    public static final RegistryObject<Item> SOUL_GEM =
            ITEMS.register("soul_gem", 灵魂宝石物品::new);

    public static final RegistryObject<Item> GRIEF_SEED =
            ITEMS.register("grief_seed", 悲叹之种物品::new);

    // ==================== 注册方法 ====================

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
