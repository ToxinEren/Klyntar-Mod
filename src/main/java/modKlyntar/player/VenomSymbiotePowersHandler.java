package modKlyntar.player;

import com.mojang.logging.LogUtils;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.AntivenomBombEntity;
import modKlyntar.network.ModNetwork;
import modKlyntar.symbiote.ColpoLocalizzato;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I cinque poteri simbionte dello Spider-Man pack portati su Palladium.
 *
 * <p>Ogni potere e' un'abilita' {@code palladium:command} nella barra di Venom che si limita a
 * mettere a 1 un obiettivo di richiesta; qui la richiesta viene consumata, si controlla la
 * ricarica e parte una sequenza a fasi. I tempi ricalcano quelli degli script del pack.</p>
 *
 * <p>Pull e Strike disegnano i tentacoli riusando {@link ModNetwork#syncVenomCombatTargets},
 * lo stesso canale di Fend Off Enemies: i punti mandati al client diventano braccia simbiotiche.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomSymbiotePowersHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** richieste scritte dalle abilita' Palladium, consumate qui */
    public static final String PULL_REQUEST = "Venom.Sym.Pull";
    public static final String STRIKE_REQUEST = "Venom.Sym.Strike";
    public static final String TEMPEST_REQUEST = "Venom.Sym.Tempest";
    public static final String BOMB_REQUEST = "Venom.Sym.Bomb";
    public static final String BLAST_REQUEST = "Venom.Sym.Blast";
    public static final String RAGE_REQUEST = "Venom.Sym.Rage";
    /** 1 mentre la furia e' attiva: le altre ricariche scalano piu' in fretta */
    public static final String RAGE_ACTIVE = "Venom.Sym.Rage.Active";
    /** quale animazione del pack deve partire: 1 pull, 2 strike, 3 tempest, 4 bomb, 5 blast, 6 rage */
    public static final String ANIM_OBJECTIVE = "Venom.Sym.Anim";
    /** per quanti tick resta alzato l'obiettivo dell'animazione */
    private static final int ANIM_TICKS = 3;
    /** 1 finche' una animazione simbionte e' in scena: zittisce idle, walk e run */
    public static final String ANIM_PLAYING = "Venom.Sym.Anim.Playing";
    /**
     * 1 per tutta la sequenza della bomba: accende il layer dei tendini.
     *
     * <p>Non basta {@link #ANIM_OBJECTIVE}, che resta alzato solo tre tick perche' serve a far
     * scattare il trigger dell'animazione, non a tenere acceso un layer.</p>
     */
    public static final String BOMB_TENDRILS = "Venom.Sym.Bomb.Tendrils";
    /** durata in tick di ogni animazione portata dal pack, nell'ordine degli indici 1..6 */
    private static final int[] ANIM_DURATA = {40, 55, 50, 40, 30, 30};

    private static final String CAMERA_OBJECTIVE = VenomCameraHeightHandler.OBJECTIVE_NAME;

    // --- Symbiont Pull: aggancia, solleva e schianta ---
    private static final double PULL_RANGE = 12.0D;
    private static final int PULL_MAX_TARGETS = 10;
    private static final int PULL_CAPTURE_TICK = 4;
    private static final int PULL_LIFT_TICK = 10;
    private static final int PULL_SLAM_TICK = 25;
    private static final int PULL_IMPACT_TICK = 28;
    private static final int PULL_END_TICK = 32;
    private static final double PULL_LIFT_HEIGHT = 5.0D;
    private static final float PULL_DAMAGE = 28.0F;
    private static final int PULL_COOLDOWN = 0;   // nessuna ricarica

    // --- Symbiote Strike: si alza, si lancia sul bersaglio, tre colpi e la stoccata ---
    private static final double STRIKE_RANGE = 9.0D;
    private static final int STRIKE_MAX_TARGETS = 8;
    private static final int STRIKE_RISE_TICKS = 20;
    private static final int STRIKE_DASH_TICK = 21;
    /** i colpi cadono a tick fissi, non quando capita di essere vicini */
    private static final int[] STRIKE_HIT_TICKS = {26, 32, 38};
    private static final int STRIKE_FINAL_TICK = 44;
    private static final int STRIKE_END_TICK = 48;
    private static final double STRIKE_RISE_POWER = 0.22D;
    private static final double STRIKE_DASH_SPEED = 0.9D;
    private static final double STRIKE_DASH_VERTICAL = 0.05D;
    private static final double STRIKE_DASH_STOP = 2.4D;
    /** i bersagli vengono tirati davanti al giocatore, non restano dove sono */
    private static final double STRIKE_POINT_DISTANCE = 3.2D;
    private static final double STRIKE_POINT_HEIGHT = 1.2D;
    private static final double STRIKE_PULL_STRENGTH = 0.45D;
    private static final double STRIKE_FINAL_KNOCKBACK = 1.2D;
    private static final double STRIKE_FINAL_VERTICAL = 0.9D;
    private static final float STRIKE_SMALL_DAMAGE = 5.0F;
    private static final float STRIKE_FINAL_DAMAGE = 24.0F;
    /** quanto avanti arriva la mano nel momento del colpo: il punto d'aggancio piu' il braccio */
    private static final double STRIKE_HAND_REACH = STRIKE_POINT_DISTANCE + 1.2D;
    /** quanto e' grosso il pugno del simbionte */
    private static final double STRIKE_HAND_THICKNESS = 1.1D;
    private static final int STRIKE_COOLDOWN = 0;   // nessuna ricarica

    // --- Antivenom Tempest: solleva in aria il giocatore e chi gli sta attorno ---
    private static final double TEMPEST_RADIUS = 8.0D;
    private static final float TEMPEST_DAMAGE = 26.0F;
    private static final int TEMPEST_LIFT_TICK = 5;
    private static final int TEMPEST_SLOW_FALL_TICK = 20;
    private static final int TEMPEST_SECOND_LIFT_TICK = 30;
    private static final int TEMPEST_DAMAGE_TICK = 40;
    private static final int TEMPEST_END_TICK = 43;
    /** il pack usa levitazione di grado 14 per un secondo: e' quella che porta tutti in alto */
    private static final int TEMPEST_LIFT_LEVEL = 14;
    private static final int TEMPEST_LIFT_DURATION = 20;
    private static final int TEMPEST_SECOND_LIFT_LEVEL = 1;
    private static final double TEMPEST_KNOCKBACK = 1.1D;
    private static final double TEMPEST_KNOCKBACK_VERTICAL = 0.5D;
    private static final int TEMPEST_COOLDOWN = 0;   // nessuna ricarica

    // --- Antivenom Bomb ---
    private static final double BOMB_SPEED = 1.8D;
    /**
     * Il lancio cade dove il braccio scatta davvero in avanti: fra il tick 18 e il 22 la
     * rotazione passa da +10 a -107 gradi. Lo script del pack dice 16, ma i suoi tick e quelli
     * dell'animazione non coincidono, e a schermo comanda l'animazione: fino a li' la bomba
     * resta posata sulla mano.
     */
    private static final int BOMB_THROW_TICK = 22;
    /** la sequenza copre l'animazione intera, 2 secondi */
    private static final int BOMB_END_TICK = 40;
    /**
     * Da dove esce la bomba, misurato nel sistema di riferimento del giocatore e in frazioni
     * della sua statura, cosi' resta giusto qualunque taglia abbia Venom.
     *
     * <p>Al tick del lancio l'animazione del pack tiene il braccio destro ruotato di circa 98
     * gradi sull'asse Z e 27 sull'asse Y: la mano finisce all'altezza della spalla, spostata di
     * lato e in avanti. Questi tre numeri descrivono quel punto.</p>
     */
    private static final double BOMB_HAND_UP = 0.983D;
    /**
     * Spostamento laterale della mano, positivo verso destra. Il calcolo sull'animazione dava
     * il lato opposto: la corrispondenza fra il segno di X nel modello e il lato del giocatore
     * era invertita rispetto a come l'avevo assunta, e in gioco si vedeva subito.
     */
    private static final double BOMB_HAND_SIDE = -0.170D;
    private static final double BOMB_HAND_FORWARD = 0.135D;
    private static final double BOMB_BLAST_RADIUS = 4.0D;
    private static final float BOMB_DAMAGE = 18.0F;
    private static final int BOMB_COOLDOWN = 0;   // nessuna ricarica

    // --- Symbiote Blast: due detonazioni ravvicinate attorno al giocatore ---
    private static final double BLAST_RADIUS = 6.0D;
    private static final float BLAST_DAMAGE = 30.0F;
    private static final int BLAST_FIRST_TICK = 5;
    private static final int BLAST_SECOND_TICK = 10;
    private static final int BLAST_END_TICK = 14;
    private static final int BLAST_COOLDOWN = 0;   // nessuna ricarica

    // --- Rage Mode ---
    private static final int RAGE_DURATION = 20 * 20;
    private static final int RAGE_COOLDOWN = 0;   // nessuna ricarica
    private static final int RAGE_COOLDOWN_MULTIPLIER = 4;

    private static final Map<UUID, PowerState> STATES = new ConcurrentHashMap<>();

    private VenomSymbiotePowersHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        PowerState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PowerState());

        if (getScore(player, CAMERA_OBJECTIVE) <= 0) {
            // fuori da Venom nessun potere resta in piedi
            if (state.anyRunning()) {
                stopAll(player, state);
            }
            return;
        }

        tickRage(player, state);
        tickAnim(player, state);
        tickCooldowns(state, isRaging(player));

        consumeRequests(player, state);

        tickPull(player, state);
        tickStrike(player, state);
        tickTempest(player, state);
        tickBomb(player, state);
        tickBlast(player, state);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.getUUID());
        }
    }

    // ------------------------------------------------------------------ richieste

    /** alza l'obiettivo che fa scattare l'animazione Bedrock, poi si spegne da solo */
    private static void playAnim(ServerPlayer player, PowerState state, int indice) {
        setScore(player, ANIM_OBJECTIVE, indice);
        state.animTicks = ANIM_TICKS;
        // le animazioni simbionte stanno sul controller venom_actions insieme a idle, walk e
        // run: senza questo quelli continuerebbero a pulsare e le coprirebbero
        state.animHold = ANIM_DURATA[indice - 1];
        setScore(player, ANIM_PLAYING, 1);
    }

    private static void tickAnim(ServerPlayer player, PowerState state) {
        if (state.animTicks > 0 && --state.animTicks == 0) {
            setScore(player, ANIM_OBJECTIVE, 0);
        }
        if (state.animHold > 0 && --state.animHold == 0) {
            setScore(player, ANIM_PLAYING, 0);
        }
    }

    private static void consumeRequests(ServerPlayer player, PowerState state) {
        if (consume(player, PULL_REQUEST) && state.pullTick < 0 && state.pullCooldown <= 0) {
            state.pullTick = 0;
            state.pullCooldown = PULL_COOLDOWN;
            playAnim(player, state, 1);
            state.captured.clear();
        }
        if (consume(player, STRIKE_REQUEST) && state.strikeTick < 0 && state.strikeCooldown <= 0) {
            state.strikeTick = 0;
            state.strikeCooldown = STRIKE_COOLDOWN;
            playAnim(player, state, 2);
            state.struck.clear();
        }
        if (consume(player, TEMPEST_REQUEST) && state.tempestTick < 0 && state.tempestCooldown <= 0) {
            state.tempestTick = 0;
            state.tempestCooldown = TEMPEST_COOLDOWN;
            playAnim(player, state, 3);
        }
        if (consume(player, BOMB_REQUEST) && state.bombTick < 0 && state.bombCooldown <= 0) {
            state.bombTick = 0;
            state.bombCooldown = BOMB_COOLDOWN;
            playAnim(player, state, 4);
        }
        if (consume(player, BLAST_REQUEST) && state.blastTick < 0 && state.blastCooldown <= 0) {
            state.blastTick = 0;
            state.blastCooldown = BLAST_COOLDOWN;
            playAnim(player, state, 5);
        }
        if (consume(player, RAGE_REQUEST) && state.rageTicks <= 0 && state.rageCooldown <= 0) {
            startRage(player, state);
            playAnim(player, state, 6);
        }
    }

    /** legge la richiesta e la azzera: l'abilita' Palladium la rialza a ogni pressione */
    private static boolean consume(ServerPlayer player, String objective) {
        if (getScore(player, objective) <= 0) {
            return false;
        }
        setScore(player, objective, 0);
        LOGGER.info("Sym: richiesta {} ricevuta", objective);
        return true;
    }

    // ------------------------------------------------------------------ Symbiont Pull

    private static void tickPull(ServerPlayer player, PowerState state) {
        if (state.pullTick < 0) {
            return;
        }
        int t = state.pullTick++;

        if (t < PULL_CAPTURE_TICK) {
            // i tentacoli escono e cercano: i bersagli non sono ancora bloccati
            sendTentacles(player, centers(findTargets(player, PULL_RANGE, PULL_MAX_TARGETS)));
            return;
        }

        if (t == PULL_CAPTURE_TICK) {
            for (LivingEntity target : findTargets(player, PULL_RANGE, PULL_MAX_TARGETS)) {
                state.captured.add(target);
                state.captureY.add(target.getY());
            }
        }

        state.captured.removeIf(e -> e == null || !e.isAlive());
        if (state.captured.isEmpty()) {
            endPull(player, state);
            return;
        }

        sendTentacles(player, centers(state.captured));

        if (t >= PULL_LIFT_TICK && t < PULL_SLAM_TICK) {
            for (int i = 0; i < state.captured.size(); i++) {
                LivingEntity target = state.captured.get(i);
                double bersaglio = state.captureY.get(Math.min(i, state.captureY.size() - 1)) + PULL_LIFT_HEIGHT;
                if (target.getY() < bersaglio) {
                    target.setDeltaMovement(target.getDeltaMovement().x, 0.42D, target.getDeltaMovement().z);
                    target.hurtMarked = true;
                }
                target.fallDistance = 0.0F;
            }
        } else if (t >= PULL_SLAM_TICK && t < PULL_IMPACT_TICK) {
            for (LivingEntity target : state.captured) {
                target.setDeltaMovement(target.getDeltaMovement().x, -1.6D, target.getDeltaMovement().z);
                target.hurtMarked = true;
            }
        } else if (t == PULL_IMPACT_TICK) {
            // solo chi il tendine sta ancora tenendo: chi si e' allontanato non prende lo schianto
            for (LivingEntity target : ColpoLocalizzato.soloRaggiunti(player, state.captured, PULL_RANGE)) {
                target.hurt(player.damageSources().playerAttack(player), PULL_DAMAGE);
                spawnBurst(player, target.position(), 30);
            }
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.4F);
        }

        if (t >= PULL_END_TICK) {
            endPull(player, state);
        }
    }

    private static void endPull(ServerPlayer player, PowerState state) {
        state.pullTick = -1;
        state.captured.clear();
        state.captureY.clear();
        sendTentacles(player, List.of());
    }

    // ------------------------------------------------------------------ Symbiote Strike

    private static void tickStrike(ServerPlayer player, PowerState state) {
        if (state.strikeTick < 0) {
            return;
        }
        int t = state.strikeTick++;

        if (t == 0) {
            // i bersagli si fissano all'inizio: nel pack la sequenza li tiene, non li ricerca
            state.captured.addAll(findTargets(player, STRIKE_RANGE, STRIKE_MAX_TARGETS));
        }
        state.captured.removeIf(e -> e == null || !e.isAlive());
        if (state.captured.isEmpty()) {
            endStrike(player, state);
            return;
        }
        sendTentacles(player, centers(state.captured));
        player.fallDistance = 0.0F;

        if (t < STRIKE_RISE_TICKS) {
            // i tentacoli tirano su il giocatore per caricare lo slancio
            player.setDeltaMovement(player.getDeltaMovement().x, STRIKE_RISE_POWER,
                    player.getDeltaMovement().z);
            player.hurtMarked = true;
            return;
        }

        if (t >= STRIKE_DASH_TICK && t < STRIKE_FINAL_TICK) {
            dashTowards(player, state.captured.get(0));
            pullToStrikePoint(player, state.captured);
        }

        for (int hitTick : STRIKE_HIT_TICKS) {
            if (t == hitTick) {
                // il colpo lo tira la mano: prende chi e' davvero sotto il braccio
                for (LivingEntity target : ColpoLocalizzato.soloToccati(player, state.captured,
                        STRIKE_HAND_REACH, STRIKE_HAND_THICKNESS)) {
                    target.hurt(player.damageSources().playerAttack(player), STRIKE_SMALL_DAMAGE);
                    spawnBurst(player, target.position(), 12);
                }
                player.level().playSound(null, player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.7F);
            }
        }

        if (t == STRIKE_FINAL_TICK) {
            Vec3 sguardo = player.getLookAngle();
            for (LivingEntity target : ColpoLocalizzato.soloToccati(player, state.captured,
                    STRIKE_HAND_REACH, STRIKE_HAND_THICKNESS)) {
                target.hurt(player.damageSources().playerAttack(player), STRIKE_FINAL_DAMAGE);
                // la stoccata li spara nella direzione in cui guardi, non li allontana e basta
                target.setDeltaMovement(sguardo.x * STRIKE_FINAL_KNOCKBACK, STRIKE_FINAL_VERTICAL,
                        sguardo.z * STRIKE_FINAL_KNOCKBACK);
                target.hasImpulse = true;
                target.hurtMarked = true;
                spawnBurst(player, target.position(), 40);
            }
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
        }

        if (t >= STRIKE_END_TICK) {
            endStrike(player, state);
        }
    }

    private static void endStrike(ServerPlayer player, PowerState state) {
        state.strikeTick = -1;
        state.captured.clear();
        state.struck.clear();
        sendTentacles(player, List.of());
    }

    /** il giocatore si lancia sul bersaglio e si ferma poco prima di entrarci dentro */
    private static void dashTowards(ServerPlayer player, LivingEntity target) {
        Vec3 verso = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D));
        if (verso.length() <= STRIKE_DASH_STOP) {
            return;
        }
        Vec3 dir = verso.normalize().scale(STRIKE_DASH_SPEED);
        player.setDeltaMovement(dir.x, dir.y + STRIKE_DASH_VERTICAL, dir.z);
        player.hurtMarked = true;
    }

    /** i tentacoli trascinano i nemici in un punto davanti al giocatore */
    private static void pullToStrikePoint(ServerPlayer player, List<LivingEntity> targets) {
        Vec3 sguardo = player.getLookAngle();
        Vec3 punto = player.position()
                .add(sguardo.x * STRIKE_POINT_DISTANCE, STRIKE_POINT_HEIGHT, sguardo.z * STRIKE_POINT_DISTANCE);
        for (LivingEntity target : targets) {
            Vec3 verso = punto.subtract(target.position());
            if (verso.lengthSqr() < 0.04D) {
                continue;
            }
            Vec3 tiro = verso.normalize().scale(STRIKE_PULL_STRENGTH);
            target.setDeltaMovement(tiro);
            target.hasImpulse = true;
            target.hurtMarked = true;
            target.fallDistance = 0.0F;
        }
    }

    // ------------------------------------------------------------------ Antivenom Tempest

    private static void tickTempest(ServerPlayer player, PowerState state) {
        if (state.tempestTick < 0) {
            return;
        }
        int t = state.tempestTick++;
        List<LivingEntity> targets = findTargets(player, TEMPEST_RADIUS, 32);

        if (t == TEMPEST_LIFT_TICK) {
            // il giocatore sale insieme ai nemici: e' il cuore dell'abilita'
            levita(player, TEMPEST_LIFT_LEVEL);
            for (LivingEntity target : targets) {
                levita(target, TEMPEST_LIFT_LEVEL);
            }
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.3F);
        }

        if (t == TEMPEST_SLOW_FALL_TICK) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            }
        }

        if (t == TEMPEST_SECOND_LIFT_TICK) {
            levita(player, TEMPEST_SECOND_LIFT_LEVEL);
            for (LivingEntity target : targets) {
                levita(target, TEMPEST_LIFT_LEVEL);
            }
        }

        if (t == TEMPEST_DAMAGE_TICK) {
            // la tempesta di Anti-Venom avvelena i simbionti che investe
            AntiVenomEffectHandler.colpisci(targets, player);
            levita(player, TEMPEST_SECOND_LIFT_LEVEL);
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().playerAttack(player), TEMPEST_DAMAGE);
                pushAway(player, target, TEMPEST_KNOCKBACK, TEMPEST_KNOCKBACK_VERTICAL);
            }
            symbioteBurst(player, player.position().add(0.0D, 1.0D, 0.0D), TEMPEST_RADIUS, 140);
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.9F);
        }

        spawnRing(player, t);

        if (t >= TEMPEST_END_TICK) {
            state.tempestTick = -1;
        }
    }

    /** levitazione come nel pack: dura poco ma di grado alto, cosi' strappa via da terra */
    private static void levita(LivingEntity chi, int grado) {
        chi.addEffect(new MobEffectInstance(MobEffects.LEVITATION, TEMPEST_LIFT_DURATION,
                grado, false, false));
        chi.fallDistance = 0.0F;
    }

    // ------------------------------------------------------------------ Antivenom Bomb

    /**
     * La bomba resta in mano per tutta la posa e parte quando animation.venom.antivenom_bomb
     * porta il braccio in avanti, fra il tick 20 e il 25.
     */
    private static void tickBomb(ServerPlayer player, PowerState state) {
        if (state.bombTick < 0) {
            return;
        }
        int t = state.bombTick++;

        if (t == 0) {
            setScore(player, BOMB_TENDRILS, 1);
        }
        if (t == 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
        }
        if (t == BOMB_THROW_TICK) {
            // finche' sta in mano la bomba sono i tendini innestati sul pugno, che spariscono
            // al tick 21: l'entita' nasce qui, un tick dopo, e parte subito
            AntivenomBombEntity bomba = new AntivenomBombEntity(player.level(), player);
            player.level().addFreshEntity(bomba);
            bomba.lancia(player.getLookAngle(), BOMB_SPEED);
            state.bomba = null;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SNOWBALL_THROW,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.6F);
        }
        if (t >= BOMB_END_TICK) {
            state.bombTick = -1;
            setScore(player, BOMB_TENDRILS, 0);
        }
    }

    /** la mano destra al momento del lancio, con rientro se finirebbe dentro un blocco */
    public static Vec3 puntoDiLancio(LivingEntity chi) {
        double statura = chi.getBbHeight();
        // il braccio segue il corpo, non lo sguardo: la mano non deve spostarsi se alzi la testa
        double imbardata = Math.toRadians(chi.yBodyRot);
        Vec3 avanti = new Vec3(-Math.sin(imbardata), 0.0D, Math.cos(imbardata));
        // guardando a sud (imbardata 0) avanti e' +Z; questo versore punta a sinistra del giocatore
        Vec3 lato = new Vec3(avanti.z, 0.0D, -avanti.x);

        Vec3 spalla = chi.position().add(0.0D, statura * BOMB_HAND_UP, 0.0D);
        Vec3 mano = spalla
                .add(lato.scale(statura * BOMB_HAND_SIDE))
                .add(avanti.scale(statura * BOMB_HAND_FORWARD));

        BlockHitResult ostacolo = chi.level().clip(new ClipContext(spalla, mano,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, chi));
        if (ostacolo.getType() == HitResult.Type.MISS) {
            return mano;
        }
        // muro addosso: si nasce appena prima, o la bomba esplode nello stesso tick in cui nasce
        return ostacolo.getLocation().subtract(mano.subtract(spalla).normalize().scale(0.3D));
    }

    /** invocata dall'entita' bomba quando tocca qualcosa */
    public static void detonateBomb(net.minecraft.world.level.Level level,
                                    net.minecraft.world.entity.Entity lanciatore, Vec3 centro) {
        if (!(lanciatore instanceof ServerPlayer player)) {
            return;
        }
        detonate(player, centro);
    }

    private static void detonate(ServerPlayer player, Vec3 centro) {
        AABB area = new AABB(centro, centro).inflate(BOMB_BLAST_RADIUS);
        List<LivingEntity> investiti = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator());
        for (LivingEntity target : investiti) {
            target.hurt(player.damageSources().playerAttack(player), BOMB_DAMAGE);
            pushAway(player, target, 0.7D, 0.3D);
        }
        // la bomba di Anti-Venom avvelena i simbionti presi dallo scoppio
        AntiVenomEffectHandler.colpisci(investiti, player);
        if (player.level() instanceof ServerLevel level) {
            // stesse emissioni del pack: due schizzi e tre sbuffi, ad altezze diverse
            level.sendParticles(MyMod.SYMBIOTE_SPLASH.get(), centro.x, centro.y + 1.0D, centro.z,
                    10, 0.3D, 0.3D, 0.3D, 0.0D);
            level.sendParticles(MyMod.SYMBIOTE_SPLASH.get(), centro.x, centro.y + 0.1D, centro.z,
                    10, 0.3D, 0.2D, 0.3D, 0.0D);
            for (double h : new double[]{0.75D, 1.25D, 1.75D}) {
                level.sendParticles(MyMod.ANTIVENOM_PARTICLE.get(), centro.x, centro.y + h, centro.z,
                        90, BOMB_BLAST_RADIUS * 0.3D, BOMB_BLAST_RADIUS * 0.2D,
                        BOMB_BLAST_RADIUS * 0.3D, 0.0D);
            }
        }
        player.level().playSound(null, net.minecraft.core.BlockPos.containing(centro),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.6F);
    }

    // ------------------------------------------------------------------ Symbiote Blast

    private static void tickBlast(ServerPlayer player, PowerState state) {
        if (state.blastTick < 0) {
            return;
        }
        int t = state.blastTick++;

        if (t == BLAST_FIRST_TICK || t == BLAST_SECOND_TICK) {
            // la seconda detonazione spinge piu' forte della prima, come nel pack
            boolean seconda = t == BLAST_SECOND_TICK;
            Vec3 centro = player.position().add(0.0D, 1.0D, 0.0D);
            for (LivingEntity target : findTargets(player, BLAST_RADIUS, 32)) {
                target.hurt(player.damageSources().playerAttack(player), BLAST_DAMAGE);
                pushAway(player, target, seconda ? 1.0D : 0.8D, seconda ? 0.6D : 0.45D);
            }
            symbioteBurst(player, centro, BLAST_RADIUS, seconda ? 160 : 120);
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, seconda ? 0.8F : 1.1F);
        }

        if (t >= BLAST_END_TICK) {
            state.blastTick = -1;
        }
    }

    // ------------------------------------------------------------------ Rage Mode

    private static void startRage(ServerPlayer player, PowerState state) {
        state.rageTicks = RAGE_DURATION;
        state.rageCooldown = RAGE_COOLDOWN;
        setScore(player, RAGE_ACTIVE, 1);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, RAGE_DURATION, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, RAGE_DURATION * 2, 3, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, RAGE_DURATION, 1, false, true));
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.WARDEN_ROAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private static void tickRage(ServerPlayer player, PowerState state) {
        if (state.rageTicks <= 0) {
            return;
        }
        if (--state.rageTicks <= 0) {
            setScore(player, RAGE_ACTIVE, 0);
        }
    }

    private static boolean isRaging(ServerPlayer player) {
        return getScore(player, RAGE_ACTIVE) > 0;
    }

    /** in furia le ricariche scalano quattro volte piu' in fretta, come nel pack */
    private static void tickCooldowns(PowerState state, boolean raging) {
        int passo = raging ? RAGE_COOLDOWN_MULTIPLIER : 1;
        state.pullCooldown = Math.max(0, state.pullCooldown - passo);
        state.strikeCooldown = Math.max(0, state.strikeCooldown - passo);
        state.tempestCooldown = Math.max(0, state.tempestCooldown - passo);
        state.bombCooldown = Math.max(0, state.bombCooldown - passo);
        state.blastCooldown = Math.max(0, state.blastCooldown - passo);
        state.rageCooldown = Math.max(0, state.rageCooldown - 1);
    }

    // ------------------------------------------------------------------ utilita'

    /**
     * Vero mentre Pull o Strike hanno braccia in scena. Il canale dei tentacoli e' condiviso,
     * e chi lo svuota deve sapere di non calpestarle.
     */
    public static boolean isShowingTentacles(ServerPlayer player) {
        PowerState state = STATES.get(player.getUUID());
        return state != null && (state.pullTick >= 0 || state.strikeTick >= 0);
    }

    private static void stopAll(ServerPlayer player, PowerState state) {
        state.pullTick = -1;
        state.strikeTick = -1;
        state.tempestTick = -1;
        state.blastTick = -1;
        state.captured.clear();
        state.captureY.clear();
        state.struck.clear();
        state.animHold = 0;
        setScore(player, ANIM_PLAYING, 0);
        setScore(player, ANIM_OBJECTIVE, 0);
        setScore(player, BOMB_TENDRILS, 0);
        sendTentacles(player, List.of());
    }

    private static List<LivingEntity> findTargets(ServerPlayer player, double range, int max) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != null && e.isAlive() && !e.isSpectator() && e != player
                        && e.distanceToSqr(player) <= range * range
                        && player.hasLineOfSight(e)
                        && (e instanceof Enemy || (e instanceof Mob mob && mob.getTarget() == player)));
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (targets.isEmpty()) {
            LOGGER.info("Sym: nessun bersaglio entro {} blocchi", range);
        }
        return targets.size() > max ? new ArrayList<>(targets.subList(0, max)) : targets;
    }

    private static List<Vec3> centers(List<LivingEntity> targets) {
        List<Vec3> punti = new ArrayList<>(targets.size());
        for (LivingEntity t : targets) {
            punti.add(t.position().add(0.0D, t.getBbHeight() * 0.55D, 0.0D));
        }
        return punti;
    }

    private static void sendTentacles(ServerPlayer player, List<Vec3> punti) {
        ModNetwork.syncVenomCombatTargets(player, punti);
    }

    private static void pushAway(ServerPlayer player, LivingEntity target, double forza, double su) {
        Vec3 spinta = target.position().subtract(player.position());
        if (spinta.lengthSqr() < 1.0E-4D) {
            spinta = player.getLookAngle();
        }
        spinta = spinta.normalize().scale(forza);
        target.setDeltaMovement(target.getDeltaMovement().add(spinta.x, su, spinta.z));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    /**
     * Sbuffo simbiotico. Nel pack basta una chiamata perche' {@code spidermanaddon:venom_particle}
     * e' un emettitore Bedrock completo; qui l'effetto va composto a mano, sovrapponendo inchiostro
     * nero per la massa, anime sculk per i filamenti e fumo per il volume.
     */
    private static void spawnBurst(ServerPlayer player, Vec3 dove, int quante) {
        symbioteBurst(player, dove, 0.8D, quante);
    }

    private static void symbioteBurst(ServerPlayer player, Vec3 centro, double raggio, int quante) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // l'emettitore Bedrock e' un punto che spara in tutte le direzioni: qui lo si ottiene
        // mandando le particelle ferme, e' la particella stessa a darsi una direzione a caso
        double sparsi = raggio * 0.35D;
        level.sendParticles(MyMod.VENOM_PARTICLE.get(), centro.x, centro.y + 0.4D, centro.z,
                quante, sparsi, sparsi * 0.6D, sparsi, 0.0D);
        if (quante >= 60) {
            // le detonazioni grosse buttano fuori anche un anello raso terra
            for (int i = 0; i < 40; i++) {
                double a = (Math.PI * 2 / 40) * i;
                level.sendParticles(MyMod.VENOM_PARTICLE.get(),
                        centro.x + Math.cos(a) * raggio, centro.y + 0.3D, centro.z + Math.sin(a) * raggio,
                        4, 0.2D, 0.3D, 0.2D, 0.0D);
            }
        }
    }

    /** il vortice della tempesta: due spirali che salgono mentre il raggio si allarga */
    private static void spawnRing(ServerPlayer player, int t) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double raggio = Math.min(TEMPEST_RADIUS, 1.0D + t * 0.25D);
        for (int i = 0; i < 24; i++) {
            double a = (Math.PI * 2 / 24) * i + t * 0.35D;
            double altezza = 0.3D + ((i % 6) * 0.35D) + Math.sin(t * 0.25D + i) * 0.4D;
            level.sendParticles(MyMod.VENOM_PARTICLE.get(),
                    player.getX() + Math.cos(a) * raggio, player.getY() + altezza,
                    player.getZ() + Math.sin(a) * raggio, 2, 0.08D, 0.2D, 0.08D, 0.0D);
        }
    }

    private static int getScore(Player player, String objectiveName) {
        Objective objective = player.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        return player.getScoreboard()
                .getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
    }

    private static void setScore(Player player, String objectiveName, int value) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                    Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(value);
    }

    private static final class PowerState {
        private int pullTick = -1;
        private int strikeTick = -1;
        private int tempestTick = -1;
        private int bombTick = -1;
        private AntivenomBombEntity bomba;
        private int blastTick = -1;

        private int pullCooldown;
        private int strikeCooldown;
        private int tempestCooldown;
        private int bombCooldown;
        private int blastCooldown;
        private int rageCooldown;
        private int rageTicks;
        private int animTicks;
        private int animHold;

        private final List<LivingEntity> captured = new ArrayList<>();
        private final List<Double> captureY = new ArrayList<>();
        private final List<Integer> struck = new ArrayList<>();


        private boolean anyRunning() {
            return pullTick >= 0 || strikeTick >= 0 || tempestTick >= 0 || bombTick >= 0 || blastTick >= 0;
        }
    }
}
