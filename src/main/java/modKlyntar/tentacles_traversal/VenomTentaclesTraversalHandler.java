package modKlyntar.tentacles_traversal;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomTentaclesTraversalHandler {
    /** Letto anche dal berserk, che accende la locomozione per scavalcare un ostacolo. */
    public static final String OBJECTIVE_NAME = "Venom.TentaclesTraversal";
    private static final double GRAB_RANGE = 30.0D;
    private static final int MIN_ARMS_FOR_TENTACLES_TRAVERSAL = 4;
    private static final int MAX_ARMS = 6;

    /**
     * Oltre questa distanza la presa si apre. E' piu' larga di {@link #GRAB_RANGE} di proposito:
     * un braccio si aggancia entro trenta blocchi ma non molla finche' non arriva a trentaquattro,
     * altrimenti sul filo dei trenta si aggrapperebbe e mollerebbe a tick alterni.
     */
    private static final double RILASCIO_DISTANZA = 34.0D;
    /** Sotto questa distanza il braccio e' ormai addosso all'appiglio e conviene cercarne un altro. */
    private static final double RILASCIO_VICINO = 1.5D;
    /**
     * Quanto puo' restare indietro un appiglio prima che la presa si apra, come coseno
     * dell'angolo fra la direzione di marcia e l'appiglio. -0.5 vale centoventi gradi: davanti e
     * ai lati si tiene, dietro le spalle si molla.
     */
    private static final double RILASCIO_COSENO = -0.5D;
    /** Due appigli piu' vicini di cosi' verrebbero disegnati uno sopra l'altro. */
    private static final double DISTANZA_MINIMA_FRA_APPIGLI_SQR = 2.25D;

    /** Un braccio per posto, e il posto si ricorda il suo appiglio fra un tick e l'altro. */
    private static final Map<UUID, Presa[]> PRESE = new ConcurrentHashMap<>();

    /**
     * Un appiglio: il punto sulla superficie e il blocco che lo regge.
     *
     * <p>Il blocco va tenuto da parte, non ridedotto dal punto. Il punto sta <i>sulla faccia</i>
     * del blocco, quindi ricavarne la cella con un passo di dieci centimetri e' una lotteria che
     * dipende dall'angolo del raggio, e sbagliandola si legge l'aria davanti alla parete invece
     * della pietra: la presa risultava sempre appesa al vuoto e si apriva a ogni tick.</p>
     */
    private record Presa(Vec3 punto, BlockPos blocco) {
    }

    private static final Logger LOGGER = LogManager.getLogger("KlyntarTraversal");

    private VenomTentaclesTraversalHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        int ticks = getTentaclesTraversalTicks(player);
        if (ticks <= 0) {
            PRESE.remove(player.getUUID());
            ModNetwork.syncVenomTentaclesTraversal(player, Collections.emptyList(), false);
            return;
        }
        tickTentaclesTraversal(player);
    }

    /** Chi esce dal mondo non lascia la sua presa nella mappa. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PRESE.remove(event.getEntity().getUUID());
    }

    private static void tickTentaclesTraversal(ServerPlayer player) {
        List<Vec3> anchors = aggiornaPresa(player);
        ModNetwork.syncVenomTentaclesTraversal(player, anchors);
        player.fallDistance = 0.0F;
    }

    /**
     * La presa: ogni braccio tiene il suo appiglio finche' regge, e solo quello che ha mollato
     * ne cerca un altro.
     *
     * <p>Prima gli appigli venivano ricalcolati da capo a ogni tick, tutti e sei, con dodici
     * raggi ricavati da dove guardava il giocatore: bastava girare la testa e le braccia
     * saltavano altrove. Non erano appigli, erano una fotografia rifatta di continuo. Adesso
     * l'appiglio e' uno stato che si apre per un motivo — fuori portata, arrivati addosso,
     * finito dietro le spalle, il blocco non c'e' piu' — e finche' nessuno di quei motivi
     * ricorre il braccio resta dov'e'.</p>
     *
     * <p>Il posto nell'elenco e' fisso, e conta: il renderer ricava da quell'indice la fase
     * dell'onda del tentacolo, quindi un braccio che resta al suo posto continua a ondeggiare
     * allo stesso modo invece di ripartire ogni volta.</p>
     */
    private static List<Vec3> aggiornaPresa(ServerPlayer player) {
        Presa[] presa = PRESE.computeIfAbsent(player.getUUID(), id -> new Presa[MAX_ARMS]);
        Vec3 posizione = player.position();
        Vec3 avanti = direzioneDiMarcia(player);

        for (int i = 0; i < presa.length; i++) {
            if (presa[i] != null && !reggeAncora(player, presa[i], posizione, avanti)) {
                presa[i] = null;
            }
        }

        List<Vec3> occupati = new ArrayList<>();
        for (Presa appiglio : presa) {
            if (appiglio != null) {
                occupati.add(appiglio.punto());
            }
        }

        // un raggio per ogni posto rimasto vuoto, non dodici per tutti
        Vec3[] direzioni = direzioniDiRicerca(player);
        for (int i = 0; i < presa.length && occupati.size() < MAX_ARMS; i++) {
            if (presa[i] != null) {
                continue;
            }
            for (int d = 0; d < direzioni.length; d++) {
                // ogni posto parte da una direzione diversa, cosi' le braccia si spartiscono
                // il ventaglio invece di accalcarsi tutte sullo stesso appiglio
                Vec3 direzione = direzioni[(i + d) % direzioni.length];
                Presa candidato = tryRaycast(player, direzione, GRAB_RANGE);
                if (candidato != null && !isTooCloseToExistingAnchor(occupati, candidato.punto())) {
                    presa[i] = candidato;
                    occupati.add(candidato.punto());
                    break;
                }
            }
        }

        // per posto, con null dove il braccio non ha presa: il client riconosce ogni braccio
        // dal suo posto, e se la lista si compattasse il braccio numero tre diventerebbe il
        // numero due appena il secondo molla, cambiando spalla da un fotogramma all'altro
        List<Vec3> anchors = new ArrayList<>(MAX_ARMS);
        int aggrappati = 0;
        for (Presa appiglio : presa) {
            anchors.add(appiglio == null ? null : appiglio.punto());
            if (appiglio != null) {
                aggrappati++;
            }
        }
        if (aggrappati < MIN_ARMS_FOR_TENTACLES_TRAVERSAL) {
            LOGGER.debug("Traversal of {}: only {} arms gripping", player.getGameProfile().getName(), aggrappati);
        }
        return anchors;
    }

    /** Se questo appiglio vale ancora. Un motivo per aprirsi, non l'abitudine di rifare tutto. */
    private static boolean reggeAncora(ServerPlayer player, Presa appiglioPresa, Vec3 posizione, Vec3 avanti) {
        Vec3 appiglio = appiglioPresa.punto();
        double distanzaSqr = appiglio.distanceToSqr(posizione);
        if (distanzaSqr > RILASCIO_DISTANZA * RILASCIO_DISTANZA || distanzaSqr < RILASCIO_VICINO * RILASCIO_VICINO) {
            return false;
        }
        Vec3 verso = appiglio.subtract(posizione);
        if (verso.lengthSqr() > 1.0E-4D && verso.normalize().dot(avanti) < RILASCIO_COSENO) {
            return false;   // rimasto dietro le spalle: il braccio dovrebbe passare attraverso il corpo
        }
        // il blocco potrebbe non esserci piu': lo si e' scavato, o e' caduto
        return !player.level().getBlockState(appiglioPresa.blocco()).isAir();
    }

    /** Dove sta andando: la velocita' se si muove, altrimenti dove guarda. */
    private static Vec3 direzioneDiMarcia(ServerPlayer player) {
        Vec3 velocita = player.getDeltaMovement();
        if (velocita.lengthSqr() > 0.01D) {
            return velocita.normalize();
        }
        return safeNormalize(player.getLookAngle(), new Vec3(0.0D, 0.0D, 1.0D));
    }

    private static Vec3[] direzioniDiRicerca(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement();
        Vec3 horizontalLook = safeNormalize(new Vec3(look.x, 0.0D, look.z), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 strafe = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        return new Vec3[] {
                safeNormalize(new Vec3(look.x, look.y + 0.3D, look.z), horizontalLook),
                safeNormalize(new Vec3(horizontalLook.x, 0.55D, horizontalLook.z), horizontalLook),
                safeNormalize(new Vec3(horizontalLook.x, -0.35D, horizontalLook.z), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(0.9D)).add(0.0D, 0.15D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(-0.9D)).add(0.0D, 0.15D, 0.0D), horizontalLook),
                safeNormalize(new Vec3(velocity.x * 1.5D + horizontalLook.x, velocity.y + 0.2D, velocity.z * 1.5D + horizontalLook.z), horizontalLook),
                safeNormalize(strafe.add(0.0D, 0.25D, 0.0D), strafe),
                safeNormalize(strafe.scale(-1.0D).add(0.0D, 0.25D, 0.0D), strafe.scale(-1.0D)),
                safeNormalize(horizontalLook.add(strafe.scale(1.35D)).add(0.0D, 0.65D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(-1.35D)).add(0.0D, 0.65D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(1.35D)).add(0.0D, -0.45D, 0.0D), horizontalLook),
                safeNormalize(horizontalLook.add(strafe.scale(-1.35D)).add(0.0D, -0.45D, 0.0D), horizontalLook)
        };
    }

    private static Presa tryRaycast(ServerPlayer player, Vec3 direction, double range) {
        Vec3 start = player.getEyePosition().add(0.0D, -0.35D, 0.0D);
        Vec3 end = start.add(direction.normalize().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockState state = player.level().getBlockState(hit.getBlockPos());
        if (state.isAir() || state.getCollisionShape(player.level(), hit.getBlockPos()).isEmpty()) {
            return null;
        }
        return new Presa(hit.getLocation(), hit.getBlockPos());
    }

    private static boolean isTooCloseToExistingAnchor(List<Vec3> anchors, Vec3 candidate) {
        for (Vec3 anchor : anchors) {
            if (anchor.distanceToSqr(candidate) < DISTANZA_MINIMA_FRA_APPIGLI_SQR) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() < 1.0E-4D) {
            return fallback;
        }
        return vector.normalize();
    }

    private static int getTentaclesTraversalTicks(ServerPlayer player) {
        Objective objective = getObjective(player, false);
        if (objective == null) {
            return 0;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static Objective getObjective(Player player, boolean create) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null && create) {
            objective = scoreboard.addObjective(OBJECTIVE_NAME, ObjectiveCriteria.DUMMY, Component.literal(OBJECTIVE_NAME), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }
}
