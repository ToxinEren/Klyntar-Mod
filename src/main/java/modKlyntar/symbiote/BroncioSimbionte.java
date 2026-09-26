package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.player.VenomPlayerSizeHandler;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il broncio: un simbionte furioso e poco legato all'ospite, ogni tanto, si rifiuta di combattere.
 *
 * <p>Per otto secondi le abilita' offensive restano chiuse - presa, pugno, tentacoli di presa,
 * pull, strike, rage, blast, assimilazione - col lucchetto {@link #OBIETTIVO} nelle condizioni
 * dei poteri, come fa l'indebolimento. Non si toccano mai quelle che servono a scappare:
 * traversata, volo, arrampicata, invisibilita'. E lo dice: il giocatore sente il rifiuto prima
 * di scoprirlo premendo un tasto a vuoto, e sente anche quando e' finito.</p>
 *
 * <p>Condizioni strette apposta: umore sotto {@link UmoreSimbionte#FURIOSO}, affinita' sotto
 * {@link #AFFINITA_MASSIMA}, corpo fuori, niente berserk; una prova ogni trenta secondi con una
 * possibilita' su quattro, e due minuti di pace dopo ogni broncio. Chi tratta bene il suo
 * simbionte non lo vedra' mai.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class BroncioSimbionte {
    public static final String OBIETTIVO = "Klyntar.Sulk";

    private static final int OGNI = 5;
    private static final int PROVA_OGNI = 20 * 30;
    private static final float PROBABILITA = 0.25F;
    private static final int DURATA = 20 * 8;
    private static final int PACE_DOPO = 20 * 60 * 2;
    private static final int AFFINITA_MASSIMA = 50;

    private static final Map<UUID, Integer> PACE = new ConcurrentHashMap<>();

    private BroncioSimbionte() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0) {
            return;
        }
        int broncio = SymbioteState.getScore(ospite, OBIETTIVO);
        if (broncio > 0) {
            int resta = Math.max(0, broncio - OGNI);
            SymbioteState.setScore(ospite, OBIETTIVO, resta);
            if (resta == 0) {
                PACE.put(ospite.getUUID(), PACE_DOPO);
                if (SymbioteState.haSimbionte(ospite)) {
                    VoceSimbionte.di(ospite, "broncio_fine", Tono.NEUTRO, true);
                }
            }
            return;
        }
        int pace = PACE.getOrDefault(ospite.getUUID(), 0);
        if (pace > 0) {
            PACE.put(ospite.getUUID(), pace - OGNI);
            return;
        }
        if (ospite.tickCount % PROVA_OGNI != 0 || !puoMettereIlBroncio(ospite)
                || ospite.getRandom().nextFloat() >= PROBABILITA) {
            return;
        }
        SymbioteState.setScore(ospite, OBIETTIVO, DURATA);
        VoceSimbionte.di(ospite, "broncio", Tono.AGGRESSIVO, true);
    }

    /**
     * Gli obiettivi di broncio e umore devono esistere da subito: uno mai scritto non esiste, e
     * nemmeno un comando riesce a scriverlo o interrogarlo.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SymbioteState.assicuraObiettivo(event.getEntity(), OBIETTIVO);
        SymbioteState.assicuraObiettivo(event.getEntity(), UmoreSimbionte.OBIETTIVO);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // niente broncio lasciato acceso nel mondo salvato: al rientro le abilita' sarebbero chiuse
        SymbioteState.setScore(event.getEntity(), OBIETTIVO, 0);
        PACE.remove(event.getEntity().getUUID());
    }

    private static boolean puoMettereIlBroncio(ServerPlayer ospite) {
        return SymbioteState.haSimbionte(ospite)
                && SymbioteState.getScore(ospite, VenomPlayerSizeHandler.SIZE_OBJECTIVE) > 0
                && SymbioteState.getScore(ospite, "Venom.Berserk") <= 0
                && UmoreSimbionte.umore(ospite) <= UmoreSimbionte.FURIOSO
                && SymbioteState.affinita(ospite) < AFFINITA_MASSIMA;
    }
}
