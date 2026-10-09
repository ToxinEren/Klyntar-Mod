package modKlyntar.symbiote;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chi sono i simbionti della mod, secondo il documento di design della progressione
 * ("Klyntars - progressione simbionti e All-Black", 5/10/2026).
 *
 * <p>Ogni simbionte ha un tier, un temperamento (decide come reagisce a un secondo simbionte
 * nello stesso corpo), una forza base (pesa nel conflitto e nella doppia unione), un'origine,
 * eventualmente un padre (Carnage nasce da Venom, Toxin da Carnage) e i tratti che possiede di
 * natura e che passa a chi lo digerisce.</p>
 *
 * <p><b>Segnalibri.</b> I simbionti non ancora aggiunti alla mod sono gia' qui, con
 * {@code disponibile = false}: i sistemi (conflitto, digestione, nascita, doppia unione) li
 * conoscono ma non li fanno mai comparire. Per aggiungerne uno: il potere Palladium con la skill
 * nuovo-simbionte, poi qui {@code disponibile = true}, e i tratti nativi se ne ha di nuovi.</p>
 */
public final class RegistroSimbionti {

    /** Come reagisce un simbionte quando un altro entra nel suo corpo. */
    public enum Temperamento {
        /** Venom: difende l'ospite, combatte solo se minacciato. */
        PROTETTIVO,
        /** Riot: attacca e cerca di assorbire. */
        DOMINANTE,
        /** Carnage: attacca sempre, vuole assorbire. */
        PSICOPATICO,
        /** Toxin: combatte se l'altro e' piu' debole, altrimenti si adatta. */
        CACCIATORE,
        /** Anti-Venom: tenta di espellere l'altro simbionte. */
        PURIFICATORE,
        /** All-Black: domina sempre, nessuna scelta per gli altri. */
        SOVRANO,
        /** I simbionti della Life Foundation: deboli, fuggono o si fanno inglobare. */
        TIMIDO;

        /** Attacca per primo, senza aspettare di essere minacciato. */
        public boolean attacca() {
            return this == DOMINANTE || this == PSICOPATICO || this == SOVRANO;
        }

        /** Chi vince, assorbe: questi lasciano scappare il perdente di rado. */
        public boolean vuoleAssorbire() {
            return this == DOMINANTE || this == PSICOPATICO || this == SOVRANO || this == CACCIATORE;
        }

        /**
         * Chi non lascia mai scappare nessuno (Riot, e All-Black che domina sempre): quando vince
         * assorbe sempre, chi cede il corpo a lui viene inghiottito invece di andarsene, chi e'
         * nel suo mirino non riesce a fuggirgli, e lui stesso non molla la preda.
         */
        public boolean nonLasciaScappare() {
            return this == DOMINANTE || this == SOVRANO;
        }
    }

    /** Da dove arriva: ognuno ha la sua sorgente, cosi' il giocatore capisce in che tier e'. */
    public enum Origine {
        /** cade col meteorite e esce rompendolo */
        METEORITE,
        /** nasce da un giocatore che indossa il padre */
        NASCITA,
        /** si ottiene vincendo un boss */
        BOSS,
        /** sta in un cratere, una volta sola per mondo */
        CRATERE,
        /** forma evoluta di un altro (Venomspidey da Venom) */
        EVOLUZIONE,
        /** ancora da decidere (Anti-Venom) */
        DA_DECIDERE
    }

    /**
     * Le abilita' che un simbionte puo' possedere e passare a chi lo digerisce.
     *
     * <p>Ogni tratto ha il suo objective, che vale 1 finche' il simbionte indossato lo possiede
     * (di natura o perche' l'ha digerito): e' cosi' che i JSON dei poteri le accendono.</p>
     */
    public enum Tratto {
        /** Le armi create dal corpo, la ruota frusta/ascia/mazza. Di Riot. */
        ARMI_DAL_CORPO("Klyntar.BodyWeapons"),
        /** Lo scudo col tasto destro. Di Venom: Riot lo ottiene digerendolo. */
        SCUDO("Klyntar.Trait.Shield"),
        /** Resistenza parziale al suono: un colpo sonoro in piu' prima dello strappo, malus piu' corti. Di Scream. */
        RESISTENZA_SONORA("Klyntar.Trait.SonicResistance"),
        /** SEGNALIBRO: i tentacoli assassini di Carnage. */
        TENTACOLI_ASSASSINI("Klyntar.Trait.KillerTentacles"),
        /** SEGNALIBRO: le lame di Phage. */
        LAME("Klyntar.Trait.Blades"),
        /** SEGNALIBRO: le fruste di Lasher. */
        FRUSTE("Klyntar.Trait.Whips"),
        /** SEGNALIBRO: la corrosione di Agony. */
        CORROSIONE("Klyntar.Trait.Corrosion"),
        /** SEGNALIBRO: l'urlo sonoro di Scream. */
        URLO("Klyntar.Trait.Scream"),
        /** SEGNALIBRO: la purificazione di Anti-Venom. */
        PURIFICAZIONE("Klyntar.Trait.Purification"),
        /** SEGNALIBRO: la forma Drago di Grendel. */
        FORMA_DRAGO("Klyntar.Trait.DragonForm");

