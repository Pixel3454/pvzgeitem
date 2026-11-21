package com.pvzge.pvzgeitem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, PvzgeitemMod.MODID);

    /**
     * 攻击伤害 8 点：基础 1 + 下界合金等级加成 4 + 3 = 8（与 1.21.1 下界合金剑相同）。
     * 攻击速度 4 - 2.4 = 1.6，与原版剑一致。
     */
    public static final Supplier<Item> NUNCHAKU = ITEMS.register("nunchaku",
            () -> new NunchakuItem(new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.4F))));

    private ModItems() {}
}

/** 双截棍本体。 */
class NunchakuItem extends Item {

    NunchakuItem(Properties properties) {
        super(properties);
    }

    // 左键：8 点伤害由注册的属性修饰符提供；「抽打」第一人称动画在 ClientSetup 里实现。

    /** 右键：开始持续旋转，松开右键即停止。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // 足够长，实际停止靠松开右键
    }

    /** 每 200 tick（10 秒）扣 1 点饱食度，只在服务端执行。 */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        int elapsed = getUseDuration(stack, entity) - remainingUseDuration;
        if (elapsed > 0 && elapsed % 200 == 0) {
            int food = player.getFoodData().getFoodLevel();
            if (food > 0) {
                player.getFoodData().setFoodLevel(food - 1);
            }
        }
    }
}
