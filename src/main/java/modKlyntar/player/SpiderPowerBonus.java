package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.power.SuperpowerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Locale;

/**
 * La seconda strada per {@code venomspidey}: il ragno che il giocatore era gia'.
 *
 * <p>La prima e' {@link SpiderSlainHandler}, cioe' divorare un ragno da Venom. Questa vale
 * invece al momento della fusione: se chi riceve il simbionte porta gia' un superpotere da
 * ragno — di questa mod o di un'altra qualsiasi — il simbionte non ha niente da rubargli,
 * l'arrampicata ce l'ha gia'. Salta quindi il passaggio da Venom base e nasce direttamente
 * Venom col dono del ragno.</p>
 *
 * <p>Il riconoscimento e' volutamente largo: basta la parola <b>spider</b> dentro l'id del
 * potere, spazio dei nomi compreso. Cosi' rientrano sia {@code marvel:spider_man} sia
 * {@code spiderman:peter_parker}, senza dover elencare le mod una per una.</p>
 */
public final class SpiderPowerBonus {

    private static final Logger LOGGER = LogManager.getLogger("KlyntarSpiderBonus");

    /** Quello che deve comparire nell'id del potere. */
    private static final String PAROLA = "spider";

    /** La forma di partenza: solo la fusione con Venom base viene deviata. */
    private static final String FORMA_BASE = "venom";
    /** Dove si arriva. */
    private static final String FORMA_RAGNO = "venomspidey";

    private SpiderPowerBonus() {
    }

    /**
     * Il giocatore porta gia' un potere da ragno?
     *
     * <p>I poteri di Klyntar non contano. Sono esclusi apposta: sono le nostre forme, e senza
     * il filtro una forma nostra che un giorno si chiamasse "spider qualcosa" finirebbe per
     * promuovere se stessa a ogni fusione.</p>
     */
    public static boolean haPoteriDaRagno(Player giocatore) {
        Collection<?> poteri = SuperpowerUtil.getSuperpowerIds(giocatore);
        if (poteri == null) {
            return false;
        }
        for (Object potere : poteri) {
            String id = String.valueOf(potere).toLowerCase(Locale.ROOT);
            if (id.startsWith(MyMod.MOD_ID + ":")) {
                continue;
            }
            if (id.contains(PAROLA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * La forma con cui chiudere davvero la fusione.
     *
     * <p>Restituisce quella richiesta in tutti i casi tranne uno: Venom base addosso a chi ha
     * gia' i poteri del ragno, che diventa {@code venomspidey}. Le altre forme — carnage,
     * toxin, anti-venom — passano intatte: non hanno una versione col ragno.</p>
     */
    public static String formaDellaFusione(ServerPlayer giocatore, String forma) {
        if (!FORMA_BASE.equals(forma) || !haPoteriDaRagno(giocatore)) {
            return forma;
        }
        LOGGER.info("{} ha gia' poteri da ragno: la fusione salta a {}",
                giocatore.getGameProfile().getName(), FORMA_RAGNO);
        return FORMA_RAGNO;
    }
}
