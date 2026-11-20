package com.pvzge.pvzgeitem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 将 PVZGE 的内容移植到 Minecraft。
 * 目前仅注册创造模式标签页「武林寺庙」，物品、模型、贴图、音乐等内容将在后续版本加入。
 */
@Mod(PvzgeitemMod.MODID)
public class PvzgeitemMod {

    public static final String MODID = "pvzgeitem";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /**
     * 创造模式标签页：武林寺庙。
     *
     * 注意：Minecraft 不会显示一个既没有图标、又没有内容的标签页，
     * 所以这里临时借用原版的「钟」当图标和占位内容（不需要任何贴图/模型）。
     * 以后加入 PVZGE 物品时，把 icon 和 displayItems 里的 Items.BELL 换成自己的物品即可。
     */
    public static final Supplier<CreativeModeTab> WULIN_TEMPLE_TAB =
            CREATIVE_MODE_TABS.register("wulin_temple", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".wulin_temple"))
                    .icon(() -> new ItemStack(Items.BELL))
                    .displayItems((parameters, output) -> {
                        // 临时占位：让标签页可见。以后加 PVZGE 物品时删掉这行
                        output.accept(Items.BELL);
                    })
                    .build());

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public PvzgeitemMod(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
