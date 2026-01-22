// 文件路径: src/main/java/com/v2t/puellamagi/system/soulgem/item/悲叹之种物品.java

package com.v2t.puellamagi.system.soulgem.item;

import com.v2t.puellamagi.system.soulgem.灵魂宝石管理器;
import com.v2t.puellamagi.util.能力工具;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 悲叹之种
 *魔女掉落物，用于净化灵魂宝石
 *
 * 效果：
 * - 污浊度 -50%
 * - 修复龟裂状态
 */
public class 悲叹之种物品 extends Item {

    public 悲叹之种物品() {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        //仅服务端处理
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // 检查是否为灵魂宝石系魔法少女
        if (!能力工具.是灵魂宝石系(player)) {
            player.displayClientMessage(Component.translatable("message.puellamagi.grief_seed.not_soul_gem_user")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // 使用悲叹之种
        boolean success = 灵魂宝石管理器.使用悲叹之种((ServerPlayer) player);

        if (success) {
            // 消耗物品
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // 播放音效（后续添加）
            // level.playSound(null, player.blockPosition(), ModSounds.GRIEF_SEED_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.puellamagi.grief_seed.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.puellamagi.grief_seed.effect")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