        public final String obiettivo;

        Tratto(String obiettivo) {
            this.obiettivo = obiettivo;
        }
    }

    /**
     * Un simbionte.
     *
     * @param famiglia    il simbionte "vero": Venomspidey e' Venom evoluto, e ne condivide profilo e tratti
     * @param quotaEredita che parte delle abilita' del padre passa al figlio che nasce da lui
     */
    public record Simbionte(String id, String famiglia, String nome, int tier, Temperamento temperamento,
                            int forzaBase, Origine origine, String padre, Set<Tratto> nativi,
                            boolean disponibile, boolean haMob, double quotaEredita) {
    }

    private static final Map<String, Simbionte> TUTTI = new LinkedHashMap<>();

    static {
        // --- tier 1-2: ci sono
        registra("venom", "venom", "Venom", 1, Temperamento.PROTETTIVO, 10, Origine.METEORITE, null,
                Set.of(Tratto.SCUDO), true, true, 0.5D);
        registra("venomspidey", "venom", "Venom", 1, Temperamento.PROTETTIVO, 10, Origine.EVOLUZIONE, null,
                Set.of(Tratto.SCUDO), true, false, 0.5D);
        registra("riot", "riot", "Riot", 2, Temperamento.DOMINANTE, 15, Origine.METEORITE, null,
                Set.of(Tratto.ARMI_DAL_CORPO), true, true, 0.5D);
        // --- SEGNALIBRO: tier 3-4, nascono da un giocatore che indossa il padre
        registra("carnage", "carnage", "Carnage", 3, Temperamento.PSICOPATICO, 25, Origine.NASCITA, "venom",
                Set.of(Tratto.ARMI_DAL_CORPO, Tratto.TENTACOLI_ASSASSINI), false, true, 0.5D);
        // Toxin non entra nel confronto con All-Black: la forza serve solo nei conflitti con gli altri
        registra("toxin", "toxin", "Toxin", 4, Temperamento.CACCIATORE, 40, Origine.NASCITA, "carnage",
                Set.of(), false, true, 0.75D);
        // --- SEGNALIBRO: tier 5-6, il ponte verso l'endgame
        registra("antivenom", "antivenom", "Anti-Venom", 5, Temperamento.PURIFICATORE, 30, Origine.DA_DECIDERE, null,
                Set.of(Tratto.PURIFICAZIONE), false, true, 0.5D);
        registra("grendel", "grendel", "Grendel", 6, Temperamento.DOMINANTE, 35, Origine.BOSS, null,
                Set.of(Tratto.FORMA_DRAGO), false, false, 0.5D);
        // --- SEGNALIBRO: la Life Foundation, rara dal meteorite di Riot, buona come cibo per i padri
        registra("agony", "agony", "Agony", 1, Temperamento.TIMIDO, 10, Origine.METEORITE, null,
                Set.of(Tratto.CORROSIONE), false, true, 0.5D);
        registra("phage", "phage", "Phage", 1, Temperamento.TIMIDO, 10, Origine.METEORITE, null,
                Set.of(Tratto.LAME), false, true, 0.5D);
        registra("lasher", "lasher", "Lasher", 1, Temperamento.TIMIDO, 10, Origine.METEORITE, null,
                Set.of(Tratto.FRUSTE), false, true, 0.5D);
        registra("scream", "scream", "Scream", 1, Temperamento.TIMIDO, 10, Origine.METEORITE, null,
                Set.of(Tratto.URLO, Tratto.RESISTENZA_SONORA), false, true, 0.5D);
        // --- SEGNALIBRO: tier 7, l'arma endgame, nel cratere
        registra("allblack", "allblack", "All-Black", 7, Temperamento.SOVRANO, 30, Origine.CRATERE, null,
                Set.of(), false, false, 0.5D);
    }

