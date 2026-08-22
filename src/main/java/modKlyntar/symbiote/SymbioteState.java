package modKlyntar.symbiote;

import modKlyntar.capability.PlayerPowerCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Lo stato del simbionte letto in un posto solo.
 *
 * <p>Gli obiettivi della scoreboard sono la memoria condivisa fra i gestori Java, i comandi di
 * Palladium e gli script del pack. Prima ogni gestore si riscriveva le proprie
 * {@code getScore}/{@code setScore} private; qui ce n'e' una copia sola.</p>
 */
public final class SymbioteState {
    /** alzato mentre il giocatore e' indebolito da fuoco o suono: blocca quasi tutto */
    public static final String VULNERABILITY_OBJECTIVE = "Venom.VulnerabilityLock";
    /** quanto il giocatore e' in sintonia col simbionte: apre i Venom Bond a 50 e a 100 */
    public static final String AFFINITY_OBJECTIVE = "Klyntar.Affinity";
    /** 1 quando il giocatore si e' guadagnato il Knull's Bond, il nodo che regge il volo */
    public static final String KNULL_BOND_OBJECTIVE = "Klyntar.KnullBond";

    private SymbioteState() {
    }

    public static int getScore(Player player, String objectiveName) {
        Objective objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        return player.getScoreboard()
                .getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
    }

    public static void setScore(Player player, String objectiveName, int value) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                    Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(value);
    }

    /**
     * La forma indossata ora, o stringa vuota se il giocatore non porta nessun simbionte.
     *
     * <p>La capability e' la prima fonte, ma non l'unica: chi riceve il potere direttamente da
     * Palladium — con la sua barra o con un comando — non passa mai per la trasformazione
     * nostra, e la capability resta vuota. In quel caso vale quello che dice Palladium.</p>
     */
    public static String forma(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return "";
        }
        String daCapability = player.getCapability(PlayerPowerCapability.PLAYER_POWER)
                .map(power -> power.isTransformed() ? power.getForm() : "")
                .orElse("");
        if (!daCapability.isEmpty()) {
            return daCapability;
        }
        if (player instanceof ServerPlayer giocatore) {
            String daPalladium = PlayerPowerCapability.formaSuPalladium(giocatore);
            return daPalladium == null ? "" : daPalladium;
        }
        return "";
    }

    public static boolean haSimbionte(LivingEntity entity) {
        return !forma(entity).isEmpty();
    }

    /** Anti-Venom e' l'unico che i suoi stessi veleni non toccano. */
    public static boolean isAntiVenom(LivingEntity entity) {
        return "antivenom".equals(forma(entity));
    }

    /** Indebolito da fuoco o da suono: in questa finestra i poteri passivi non lavorano. */
    public static boolean isVulnerabile(Player player) {
        return getScore(player, VULNERABILITY_OBJECTIVE) > 0;
    }

    /**
     * Crea l'obiettivo dell'affinita' se manca, a zero.
     *
     * <p>Serve perche' un obiettivo mai scritto non esiste: i comandi che lo interrogano
     * falliscono e le condizioni di Palladium che lo leggono non trovano niente.</p>
     */
    public static void assicuraAffinita(Player player) {
        assicuraObiettivo(player, AFFINITY_OBJECTIVE);
        assicuraObiettivo(player, KNULL_BOND_OBJECTIVE);
    }

    /** Crea un obiettivo a zero se non esiste ancora. */
    public static void assicuraObiettivo(Player player, String objectiveName) {
        if (player.getScoreboard().getObjective(objectiveName) == null) {
            setScore(player, objectiveName, 0);
        }
    }

    /**
     * L'affinita' col simbionte, da 0 a 100. Finche' l'albero degli avanzamenti non esiste
     * resta a zero, ma le soglie che ne dipendono la leggono gia' da qui.
     */
    public static int affinita(Player player) {
        return Math.max(0, Math.min(100, getScore(player, AFFINITY_OBJECTIVE)));
    }
}
