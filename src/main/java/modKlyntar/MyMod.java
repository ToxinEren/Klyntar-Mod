package modKlyntar;

import modKlyntar.block.PromethiumXBlock;
import modKlyntar.block.PromethiumXSmokeHandler;
import modKlyntar.block.PromethiumXTNTBlock;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.entity.custom.CustomArrowEntity;
import modKlyntar.entity.custom.GhastProjectileEntity;
import modKlyntar.entity.custom.PrimedPromethiumXTnt;
import modKlyntar.entity.custom.SmokeTrailEntity;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.entity.custom.TentacleSegmentEntity;
import modKlyntar.entity.custom.VenomEntity;
import modKlyntar.entity.custom.WebProjectileEntity;
import modKlyntar.item.CapsuleItem;
import modKlyntar.network.ModNetwork;
import modKlyntar.power.PlayersPower;
import modKlyntar.power.PlayersPowerProvider;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
    public static final String MOD_ID = "klyntars";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** ricalco di spidermanaddon:venom_particle dello Spider-Man pack */
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> VENOM_PARTICLE =
            PARTICLE_TYPES.register("venom_particle",
                    () -> new net.minecraft.core.particles.SimpleParticleType(false));

    /** variante chiara usata dai poteri antivenom */
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> ANTIVENOM_PARTICLE =
            PARTICLE_TYPES.register("antivenom_particle",
                    () -> new net.minecraft.core.particles.SimpleParticleType(false));

    /** lo schizzo grosso che la bomba lascia sul terreno */
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> SYMBIOTE_SPLASH =
            PARTICLE_TYPES.register("antivenom_splash",
                    () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static final RegistryObject<Block> PROMETHIUMX_BLOCK = BLOCKS.register("promethiumx", PromethiumXBlock::new);
    public static final RegistryObject<Item> PROMETHIUMX_BLOCK_ITEM = ITEMS.register("promethium_x",
            () -> new BlockItem(PROMETHIUMX_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Block> KNULLS_FRAGMENT_BLOCK =

            BLOCKS.register("knulls_fragment", modKlyntar.block.KnullsFragmentBlock::new);

    public static final RegistryObject<Item> KNULLS_FRAGMENT_ITEM = ITEMS.register("knulls_fragment",

            () -> new net.minecraft.world.item.BlockItem(KNULLS_FRAGMENT_BLOCK.get(), new Item.Properties()));



    /** i due cadaveri nel cratere di All-Black, e i riempitivi del loro sedime */
    public static final RegistryObject<Block> DIO_MORTO =
            BLOCKS.register("dio_morto", modKlyntar.block.DioMortoBlock::new);
    public static final RegistryObject<Block> ALIENO_FUSO =
            BLOCKS.register("alieno_fuso", modKlyntar.block.AlienoFusoBlock::new);
    public static final RegistryObject<Block> PEZZO_DIO =
            BLOCKS.register("pezzo_dio",
                    () -> new modKlyntar.block.PezzoStrutturaBlock(DIO_MORTO::get, 15));
    public static final RegistryObject<Block> PEZZO_ALIENO =
            BLOCKS.register("pezzo_alieno",
                    () -> new modKlyntar.block.PezzoStrutturaBlock(ALIENO_FUSO::get));

    public static final RegistryObject<Block> PROMETHIUMX_TNT = BLOCKS.register("promethiumx_tnt", PromethiumXTNTBlock::new);
    public static final RegistryObject<Item> PROMETHIUMX_TNT_ITEM = ITEMS.register("promethium_x_tnt",
            () -> new BlockItem(PROMETHIUMX_TNT.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAPSULE = ITEMS.register("capsule",
            () -> new CapsuleItem(new Item.Properties().stacksTo(1).fireResistant(), "capsule", false));
    public static final RegistryObject<Item> VENOM_CAPSULE = ITEMS.register("venomcapsule",
            () -> new CapsuleItem(new Item.Properties().stacksTo(1).fireResistant(), "venomcapsule", true));
    // le altre forme riusano la geometria di venom e cambiano solo la texture del simbionte
    public static final RegistryObject<Item> ANTIVENOM_CAPSULE = ITEMS.register("antivenomcapsule",
            () -> new CapsuleItem(new Item.Properties().stacksTo(1).fireResistant(),
                    "antivenomcapsule", true, "venomcapsule", "capsule_model_antivenom"));
    public static final RegistryObject<Item> CARNAGE_CAPSULE = ITEMS.register("carnagecapsule",
            () -> new CapsuleItem(new Item.Properties().stacksTo(1).fireResistant(),
                    "carnagecapsule", true, "venomcapsule", "capsule_model_carnage"));
    public static final RegistryObject<Item> TOXIN_CAPSULE = ITEMS.register("toxincapsule",
            () -> new CapsuleItem(new Item.Properties().stacksTo(1).fireResistant(),
                    "toxincapsule", true, "venomcapsule", "capsule_model_toxin"));

    public static final RegistryObject<CreativeModeTab> KLYNTAR_TAB = CREATIVE_MODE_TABS.register("klyntar_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.klyntars.klyntar_tab"))
            .icon(() -> PROMETHIUMX_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PROMETHIUMX_BLOCK_ITEM.get());
                output.accept(PROMETHIUMX_TNT_ITEM.get());
                output.accept(CAPSULE.get());
                output.accept(VENOM_CAPSULE.get());
                output.accept(ANTIVENOM_CAPSULE.get());
                output.accept(CARNAGE_CAPSULE.get());
                output.accept(TOXIN_CAPSULE.get());

                output.accept(KNULLS_FRAGMENT_ITEM.get());
            })
            .build());

    public static final EntityDataAccessor<Float> PLAYER_SCALE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);

    public static final RegistryObject<EntityType<PrimedPromethiumXTnt>> PROMETHIUMX_TNT_ENTITY = ENTITY_TYPES.register("primed_promethiumx_tnt",
            () -> EntityType.Builder.<PrimedPromethiumXTnt>of(PrimedPromethiumXTnt::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .setTrackingRange(10)
                    .setUpdateInterval(10)
                    .build(new ResourceLocation(MOD_ID, "primed_promethiumx_tnt").toString()));

    /** la bomba simbiotica dell'Antivenom Bomb, col modello preso dallo Spider-Man pack */
    public static final RegistryObject<EntityType<modKlyntar.entity.custom.AntivenomBombEntity>> ANTIVENOM_BOMB_ENTITY =
            ENTITY_TYPES.register("antivenom_bomb",
                    () -> EntityType.Builder.<modKlyntar.entity.custom.AntivenomBombEntity>of(
                                    modKlyntar.entity.custom.AntivenomBombEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .setTrackingRange(64)
                            .setUpdateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("antivenom_bomb"));

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

    public static final RegistryObject<EntityType<modKlyntar.entity.custom.CarnageSymbioteEntity>> CARNAGE_SYMBIOTE_ENTITY =

            ENTITY_TYPES.register("carnage_symbiote",

                    () -> EntityType.Builder.<modKlyntar.entity.custom.CarnageSymbioteEntity>of(

                                    modKlyntar.entity.custom.CarnageSymbioteEntity::new, MobCategory.MONSTER)

                            .sized(0.9F, 0.5F)

                            .build(new ResourceLocation(MOD_ID, "carnage_symbiote").toString()));



    public static final RegistryObject<EntityType<modKlyntar.entity.custom.AntivenomSymbioteEntity>> ANTIVENOM_SYMBIOTE_ENTITY =

            ENTITY_TYPES.register("antivenom_symbiote",

                    () -> EntityType.Builder.<modKlyntar.entity.custom.AntivenomSymbioteEntity>of(

                                    modKlyntar.entity.custom.AntivenomSymbioteEntity::new, MobCategory.MONSTER)

                            .sized(0.9F, 0.5F)

                            .build(new ResourceLocation(MOD_ID, "antivenom_symbiote").toString()));



    public static final RegistryObject<EntityType<modKlyntar.entity.custom.ToxinSymbioteEntity>> TOXIN_SYMBIOTE_ENTITY =

            ENTITY_TYPES.register("toxin_symbiote",

                    () -> EntityType.Builder.<modKlyntar.entity.custom.ToxinSymbioteEntity>of(

                                    modKlyntar.entity.custom.ToxinSymbioteEntity::new, MobCategory.MONSTER)

                            .sized(0.9F, 0.5F)

                            .build(new ResourceLocation(MOD_ID, "toxin_symbiote").toString()));



    public static final RegistryObject<EntityType<modKlyntar.entity.custom.GrendelsFragmentEntity>> GRENDELS_FRAGMENT_ENTITY =

            ENTITY_TYPES.register("grendels_fragment",

                    () -> EntityType.Builder.<modKlyntar.entity.custom.GrendelsFragmentEntity>of(

                                    modKlyntar.entity.custom.GrendelsFragmentEntity::new, MobCategory.MONSTER)

                            .sized(0.9F, 0.5F)

                            .build(new ResourceLocation(MOD_ID, "grendels_fragment").toString()));



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

    public static final RegistryObject<EntityType<modKlyntar.entity.custom.ThrownCapsuleEntity>> THROWN_CAPSULE_ENTITY =

            ENTITY_TYPES.register("thrown_capsule",

                    () -> EntityType.Builder.<modKlyntar.entity.custom.ThrownCapsuleEntity>of(

                                    modKlyntar.entity.custom.ThrownCapsuleEntity::new, MobCategory.MISC)

                            .sized(0.4F, 0.4F)

                            .setTrackingRange(64)

                            .setUpdateInterval(10)

                            .setShouldReceiveVelocityUpdates(true)

                            .build(new ResourceLocation(MOD_ID, "thrown_capsule").toString()));



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
        modKlyntar.effect.ModEffects.EFFECTS.register(modEventBus);

        modKlyntar.worldgen.ModFeatures.FEATURES.register(modEventBus);
        modKlyntar.block.entity.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerAttributes);

        GeckoLib.initialize();
        ModNetwork.registerPackets();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PlayerPowerCapability.class);
        MinecraftForge.EVENT_BUS.register(PlayersPowerProvider.class);
        MinecraftForge.EVENT_BUS.register(PlayersPower.class);
        MinecraftForge.EVENT_BUS.register(PlayerEventSubscriber.class);
        MinecraftForge.EVENT_BUS.register(PromethiumXSmokeHandler.class);
    }

    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }


    private void onServerStarting(ServerStartingEvent event) {
    }

    @SubscribeEvent
    public void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SYMBIOTE_ENTITY.get(), SymbioteEntity.createAttributes().build());

        event.put(GRENDELS_FRAGMENT_ENTITY.get(), SymbioteEntity.createAttributes().build());

        event.put(CARNAGE_SYMBIOTE_ENTITY.get(), SymbioteEntity.createAttributes().build());

        event.put(ANTIVENOM_SYMBIOTE_ENTITY.get(), SymbioteEntity.createAttributes().build());

        event.put(TOXIN_SYMBIOTE_ENTITY.get(), SymbioteEntity.createAttributes().build());
        event.put(VENOM_ENTITY2.get(), VenomEntity.createAttributes().build());
        event.put(TENTACLE_SEGMENT.get(), TentacleSegmentEntity.createAttributes().build());
    }
}
