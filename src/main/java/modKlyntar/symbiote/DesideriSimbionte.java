package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * I desideri del simbionte: ogni tanto chiede qualcosa, e ha cinque minuti di pazienza.
 *
 * <p>Esaudirlo vale un giorno di legame (due punti d'affinita') e lo rallegra; ignorarlo lo
 * irrita. Prima l'affinita' saliva solo col calendario; ora si puo' anche guadagnare, o
 * perdere di umore, con quello che si fa insieme. Tutto sta nello scoreboard, cosi' un
 * desiderio a meta' sopravvive alla chiusura del gioco.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class DesideriSimbionte {

    /** Quello che puo' chiedere: la chiave delle battute e cosa si legge nella barra. */
    public enum Desiderio {
        NESSUNO("", ""),
        CACCIA("des_caccia", "Hunt at night"),
        CARNE("des_carne", "Eat meat"),
        ALTEZZA("des_altezza", "Climb above Y 140"),
        PROFONDITA("des_profondita", "Go below Y 0"),
        SCONTRO("des_scontro", "Kill 3 foes");

        final String chiave;
        final String etichetta;

        Desiderio(String chiave, String etichetta) {
            this.chiave = chiave;
            this.etichetta = etichetta;
        }
    }

    private static final String DESIDERIO = "Klyntar.Desire";
    private static final String TEMPO = "Klyntar.Desire.Time";
    private static final String PROGRESSO = "Klyntar.Desire.Progress";
    private static final String PROSSIMO = "Klyntar.Desire.Next";

    private static final int OGNI = 20;
    private static final int PAZIENZA = 20 * 60 * 5;
    /** Fra un desiderio e l'altro: dieci-venti minuti. */
    private static final int ATTESA_MIN = 20 * 60 * 10;
    private static final int ATTESA_MAX = 20 * 60 * 20;
    private static final int ALTEZZA = 140;
    private static final int PROFONDITA = 0;
    private static final int SCONTRI = 3;
    private static final int PREMIO_AFFINITA = 2;
    private static final int PREMIO_UMORE = 20;
    private static final int CASTIGO_UMORE = -15;

    private DesideriSimbionte() {
    }

    public static Desiderio attuale(Player ospite) {
        int id = SymbioteState.getScore(ospite, DESIDERIO);
        Desiderio[] tutti = Desiderio.values();
        return id > 0 && id < tutti.length ? tutti[id] : Desiderio.NESSUNO;
    }

    /** Cosa scrivere nella barra della fame, o null se non vuole niente. */
    public static String etichetta(Player ospite) {
        Desiderio desiderio = attuale(ospite);
        if (desiderio == Desiderio.NESSUNO) {
            return null;
        }
        int secondi = (SymbioteState.getScore(ospite, TEMPO) + 19) / 20;
        String testo = "Wants: " + desiderio.etichetta;
        if (desiderio == Desiderio.SCONTRO) {
            testo += " (" + SymbioteState.getScore(ospite, PROGRESSO) + "/" + SCONTRI + ")";
        }
        return testo + " " + (secondi / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", secondi % 60);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0 || !SymbioteState.haSimbionte(ospite)) {
            return;
        }
        // in berserk il simbionte vuole una cosa sola, e se la prende da se'
        if (SymbioteState.getScore(ospite, "Venom.Berserk") > 0) {
            return;
        }
        Desiderio desiderio = attuale(ospite);
        if (desiderio == Desiderio.NESSUNO) {
            aspetta(ospite);
            return;
        }
        if ((desiderio == Desiderio.ALTEZZA && ospite.getY() >= ALTEZZA)
                || (desiderio == Desiderio.PROFONDITA && ospite.getY() <= PROFONDITA)) {
            esaudito(ospite, desiderio);
            return;
        }
        int resta = SymbioteState.getScore(ospite, TEMPO) - OGNI;
        if (resta <= 0) {
            ignorato(ospite, desiderio);
        } else {
            SymbioteState.setScore(ospite, TEMPO, resta);
        }
    }

    /** Un nemico ucciso: di notte e' una caccia, e ogni uccisione conta per lo scontro. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer ospite) || !(event.getEntity() instanceof Enemy)) {
            return;
        }
        Desiderio desiderio = attuale(ospite);
        if (desiderio == Desiderio.CACCIA && ospite.level().isNight()) {
            esaudito(ospite, desiderio);
        } else if (desiderio == Desiderio.SCONTRO) {
            int fatti = SymbioteState.getScore(ospite, PROGRESSO) + 1;
            SymbioteState.setScore(ospite, PROGRESSO, fatti);
            if (fatti >= SCONTRI) {
                esaudito(ospite, desiderio);
            }
        }
    }

    /** Carne mangiata dall'ospite. */
    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer ospite) || attuale(ospite) != Desiderio.CARNE) {
            return;
        }
        FoodProperties cibo = event.getItem().getFoodProperties(ospite);
        if (cibo != null && cibo.isMeat()) {
            esaudito(ospite, Desiderio.CARNE);
        }
    }

    /** Un pasto del simbionte stesso, con Regeneration: vale come carne. La chiama la voce. */
    public static void pastoDelSimbionte(ServerPlayer ospite) {
        if (attuale(ospite) == Desiderio.CARNE) {
            esaudito(ospite, Desiderio.CARNE);
        }
    }

    private static void aspetta(ServerPlayer ospite) {
        // un obiettivo mai scritto non esiste: la prima volta si fissa l'attesa, senza chiedere
        if (ospite.getScoreboard().getObjective(PROSSIMO) == null) {
            SymbioteState.setScore(ospite, PROSSIMO, attesaACaso(ospite));
            return;
        }
        int resta = SymbioteState.getScore(ospite, PROSSIMO) - OGNI;
        if (resta > 0) {
            SymbioteState.setScore(ospite, PROSSIMO, resta);
            return;
        }
        Desiderio[] scelte = {Desiderio.CACCIA, Desiderio.CARNE, Desiderio.ALTEZZA, Desiderio.PROFONDITA, Desiderio.SCONTRO};
        Desiderio scelto = scelte[ospite.getRandom().nextInt(scelte.length)];
        // di giorno una caccia notturna sarebbe impossibile nei cinque minuti: meglio lo scontro
        if (scelto == Desiderio.CACCIA && !ospite.level().isNight()) {
            scelto = Desiderio.SCONTRO;
        }
        SymbioteState.setScore(ospite, DESIDERIO, scelto.ordinal());
        SymbioteState.setScore(ospite, TEMPO, PAZIENZA);
        SymbioteState.setScore(ospite, PROGRESSO, 0);
        VoceSimbionte.di(ospite, scelto.chiave + "_chiede", Tono.NEUTRO, true);
    }

    private static void esaudito(ServerPlayer ospite, Desiderio desiderio) {
        chiudi(ospite);
        UmoreSimbionte.cambia(ospite, PREMIO_UMORE);
        String forma = SymbioteState.forma(ospite);
        if (!forma.isEmpty()) {
            String affinita = "Klyntar.Affinity." + forma;
            SymbioteState.setScore(ospite, affinita,
                    Math.min(100, SymbioteState.getScore(ospite, affinita) + PREMIO_AFFINITA));
        }
        VoceSimbionte.di(ospite, desiderio.chiave + "_fatto", Tono.LEGAME_SU, true);
    }

    private static void ignorato(ServerPlayer ospite, Desiderio desiderio) {
        chiudi(ospite);
        UmoreSimbionte.cambia(ospite, CASTIGO_UMORE);
        VoceSimbionte.di(ospite, desiderio.chiave + "_fallito", Tono.LEGAME_GIU, true);
    }

    private static void chiudi(ServerPlayer ospite) {
        SymbioteState.setScore(ospite, DESIDERIO, 0);
        SymbioteState.setScore(ospite, TEMPO, 0);
        SymbioteState.setScore(ospite, PROGRESSO, 0);
        SymbioteState.setScore(ospite, PROSSIMO, attesaACaso(ospite));
    }

    private static int attesaACaso(ServerPlayer ospite) {
        return ATTESA_MIN + ospite.getRandom().nextInt(ATTESA_MAX - ATTESA_MIN);
    }
}
