package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Symbiote Punch: il braccio diventa un tentacolo che finisce in un pugno, va a colpire quello che
 * il giocatore guarda e poi rientra (10/10/2026, al posto del braccio allungato e del proiettile
 * invisibile).
 *
 * <p>Il potere, al momento del colpo (il tick 8 di {@code venompunch}), mette {@value #RICHIESTA}
 * a 1; qui la richiesta si raccoglie, si cerca il bersaglio lungo lo sguardo, si manda ai client
 * il tentacolo ({@code PugnoPacket}, disegnato da {@code TentacoloPugno}) e il colpo arriva quando
 * la punta tocca il bersaglio, qualche tick dopo. Se non c'e' nessuno, il pugno si allunga fino al
 * blocco, o di {@value #PORTATA_VUOTO} blocchi, e torna indietro a vuoto.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class PugnoTentacolo {
    public static final String RICHIESTA = "Klyntar.Punch";
    /** Quanto arriva lontano il tentacolo. */
    private static final double PORTATA = 18.0D;
    /** Quanto si allunga quando non c'e' niente da colpire. */
    private static final double PORTATA_VUOTO = 8.0D;
    /** Quanto "largo" e' il colpo attorno alla linea dello sguardo. */
    private static final double TOLLERANZA = 0.6D;
    /** Dopo quanti tick la punta arriva al bersaglio (lo stesso tempo del disegno). */
    private static final int ARRIVO = 3;
    private static final float DANNO = 12.0F;
    private static final double SPINTA = 0.9D;

    private record Colpo(int bersaglio, int tick) {
    }

    private static final Map<UUID, Colpo> IN_VOLO = new ConcurrentHashMap<>();

    private PugnoTentacolo() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)) {
            return;
        }
        Colpo colpo = IN_VOLO.get(giocatore.getUUID());
        if (colpo != null) {
            if (colpo.tick() <= 1) {
                IN_VOLO.remove(giocatore.getUUID());
                arriva(giocatore, colpo.bersaglio());
            } else {
                IN_VOLO.put(giocatore.getUUID(), new Colpo(colpo.bersaglio(), colpo.tick() - 1));
            }
        }
        if (punteggio(giocatore) <= 0) {
            return;
        }
        azzera(giocatore);
        lancia(giocatore);
    }

    /** Il tentacolo parte: si cerca il bersaglio e lo si mostra a tutti quelli che vedono il giocatore. */
    private static void lancia(ServerPlayer giocatore) {
        Vec3 occhi = giocatore.getEyePosition();
        Vec3 sguardo = giocatore.getLookAngle();
        Vec3 fine = occhi.add(sguardo.scale(PORTATA));
        BlockHitResult muro = giocatore.level().clip(new ClipContext(occhi, fine, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, giocatore));
        if (muro.getType() != HitResult.Type.MISS) {
            fine = muro.getLocation();
        }
        LivingEntity bersaglio = null;
        double meglio = Double.MAX_VALUE;
        AABB corsia = new AABB(occhi, fine).inflate(TOLLERANZA + 1.0D);
        for (Entity e : giocatore.level().getEntities(giocatore, corsia, e -> colpibile(giocatore, e))) {
            Optional<Vec3> dove = e.getBoundingBox().inflate(TOLLERANZA).clip(occhi, fine);
            if (dove.isPresent()) {
                double d = occhi.distanceToSqr(dove.get());
                if (d < meglio) {
                    meglio = d;
                    bersaglio = (LivingEntity) e;
                }
            }
        }
        String forma = PlayerPowerCapability.formaSuPalladium(giocatore);
        if (bersaglio != null) {
            ModNetwork.mandaPugno(giocatore, bersaglio.getId(), bersaglio.position(), forma);
            IN_VOLO.put(giocatore.getUUID(), new Colpo(bersaglio.getId(), ARRIVO));
        } else {
            Vec3 vuoto = occhi.distanceTo(fine) > PORTATA_VUOTO ? occhi.add(sguardo.scale(PORTATA_VUOTO)) : fine;
            ModNetwork.mandaPugno(giocatore, -1, vuoto, forma);
        }
        giocatore.level().playSound(null, giocatore.getX(), giocatore.getY(), giocatore.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6F, 0.7F);
    }

    /** La punta tocca il bersaglio: danno e spinta, se e' ancora li'. */
    private static void arriva(ServerPlayer giocatore, int id) {
        if (!(giocatore.level().getEntity(id) instanceof LivingEntity bersaglio)
                || !colpibile(giocatore, bersaglio)
                || bersaglio.distanceTo(giocatore) > PORTATA + 3.0D) {
            return;
        }
        if (bersaglio.hurt(giocatore.damageSources().playerAttack(giocatore), DANNO)) {
            Vec3 via = bersaglio.position().subtract(giocatore.position()).multiply(1.0D, 0.0D, 1.0D);
            if (via.lengthSqr() > 1.0E-4D) {
                bersaglio.knockback(SPINTA, -via.x, -via.z);
            }
            if (giocatore.level() instanceof ServerLevel livello) {
                livello.playSound(null, bersaglio.getX(), bersaglio.getY(), bersaglio.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9F, 0.8F);
            }
        }
    }

    private static boolean colpibile(ServerPlayer giocatore, Entity e) {
        if (!(e instanceof LivingEntity vivo) || !vivo.isAlive() || e instanceof ArmorStand || e.isSpectator()) {
            return false;
        }
        if (e instanceof Player altro) {
            return PvpRules.colpibile(giocatore, altro);
        }
        return !giocatore.isAlliedTo(e);
    }

    private static int punteggio(Player giocatore) {
        Objective obiettivo = giocatore.getScoreboard().getObjective(RICHIESTA);
        if (obiettivo == null) {
            return 0;
        }
        return giocatore.getScoreboard().getOrCreatePlayerScore(giocatore.getScoreboardName(), obiettivo).getScore();
    }

    private static void azzera(Player giocatore) {
        Scoreboard tabellone = giocatore.getScoreboard();
        Objective obiettivo = tabellone.getObjective(RICHIESTA);
        if (obiettivo == null) {
            obiettivo = tabellone.addObjective(RICHIESTA, ObjectiveCriteria.DUMMY, Component.literal(RICHIESTA),
                    ObjectiveCriteria.RenderType.INTEGER);
        }
        tabellone.getOrCreatePlayerScore(giocatore.getScoreboardName(), obiettivo).setScore(0);
    }
}
