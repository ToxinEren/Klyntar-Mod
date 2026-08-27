package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Il ragno divorato da Venom con la Regeneration.
 *
 * <p>L'abilita' Regeneration ({@code assimilate}) afferra il bersaglio piu' vicino, gli mette il
 * tag {@code venom_feed_target} e alla fine lo uccide con un comando. Il colpo mortale arriva
 * quindi senza nessuna entita' attaccante: l'unico modo per riconoscerlo sono i tag, non la
 * sorgente del danno.</p>
 *
 * <p>Quando la vittima e' un ragno, il simbionte gli ruba l'arrampicata: {@code
 * Klyntar.SpiderSlain} passa a 1 e il giocatore diventa {@code venomspidey}, cioe' Venom piu' il
 * climb, col vecchio simbolo come icona. E' un traguardo, non torna indietro, e vale solo per
 * chi indossa venom base.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SpiderSlainHandler {

    /** Il traguardo raggiunto. */
    public static final String OBIETTIVO = "Klyntar.SpiderSlain";

    /** Il tag che l'abilita' Regeneration mette su chi sta divorando. */
    private static final String TAG_DIVORATORE = "venom_feeding";
    /** Il tag che l'abilita' Regeneration mette sulla preda. */
    private static final String TAG_PREDA = "venom_feed_target";

    /** La forma di partenza. */
    private static final String FORMA = "venom";
    /** Quello che il giocatore diventa dopo il ragno. */
    private static final String FORMA_RAGNO = "venomspidey";

    /** Il feed lavora entro 16 blocchi, come i suoi comandi. */
    private static final double RAGGIO = 16.0;

    private static final Logger LOGGER = LogManager.getLogger("KlyntarSpider");

    private SpiderSlainHandler() {
    }

    /** Vero se il traguardo e' gia' stato raggiunto. */
    public static boolean ragnoUcciso(ServerPlayer giocatore) {
        return SymbioteState.getScore(giocatore, OBIETTIVO) >= 1;
    }

    private static boolean eRagno(LivingEntity vittima) {
        EntityType<?> tipo = vittima.getType();
        return tipo == EntityType.SPIDER || tipo == EntityType.CAVE_SPIDER;
    }

    /** Chi sta divorando questa preda: il piu' vicino che porta il tag e indossa venom. */
    private static ServerPlayer divoratore(LivingEntity preda) {
        List<ServerPlayer> candidati = preda.level().getEntitiesOfClass(
                ServerPlayer.class,
                preda.getBoundingBox().inflate(RAGGIO),
                giocatore -> giocatore.getTags().contains(TAG_DIVORATORE)
                        && FORMA.equals(SymbioteState.forma(giocatore)));
        ServerPlayer vicino = null;
        double minima = Double.MAX_VALUE;
        for (ServerPlayer giocatore : candidati) {
            double d = giocatore.distanceToSqr(preda);
            if (d < minima) {
                minima = d;
                vicino = giocatore;
            }
        }
        return vicino;
    }

    @SubscribeEvent
    public static void onMorte(LivingDeathEvent event) {
        LivingEntity preda = event.getEntity();
        if (preda.level().isClientSide()) {
            return;
        }

        // il colpo mortale del feed arriva da un comando, quindi la sorgente non ha attaccante:
        // e' il tag della preda a dirci che e' stata divorata e non uccisa in altro modo
        if (!eRagno(preda) || !preda.getTags().contains(TAG_PREDA)) {
            return;
        }

        ServerPlayer giocatore = divoratore(preda);
        if (giocatore == null) {
            return;
        }

        // il traguardo e' solo la memoria dell'impresa, non la guardia: a decidere se c'e'
        // qualcosa da fare e' la forma indossata, gia' filtrata in divoratore(). Usare il
        // punteggio come guardia bloccava tutto per sempre se restava a 1 su un giocatore
        // ancora in venom, per esempio dopo averlo impostato a mano
        SymbioteState.assicuraObiettivo(giocatore, OBIETTIVO);
        SymbioteState.setScore(giocatore, OBIETTIVO, 1);

        PlayerPowerCapability.infectPlayer(giocatore, FORMA_RAGNO);
        LOGGER.info("{} ha divorato un ragno: passa a {}",
                giocatore.getGameProfile().getName(), FORMA_RAGNO);
    }
}
