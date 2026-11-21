package com.pvzge.pvzgeitem;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 将 PVZGE 的内容移植到 Minecraft。
 */
@Mod(PvzgeitemMod.MODID)
public class PvzgeitemMod {

    public static final String MODID = "pvzgeitem";

    // ── 创造模式标签页：武林寺庙 ──────────────────────────────
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> WULIN_TEMPLE_TAB =
            TABS.register("wulin_temple", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".wulin_temple"))
                    .icon(() -> new ItemStack(ModItems.NUNCHAKU.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.NUNCHAKU.get()))
                    .build());

    public PvzgeitemMod(IEventBus modBus) {
        // 先注册物品，再注册标签页（标签页图标和内容要用到双截棍）
        ModItems.ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(PvzgeitemMod::registerPayloads);
        NeoForge.EVENT_BUS.addListener(PvzgeitemMod::onProjectileImpact);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                NunchakuKnockbackPayload.TYPE,
                NunchakuKnockbackPayload.STREAM_CODEC,
                NunchakuKnockbackPayload::handle);
    }

    /**
     * 反弹弹射物：按住右键转棍时，身边 3 格内任何弹射物的撞击都会被取消并原路弹回。
     * 末影人是被弹射物打中时瞬移躲开，这里按需求改成反向弹回。
     */
    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        if (projectile.level().isClientSide) return;

        Player defender = null;
        for (Player candidate : projectile.level().getEntitiesOfClass(
                Player.class, projectile.getBoundingBox().inflate(3.0D))) {
            if (candidate.isUsingItem() && candidate.getUseItem().is(ModItems.NUNCHAKU.get())) {
                defender = candidate;
                break;
            }
        }
        if (defender == null) return;

        event.setCanceled(true);
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.0D));
        projectile.setOwner(defender);
    }

    /** R 键包：客户端按下 R 后发到服务端，由服务端校验并击退。 */
    public record NunchakuKnockbackPayload() implements CustomPacketPayload {

        public static final Type<NunchakuKnockbackPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "nunchaku_knockback"));

        public static final StreamCodec<ByteBuf, NunchakuKnockbackPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> {}, buf -> new NunchakuKnockbackPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** 参考击退附魔：强度 1.5 约等于击退 III，只推正前方 4 格内的生物。 */
        public static void handle(NunchakuKnockbackPayload payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.getMainHandItem().is(ModItems.NUNCHAKU.get())
                    && !player.getOffhandItem().is(ModItems.NUNCHAKU.get())) return;

            Vec3 look = player.getLookAngle();
            Vec3 eye = player.getEyePosition();
            for (LivingEntity target : player.level().getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(4.0D))) {
                if (target == player || !target.isAlive()) continue;
                Vec3 direction = target.position().subtract(eye).normalize();
                if (direction.dot(look) < 0.55D) continue;
                target.knockback(1.5D, look.x, look.z);
            }
        }
    }
}