    private RegistroSimbionti() {
    }

    private static void registra(String id, String famiglia, String nome, int tier, Temperamento temperamento,
                                 int forzaBase, Origine origine, String padre, Set<Tratto> nativi,
                                 boolean disponibile, boolean haMob, double quotaEredita) {
        Set<Tratto> tratti = nativi.isEmpty() ? EnumSet.noneOf(Tratto.class) : EnumSet.copyOf(nativi);
        TUTTI.put(id, new Simbionte(id, famiglia, nome, tier, temperamento, forzaBase, origine, padre,
                Collections.unmodifiableSet(tratti), disponibile, haMob, quotaEredita));
    }

    /** Il simbionte con questo id (una forma), o null. */
    public static Simbionte di(String id) {
        return id == null ? null : TUTTI.get(id);
    }

    public static Collection<Simbionte> tutti() {
        return Collections.unmodifiableCollection(TUTTI.values());
    }

    /** La famiglia di una forma: Venomspidey e' Venom. Una forma sconosciuta e' la famiglia di se stessa. */
    public static String famiglia(String id) {
        Simbionte s = di(id);
        return s == null ? id : s.famiglia();
    }

    public static boolean disponibile(String id) {
        Simbionte s = di(id);
        return s != null && s.disponibile();
    }

    /** Le forme che un mob simbionte puo' avere oggi: quelle disponibili che esistono come mob. */
    public static boolean formaDelMob(String id) {
        Simbionte s = di(id);
        return s != null && s.disponibile() && s.haMob();
    }

    /**
     * Il colore di ogni famiglia, per le particelle dei filamenti quando due simbionti si
     * contendono un corpo: si deve vedere chi frusta chi. Quelli non ancora nella mod ce l'hanno
     * gia', cosi' il giorno che arrivano lottano col loro colore.
     */
    private static final Map<String, Integer> COLORI = Map.ofEntries(
            Map.entry("venom", 0x15151C),
            Map.entry("riot", 0x6E747C),
            Map.entry("carnage", 0xA81414),
            Map.entry("toxin", 0x2A4FB0),
            Map.entry("antivenom", 0xE6E6EA),
            Map.entry("grendel", 0x2B1E14),
            Map.entry("agony", 0x7B2A9E),
            Map.entry("phage", 0xC9B21A),
            Map.entry("lasher", 0x2F8A3A),
            Map.entry("scream", 0xD9A520),
            Map.entry("allblack", 0x050507));

    /** Il colore (RGB) della famiglia di questa forma; nero se non lo conosce. */
    public static int colore(String id) {
        return COLORI.getOrDefault(famiglia(id), 0x15151C);
    }

    /** Il figlio che nasce da un padre, se il documento ne prevede uno (Carnage da Venom, Toxin da Carnage). */
    public static Optional<Simbionte> figlioDi(String famigliaPadre) {
        return TUTTI.values().stream()
                .filter(s -> s.origine() == Origine.NASCITA && famigliaPadre.equals(s.padre()))
                .findFirst();
    }

    /** La probabilita' che il meteorite dia un simbionte della Life Foundation al posto di Riot. */
    private static final float QUOTA_LIFE_FOUNDATION = 0.1F;

    /**
     * Che simbionte esce rompendo un meteorite: Venom e Riot meta' e meta'; quando esce Riot, con
     * una piccola probabilita' al suo posto un simbionte della Life Foundation. Finche' la Life
     * Foundation e' un segnalibro, quella probabilita' resta Riot.
     */
    public static String dalMeteorite(RandomSource caso) {
        if (caso.nextBoolean()) {
            return "venom";
        }
        if (caso.nextFloat() < QUOTA_LIFE_FOUNDATION) {
            List<Simbionte> lifeFoundation = new ArrayList<>();
            for (Simbionte s : TUTTI.values()) {
                if (s.temperamento() == Temperamento.TIMIDO && s.origine() == Origine.METEORITE && s.disponibile()) {
                    lifeFoundation.add(s);
                }
            }
            if (!lifeFoundation.isEmpty()) {
                return lifeFoundation.get(caso.nextInt(lifeFoundation.size())).id();
            }
        }
        return "riot";
    }
}
