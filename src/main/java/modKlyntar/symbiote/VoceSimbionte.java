package modKlyntar.symbiote;

import modKlyntar.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La voce del simbionte: quello che dice al suo ospite, e solo a lui.
 *
 * <p>Il server decide quando parlare e di cosa (la situazione); il client sceglie la battuta,
 * a caso fra quelle che il file di lingua ha per quella situazione
 * ({@code klyntars.voice.<situazione>.<n>}), e la scrive in un riquadro sullo schermo. Aggiungere
 * varianti vuol dire aggiungere righe al file di lingua, senza toccare il codice.</p>
 *
 * <p>Il simbionte non deve parlare sopra se stesso: fra due battute qualsiasi passano almeno
 * {@link #PAUSA_FRA_BATTUTE} tick, e la stessa situazione non si ripete prima della sua
 * ricarica. Una battuta che arriva troppo presto si perde, non si mette in coda: una frase
 * detta cinque secondi dopo il fatto suona fuori posto. Le urgenti - lo strappo, l'ultimo colpo
 * prima dello strappo, il primo legame - passano comunque.</p>
 *
 * <p>Come parla dipende da quanto si fida, cioe' dall'affinita', a gradini allineati coi livelli
 * del legame: con Bond 1 e' {@link #DIFFIDENTE} - cattivo, sprezzante, e ogni tanto si guarda
 * intorno in cerca di un ospite piu' forte; con Bond 2 e' {@link #ALLEATO}, la voce di sempre;
 * con Bond 3 e' {@link #AMICO}. Il client cerca prima le battute del suo gradino
 * ({@code <situazione>.hostile.<n>}, {@code <situazione>.friend.<n>}) e, se la situazione non ne
 * ha, ripiega su quelle comuni.</p>
 */
public final class VoceSimbionte {

    /** Il tono della battuta: decide il colore del riquadro e il suono che l'accompagna. */
    public enum Tono {
        NEUTRO, LEGAME_SU, LEGAME_GIU, AGGRESSIVO, AVVISO
    }

    /** I gradini della fiducia. */
    public static final int DIFFIDENTE = 0;
    public static final int ALLEATO = 1;
    public static final int AMICO = 2;
    private static final int SOGLIA_ALLEATO = 50;
    private static final int SOGLIA_AMICO = 100;

    /** Un secondo e mezzo fra due battute qualsiasi. */
    private static final long PAUSA_FRA_BATTUTE = 30L;
    /** Quanto aspetta una situazione prima di poter tornare, se non ha una ricarica sua. */
    private static final long RICARICA_NORMALE = 100L;
    /** Le situazioni che capitano spesso aspettano di piu', o il simbionte non starebbe mai zitto. */
    private static final Map<String, Long> RICARICHE = Map.of(
            "pasto", 20L * 30L,
            "salute_bassa", 20L * 30L,
            "uccisione", 20L * 20L,
            "colpo_sonoro", 20L * 3L,
            "berserk_durante", 20L * 8L);

    private static final Map<UUID, Long> ULTIMA_BATTUTA = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> ULTIMA_PER_SITUAZIONE = new ConcurrentHashMap<>();

    private VoceSimbionte() {
    }

    /** Il simbionte dice qualcosa sulla situazione, se non ha appena parlato. */
    public static boolean di(ServerPlayer ospite, String situazione, Tono tono) {
        return di(ospite, situazione, tono, false);
    }

    /** Come sopra; se urgente passa anche sopra la pausa fra le battute. */
    public static boolean di(ServerPlayer ospite, String situazione, Tono tono, boolean urgente) {
        return di(ospite, situazione, tono, urgente, RICARICHE.getOrDefault(situazione, RICARICA_NORMALE));
    }

    /**
     * Con una ricarica decisa da chi chiama: la curiosita' la usa lunga, perche' lo stesso
     * commento su una campana ogni cinque secondi diventerebbe una nenia.
     *
     * @return se la battuta e' partita davvero
     */
    public static boolean di(ServerPlayer ospite, String situazione, Tono tono, boolean urgente, long ricarica) {
        long adesso = ospite.level().getGameTime();
        UUID id = ospite.getUUID();
        Map<String, Long> perSituazione = ULTIMA_PER_SITUAZIONE.computeIfAbsent(id, k -> new HashMap<>());
        Long ultimaQuesta = perSituazione.get(situazione);
        if (ultimaQuesta != null && adesso - ultimaQuesta < ricarica) {
            return false;
        }
        Long ultimaQualsiasi = ULTIMA_BATTUTA.get(id);
        if (!urgente && ultimaQualsiasi != null && adesso - ultimaQualsiasi < PAUSA_FRA_BATTUTE) {
            return false;
        }
        perSituazione.put(situazione, adesso);
        ULTIMA_BATTUTA.put(id, adesso);
        int fiducia = fiducia(ospite);
        // chi non si fida non si illumina d'affetto: il riquadro azzurro del legame resta grigio
        if (fiducia == DIFFIDENTE && tono == Tono.LEGAME_SU) {
            tono = Tono.NEUTRO;
        }
        ModNetwork.mandaVoce(ospite, situazione, tono.ordinal(), fiducia);
        return true;
    }

    /** Quanto il simbionte si fida dell'ospite, dall'affinita': DIFFIDENTE, ALLEATO o AMICO. */
    public static int fiducia(Player ospite) {
        int affinita = SymbioteState.affinita(ospite);
        return affinita >= SOGLIA_AMICO ? AMICO : affinita >= SOGLIA_ALLEATO ? ALLEATO : DIFFIDENTE;
    }

    /** Chi esce non lascia le sue ricariche nelle mappe. */
    public static void dimentica(UUID id) {
        ULTIMA_BATTUTA.remove(id);
        ULTIMA_PER_SITUAZIONE.remove(id);
    }
}
