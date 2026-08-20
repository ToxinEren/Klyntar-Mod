package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Carnage non aspetta gli ordini.
 *
 * <p>Ogni venti secondi, se attorno c'e' qualcosa da colpire, il simbionte fa partire da solo
 * lo slam. Tace se un'altra abilita' e' gia' in scena e se la fame e' sopra la soglia: piu' il
 * giocatore e' in sintonia col simbionte, piu' quella soglia si abbassa e meno il simbionte
 * prende l'iniziativa da solo.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class CarnageInstinctHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarCarnage");

    /** ogni quanti tick il simbionte riprova: venti secondi */
    private static final int INTERVALLO = 20 * 20;
    /** entro quanti blocchi deve esserci un mob perche' valga la pena */
    private static final double RAGGIO = 8.0D;
    /** soglia di fame a affinita' zero: sopra questa il simbionte e' sazio e sta buono */
    private static final int SOGLIA_BASE = 90;
    /** di quanto la piena affinita' abbassa la soglia */
    private static final int SCONTO_AFFINITA = 40;

    private static final String ATTESA = "Klyntar.Carnage.SlamTick";

    private CarnageInstinctHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer giocatore)) {
            return;
        }
        if (!"carnage".equals(SymbioteState.forma(giocatore))) {
            SymbioteState.setScore(giocatore, ATTESA, 0);
            return;
        }

        int attesa = SymbioteState.getScore(giocatore, ATTESA);
        if (attesa > 0) {
            SymbioteState.setScore(giocatore, ATTESA, attesa - 1);
            return;
        }

        SymbioteState.setScore(giocatore, ATTESA, INTERVALLO);
        if (puoScatenarsi(giocatore)) {
            // stessa richiesta che scrive l'abilita' nella barra: la sequenza la esegue
            // VenomSymbiotePowersHandler, che solleva i bersagli e li schianta a terra
            SymbioteState.setScore(giocatore, VenomSymbiotePowersHandler.PULL_REQUEST, 1);
            LOGGER.info("Carnage ha lanciato lo slam da solo per {}", giocatore.getGameProfile().getName());
        }
    }

    private static boolean puoScatenarsi(ServerPlayer giocatore) {
        if (VenomSymbioteSystemsHandler.isPlayerVulnerable(giocatore)) {
            return false;
        }
        // un'altra abilita' e' gia' in scena: il simbionte non la interrompe
        if (SymbioteState.getScore(giocatore, VenomSymbiotePowersHandler.ANIM_PLAYING) > 0) {
            return false;
        }
        if (VenomSymbioteSystemsHandler.getHunger(giocatore) > sogliaFame(giocatore)) {
            return false;
        }
        return !mobVicini(giocatore).isEmpty();
    }

    /** Piu' affinita' c'e', piu' bassa la fame a cui il simbionte si muove per conto suo. */
    private static int sogliaFame(ServerPlayer giocatore) {
        return SOGLIA_BASE - SCONTO_AFFINITA * SymbioteState.affinita(giocatore) / 100;
    }

    private static List<Mob> mobVicini(ServerPlayer giocatore) {
        AABB zona = giocatore.getBoundingBox().inflate(RAGGIO);
        return giocatore.level().getEntitiesOfClass(Mob.class, zona,
                mob -> mob.isAlive() && mob.distanceToSqr(giocatore) <= RAGGIO * RAGGIO);
    }
}
