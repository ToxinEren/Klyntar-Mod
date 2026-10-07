package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.symbiote.RegistroSimbionti.Tratto;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import java.util.Set;

/**
 * La doppia unione: All-Black e un altro simbionte nello stesso corpo.
 *
 * <p>Dal documento di design. Se il giocatore ha gia' un simbionte quando raccoglie All-Black,
 * l'esito e' deterministico: la <b>dominanza</b> di All-Black (30, piu' 10 per stadio di
 * corruzione) contro la <b>resistenza</b> del simbionte ospitato (la sua forza: base, +5 per
 * digerito, +5 per livello di bond fino a +15, vedi {@link ProfiliSimbionti#forza}).</p>
 * <ul>
 *   <li>dominanza maggiore: All-Black digerisce il simbionte e ne prende tutte le abilita'. Il
 *   suo bond resta salvato;</li>
 *   <li>dominanza minore o uguale: doppia unione stabile. Restano tutti e due, con le abilita' di
 *   entrambi, e il conflitto resta sempre acceso: danni periodici che salgono con lo stadio, fame
 *   piu' rapida, il controllo conteso fra due volonta', e a corruzione alta le abilita' del
 *   simbionte ospitato arretrano.</li>
 * </ul>
 * <p>Il confronto si rifa' a ogni cambio di affinita' e di stadio: chi resiste all'inizio puo'
 * essere digerito piu' avanti. <b>Eccezione: Toxin.</b> All-Black lo preferisce agli altri -
 * nei fumetti e' la millesima generazione della sua stirpe - e non lo digerisce mai: l'unione
 * resta stabile, con un dialogo tutto loro.</p>
 *
 * <p><b>Segnalibro.</b> All-Black non e' ancora nella mod: il punto d'ingresso
 * {@link #entraAllBlack} lo chiamera' il cratere quando il giocatore lo raccoglie (e il
 * conflitto, se All-Black arriva come mob).</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class UnioneDoppia {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarUnione");
    private static final String OSPITATO = "Klyntar.Union.Hosted";
    /** 1 quando le abilita' del simbionte ospitato arretrano: lo leggono i JSON dei poteri. */
    public static final String ARRETRA = "Klyntar.Union.Recede";
    private static final int DOMINANZA_BASE = 30;
    private static final int DOMINANZA_PER_STADIO = 10;
    private static final int STADIO_ARRETRA = 3;
    private static final int OGNI = 100;
    private static final int DANNO_OGNI = 20 * 10;
    private static final int FAME_OGNI = 20 * 6;
    private static final int CONTESA_OGNI = 20 * 60;
    private static final int DIALOGO_TOXIN_OGNI = 20 * 60 * 5;
    /** Toxin risponde ad All-Black dopo tre secondi. */
    private static final long RISPOSTA_TOXIN_DOPO = 60L;
    private static final java.util.Map<java.util.UUID, Long> RISPOSTA_TOXIN = new java.util.concurrent.ConcurrentHashMap<>();

    private UnioneDoppia() {
    }

    // ------------------------------------------------------------------ lo stato

    private static CompoundTag persistenti(Player giocatore) {
        return giocatore.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** Il simbionte che vive insieme ad All-Black, o stringa vuota. */
    public static String ospitato(Player giocatore) {
        return persistenti(giocatore).getString(OSPITATO);
    }

    private static void scriviOspitato(Player giocatore, String forma) {
        CompoundTag dati = giocatore.getPersistentData();
        CompoundTag p = persistenti(giocatore);
        if (forma == null || forma.isEmpty()) {
            p.remove(OSPITATO);
        } else {
            p.putString(OSPITATO, forma);
        }
        dati.put(Player.PERSISTED_NBT_TAG, p);
    }

    public static boolean attiva(Player giocatore) {
        return CorruzioneAllBlack.attivo(giocatore) && !ospitato(giocatore).isEmpty();
    }

    public static int dominanza(Player giocatore) {
        return DOMINANZA_BASE + DOMINANZA_PER_STADIO * CorruzioneAllBlack.stadio(giocatore);
    }

    private static boolean eToxin(String forma) {
        return "toxin".equals(RegistroSimbionti.famiglia(forma));
    }

    // ------------------------------------------------------------------ l'ingresso

    /**
     * Il giocatore raccoglie All-Black. SEGNALIBRO: lo chiamera' il cratere di All-Black, che
     * spawna una volta sola per mondo ({@link ApparizioniUniche}).
     */
    public static void entraAllBlack(ServerPlayer ospite) {
        if (!RegistroSimbionti.disponibile(CorruzioneAllBlack.FORMA)) {
            LOGGER.info("All-Black reached {}, but it is not in the mod yet", ospite.getGameProfile().getName());
            return;
        }
        String prima = SymbioteState.forma(ospite);
        ConflittoSimbionti.cambiaForma(ospite, CorruzioneAllBlack.FORMA);
        if (prima.isEmpty() || CorruzioneAllBlack.FORMA.equals(prima)) {
            return;
        }
        // la corruzione parte da 0 alla raccolta e non conta: la dominanza e' quella base
        if (!eToxin(prima) && dominanza(ospite) > ProfiliSimbionti.forza(ospite, prima)) {
            digerisce(ospite, prima);
        } else {
            scriviOspitato(ospite, prima);
            if (eToxin(prima)) {
                dialogoToxin(ospite);
            } else {
                VoceSimbionte.di(ospite, "unione_doppia", Tono.AGGRESSIVO, true);
            }
            ospite.displayClientMessage(Component.translatable("klyntars.unione.stabile", nome(prima)), false);
        }
    }

    private static void digerisce(ServerPlayer ospite, String ospitato) {
        ProfiliSimbionti.digerisci(ospite, CorruzioneAllBlack.FORMA, ospitato);
        scriviOspitato(ospite, "");
        SymbioteState.setScore(ospite, ARRETRA, 0);
        VoceSimbionte.di(ospite, "unione_digerito", Tono.AGGRESSIVO, true);
        ospite.displayClientMessage(Component.translatable("klyntars.conflitto.assorbe",
                Component.literal("All-Black"), nome(ospitato)), false);
        LOGGER.info("{}: All-Black digested {}", ospite.getGameProfile().getName(), ospitato);
    }

    /** Rifa' il confronto: lo chiama la corruzione a ogni cambio di stadio, e il tick. */
    public static void ricalcola(ServerPlayer ospite) {
        String ospitato = ospitato(ospite);
        if (ospitato.isEmpty() || !CorruzioneAllBlack.attivo(ospite) || eToxin(ospitato)) {
            return;
        }
        if (dominanza(ospite) > ProfiliSimbionti.forza(ospite, ospitato)) {
            digerisce(ospite, ospitato);
        }
    }

    /**
     * Le abilita' del simbionte ospitato, che la doppia unione aggiunge a quelle di All-Black.
     * A corruzione alta arretrano e restano solo quelle di All-Black.
     */
    public static Set<Tratto> trattiAggiunti(Player giocatore) {
        if (!attiva(giocatore) || CorruzioneAllBlack.stadio(giocatore) >= STADIO_ARRETRA) {
            return EnumSet.noneOf(Tratto.class);
        }
        return ProfiliSimbionti.tratti(giocatore, ospitato(giocatore));
    }

    // ------------------------------------------------------------------ i costi

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)) {
            return;
        }
        if (!attiva(ospite)) {
            return;
        }
        if (ospite.tickCount % OGNI == 0) {
            ricalcola(ospite);
            SymbioteState.setScore(ospite, ARRETRA, CorruzioneAllBlack.stadio(ospite) >= STADIO_ARRETRA ? 1 : 0);
        }
        int stadio = CorruzioneAllBlack.stadio(ospite);
        if (ospite.tickCount % DANNO_OGNI == 0) {
            // due volonta' in un corpo solo: danni periodici che salgono con lo stadio
            ospite.hurt(ospite.damageSources().magic(), 1.0F + 0.5F * stadio);
        }
        if (ospite.tickCount % FAME_OGNI == 0) {
            modKlyntar.player.VenomSymbioteSystemsHandler.togliFame(ospite, 1);
        }
        if (ospite.tickCount % CONTESA_OGNI == 0 && ospite.getRandom().nextFloat() < 0.15F + 0.1F * stadio) {
            // il controllo conteso: per qualche secondo il corpo non risponde come dovrebbe
            ospite.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
            ospite.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
            VoceSimbionte.di(ospite, "unione_contesa", Tono.AGGRESSIVO, true);
        }
        if (eToxin(ospitato(ospite)) && ospite.tickCount % DIALOGO_TOXIN_OGNI == 0) {
            dialogoToxin(ospite);
        }
        Long risposta = RISPOSTA_TOXIN.get(ospite.getUUID());
        if (risposta != null && ospite.level().getGameTime() >= risposta) {
            RISPOSTA_TOXIN.remove(ospite.getUUID());
            VoceSimbionte.di(ospite, "unione_toxin_risposta", Tono.LEGAME_SU, true);
        }
    }

    /**
     * Il dialogo del documento: All-Black parla ("ti sopporto solo per la tua forza e il tuo
     * lignaggio"), Toxin risponde ("non mi aspettavo di conoscere cosi' il mio bisnonno").
     */
    private static void dialogoToxin(ServerPlayer ospite) {
        VoceSimbionte.di(ospite, "unione_toxin", Tono.AGGRESSIVO, true);
        RISPOSTA_TOXIN.put(ospite.getUUID(), ospite.level().getGameTime() + RISPOSTA_TOXIN_DOPO);
    }

    private static Component nome(String forma) {
        RegistroSimbionti.Simbionte s = RegistroSimbionti.di(forma);
        return Component.literal(s == null ? forma : s.nome());
    }
}
