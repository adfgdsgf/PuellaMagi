// 文件路径: src/main/java/com/v2t/puellamagi/mixin/access/GuiAccessor.java

package com.v2t.puellamagi.mixin.access;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问Gui的聊天组件
 */
@Mixin(Gui.class)
public interface GuiAccessor {

    @Accessor("chat")
    ChatComponent puellamagi$getChat();

    @Accessor("tickCount")
    int puellamagi$getTickCount();
}
