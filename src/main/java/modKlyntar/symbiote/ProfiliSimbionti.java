package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.symbiote.RegistroSimbionti.Simbionte;
import modKlyntar.symbiote.RegistroSimbionti.Tratto;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Quello che ogni giocatore ha costruito con ciascun simbionte, oltre al bond.
 *
 * <p>Per ogni simbionte (per famiglia: Venomspidey e' Venom) si tengono tre cose:</p>
 * <ul>
 *   <li><b>inglobati</b> - i simbionti che ha inglobato da quando e' nato l'ultimo figlio: a due
 *   e' pronto a generare (Carnage da Venom, Toxin da Carnage);</li>
 *   <li><b>digeriti</b> - tutti i simbionti che ha digerito: ognuno vale +5 di forza nel
 *   conflitto e nella resistenza ad All-Black;</li>
 *   <li><b>tratti acquisiti</b> - le abilita' prese digerendo gli altri. Sono permanenti, non hanno
 *   limite e non si sommano: chi le ha gia' non guadagna niente.</li>
 * </ul>
 *
 * <p>Il bond resta dov'era, nell'objective {@code Klyntar.Affinity.<forma>}: un simbionte
 * inglobato o scappato conserva il suo.</p>
 *
 * <p>Tutto sta nella parte dei dati del giocatore che Forge copia alla morte e al cambio di
 * dimensione ({@link Player#PERSISTED_NBT_TAG}): i profili non si perdono morendo.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class ProfiliSimbionti {
    private static final String RADICE = "Klyntar.Simbionti";
    private static final String INGLOBATI = "Inglobati";
    private static final String DIGERITI = "Digeriti";
    private static final String TRATTI = "Tratti";
    /** A quanti inglobati il padre e' pronto a generare. */
    public static final int INGLOBATI_PER_NASCERE = 2;
    private static final int FORZA_PER_DIGERITO = 5;
    private static final int FORZA_PER_LIVELLO_BOND = 5;
    private static final int LIVELLI_BOND_MASSIMI = 3;
    private static final int OGNI = 20;

    private ProfiliSimbionti() {
    }

    // ------------------------------------------------------------------ lettura e scrittura

    private static CompoundTag profilo(Player giocatore, String id) {
        CompoundTag persistenti = giocatore.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persistenti.getCompound(RADICE).getCompound(RegistroSimbionti.famiglia(id));
    }

    private static void salva(Player giocatore, String id, CompoundTag profilo) {
        CompoundTag dati = giocatore.getPersistentData();
        CompoundTag persistenti = dati.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tutti = persistenti.getCompound(RADICE);
        tutti.put(RegistroSimbionti.famiglia(id), profilo);
        persistenti.put(RADICE, tutti);
        dati.put(Player.PERSISTED_NBT_TAG, persistenti);
    }

    public static int inglobati(Player giocatore, String id) {
        return profilo(giocatore, id).getInt(INGLOBATI);
    }

    public static void azzeraInglobati(Player giocatore, String id) {
        CompoundTag p = profilo(giocatore, id);
        p.putInt(INGLOBATI, 0);
        salva(giocatore, id, p);
    }

    public static int digeriti(Player giocatore, String id) {
        return profilo(giocatore, id).getInt(DIGERITI);
    }

    /** I tratti presi digerendo altri simbionti. */
    public static Set<Tratto> acquisiti(Player giocatore, String id) {
        Set<Tratto> fuori = EnumSet.noneOf(Tratto.class);
        ListTag lista = profilo(giocatore, id).getList(TRATTI, Tag.TAG_STRING);
        for (int i = 0; i < lista.size(); i++) {
            try {
                fuori.add(Tratto.valueOf(lista.getString(i)));
            } catch (IllegalArgumentException ignorato) {
                // un tratto tolto dalla mod in una versione successiva: si lascia perdere
            }
        }
        return fuori;
    }

    /** Tutti i tratti di un simbionte per questo giocatore: quelli di natura piu' quelli acquisiti. */
    public static Set<Tratto> tratti(Player giocatore, String id) {
        Set<Tratto> fuori = acquisiti(giocatore, id);
        Simbionte s = RegistroSimbionti.di(id);
        if (s != null) {
            fuori.addAll(s.nativi());
        }
        return fuori;
    }

    private static void scriviAcquisiti(Player giocatore, String id, Set<Tratto> tratti) {
        CompoundTag p = profilo(giocatore, id);
        ListTag lista = new ListTag();
        for (Tratto t : tratti) {
            lista.add(StringTag.valueOf(t.name()));
        }
        p.put(TRATTI, lista);
        salva(giocatore, id, p);
    }

    // ------------------------------------------------------------------ digestione e nascita

    /**
     * Il vincitore inghiotte il perdente: ne prende tutti i tratti che non possiede gia' (quelli
     * di natura del perdente e quelli che il perdente aveva digerito a sua volta), sale di un
     * digerito e di un inglobato. Restituisce i tratti davvero nuovi.
     */
    public static List<Tratto> digerisci(Player giocatore, String vincitore, String perdente) {
        Set<Tratto> giaSuoi = tratti(giocatore, vincitore);
        List<Tratto> nuovi = new ArrayList<>();
        for (Tratto t : tratti(giocatore, perdente)) {
            if (!giaSuoi.contains(t)) {
                nuovi.add(t);
            }
        }
        Set<Tratto> acquisiti = acquisiti(giocatore, vincitore);
        acquisiti.addAll(nuovi);
        scriviAcquisiti(giocatore, vincitore, acquisiti);
        CompoundTag p = profilo(giocatore, vincitore);
        p.putInt(DIGERITI, p.getInt(DIGERITI) + 1);
        p.putInt(INGLOBATI, p.getInt(INGLOBATI) + 1);
        salva(giocatore, vincitore, p);
        return nuovi;
    }

    /**
     * Il figlio eredita una parte delle abilita' del padre, non tutte: ogni tratto del padre
     * passa con la probabilita' del figlio (Toxin, il piu' forte, ne prende di piu').
     */
    public static List<Tratto> eredita(Player giocatore, String padre, String figlio, RandomSource caso) {
        Simbionte s = RegistroSimbionti.di(figlio);
        double quota = s == null ? 0.5D : s.quotaEredita();
        Set<Tratto> giaSuoi = tratti(giocatore, figlio);
        List<Tratto> ereditati = new ArrayList<>();
        for (Tratto t : tratti(giocatore, padre)) {
            if (!giaSuoi.contains(t) && caso.nextDouble() < quota) {
                ereditati.add(t);
            }
        }
        Set<Tratto> acquisiti = acquisiti(giocatore, figlio);
        acquisiti.addAll(ereditati);
        scriviAcquisiti(giocatore, figlio, acquisiti);
        return ereditati;
    }

    // ------------------------------------------------------------------ la forza

    /** Il livello di bond col simbionte: 0 mai indossato, poi 1, 2 da 50 di affinita', 3 a 100. */
    public static int livelloBond(Player giocatore, String id) {
        int affinita = SymbioteState.getScore(giocatore, "Klyntar.Affinity." + id);
        if (affinita >= 100) {
            return 3;
        }
        return affinita >= 50 ? 2 : affinita > 0 ? 1 : 0;
    }

    /**
     * La forza di un simbionte per questo giocatore: base, piu' 5 per ogni digerito, piu' 5 per
     * livello di bond (al massimo 15). Pesa nel conflitto, ed e' la resistenza ad All-Black.
     */
    public static int forza(Player giocatore, String id) {
        Simbionte s = RegistroSimbionti.di(id);
        int base = s == null ? 10 : s.forzaBase();
        return base + FORZA_PER_DIGERITO * digeriti(giocatore, id)
                + FORZA_PER_LIVELLO_BOND * Math.min(LIVELLI_BOND_MASSIMI, livelloBond(giocatore, id));
    }

    // ------------------------------------------------------------------ i tratti accesi

    /**
     * Tiene aggiornati gli objective dei tratti per la forma indossata: 1 per quelli che possiede,
     * 0 per gli altri. I JSON dei poteri li leggono nelle condizioni (la ruota delle armi di Venom
     * si sblocca con {@code Klyntar.BodyWeapons}).
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)
                || giocatore.tickCount % OGNI != 0) {
            return;
        }
        String forma = SymbioteState.forma(giocatore);
        Set<Tratto> tratti = forma.isEmpty() ? EnumSet.noneOf(Tratto.class) : tratti(giocatore, forma);
        // nella doppia unione con All-Black ci sono anche le abilita' del simbionte ospitato
        tratti.addAll(UnioneDoppia.trattiAggiunti(giocatore));
        for (Tratto t : Tratto.values()) {
            int voluto = tratti.contains(t) ? 1 : 0;
            if (SymbioteState.getScore(giocatore, t.obiettivo) != voluto
                    || giocatore.getScoreboard().getObjective(t.obiettivo) == null) {
                SymbioteState.setScore(giocatore, t.obiettivo, voluto);
            }
        }
    }

    /** Ha questo tratto il simbionte indossato? */
    public static boolean haTratto(Player giocatore, Tratto tratto) {
        String forma = SymbioteState.forma(giocatore);
        return !forma.isEmpty() && (tratti(giocatore, forma).contains(tratto)
                || UnioneDoppia.trattiAggiunti(giocatore).contains(tratto));
    }
}
