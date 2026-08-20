package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.client.ClientSymbioteMiningState;
import modKlyntar.network.ModNetwork;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il simbionte scava come la netherite.
 *
 * <p>Vale col corpo simbionte fuori e col piccone della ruota degli attrezzi in pugno: in
 * entrambi i casi la velocita' e la capacita' di raccogliere i blocchi salgono al livello
 * dell'attrezzo migliore del gioco.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteMiningHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarMining");

    /** 1 mentre il corpo simbionte e' fuori: lo scrive l'abilita' "Unleash the symbiote" */
    public static final String CORPO_OBJECTIVE = "Klyntar.VenomSize";
    /** quale attrezzo ha scelto la ruota: 0 nessuno, 1 piccone, 2 ascia */
    public static final String ATTREZZO_OBJECTIVE = "Venom.Tool";
    public static final int ATTREZZO_PICCONE = 1;

    /** quanto il simbionte va oltre la netherite: meta' piu' veloce */
    private static final float SPINTA = 1.5F;
    /** la velocita' di scavo del simbionte, presa dalla netherite e aumentata */
    private static final float VELOCITA_SIMBIONTE = Tiers.NETHERITE.getSpeed() * SPINTA;

    /** ultimo stato mandato a ciascun giocatore, per non ripetere il pacchetto ogni tick */
    private static final Map<UUID, Integer> ULTIMO_STATO = new ConcurrentHashMap<>();

    private SymbioteMiningHandler() {
    }

    /** Il corpo simbionte e' fuori? Lo chiedono anche gli altri passivi legati alla forma. */
    public static boolean corpoAttivo(Player giocatore) {
        if (giocatore.level().isClientSide) {
            return ClientSymbioteMiningState.corpoAttivo();
        }
        return SymbioteState.getScore(giocatore, CORPO_OBJECTIVE) > 0;
    }

    private static boolean picconeInPugno(Player giocatore) {
        if (giocatore.level().isClientSide) {
            return ClientSymbioteMiningState.attrezzo() == ATTREZZO_PICCONE;
        }
        return SymbioteState.getScore(giocatore, ATTREZZO_OBJECTIVE) == ATTREZZO_PICCONE;
    }

    /**
     * Il giocatore scava col simbionte?
     *
     * <p>Sul client non si puo' chiedere se porti un simbionte: quel dato vive nella capability
     * del server e non viene sincronizzato. Ci si fida invece dello stato appena arrivato, che
     * il server manda solo per chi un simbionte ce l'ha davvero.</p>
     */
    private static boolean scavaComeNetherite(Player giocatore) {
        if (giocatore.level().isClientSide) {
            return ClientSymbioteMiningState.corpoAttivo()
                    || ClientSymbioteMiningState.attrezzo() == ATTREZZO_PICCONE;
        }
        return SymbioteState.haSimbionte(giocatore)
                && (corpoAttivo(giocatore) || picconeInPugno(giocatore));
    }

    /** Tiene il client aggiornato: senza, scaverebbe al ritmo di un giocatore normale. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)) {
            return;
        }
        if (giocatore.tickCount % 5 != 0) {
            return;
        }

        // senza simbionte si manda lo stato spento, altrimenti il client resterebbe
        // convinto di scavare col simbionte anche dopo averlo perso
        boolean simbionte = SymbioteState.haSimbionte(giocatore);
        boolean corpo = simbionte && SymbioteState.getScore(giocatore, CORPO_OBJECTIVE) > 0;
        int attrezzo = simbionte ? SymbioteState.getScore(giocatore, ATTREZZO_OBJECTIVE) : 0;

        int stato = (corpo ? 1 : 0) | (attrezzo << 1);
        if (!Integer.valueOf(stato).equals(ULTIMO_STATO.get(giocatore.getUUID()))) {
            ULTIMO_STATO.put(giocatore.getUUID(), stato);
            ModNetwork.syncSymbioteMining(giocatore, corpo, attrezzo);
            LOGGER.info("Scavo simbionte per {}: corpo={}, attrezzo={}",
                    giocatore.getGameProfile().getName(), corpo, attrezzo);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!scavaComeNetherite(event.getEntity())) {
            return;
        }
        // il minimo, non un moltiplicatore: chi ha gia' in mano di meglio non viene rallentato
        event.setNewSpeed(Math.max(event.getOriginalSpeed(), VELOCITA_SIMBIONTE));
    }

    /** Senza questo la velocita' non servirebbe a nulla: i blocchi duri cadrebbero a vuoto. */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (scavaComeNetherite(event.getEntity())) {
            event.setCanHarvest(true);
        }
    }
}
