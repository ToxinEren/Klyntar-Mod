package modKlyntar;

import modKlyntar.block.PromethiumXBlock;
import modKlyntar.block.PromethiumXTNTBlock;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.client.ClientEventHandler;
import modKlyntar.client.renderer.GhastProjectileRenderer;
import modKlyntar.client.renderer.PrimedPromethiumXTntRenderer;
import modKlyntar.client.renderer.SmokeTrailRenderer;
import modKlyntar.client.renderer.SymbioteRenderer;
import modKlyntar.client.renderer.VenomRenderer;
import modKlyntar.client.renderer.WebProjectileRenderer;
import modKlyntar.commands.TransformCommand;
import modKlyntar.entity.custom.CustomArrowEntity;
import modKlyntar.entity.custom.GhastProjectileEntity;
import modKlyntar.entity.custom.PrimedPromethiumXTnt;
import modKlyntar.entity.custom.SmokeTrailEntity;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.entity.custom.TentacleSegmentEntity;
import modKlyntar.entity.custom.VenomEntity;
import modKlyntar.entity.custom.WebProjectileEntity;
import modKlyntar.input.KeyInputHandler;
import modKlyntar.network.ModNetwork;
import modKlyntar.power.PlayersPower;
import modKlyntar.power.PlayersPowerProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "mymod";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Block> PROMETHIUMX_BLOCK = BLOCKS.register("promethiumx", PromethiumXBlock::new);
    public static final RegistryObject<Item> PROMETHIUMX_BLOCK_ITEM = ITEMS.register("promethium_x",
            () -> new BlockItem(PROMETHIUMX_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Block> PROMETHIUMX_TNT = BLOCKS.register("promethiumx_tnt", PromethiumXTNTBlock::new);
    public static final RegistryObject<Item> PROMETHIUMX_TNT_ITEM = ITEMS.register("promethium_x_tnt",
            () -> new BlockItem(PROMETHIUMX_TNT.get(), new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> KLYNTAR_TAB = CREATIVE_MODE_TABS.register("klyntar_tab", () -> CreativeModeTab.builder()
            .icon(() -> PROMETHIUMX_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PROMETHIUMX_BLOCK_ITEM.get());
                output.accept(PROMETHIUMX_TNT_ITEM.get());
            })
            .build());

    public static final EntityDataAccessor<Float> PLAYER_SCALE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);

    public static final RegistryObject<EntityType<PrimedPromethiumXTnt>> PROMETHIUMX_TNT_ENTITY = ENTITY_TYPES.register("primed_promethiumx_tnt",
            () -> EntityType.Builder.<PrimedPromethiumXTnt>of(PrimedPromethiumXTnt::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .setTrackingRange(10)
                    .setUpdateInterval(10)
                    .build(new ResourceLocation(MOD_ID, "primed_promethiumx_tnt").toString()));

    public static final RegistryObject<EntityType<GhastProjectileEntity>> GHAST_PROJECTILE_ENTITY = ENTITY_TYPES.register("ghast_projectile",
            () -> EntityType.Builder.<GhastProjectileEntity>of(GhastProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(80)
                    .setUpdateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(MOD_ID, "ghast_projectile").toString()));

    public static final RegistryObject<EntityType<SymbioteEntity>> SYMBIOTE_ENTITY = ENTITY_TYPES.register("symbiote",
            () -> EntityType.Builder.of(SymbioteEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build(new ResourceLocation(MOD_ID, "symbiote").toString()));

    public static final RegistryObject<EntityType<VenomEntity>> VENOM_ENTITY2 = ENTITY_TYPES.register("venom",
            () -> EntityType.Builder.of(VenomEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 4.0F)
                    .build(new ResourceLocation(MOD_ID, "venom").toString()));

    public static final RegistryObject<EntityType<SmokeTrailEntity>> SMOKE_TRAIL_ENTITY = ENTITY_TYPES.register("smoke_trail",
            () -> EntityType.Builder.<SmokeTrailEntity>of(SmokeTrailEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build(new ResourceLocation(MOD_ID, "smoke_trail").toString()));

    public static final RegistryObject<EntityType<CustomArrowEntity>> CUSTOM_ARROW = ENTITY_TYPES.register("custom_arrow",
            () -> EntityType.Builder.<CustomArrowEntity>of(CustomArrowEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build(new ResourceLocation(MOD_ID, "custom_arrow").toString()));

    public static final RegistryObject<EntityType<TentacleSegmentEntity>> TENTACLE_SEGMENT = ENTITY_TYPES.register("tentacle_segment",
            () -> EntityType.Builder.<TentacleSegmentEntity>of(TentacleSegmentEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build(new ResourceLocation(MOD_ID, "tentacle_segment").toString()));

    public static final RegistryObject<EntityType<WebProjectileEntity>> WEB_PROJECTILE_ENTITY = ENTITY_TYPES.register("web_projectile",
            () -> EntityType.Builder.<WebProjectileEntity>of(WebProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(80)
                    .setUpdateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(MOD_ID, "web_projectile").toString()));

    public MyMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);
        modEventBus.addListener(this::registerAttributes);

        GeckoLib.initialize();
        ModNetwork.registerPackets();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.class);
        MinecraftForge.EVENT_BUS.register(PlayerPowerCapability.class);
        MinecraftForge.EVENT_BUS.register(PlayersPowerProvider.class);
        MinecraftForge.EVENT_BUS.register(PlayersPower.class);
        MinecraftForge.EVENT_BUS.register(PlayerEventSubscriber.class);
        MinecraftForge.EVENT_BUS.register(KeyInputHandler.class);
    }

    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        EntityRenderers.register(SYMBIOTE_ENTITY.get(), SymbioteRenderer::new);
        EntityRenderers.register(VENOM_ENTITY2.get(), VenomRenderer::new);
        EntityRenderers.register(PROMETHIUMX_TNT_ENTITY.get(), PrimedPromethiumXTntRenderer::new);
        EntityRenderers.register(SMOKE_TRAIL_ENTITY.get(), SmokeTrailRenderer::new);
        EntityRenderers.register(GHAST_PROJECTILE_ENTITY.get(), GhastProjectileRenderer::new);
        EntityRenderers.register(WEB_PROJECTILE_ENTITY.get(), WebProjectileRenderer::new);
    }

    private void onServerStarting(ServerStartingEvent event) {
        TransformCommand.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SYMBIOTE_ENTITY.get(), SymbioteEntity.createAttributes().build());
        event.put(VENOM_ENTITY2.get(), VenomEntity.createAttributes().build());
        event.put(TENTACLE_SEGMENT.get(), TentacleSegmentEntity.createAttributes().build());
    }
}
