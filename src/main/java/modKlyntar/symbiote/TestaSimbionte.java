package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.player.VenomPlayerSizeHandler;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La testa del simbionte: parla, si fa chiamare, si arrangia a mangiare e guarda le spalle.
 *
 * <p>Tre cose, tutte legate alla testa che spunta dal giocatore umano:</p>
 * <ul>
 *   <li><b>la chat</b> - chi nomina il simbionte ("venom", "symbiote") gli parla, e lui risponde
 *   nella voce: esce e rientra a comando, dice se ha fame, come sta e cosa vuole, e si offende
 *   se lo si chiama parassita. Prima "Come out Venom" alzava una proprieta' che nessuno leggeva,
 *   e non succedeva niente;</li>
 *   <li><b>lo spuntino dalla borsa</b> - quando la testa ha fame e per terra non c'e' carne, se
 *   ne prende una dall'inventario, al massimo ogni {@link #ATTESA_BORSA} tick;</li>
 *   <li><b>le spalle</b> - un mostro che punta il giocatore da dietro, vicino, viene segnalato.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class TestaSimbionte {
    /** Il lucchetto con cui il Java fa uscire la testa: e' una delle condizioni di enablevenomhead. */
    public static final String FUORI = "Venom.Head.Out";
    private static final String AUTO = "Venom.AutoHead";
    private static final String AUTO_SPENTA = "Venom.AutoHead.Disabled";
    private static final String FAME = "Venom.SymbioteHunger";
    private static final String ULTIMO_SPUNTINO = "Klyntar.HeadSnack";

    /** Quanto rende una carne presa dall'inventario: meta' di quella raccolta da terra. */
    private static final int SPUNTINO = 10;
    private static final long ATTESA_BORSA = 20L * 30L;
    /** Quanto resta affamata la testa prima di frugare nella borsa: il tempo di dirlo. */
    private static final long ATTESA_FAME = 20L * 3L;
    private static final int GUARDIA_OGNI = 10;
    private static final double GUARDIA_RAGGIO = 6.0D;
    private static final long GUARDIA_RICARICA = 20L * 45L;

    /** Le carni che la testa accetta, le stesse che raccoglie da terra. */
    private static final Set<Item> CARNI = Set.of(Items.CHICKEN, Items.COOKED_CHICKEN, Items.PORKCHOP,
            Items.COOKED_PORKCHOP, Items.BEEF, Items.COOKED_BEEF, Items.MUTTON, Items.COOKED_MUTTON,
            Items.RABBIT, Items.COOKED_RABBIT, Items.COD, Items.COOKED_COD, Items.SALMON,
            Items.COOKED_SALMON, Items.ROTTEN_FLESH);

    /** Le parole che dicono che il messaggio e' per il simbionte. */
    private static final List<String> NOMI = List.of("venom", "symbiote", "symbiont", "klyntar");
    /** Le due frasi storiche dei poteri, che valgono anche senza nominarlo. */
    private static final List<String> FRASI_STORICHE = List.of("come out venom", "get back inside");

    /** Da quando la testa cerca da mangiare, per chi la fame ha fatto uscire. */
    private static final Map<UUID, Long> AFFAMATA_DA = new ConcurrentHashMap<>();

    private TestaSimbionte() {
    }

    // ------------------------------------------------------------------ chat

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer ospite = event.getPlayer();
        String testo = event.getRawText().toLowerCase(Locale.ROOT).trim();
        if (!SymbioteState.haSimbionte(ospite) || !rivoltoAlSimbionte(testo)) {
            return;
        }
        // la risposta sul thread del server, dopo che il messaggio e' stato mostrato
        ospite.server.execute(() -> rispondi(ospite, testo));
    }

    private static boolean rivoltoAlSimbionte(String testo) {
        for (String frase : FRASI_STORICHE) {
            if (testo.contains(frase)) {
                return true;
            }
        }
        for (String nome : NOMI) {
            if (testo.contains(nome)) {
                return true;
            }
        }
        return false;
    }

    private static void rispondi(ServerPlayer ospite, String testo) {
        if (contiene(testo, "parasite", "leech")) {
            UmoreSimbionte.cambia(ospite, -5);
            VoceSimbionte.di(ospite, "chat_parassita", Tono.AGGRESSIVO, true);
        } else if (contiene(testo, "get back", "back inside", "go back", "hide", "inside")) {
            rientra(ospite);
            VoceSimbionte.di(ospite, "testa_zittita", Tono.LEGAME_GIU, true);
        } else if (contiene(testo, "come out", "show yourself", "appear", "out")) {
            if (corpoFuori(ospite)) {
                VoceSimbionte.di(ospite, "chat_gia_fuori", Tono.NEUTRO, true);
            } else if (SymbioteState.isVulnerabile(ospite)) {
                // indebolito la testa e' chiusa come tutto il resto: lo dice, invece di non uscire muto
                VoceSimbionte.di(ospite, "testa_debole", Tono.LEGAME_GIU, true);
            } else {
                SymbioteState.setScore(ospite, FUORI, 1);
                VoceSimbionte.di(ospite, "testa_fuori", Tono.NEUTRO, true);
            }
        } else if (contiene(testo, "hungry", "hunger", "food", "eat", "starving")) {
            int fame = SymbioteState.getScore(ospite, FAME);
            VoceSimbionte.di(ospite, fame >= 70 ? "chat_fame_ok" : fame >= 30 ? "chat_fame_mezza" : "chat_fame_bassa",
                    fame >= 30 ? Tono.NEUTRO : Tono.AGGRESSIVO, true);
        } else if (contiene(testo, "how are you", "how do you feel", "you ok", "are you ok", "mood", "feeling")) {
            int umore = UmoreSimbionte.umore(ospite);
            VoceSimbionte.di(ospite, umore >= UmoreSimbionte.CONTENTO ? "chat_umore_felice"
                            : umore <= UmoreSimbionte.FURIOSO ? "chat_umore_irritato" : "chat_umore_neutro",
                    umore <= UmoreSimbionte.FURIOSO ? Tono.AGGRESSIVO : Tono.NEUTRO, true);
        } else if (contiene(testo, "want", "need", "wish")) {
            DesideriSimbionte.Desiderio desiderio = DesideriSimbionte.attuale(ospite);
            if (desiderio == DesideriSimbionte.Desiderio.NESSUNO) {
                VoceSimbionte.di(ospite, "chat_desiderio_nessuno", Tono.NEUTRO, true);
            } else {
                VoceSimbionte.di(ospite, desiderio.chiave + "_chiede", Tono.NEUTRO, true);
            }
        } else if (contiene(testo, "thank", "good boy", "good job", "well done", "nice work")) {
            UmoreSimbionte.cambia(ospite, 3);
            VoceSimbionte.di(ospite, "chat_grazie", Tono.LEGAME_SU, true);
        } else if (contiene(testo, "love you", "i love")) {
            UmoreSimbionte.cambia(ospite, 2);
            VoceSimbionte.di(ospite, "chat_amore", Tono.LEGAME_SU, true);
        } else {
            VoceSimbionte.di(ospite, "chat_saluto", Tono.NEUTRO, true);
        }
    }

    private static boolean contiene(String testo, String... parole) {
        for (String parola : parole) {
            if (parola.contains(" ") ? testo.contains(parola) : parolaIntera(testo, parola)) {
                return true;
            }
        }
        return false;
    }

    /** "out" non deve scattare dentro "about", ne' "eat" dentro "great". */
    private static boolean parolaIntera(String testo, String parola) {
        for (String pezzo : testo.split("[^a-z]+")) {
            if (pezzo.equals(parola) || (parola.length() > 4 && pezzo.startsWith(parola))) {
                return true;
            }
        }
        return false;
    }

    /** Rientra: niente testa chiamata a voce, niente pasti da sola finche' non ha di nuovo fame. */
    public static void rientra(ServerPlayer ospite) {
        SymbioteState.setScore(ospite, FUORI, 0);
        SymbioteState.setScore(ospite, AUTO, 0);
        SymbioteState.setScore(ospite, AUTO_SPENTA, 1);
    }

    private static boolean corpoFuori(ServerPlayer ospite) {
        return SymbioteState.getScore(ospite, VenomPlayerSizeHandler.SIZE_OBJECTIVE) > 0;
    }

    // ------------------------------------------------------------------ spuntino dalla borsa

    /**
     * La testa ha fame e per terra non trova niente: prende una carne dall'inventario. La chiama
     * il pasto automatico della testa. Restituisce se ha mangiato.
     */
    public static boolean spuntinoDallaBorsa(ServerPlayer ospite) {
        if (SymbioteState.isVulnerabile(ospite)) {
            return false;
        }
        long adesso = ospite.level().getGameTime();
        // prima si lamenta, poi ruba: senza l'attesa lo spuntino arrivava nello stesso tick in cui
        // la fame faceva uscire la testa, e la sua battuta copriva quella della fame
        Long affamataDa = AFFAMATA_DA.putIfAbsent(ospite.getUUID(), adesso);
        if (affamataDa == null || adesso - affamataDa < ATTESA_FAME) {
            return false;
        }
        long ultimo = ospite.getPersistentData().getLong(ULTIMO_SPUNTINO);
        if (adesso - ultimo < ATTESA_BORSA && adesso >= ultimo) {
            return false;
        }
        Inventory borsa = ospite.getInventory();
        for (int i = 0; i < borsa.getContainerSize(); i++) {
            ItemStack pila = borsa.getItem(i);
            if (!pila.isEmpty() && CARNI.contains(pila.getItem())) {
                pila.shrink(1);
                ospite.getPersistentData().putLong(ULTIMO_SPUNTINO, adesso);
                SymbioteState.setScore(ospite, FAME, Math.min(100, SymbioteState.getScore(ospite, FAME) + SPUNTINO));
                VoceSimbionte.di(ospite, "testa_ruba", Tono.NEUTRO);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ le spalle

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % GUARDIA_OGNI != 0 || !SymbioteState.haSimbionte(ospite)) {
            return;
        }
        if (SymbioteState.getScore(ospite, AUTO) <= 0) {
            AFFAMATA_DA.remove(ospite.getUUID());
        }
        // il corpo fuori chiude la testa chiamata a voce: e' gia' tutto fuori
        if (corpoFuori(ospite) && SymbioteState.getScore(ospite, FUORI) > 0) {
            SymbioteState.setScore(ospite, FUORI, 0);
        }
        if (mostroAlleSpalle(ospite)) {
            VoceSimbionte.di(ospite, "testa_dietro", Tono.AVVISO, false, GUARDIA_RICARICA);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SymbioteState.setScore(event.getEntity(), FUORI, 0);
        AFFAMATA_DA.remove(event.getEntity().getUUID());
    }

    /** Un mostro che ce l'ha col giocatore, vicino, e alle sue spalle: fuori dal suo campo visivo. */
    private static boolean mostroAlleSpalle(ServerPlayer ospite) {
        Vec3 sguardo = ospite.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (sguardo.lengthSqr() < 1.0E-4D) {
            return false;
        }
        sguardo = sguardo.normalize();
        AABB zona = ospite.getBoundingBox().inflate(GUARDIA_RAGGIO);
        for (Mob mob : ospite.level().getEntitiesOfClass(Mob.class, zona,
                m -> m.isAlive() && m instanceof Enemy && m.getTarget() == ospite)) {
            Vec3 verso = mob.position().subtract(ospite.position()).multiply(1.0D, 0.0D, 1.0D);
            if (verso.lengthSqr() > 1.0E-4D && verso.normalize().dot(sguardo) < -0.3D) {
                return true;
            }
        }
        return false;
    }
}
