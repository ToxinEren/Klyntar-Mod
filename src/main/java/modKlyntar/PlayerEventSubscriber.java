package modKlyntar;

import modKlyntar.power.PlayersPowerProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public class PlayerEventSubscriber {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MyMod.MOD_ID, "properties"), new PlayersPowerProvider());
        }
    }
        
        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
        	if(event.isWasDeath()) {
        		event.getOriginal().getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(oldStore -> {
        			event.getOriginal().getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(newStore -> {
        				newStore.copyFrom(oldStore);
        			});
        		});
        	}
        	
            if (!event.isWasDeath()) {
                event.getOriginal().getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                    event.getEntity().getEntityData().set(MyMod.PLAYER_SCALE, 1.0f);
                });
            }
    }
        @SubscribeEvent
        public static void onLivingFall(LivingFallEvent event) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                player.getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                    if (power.isImmuneToFallDamage()) {
                        event.setCanceled(true);
                    }
                });
            }
        }
        
        @SubscribeEvent
        public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                player.getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                    if (power.getSymbioteLevel() == 1) {
                        // Visione notturna
                        if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
                            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
                        }
                        // Arrampicarsi come un ragno
                        if (player.horizontalCollision && power.canClimbWalls()) {
                            player.setDeltaMovement(player.getDeltaMovement().x, 0.2D, player.getDeltaMovement().z);
                        }
                    }
                });
            }
        }
}