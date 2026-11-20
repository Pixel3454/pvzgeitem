package com.pvzge.pvzgeitem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
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
     * 物品尚未添加，NeoForge 会自动使用一个占位图标，待后续加入物品后再设置正式图标。
     */
    public static final Supplier<CreativeModeTab> WULIN_TEMPLE_TAB =
            CREATIVE_MODE_TABS.register("wulin_temple", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".wulin_temple"))
                    .icon(PvzgeitemMod::createTabIcon)
                    .displayItems((parameters, output) -> {
                        // 后续版本在此处添加物品
                    })
                    .build());

    /**
     * 暂未制作图标：有物品后改为 new ItemStack(物品.get())，当前先返回空图标。
     */
    private static ItemStack createTabIcon() {
        return ItemStack.EMPTY;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public PvzgeitemMod(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
