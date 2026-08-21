package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * La campana come sorgente di sonic damage, sul modello del "vibrant" del pack Spider-Man.
 *
 * <p>Suonarne una scuote ogni simbionte nel raggio: i portatori si prendono i malus e un colpo
 * di sonic damage, che al terzo strappa via il simbionte. Il pack la chiama vibrazione; da noi
 * finisce nello stesso contatore del grido del Warden.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SonicBellHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarSonic");

    /** entro quanti blocchi la vibrazione arriva: come nel pack */
    private static final double RAGGIO = 12.0D;
    /** oltre questa sintonia col simbionte la vibrazione non fa piu' presa */
    private static final int AFFINITA_IMMUNE = 80;

    // durate e livelli ricalcati sullo script del pack
    private static final MobEffectInstance[] MALUS = {
            new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 3, false, false),
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 2, false, false),
            new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0, false, false),
            new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 15 * 20, 2, false, false)
    };

    private SonicBellHandler() {
    }

    /** Campana piazzata e cliccata. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).is(Blocks.BELL)) {
            suona(event.getEntity());
        }
    }

    /** Campana tenuta in mano e usata. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().is(Items.BELL)) {
            suona(event.getEntity());
        }
    }

    private static void suona(Player suonatore) {
        if (!(suonatore.level() instanceof ServerLevel livello)) {
            return;
        }

        AABB zona = suonatore.getBoundingBox().inflate(RAGGIO);
        List<LivingEntity> intorno = livello.getEntitiesOfClass(LivingEntity.class, zona,
                e -> e.isAlive() && e.distanceToSqr(suonatore) <= RAGGIO * RAGGIO);

        int colpiti = 0;
        for (LivingEntity bersaglio : intorno) {
            if (scuoti(livello, bersaglio)) {
                colpiti++;
            }
        }
        if (colpiti > 0) {
            LOGGER.info("Campana suonata da {}: {} simbionti scossi",
                    suonatore.getGameProfile().getName(), colpiti);
        }
    }

    /**
     * @return true se il bersaglio era un simbionte e la vibrazione lo ha preso
     */
    private static boolean scuoti(ServerLevel livello, LivingEntity bersaglio) {
        boolean portatore = SymbioteState.haSimbionte(bersaglio);
        if (!portatore && !(bersaglio instanceof SymbioteEntity)) {
            return false;
        }
        // Anti-Venom e' immune a ogni debolezza, vibrazione della campana compresa
        if (SymbioteState.isAntiVenom(bersaglio)) {
            return false;
        }
        // chi e' in piena sintonia col simbionte regge la vibrazione senza scomporsi
        if (bersaglio instanceof Player giocatore && SymbioteState.affinita(giocatore) >= AFFINITA_IMMUNE) {
            return false;
        }

        for (MobEffectInstance malus : MALUS) {
            bersaglio.addEffect(new MobEffectInstance(malus));
        }
        particelle(livello, bersaglio);

        if (portatore && bersaglio instanceof ServerPlayer giocatore) {
            VenomSymbioteSystemsHandler.applySonicWeakness(giocatore);
        }
        return true;
    }

    private static void particelle(ServerLevel livello, LivingEntity bersaglio) {
        SimpleParticleType particella = SymbioteState.isAntiVenom(bersaglio)
                ? MyMod.ANTIVENOM_PARTICLE.get()
                : MyMod.VENOM_PARTICLE.get();
        livello.sendParticles(particella,
                bersaglio.getX(), bersaglio.getY() + 1.0D, bersaglio.getZ(),
                8, 0.3D, 0.5D, 0.3D, 0.0D);
    }
}
