package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * L'umore del simbionte: da -100 (furioso) a 100 (contento), zero di partenza.
 *
 * <p>Lo muovono quello che l'ospite gli fa passare - nutrirlo lo rallegra, lasciarlo a digiuno,
 * bruciarlo o assordarlo lo irrita, i desideri esauditi o ignorati pesano di piu' - e col tempo
 * torna da solo verso zero. Sta nello scoreboard, cosi' si salva col mondo.</p>
 *
 * <p>Cosa cambia: di tanto in tanto il simbionte chiacchiera, col tono del suo umore; contento
 * restituisce un punto di fame al minuto, furioso ne brucia uno in piu'; e sotto -40, con
 * un'affinita' bassa, puo' mettere il broncio (vedi BroncioSimbionte).</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class UmoreSimbionte {
    public static final String OBIETTIVO = "Klyntar.Mood";
    public static final int CONTENTO = 40;
    public static final int FURIOSO = -40;

    private static final String FAME = "Venom.SymbioteHunger";
    private static final int FAME_MASSIMA = 100;
    /** Ogni minuto l'umore si avvicina a zero di tanto, e contento o furioso tocca la fame. */
    private static final int OGNI_MINUTO = 20 * 60;
    private static final int RITORNO_A_ZERO = 2;
    /** Le chiacchiere a riposo: una ogni sei-dodici minuti, mai in combattimento. */
    private static final int CHIACCHIERA_MIN = 20 * 60 * 6;
    private static final int CHIACCHIERA_MAX = 20 * 60 * 12;
    /** Colpito da poco vuol dire in combattimento: niente chiacchiere. */
    private static final int CALMA_DOPO_COLPO = 20 * 10;
    /** Con Bond 1, quante chiacchiere su dieci parlano di trovarsi un ospite migliore. */
    private static final float CERCA_OSPITE = 0.4F;

    private static final Map<UUID, Integer> PROSSIMA_CHIACCHIERA = new ConcurrentHashMap<>();

    private UmoreSimbionte() {
    }

    public static int umore(Player ospite) {
        return Math.max(-100, Math.min(100, SymbioteState.getScore(ospite, OBIETTIVO)));
    }

    public static void cambia(ServerPlayer ospite, int quanto) {
        SymbioteState.setScore(ospite, OBIETTIVO, Math.max(-100, Math.min(100, umore(ospite) + quanto)));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || !SymbioteState.haSimbionte(ospite)) {
            return;
        }
        if (ospite.tickCount % OGNI_MINUTO == 0) {
            ogniMinuto(ospite);
        }
        int attesa = PROSSIMA_CHIACCHIERA.merge(ospite.getUUID(), -1, Integer::sum);
        if (attesa <= 0) {
            PROSSIMA_CHIACCHIERA.put(ospite.getUUID(), CHIACCHIERA_MIN
                    + ospite.getRandom().nextInt(CHIACCHIERA_MAX - CHIACCHIERA_MIN));
            if (attesa == 0 && ospite.tickCount - ospite.getLastHurtByMobTimestamp() > CALMA_DOPO_COLPO) {
                chiacchiera(ospite);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PROSSIMA_CHIACCHIERA.remove(event.getEntity().getUUID());
    }

    private static void ogniMinuto(ServerPlayer ospite) {
        int ora = umore(ospite);
        if (ora > 0) {
            cambia(ospite, -Math.min(ora, RITORNO_A_ZERO));
        } else if (ora < 0) {
            cambia(ospite, Math.min(-ora, RITORNO_A_ZERO));
        }
        // la fame la lascia stare il berserk, che ha le sue regole
        if (SymbioteState.getScore(ospite, "Venom.Berserk") > 0) {
            return;
        }
        int fame = SymbioteState.getScore(ospite, FAME);
        if (ora >= CONTENTO && fame < FAME_MASSIMA) {
            SymbioteState.setScore(ospite, FAME, fame + 1);
        } else if (ora <= FURIOSO && fame > 0) {
            SymbioteState.setScore(ospite, FAME, fame - 1);
        }
    }

    private static void chiacchiera(ServerPlayer ospite) {
        int ora = umore(ospite);
        // chi non si fida ancora si guarda intorno: l'ospite, per lui, e' solo il primo che capitava
        if (VoceSimbionte.fiducia(ospite) == VoceSimbionte.DIFFIDENTE
                && ospite.getRandom().nextFloat() < CERCA_OSPITE) {
            VoceSimbionte.di(ospite, "cerca_ospite", Tono.AGGRESSIVO);
        } else if (ora >= CONTENTO) {
            VoceSimbionte.di(ospite, "umore_felice", Tono.LEGAME_SU);
        } else if (ora <= FURIOSO) {
            VoceSimbionte.di(ospite, "umore_irritato", Tono.AGGRESSIVO);
        } else {
            VoceSimbionte.di(ospite, "umore_neutro", Tono.NEUTRO);
        }
    }
}
