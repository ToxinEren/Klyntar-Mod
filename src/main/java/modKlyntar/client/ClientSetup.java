package modKlyntar.client;

import modKlyntar.MyMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import modKlyntar.client.renderer.CustomArrowRenderer;
import modKlyntar.client.renderer.GhastProjectileRenderer;
import modKlyntar.client.renderer.PrimedPromethiumXTntRenderer;
import modKlyntar.client.renderer.SmokeTrailRenderer;
import modKlyntar.client.renderer.SymbioteRenderer;
import modKlyntar.client.renderer.TentacleSegmentRenderer;
import modKlyntar.client.renderer.VenomPlayerRenderer;
import modKlyntar.client.renderer.VenomRenderer;
import modKlyntar.client.renderer.WebProjectileRenderer;
import modKlyntar.entity.client.model.Symbiote;
import modKlyntar.entity.client.model.TentacleSegmentModel;
import modKlyntar.entity.custom.SymbioteEntity;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
	public static final ModelLayerLocation TENTACLE_LAYER = new ModelLayerLocation(new ResourceLocation(MyMod.MOD_ID, "tentacle"), "main");
	public static final ModelLayerLocation TENTACLE_SEGMENT_LAYER = new ModelLayerLocation(new ResourceLocation(MyMod.MOD_ID, "tentacle_segment"), "main");
	@SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TENTACLE_SEGMENT_LAYER, TentacleSegmentModel::createLayer);
    }
	
	@SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    	EntityRenderers.register(MyMod.SYMBIOTE_ENTITY.get(), SymbioteRenderer::new);
    	EntityRenderers.register(MyMod.VENOM_ENTITY2.get(), VenomRenderer::new);
    	EntityRenderers.register(MyMod.SMOKE_TRAIL_ENTITY.get(), SmokeTrailRenderer::new);
    	EntityRenderers.register(MyMod.GHAST_PROJECTILE_ENTITY.get(), GhastProjectileRenderer::new);
    	EntityRenderers.register(MyMod.ANTIVENOM_BOMB_ENTITY.get(), modKlyntar.client.renderer.AntivenomBombRenderer::new);
    	EntityRenderers.register(MyMod.PROMETHIUMX_TNT_ENTITY.get(), PrimedPromethiumXTntRenderer::new);
    	EntityRenderers.register(MyMod.CUSTOM_ARROW.get(), CustomArrowRenderer::new);
    	EntityRenderers.register(MyMod.TENTACLE_SEGMENT.get(), TentacleSegmentRenderer::new);
    	EntityRenderers.register(MyMod.WEB_PROJECTILE_ENTITY.get(), WebProjectileRenderer::new);
    	//EntityRenderers.register(MyMod.VENOM_PLAYER_ENTITY.get(), VenomPlayerRenderer::new);
    }
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    	event.registerEntityRenderer(MyMod.WEB_PROJECTILE_ENTITY.get(), WebProjectileRenderer::new);
        event.registerEntityRenderer(MyMod.SYMBIOTE_ENTITY.get(), SymbioteRenderer::new);
        event.registerEntityRenderer(MyMod.VENOM_ENTITY2.get(), VenomRenderer::new);
        event.registerEntityRenderer(MyMod.SMOKE_TRAIL_ENTITY.get(), SmokeTrailRenderer::new);
        event.registerEntityRenderer(MyMod.GHAST_PROJECTILE_ENTITY.get(), GhastProjectileRenderer::new);
        event.registerEntityRenderer(MyMod.ANTIVENOM_BOMB_ENTITY.get(), modKlyntar.client.renderer.AntivenomBombRenderer::new);
        event.registerEntityRenderer(MyMod.PROMETHIUMX_TNT_ENTITY.get(), PrimedPromethiumXTntRenderer::new);
        event.registerEntityRenderer(MyMod.CUSTOM_ARROW.get(), CustomArrowRenderer::new);
        event.registerEntityRenderer(MyMod.TENTACLE_SEGMENT.get(), TentacleSegmentRenderer::new);
        //event.registerEntityRenderer(MyMod.VENOM_PLAYER_ENTITY.get(), VenomPlayerRenderer::new);
    }


    @SubscribeEvent
    public static void onRegisterParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MyMod.VENOM_PARTICLE.get(),
                modKlyntar.client.particle.VenomSymbioteParticle.Provider::new);
        event.registerSpriteSet(MyMod.ANTIVENOM_PARTICLE.get(),
                modKlyntar.client.particle.VenomSymbioteParticle.Provider::new);
        event.registerSpriteSet(MyMod.SYMBIOTE_SPLASH.get(),
                modKlyntar.client.particle.SymbioteSplashParticle.Provider::new);
    }

    /** i modelli piatti usati nell'inventario non sono referenziati da nessun item: vanno registrati a mano */
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // il renderer disegna l'icona dell'inventario con un modello a parte, che va
        // registrato qui: senza, non lo trova e l'icona statica sparisce
        for (String capsula : new String[]{"capsule", "venomcapsule", "antivenomcapsule",
                "carnagecapsule", "toxincapsule"}) {
            event.register(new ModelResourceLocation(MyMod.MOD_ID, capsula + "_gui", "inventory"));
        }
    }
}