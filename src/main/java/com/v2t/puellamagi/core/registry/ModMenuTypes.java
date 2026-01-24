// 文件路径: src/main/java/com/v2t/puellamagi/core/registry/ModMenuTypes.java

package com.v2t.puellamagi.core.registry;

import com.v2t.puellamagi.system.interaction.menu.搜身菜单;
import com.v2t.puellamagi.常量;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Menu类型注册
 */
public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, 常量.MOD_ID);

    public static final RegistryObject<MenuType<搜身菜单>> 搜身菜单类型 =
            MENUS.register("search", () -> IForgeMenuType.create(搜身菜单::createClientMenu));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
