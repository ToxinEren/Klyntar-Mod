package modKlyntar.player;



import modKlyntar.tentacles_traversal.VenomTentaclesTraversalHandler;
import modKlyntar.MyMod;

import modKlyntar.capability.PlayerPowerCapability;

import modKlyntar.network.ModNetwork;

import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerBossEvent;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.tags.BlockTags;

import net.minecraft.tags.DamageTypeTags;

import net.minecraft.world.BossEvent;

import net.minecraft.world.damagesource.DamageTypes;

import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.Mob;

import net.minecraft.world.entity.animal.Animal;

import net.minecraft.world.entity.animal.allay.Allay;

import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.entity.monster.Enemy;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.npc.Villager;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.Items;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.HitResult;

import net.minecraft.world.phys.Vec3;

import net.minecraft.world.scores.Objective;

import net.minecraft.world.scores.Score;

import net.minecraft.world.scores.Scoreboard;

import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import net.minecraftforge.event.TickEvent;

import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;

import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;



import java.lang.reflect.Method;

import java.util.Comparator;

import java.util.HashSet;

import java.util.Map;

import java.util.Random;

import java.util.Set;

import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;



@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)

public final class VenomSymbioteSystemsHandler {

    private static final Logger LOGGER = LogManager.getLogger("KlyntarVenomSystems");

    private static final String FLIGHT_OBJECTIVE = "Venom.Flight";
    private static final String STANDING_OBJECTIVE = "Venom.Standing";

    private static final String TENTACLES_TRAVERSAL_OBJECTIVE = "Venom.TentaclesTraversal";

    private static final String FEND_OFF_OBJECTIVE = "Venom.FendOffEnemies";

    private static final String FLIGHT_COLLISION_LOCK_OBJECTIVE = "Venom.Flight.CollisionLock";

    private static final String CLIMB_WALL_ANIM_OBJECTIVE = "Venom.Anim.ClimbWall";

    private static final String CLIMB_HANG_ANIM_OBJECTIVE = "Venom.Anim.ClimbHang";

    private static final String CLIMB_IMPULSE_ANIM_OBJECTIVE = "Venom.Anim.ClimbImpulse";

    private static final String CLIMB_CEILING_ANIM_OBJECTIVE = "Venom.Anim.ClimbCeiling";

    private static final String CLIMB_CEILING_HOLD_ANIM_OBJECTIVE = "Venom.Anim.ClimbCeilingHold";

    private static final String CLIMB_ENABLED_OBJECTIVE = "Venom.Climb.Enabled";

    private static final String CAMERA_OBJECTIVE = VenomCameraHeightHandler.OBJECTIVE_NAME;

    private static final String HUNGER_OBJECTIVE = "Venom.SymbioteHunger";

    private static final String AUTO_HEAD_OBJECTIVE = "Venom.AutoHead";

    private static final String AUTO_HEAD_DISABLED_OBJECTIVE = "Venom.AutoHead.Disabled";

    private static final String BERSERK_OBJECTIVE = "Venom.Berserk";

    private static final String BERSERK_PHASE_OBJECTIVE = "Venom.Berserk.Phase";

    private static final String BERSERK_TICKS_OBJECTIVE = "Venom.Berserk.Ticks";

    private static final String BERSERK_REGEN_OBJECTIVE = "Venom.Berserk.Regeneration";

    private static final String VULNERABILITY_OBJECTIVE = "Venom.VulnerabilityLock";

    private static final String SONIC_HITS_OBJECTIVE = "Venom.SonicHits";

    /** conto alla rovescia della posa di scossa: la leggono le abilita' che la fanno partire */

    public static final String VIBRANT_OBJECTIVE = "Venom.Anim.Vibrant";

    /** quanto dura la posa di scossa: la durata piena dell'animazione del pack */

    private static final int VIBRANT_TICKS = 89;

    /** alzato mentre il modello si ritira: regge la maschera del ritorno finche' dura */

    public static final String REVERT_OBJECTIVE = "Venom.Revert";

    /** quanto dura il ritiro del modello, cioe' l'animazione inversa della trasformazione */

    private static final int REVERT_TICKS = 20;

    /** ultimo stato del corpo simbionte, per accorgersi del momento in cui si spegne */

    private static final Map<UUID, Boolean> CORPO_PRECEDENTE = new ConcurrentHashMap<>();



    /** costringe alla forma umana: chiude col lucchetto "Unleash the symbiote" per un istante */

    public static final String FORCE_HUMAN_OBJECTIVE = "Venom.ForceHuman";

    /** basta un secondo di lucchetto perche' il corpo simbionte rientri */

    private static final int FORCE_HUMAN_TICKS = 20;

    private static final String LOCK_MOVEMENT_OBJECTIVE = "Venom.LockMovement";

    private static final String NORMAL_REGEN_HUNGER_PAID = "Klyntar.NormalRegenHungerPaid";

    private static final int MAX_HUNGER = 100;

    private static final int HUNGER_RESTORE_FOOD = 20;

    private static final int HUNGER_DRAIN_INTERVAL = 60;

    private static final int AUTO_HEAD_START_HUNGER = 50;

    private static final int AUTO_HEAD_STOP_HUNGER = 90;

    private static final int BERSERK_STOP_HUNGER = 70;

    private static final int BERSERK_FEND_TICKS = 20 * 20;

    private static final int BERSERK_REGEN_TICKS = 40;

    private static final double BERSERK_TARGET_RANGE = 32.0D;

    private static final double BERSERK_REGEN_START_RANGE = 5.0D;

    private static final double BERSERK_CHASE_STOP_RANGE = 3.25D;

    private static final double BERSERK_CHASE_SPEED = 0.46D;

    private static final int BERSERK_STUCK_JUMP_TICKS = 40;

    private static final double BERSERK_STUCK_DISTANCE_SQR = 0.03D;

    private static final double BERSERK_STUCK_JUMP_FORWARD_SPEED = 0.62D;

    /** dopo quanti salti a vuoto il simbionte prova a scavalcare in locomozione */

    private static final int BERSERK_SALTI_PRIMA_DI_LOCOMOZIONE = 2;

    private static final double BERSERK_STUCK_JUMP_UP_SPEED = 0.48D;

    private static final int VULNERABILITY_TICKS = 20 * 60 * 5;

    private static final int AUTO_ATTACK_INTERVAL = 10;

    private static final double AUTO_ATTACK_RANGE = 8.0D;

    private static final double FOOD_SEARCH_RANGE = 8.0D;

    private static final int FLIGHT_LAUNCH_MAX_TICKS = 28;

    private static final int FLIGHT_COLLISION_LOCK_TICKS = 12;

    private static final double FLIGHT_LAUNCH_HEIGHT = 5.0D;

    private static final double FLIGHT_LAUNCH_SPEED = 0.72D;

    private static final double FLIGHT_SPEED = 0.55D;

    private static final double FLIGHT_SPRINT_SPEED = 0.85D;

    private static final double FLIGHT_FIREWORK_TARGET_SPEED = 3.4D;

    private static final double FLIGHT_FIREWORK_SPRINT_TARGET_SPEED = 5.0D;

    private static final double FLIGHT_FIREWORK_DIRECT_THRUST = 0.45D;

    private static final double FLIGHT_FIREWORK_TARGET_PULL = 1.0D;

    private static final int CLIMB_HANG_AIR_BELOW_BLOCKS = 3;

    private static final int CLIMB_WALL_STICK_TICKS = 8;

    private static final int CLIMB_CEILING_STICK_TICKS = 10;

    private static final double CLIMB_UP_SPEED = 0.46D;

    private static final double CLIMB_CEILING_SPEED = 0.32D;

    private static final double CLIMB_DESCEND_SPEED = -0.12D;

    private static final double VENOM_VISUAL_COLLISION_WIDTH = 0.6D;

    private static final double VENOM_VISUAL_COLLISION_HEIGHT = 4.0D;

    private static final Random RANDOM = new Random();

    private static final Set<Item> SYMBIOTE_FOODS = new HashSet<>();

    private static final Map<UUID, ServerBossEvent> HUNGER_BARS = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> AUTO_ATTACK_TICKS = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> BERSERK_TARGETS = new ConcurrentHashMap<>();

    private static final Map<UUID, BerserkStuckState> BERSERK_STUCK_MEMORY = new ConcurrentHashMap<>();

    private static final Map<UUID, FlightState> FLIGHT_MEMORY = new ConcurrentHashMap<>();

    private static final double STANDING_THRESHOLD_SQR = 1.0E-4D;
    private static final Map<UUID, Vec3> STANDING_LAST_POS = new ConcurrentHashMap<>();
    /**
     * Quanti tick di immobilita' servono prima di dichiarare fermo il giocatore. Sul server il
     * movimento arriva per pacchetti: se in un tick non ne arriva nessuno, posizione e velocita'
     * risultano identiche e un giocatore in corsa sembrerebbe fermo per un istante.
     */
    private static final int STANDING_CONFIRM_TICKS = 4;
    private static final Map<UUID, Integer> STANDING_STILL_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CLIMB_DEBUG_STATE = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> CLIMB_WALL_STICK_MEMORY = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> CLIMB_CEILING_STICK_MEMORY = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> CLIMB_SHIFT_MEMORY = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> CLIMB_CEILING_CTRL_MEMORY = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> CLIMB_FORWARD_INPUT = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> CLIMB_CTRL_INPUT = new ConcurrentHashMap<>();

    private static final Map<UUID, Boolean> CLIMB_SNEAK_INPUT = new ConcurrentHashMap<>();

    private static final Set<UUID> CLIMB_HANG_LOCKED = ConcurrentHashMap.newKeySet();

    private static final Set<UUID> CLIMB_CEILING_HOLD_LOCKED = ConcurrentHashMap.newKeySet();



    static {

        SYMBIOTE_FOODS.add(Items.CHICKEN);

        SYMBIOTE_FOODS.add(Items.COOKED_CHICKEN);

        SYMBIOTE_FOODS.add(Items.PORKCHOP);

        SYMBIOTE_FOODS.add(Items.COOKED_PORKCHOP);

        SYMBIOTE_FOODS.add(Items.BEEF);

        SYMBIOTE_FOODS.add(Items.COOKED_BEEF);

        SYMBIOTE_FOODS.add(Items.MUTTON);

        SYMBIOTE_FOODS.add(Items.COOKED_MUTTON);

        SYMBIOTE_FOODS.add(Items.RABBIT);

        SYMBIOTE_FOODS.add(Items.COOKED_RABBIT);

        SYMBIOTE_FOODS.add(Items.COD);

        SYMBIOTE_FOODS.add(Items.COOKED_COD);

        SYMBIOTE_FOODS.add(Items.SALMON);

        SYMBIOTE_FOODS.add(Items.COOKED_SALMON);

        SYMBIOTE_FOODS.add(Items.ROTTEN_FLESH);

    }



    private VenomSymbioteSystemsHandler() {

    }



    @SubscribeEvent

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {

            return;

        }

        if (!(event.player instanceof ServerPlayer player)) {

            return;

        }



        tickStanding(player);
        tickFlightCollisionLock(player);



        boolean flightRequested = getScore(player, FLIGHT_OBJECTIVE, false) > 0;

        boolean hasVenom = hasVenomPower(player);

        boolean bodyActive = getScore(player, VenomPlayerSizeHandler.SIZE_OBJECTIVE, false) > 0;

        if (!bodyActive) {

            resetTransformationBoundAbilities(player);

        }



        if (!hasVenom) {

            // senza simbionte la barra non deve restare a schermo nemmeno mentre il volo

            // e' ancora richiesto: prima in quel caso rimaneva ferma sull'ultimo valore

            removeHungerBar(player);

            // e il conto dell'indebolimento deve comunque scorrere: tickVulnerability sta

            // oltre l'uscita anticipata qui sotto, quindi restando senza simbionte il conto

            // si congelava e il giocatore tornava indebolito appena ne riprendeva uno

            scalaContatore(player, VULNERABILITY_OBJECTIVE);

            tickVibrant(player);

            tickRevert(player);

        }

        if (!hasVenom && !flightRequested) {

            AUTO_ATTACK_TICKS.remove(player.getUUID());

            BERSERK_TARGETS.remove(player.getUUID());

            dimenticaStalloBerserk(player);

            FLIGHT_MEMORY.remove(player.getUUID());

            clearClimbMemory(player);

            return;

        }



        if (flightRequested) {

            tickFlight(player);

        }



        if (!hasVenom) {

            return;

        }



        tickVulnerability(player);

        tickVibrant(player);

        tickRevert(player);

        tickMovementLock(player);

        // Anti-Venom non ha la fame del simbionte: niente barra, niente digiuno, niente

        // berserk. Le sue abilita' pesano invece sulla fame normale del giocatore

        if (modKlyntar.symbiote.SymbioteState.isAntiVenom(player)) {

            removeHungerBar(player);

        } else {

            tickNormalRegenerationHunger(player);

            tickHunger(player);

        }

        if (!flightRequested) {

            tickFlight(player);

        }

        tickClimbing(player);

    }



    @SubscribeEvent

    public static void onLivingHurt(LivingHurtEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player) || !hasVenomPower(player)) {

            return;

        }



        if (event.getSource().is(DamageTypeTags.IS_FIRE) || player.isOnFire() || player.isInLava()) {

            applyVulnerability(player, false);

            return;

        }



        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {

            applyVulnerability(player, true);

        }

    }



    @SubscribeEvent

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {

        if (event.getEntity() instanceof ServerPlayer player) {

            removeHungerBar(player);

            AUTO_ATTACK_TICKS.remove(player.getUUID());

            BERSERK_TARGETS.remove(player.getUUID());

            dimenticaStalloBerserk(player);

            FLIGHT_MEMORY.remove(player.getUUID());

            STANDING_STILL_TICKS.remove(player.getUUID());

            STANDING_LAST_POS.remove(player.getUUID());

            clearClimbMemory(player);

        }

    }



    private static void tickFlight(ServerPlayer player) {

        if (getScore(player, FLIGHT_OBJECTIVE, false) <= 0 || isVulnerable(player)) {

            stopFallFlyingFlag(player);

            ModNetwork.syncVenomFlightState(player, false);

            FLIGHT_MEMORY.remove(player.getUUID());

            return;

        }



        player.fallDistance = 0.0F;

        Vec3 look = player.getLookAngle().normalize();

        Vec3 current = player.getDeltaMovement();

        UUID playerId = player.getUUID();

        FlightState state = FLIGHT_MEMORY.computeIfAbsent(playerId, id -> {

            LOGGER.info("Venom flight server active for {}. score={}", player.getGameProfile().getName(), getScore(player, FLIGHT_OBJECTIVE, false));

            player.displayClientMessage(Component.literal("Venom Flight server active"), true);

            ModNetwork.syncVenomFlightState(player, true);

            return new FlightState(player.getY());

        });

        if (state.launching) {

            stopFallFlyingFlag(player);

            state.launchTicks++;

            double gainedHeight = player.getY() - state.launchStartY;

            if (gainedHeight >= FLIGHT_LAUNCH_HEIGHT || state.launchTicks >= FLIGHT_LAUNCH_MAX_TICKS) {

                state.launching = false;

            } else {

                double forwardBoost = state.launchTicks > 8 ? FLIGHT_SPEED * 0.35D : FLIGHT_SPEED * 0.12D;

                player.setDeltaMovement(

                        current.x * 0.35D + look.x * forwardBoost,

                        Math.max(current.y, FLIGHT_LAUNCH_SPEED),

                        current.z * 0.35D + look.z * forwardBoost

                );

                player.hasImpulse = true;

                return;

            }

        }



        if (hasFlightBlockCollision(player)) {

            disableFlightFromCollision(player);

            return;

        }



        if (!player.isFallFlying()) {

            Vec3 transition = player.getDeltaMovement();

            player.setDeltaMovement(

                    transition.x + look.x * FLIGHT_SPEED * 0.4D,

                    Math.max(-0.04D, transition.y * 0.35D),

                    transition.z + look.z * FLIGHT_SPEED * 0.4D

            );

        }



        startFallFlyingFlag(player);

        boolean forwardPressed = player.zza > 0.0F;

        if (forwardPressed) {

            state.propellerTicks = 2;

            if (!state.forwardPressedLastTick) {

                player.displayClientMessage(Component.literal("Venom propeller server"), true);

                LOGGER.info("Venom flight propeller triggered server-side for {}. score={}, velocity={}", player.getGameProfile().getName(), getScore(player, FLIGHT_OBJECTIVE, false), player.getDeltaMovement());

            }

        }

        state.forwardPressedLastTick = forwardPressed;



        if (state.propellerTicks > 0) {

            double targetSpeed = player.isSprinting() ? FLIGHT_FIREWORK_SPRINT_TARGET_SPEED : FLIGHT_FIREWORK_TARGET_SPEED;

            Vec3 next = applyPermanentFireworkBoost(player.getDeltaMovement(), look, targetSpeed);

            if (player.isShiftKeyDown()) {

                next = next.add(0.0D, -0.12D, 0.0D);

            }

            player.setDeltaMovement(next);

            state.propellerTicks--;

        }

        player.hasImpulse = true;

    }



    private static Vec3 applyPermanentFireworkBoost(Vec3 velocity, Vec3 look, double targetSpeed) {

        return velocity.add(

                look.x * FLIGHT_FIREWORK_DIRECT_THRUST + (look.x * targetSpeed - velocity.x) * FLIGHT_FIREWORK_TARGET_PULL,

                look.y * FLIGHT_FIREWORK_DIRECT_THRUST + (look.y * targetSpeed - velocity.y) * FLIGHT_FIREWORK_TARGET_PULL,

                look.z * FLIGHT_FIREWORK_DIRECT_THRUST + (look.z * targetSpeed - velocity.z) * FLIGHT_FIREWORK_TARGET_PULL

        );

    }



    private static void tickStanding(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        double velocitySqr = motion.x * motion.x + motion.z * motion.z;
        Vec3 last = STANDING_LAST_POS.put(player.getUUID(), player.position());
        double movedSqr = 0.0D;
        if (last != null) {
            Vec3 delta = player.position().subtract(last);
            movedSqr = delta.x * delta.x + delta.z * delta.z;
        }
        boolean stillNow = velocitySqr <= STANDING_THRESHOLD_SQR
                && movedSqr <= STANDING_THRESHOLD_SQR
                && player.onGround();
        // il movimento vale subito, l'immobilita' va confermata: cosi' un buco di pacchetti non
        // fa passare per fermo chi sta camminando
        int stillTicks = stillNow ? STANDING_STILL_TICKS.getOrDefault(player.getUUID(), 0) + 1 : 0;
        STANDING_STILL_TICKS.put(player.getUUID(), stillTicks);
        setScore(player, STANDING_OBJECTIVE, stillTicks >= STANDING_CONFIRM_TICKS ? 1 : 0);
    }

    private static void tickFlightCollisionLock(ServerPlayer player) {

        int lockTicks = getScore(player, FLIGHT_COLLISION_LOCK_OBJECTIVE, true);

        if (lockTicks > 0) {

            setScore(player, FLIGHT_COLLISION_LOCK_OBJECTIVE, lockTicks - 1);

        }

    }



    private static boolean hasFlightBlockCollision(ServerPlayer player) {

        return player.onGround() || player.horizontalCollision || (player.verticalCollision && player.getDeltaMovement().y > 0.0D);

    }



    private static void disableFlightFromCollision(ServerPlayer player) {

        setScore(player, FLIGHT_COLLISION_LOCK_OBJECTIVE, FLIGHT_COLLISION_LOCK_TICKS);

        setScore(player, FLIGHT_OBJECTIVE, 0);

        stopFallFlyingFlag(player);

        ModNetwork.syncVenomFlightState(player, false);

        FLIGHT_MEMORY.remove(player.getUUID());

        player.displayClientMessage(Component.literal("Venom Flight OFF"), true);

        LOGGER.info("Venom flight disabled after block collision for {}", player.getGameProfile().getName());

    }



    private static void tickClimbing(ServerPlayer player) {

        if (isVulnerable(player) || !hasVenomPower(player) || !isClimbAbilityEnabled(player)) {

            setClimbAnimationState(player, 0);

            logClimbState(player, 0, false);

            clearClimbMemory(player);

            player.setNoGravity(false);

            return;

        }



        boolean pressingForward = isPressingForward(player);

        boolean touchingClimbWall = isTouchingClimbWall(player, pressingForward);

        updateClimbCeilingHoldToggle(player);

        boolean holdingCeiling = isHoldingCeiling(player);

        boolean touchingClimbCeiling = isTouchingClimbCeiling(player, pressingForward, holdingCeiling, touchingClimbWall);

        if (touchingClimbCeiling) {

            updateClimbHangToggle(player, false);

            int climbState = holdingCeiling ? 5 : 4;

            setClimbAnimationState(player, climbState);

            logClimbState(player, climbState, false);

            Vec3 current = player.getDeltaMovement();

            Vec3 ceilingMotion = holdingCeiling ? Vec3.ZERO : getHorizontalLook(player).scale(pressingForward ? CLIMB_CEILING_SPEED : 0.0D);

            player.setNoGravity(true);

            player.setDeltaMovement(

                    ceilingMotion.x,

                    holdingCeiling ? 0.0D : Math.max(0.0D, Math.min(current.y, 0.02D)),

                    ceilingMotion.z

            );

            player.hurtMarked = true;

            player.fallDistance = 0.0F;

            return;

        }



        if (touchingClimbWall) {

            updateClimbHangToggle(player, true);

            boolean hanging = CLIMB_HANG_LOCKED.contains(player.getUUID());

            int climbState = hanging ? 2 : 1;

            setClimbAnimationState(player, climbState);

            logClimbState(player, climbState, hanging);

            Vec3 current = player.getDeltaMovement();

            if (hanging) {

                player.setNoGravity(true);

                player.setDeltaMovement(current.x * 0.2D, 0.0D, current.z * 0.2D);

                player.hurtMarked = true;

            } else {

                player.setNoGravity(false);

                player.setDeltaMovement(current.x, Math.max(current.y, CLIMB_UP_SPEED), current.z);

                player.hurtMarked = true;

            }

            player.fallDistance = 0.0F;

            return;

        }



        setClimbAnimationState(player, 0);

        logClimbState(player, 0, false);

        updateClimbHangToggle(player, false);

        CLIMB_CEILING_HOLD_LOCKED.remove(player.getUUID());

        player.setNoGravity(false);

    }



    private static void setClimbAnimationState(ServerPlayer player, int state) {

        setScore(player, CLIMB_WALL_ANIM_OBJECTIVE, state == 1 ? 1 : 0);

        setScore(player, CLIMB_HANG_ANIM_OBJECTIVE, state == 2 ? 1 : 0);

        setScore(player, CLIMB_IMPULSE_ANIM_OBJECTIVE, state == 3 ? 1 : 0);

        setScore(player, CLIMB_CEILING_ANIM_OBJECTIVE, state == 4 ? 1 : 0);

        setScore(player, CLIMB_CEILING_HOLD_ANIM_OBJECTIVE, state == 5 ? 1 : 0);

    }



    private static boolean isTouchingClimbWall(ServerPlayer player, boolean pressingForward) {

        boolean nearWall = hasWallInFront(player) || hasWallAround(player) || hasWallTouchingBoundingBox(player);

        boolean tryingToClimb = pressingForward || CLIMB_HANG_LOCKED.contains(player.getUUID());

        boolean directWallHit = nearWall && tryingToClimb;

        if (directWallHit) {

            CLIMB_WALL_STICK_MEMORY.put(player.getUUID(), CLIMB_WALL_STICK_TICKS);

            return true;

        }



        int memoryTicks = CLIMB_WALL_STICK_MEMORY.getOrDefault(player.getUUID(), 0);

        if (memoryTicks <= 0 || !nearWall || !pressingForward) {

            CLIMB_WALL_STICK_MEMORY.remove(player.getUUID());

            return false;

        }



        CLIMB_WALL_STICK_MEMORY.put(player.getUUID(), memoryTicks - 1);

        return true;

    }



    private static boolean isTouchingClimbCeiling(ServerPlayer player, boolean pressingForward, boolean holdingCeiling, boolean touchingClimbWall) {

        boolean ceiling = hasHeadCeilingCollision(player);

        boolean wantsCeiling = pressingForward || holdingCeiling;

        boolean canAttach = ceiling && !player.onGround() && wantsCeiling && (touchingClimbWall || CLIMB_CEILING_STICK_MEMORY.containsKey(player.getUUID()));

        if (canAttach) {

            CLIMB_CEILING_STICK_MEMORY.put(player.getUUID(), CLIMB_CEILING_STICK_TICKS);

            return true;

        }



        int memoryTicks = CLIMB_CEILING_STICK_MEMORY.getOrDefault(player.getUUID(), 0);

        if (memoryTicks <= 0 || !ceiling || !wantsCeiling) {

            CLIMB_CEILING_STICK_MEMORY.remove(player.getUUID());

            return false;

        }



        CLIMB_CEILING_STICK_MEMORY.put(player.getUUID(), memoryTicks - 1);

        return true;

    }



    private static boolean hasHeadCeilingCollision(ServerPlayer player) {

        AABB box = player.getBoundingBox();

        AABB headProbe = new AABB(

                box.minX + 0.05D,

                box.maxY - 0.02D,

                box.minZ + 0.05D,

                box.maxX - 0.05D,

                box.maxY + 0.16D,

                box.maxZ - 0.05D

        );

        return !player.level().noCollision(player, headProbe);

    }



    public static boolean shouldUseVanillaClimb(LivingEntity entity) {

        if (!(entity instanceof Player player) || player.isSpectator() || !hasVenomPower(player) || !isClimbAbilityEnabled(player)) {

            return false;

        }



        if (player.isFallFlying() || player.isInWaterOrBubble()) {

            return false;

        }



        boolean nearWall = hasWallInFront(player) || hasWallAround(player) || hasWallTouchingBoundingBox(player);

        if (!nearWall) {

            return false;

        }



        return player instanceof ServerPlayer serverPlayer

                && (isPressingForward(serverPlayer) || CLIMB_HANG_LOCKED.contains(player.getUUID()));

    }



    public static boolean isClimbingForCollision(Player player) {

        if (player == null || player.isSpectator() || !hasVenomPower(player) || !isClimbAbilityEnabled(player)) {

            return false;

        }



        return getScore(player, CLIMB_WALL_ANIM_OBJECTIVE, false) > 0

                || getScore(player, CLIMB_HANG_ANIM_OBJECTIVE, false) > 0

                || getScore(player, CLIMB_IMPULSE_ANIM_OBJECTIVE, false) > 0

                || getScore(player, CLIMB_CEILING_ANIM_OBJECTIVE, false) > 0

                || getScore(player, CLIMB_CEILING_HOLD_ANIM_OBJECTIVE, false) > 0

                || CLIMB_HANG_LOCKED.contains(player.getUUID());

    }



    private static boolean isPressingForward(ServerPlayer player) {

        if (CLIMB_FORWARD_INPUT.getOrDefault(player.getUUID(), false)) {

            return true;

        }



        if (player.zza > 0.0F) {

            return true;

        }



        Vec3 look = getHorizontalLook(player);

        Vec3 motion = player.getDeltaMovement();

        double forwardMotion = motion.x * look.x + motion.z * look.z;

        return forwardMotion > 0.015D;

    }



    private static Vec3 getHorizontalLook(LivingEntity entity) {

        Vec3 look = entity.getLookAngle();

        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);

        if (horizontalLook.lengthSqr() < 1.0E-4D) {

            return Vec3.ZERO;

        }

        return horizontalLook.normalize();

    }



    private static boolean isHoldingCeiling(ServerPlayer player) {

        return CLIMB_CEILING_HOLD_LOCKED.contains(player.getUUID());

    }



    private static void updateClimbCeilingHoldToggle(ServerPlayer player) {

        UUID playerId = player.getUUID();

        boolean ctrlDown = CLIMB_CTRL_INPUT.getOrDefault(playerId, false);

        boolean ctrlWasDown = CLIMB_CEILING_CTRL_MEMORY.getOrDefault(playerId, false);

        CLIMB_CEILING_CTRL_MEMORY.put(playerId, ctrlDown);



        if (ctrlDown && !ctrlWasDown && hasHeadCeilingCollision(player)) {

            if (CLIMB_CEILING_HOLD_LOCKED.remove(playerId)) {

                LOGGER.info("Venom ceiling hold toggle OFF by ctrl for {}", player.getGameProfile().getName());

            } else {

                CLIMB_CEILING_HOLD_LOCKED.add(playerId);

                LOGGER.info("Venom ceiling hold toggle ON by ctrl for {}", player.getGameProfile().getName());

            }

        }

    }



    private static void updateClimbHangToggle(ServerPlayer player, boolean canHang) {

        UUID playerId = player.getUUID();

        boolean ctrlDown = CLIMB_CTRL_INPUT.getOrDefault(playerId, false);

        boolean ctrlWasDown = CLIMB_SHIFT_MEMORY.getOrDefault(playerId, false);

        CLIMB_SHIFT_MEMORY.put(playerId, ctrlDown);



        if (!canHang) {

            CLIMB_HANG_LOCKED.remove(playerId);

            return;

        }



        if (ctrlDown && !ctrlWasDown) {

            if (CLIMB_HANG_LOCKED.remove(playerId)) {

                LOGGER.info("Venom hang toggle OFF by ctrl for {}", player.getGameProfile().getName());

            } else {

                CLIMB_HANG_LOCKED.add(playerId);

                LOGGER.info("Venom hang toggle ON by ctrl for {}", player.getGameProfile().getName());

            }

        }

    }



    private static void clearClimbMemory(ServerPlayer player) {

        UUID playerId = player.getUUID();

        CLIMB_WALL_STICK_MEMORY.remove(playerId);

        CLIMB_CEILING_STICK_MEMORY.remove(playerId);

        CLIMB_SHIFT_MEMORY.remove(playerId);

        CLIMB_CEILING_CTRL_MEMORY.remove(playerId);

        CLIMB_FORWARD_INPUT.remove(playerId);

        CLIMB_CTRL_INPUT.remove(playerId);

        CLIMB_SNEAK_INPUT.remove(playerId);

        CLIMB_HANG_LOCKED.remove(playerId);

        CLIMB_CEILING_HOLD_LOCKED.remove(playerId);

    }



    private static void logClimbState(ServerPlayer player, int state, boolean hanging) {

        Integer previous = CLIMB_DEBUG_STATE.put(player.getUUID(), state);

        if (previous != null && previous == state) {

            return;

        }



        String stateName = switch (state) {

            case 1 -> "CLIMB";

            case 2 -> "HANG";

            case 3 -> "IMPULSE";

            case 4 -> "CEILING";

            case 5 -> "CEILING_HOLD";

            default -> "OFF";

        };

        LOGGER.info("Venom climb state {} for {}: forward={}, horizontalCollision={}, onGround={}, hanging={}, airBelow{}={}, pos={}",

                stateName,

                player.getGameProfile().getName(),

                isPressingForward(player),

                player.horizontalCollision,

                player.onGround(),

                hanging,

                CLIMB_HANG_AIR_BELOW_BLOCKS,

                hasAirBelow(player, CLIMB_HANG_AIR_BELOW_BLOCKS),

                player.blockPosition());

    }



    private static void tickHunger(ServerPlayer player) {

        ensureHungerScore(player);

        ServerBossEvent bar = HUNGER_BARS.computeIfAbsent(player.getUUID(), id -> createHungerBar(player));

        if (!bar.getPlayers().contains(player)) {

            bar.addPlayer(player);

        }



        int hunger = Math.max(0, Math.min(MAX_HUNGER, getScore(player, HUNGER_OBJECTIVE, false)));

        bar.setProgress(hunger / (float) MAX_HUNGER);

        bar.setName(Component.literal("Symbiote Hunger: " + hunger + "%"));



        if (hunger <= 0 || getScore(player, BERSERK_OBJECTIVE, false) > 0) {

            tickBerserkSymbiote(player, hunger);

        } else if (!isBodyActive(player)) {

            tickAutoVenomHeadFeeding(player, hunger);

        } else {

            setScore(player, AUTO_HEAD_OBJECTIVE, 0);

            setScore(player, AUTO_HEAD_DISABLED_OBJECTIVE, 0);

            clearCombatTargetsIfFree(player);

        }

    }



    private static void tickAutoVenomHeadFeeding(ServerPlayer player, int hunger) {

        if (hunger >= AUTO_HEAD_STOP_HUNGER) {

            setScore(player, AUTO_HEAD_OBJECTIVE, 0);

            setScore(player, AUTO_HEAD_DISABLED_OBJECTIVE, 0);

            clearCombatTargetsIfFree(player);

            return;

        }



        boolean disabledByPlayer = getScore(player, AUTO_HEAD_DISABLED_OBJECTIVE, false) > 0;

        boolean active = getScore(player, AUTO_HEAD_OBJECTIVE, false) > 0;

        if (!active && hunger <= AUTO_HEAD_START_HUNGER && !disabledByPlayer) {

            setScore(player, AUTO_HEAD_OBJECTIVE, 1);

            active = true;

        }



        if (!active || disabledByPlayer) {

            clearCombatTargetsIfFree(player);

            return;

        }



        tryHeadFeedFromGroundFood(player);

    }



    private static void tickNormalRegenerationHunger(ServerPlayer player) {

        if (!player.getTags().contains("venom_feeding")) {

            player.getPersistentData().remove(NORMAL_REGEN_HUNGER_PAID);

            return;

        }



        if (player.getPersistentData().getBoolean(NORMAL_REGEN_HUNGER_PAID)) {

            return;

        }



        LivingEntity target = findTaggedFeedTarget(player);

        if (target == null || target.distanceToSqr(player) > 16.0D) {

            return;

        }



        int restore = getFeedHungerValue(target);

        addHunger(player, restore);

        player.getPersistentData().putBoolean(NORMAL_REGEN_HUNGER_PAID, true);

        LOGGER.info("Venom regeneration restored {} hunger from {} for {}", restore, EntityType.getKey(target.getType()), player.getGameProfile().getName());

    }



    private static void tickVulnerability(ServerPlayer player) {

        if (player.isOnFire() || player.isInLava()) {

            applyVulnerability(player, false);

        }



        int lockTicks = getScore(player, VULNERABILITY_OBJECTIVE, false);

        if (lockTicks <= 0) {

            return;

        }



        setScore(player, VULNERABILITY_OBJECTIVE, lockTicks - 1);

        player.removeEffect(MobEffects.REGENERATION);

        player.removeEffect(MobEffects.JUMP);

        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);

    }



    private static void tickMovementLock(ServerPlayer player) {

        int ticks = getScore(player, LOCK_MOVEMENT_OBJECTIVE, false);

        if (ticks <= 0) {

            return;

        }



        setScore(player, LOCK_MOVEMENT_OBJECTIVE, ticks - 1);

        Vec3 current = player.getDeltaMovement();

        player.setDeltaMovement(0.0D, Math.min(current.y, 0.0D), 0.0D);

        player.hurtMarked = true;

    }



    private static void tickBerserkSymbiote(ServerPlayer player, int hunger) {

        if (hunger >= BERSERK_STOP_HUNGER) {

            stopBerserk(player);

            return;

        }



        if (getScore(player, BERSERK_OBJECTIVE, false) <= 0) {

            setScore(player, BERSERK_OBJECTIVE, 1);

            setScore(player, BERSERK_PHASE_OBJECTIVE, 1);

            setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

            // averlo lasciato a digiuno fino alla furia costa fiducia

            SymbioteAffinityHandler.penalizza(player, SymbioteAffinityHandler.PENALITA_BERSERK);

            LOGGER.info("Venom berserk started for {}", player.getGameProfile().getName());

        }



        if (!isBodyActive(player)) {

            // rimette in scena la forma che il giocatore ha davvero, non sempre venom

            PlayerPowerCapability.riapplicaForma(player);

        }



        int phase = getScore(player, BERSERK_PHASE_OBJECTIVE, true);

        int ticks = getScore(player, BERSERK_TICKS_OBJECTIVE, true) + 1;

        setScore(player, BERSERK_TICKS_OBJECTIVE, ticks);

        LivingEntity lockedTarget = getOrFindBerserkTarget(player);



        if (phase != 2) {

            setScore(player, FEND_OFF_OBJECTIVE, 1);

            setScore(player, BERSERK_REGEN_OBJECTIVE, 0);

            if (lockedTarget != null) {

                faceBerserkTarget(player, lockedTarget);

                chaseBerserkTarget(player, lockedTarget);

                if (isNonAggressiveBerserkTarget(player, lockedTarget)

                        && lockedTarget.distanceToSqr(player) <= BERSERK_REGEN_START_RANGE * BERSERK_REGEN_START_RANGE) {

                    setScore(player, BERSERK_PHASE_OBJECTIVE, 2);

                    setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

                    setScore(player, FEND_OFF_OBJECTIVE, 0);

                    LOGGER.info("Venom berserk forced regeneration on passive target {} for {}", EntityType.getKey(lockedTarget.getType()), player.getGameProfile().getName());

                    return;

                }

            }

            pullNearbyFoodLoot(player);

            if (ticks >= BERSERK_FEND_TICKS) {

                setScore(player, BERSERK_PHASE_OBJECTIVE, 2);

                setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

            }

            return;

        }



        setScore(player, FEND_OFF_OBJECTIVE, 0);

        LivingEntity target = lockedTarget;

        if (target == null) {

            setScore(player, BERSERK_PHASE_OBJECTIVE, 1);

            setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

            setScore(player, BERSERK_REGEN_OBJECTIVE, 0);

            return;

        }



        if (target.distanceToSqr(player) > BERSERK_REGEN_START_RANGE * BERSERK_REGEN_START_RANGE) {

            setScore(player, BERSERK_REGEN_OBJECTIVE, 0);

            ModNetwork.syncVenomCombatTargets(player, java.util.List.of(getTargetCenter(target)));

            faceBerserkTarget(player, target);

            chaseBerserkTarget(player, target);

            return;

        }



        faceBerserkTarget(player, target);

        setScore(player, LOCK_MOVEMENT_OBJECTIVE, 2);

        player.setDeltaMovement(0.0D, Math.min(player.getDeltaMovement().y, 0.0D), 0.0D);

        player.hurtMarked = true;

        setScore(player, BERSERK_REGEN_OBJECTIVE, 1);

        ModNetwork.syncVenomCombatTargets(player, java.util.List.of(getTargetCenter(target)));

        Vec3 pull = player.position().add(0.0D, player.getBbHeight() * 0.85D, 0.0D).subtract(target.position());

        if (pull.lengthSqr() > 1.0D) {

            target.setDeltaMovement(pull.normalize().scale(0.42D));

            target.hasImpulse = true;

            target.hurtMarked = true;

        }

        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 8, false, false, false));



        if (ticks >= BERSERK_REGEN_TICKS || pull.lengthSqr() <= 1.3D) {

            int restore = getFeedHungerValue(target);

            target.hurt(player.damageSources().magic(), 100.0F);

            player.heal(8.0F);

            addHunger(player, restore);

            setScore(player, BERSERK_PHASE_OBJECTIVE, 1);

            setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

            setScore(player, BERSERK_REGEN_OBJECTIVE, 0);

            BERSERK_TARGETS.remove(player.getUUID());

            dimenticaStalloBerserk(player);

            ModNetwork.syncVenomCombatTargets(player, java.util.List.of());

            LOGGER.info("Venom berserk regeneration restored {} hunger from {} for {}", restore, EntityType.getKey(target.getType()), player.getGameProfile().getName());

        }

    }



    private static void stopBerserk(ServerPlayer player) {

        setScore(player, BERSERK_OBJECTIVE, 0);

        setScore(player, BERSERK_PHASE_OBJECTIVE, 0);

        setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

        setScore(player, BERSERK_REGEN_OBJECTIVE, 0);

        setScore(player, FEND_OFF_OBJECTIVE, 0);

        BERSERK_TARGETS.remove(player.getUUID());

        dimenticaStalloBerserk(player);

        ModNetwork.syncVenomCombatTargets(player, java.util.List.of());

        LOGGER.info("Venom berserk stopped for {}", player.getGameProfile().getName());

    }



    private static int getFeedHungerValue(LivingEntity target) {

        if (target instanceof Villager || target instanceof Animal && !(target instanceof Allay)) {

            return 40;

        }

        return 10;

    }



    private static void addHunger(ServerPlayer player, int amount) {

        setScore(player, HUNGER_OBJECTIVE, Math.min(MAX_HUNGER, getScore(player, HUNGER_OBJECTIVE, false) + amount));

    }



    private static LivingEntity findTaggedFeedTarget(ServerPlayer player) {

        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0D),

                        entity -> entity.isAlive() && entity.getTags().contains("venom_feed_target"))

                .stream()

                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))

                .orElse(null);

    }



    private static void pullNearbyFoodLoot(ServerPlayer player) {

        ItemEntity food = findNearestFoodLoot(player);

        if (food == null) {

            return;

        }



        ModNetwork.syncVenomCombatTargets(player, java.util.List.of(food.position().add(0.0D, food.getBbHeight() * 0.5D, 0.0D)));

        Vec3 pull = player.position().add(0.0D, player.getBbHeight() * 0.85D, 0.0D).subtract(food.position());

        if (pull.lengthSqr() > 1.0D) {

            food.setDeltaMovement(pull.normalize().scale(0.38D));

            food.hasImpulse = true;

            return;

        }



        food.getItem().shrink(1);

        if (food.getItem().isEmpty()) {

            food.discard();

        }

        addHunger(player, HUNGER_RESTORE_FOOD);

    }



    private static void tryHeadFeedFromGroundFood(ServerPlayer player) {

        ItemEntity food = findNearestFoodLoot(player);

        if (food == null) {

            clearCombatTargetsIfFree(player);

            return;

        }



        setScore(player, AUTO_HEAD_OBJECTIVE, 1);

        ModNetwork.syncVenomCombatTargets(player, java.util.List.of(food.position().add(0.0D, food.getBbHeight() * 0.5D, 0.0D)));

        Vec3 pull = player.position().add(0.0D, player.getBbHeight() * 0.85D, 0.0D).subtract(food.position());

        if (pull.lengthSqr() > 1.0D) {

            food.setDeltaMovement(pull.normalize().scale(0.35D));

            food.hasImpulse = true;

            return;

        }



        food.getItem().shrink(1);

        if (food.getItem().isEmpty()) {

            food.discard();

        }

        addHunger(player, HUNGER_RESTORE_FOOD);

    }



    private static void clearCombatTargetsIfFree(ServerPlayer player) {

        if (getScore(player, FEND_OFF_OBJECTIVE, false) > 0 || getScore(player, BERSERK_REGEN_OBJECTIVE, false) > 0

                || VenomSymbiotePowersHandler.isShowingTentacles(player)) {

            return;

        }

        ModNetwork.syncVenomCombatTargets(player, java.util.List.of());

    }



    private static ItemEntity findNearestFoodLoot(ServerPlayer player) {

        return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(FOOD_SEARCH_RANGE),

                        item -> item.isAlive() && SYMBIOTE_FOODS.contains(item.getItem().getItem()))

                .stream()

                .min(Comparator.comparingDouble(item -> item.distanceToSqr(player)))

                .orElse(null);

    }



    /**

     * Indebolisce il simbionte come farebbe il fuoco: stessa durata, stesse abilita' bloccate.

     * La usa l'Anti-Venom, che nella lista degli indebolimenti sta accanto a fuoco e suono,

     * ma senza far scattare il conteggio delle prese sonore.

     */

    /**

     * Cancella ogni indebolimento in corso, da fuoco o da suono, contatori compresi.

     * La chiama chi diventa Anti-Venom: il siero ripulisce tutto quello che c'era prima.

     */

    public static void clearWeakness(ServerPlayer player) {

        setScore(player, VULNERABILITY_OBJECTIVE, 0);

        setScore(player, SONIC_HITS_OBJECTIVE, 0);

    }



    /**

     * Un colpo di sonic damage: indebolisce e fa avanzare il conto delle prese sonore,

     * la terza delle quali strappa via il simbionte. La chiama chi suona una campana.

     */

    public static void applySonicWeakness(ServerPlayer player) {

        applyVulnerability(player, true);

    }



    public static void applyAntiVenomWeakness(ServerPlayer player) {

        applyVulnerability(player, false);

    }



    private static void applyVulnerability(ServerPlayer player, boolean sonic) {

        // Anti-Venom non si indebolisce: ne' fuoco, ne' suono, ne' il suo stesso veleno.

        // La guardia sta qui, alla sorgente, cosi' vale per ogni strada che porti a indebolire

        if (modKlyntar.symbiote.SymbioteState.isAntiVenom(player)) {

            return;

        }

        // la posa di scossa vale per ogni indebolimento — fuoco, suono, Anti-Venom — e parte

        // solo se non e' gia' in scena: cosi' restare nel fuoco non la fa ricominciare da capo

        // a ogni tick di fiamma, ma un colpo nuovo dopo che e' finita la rilancia

        if (getScore(player, VIBRANT_OBJECTIVE, false) <= 0) {

            setScore(player, VIBRANT_OBJECTIVE, VIBRANT_TICKS);

        }

        // le abilita' non si spengono piu' da qui: mentre l'indebolimento e' acceso Palladium

        // le tiene chiuse col lucchetto, quindi i punteggi non vengono nemmeno scritti

        setScore(player, VULNERABILITY_OBJECTIVE, VULNERABILITY_TICKS);

        player.removeEffect(MobEffects.REGENERATION);

        player.removeEffect(MobEffects.JUMP);

        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);



        if (!sonic) {

            return;

        }



        int hits = getScore(player, SONIC_HITS_OBJECTIVE, true) + 1;

        setScore(player, SONIC_HITS_OBJECTIVE, hits);

        if (hits < 3) {

            return;

        }



        setScore(player, SONIC_HITS_OBJECTIVE, 0);

        PlayerPowerCapability.revertPlayer(player);

        spawnSymbioteNear(player);

    }



    private static void resetTransformationBoundAbilities(ServerPlayer player) {

        boolean hadClimb = getScore(player, CLIMB_ENABLED_OBJECTIVE, false) > 0;

        boolean hadTentaclesTraversal = getScore(player, TENTACLES_TRAVERSAL_OBJECTIVE, false) > 0;

        boolean hadFendOff = getScore(player, FEND_OFF_OBJECTIVE, false) > 0;

        if (!hadClimb && !hadTentaclesTraversal && !hadFendOff) {

            return;

        }



        setScore(player, CLIMB_ENABLED_OBJECTIVE, 0);

        setScore(player, TENTACLES_TRAVERSAL_OBJECTIVE, 0);

        setScore(player, FEND_OFF_OBJECTIVE, 0);

        setClimbAnimationState(player, 0);

        clearClimbMemory(player);

        ModNetwork.syncVenomTentaclesTraversal(player, java.util.Collections.emptyList(), false);

        ModNetwork.syncVenomCombatTargets(player, java.util.List.of());

        LOGGER.info("Venom transformation-bound abilities reset for {}: climb={}, tentacles_traversal={}, fendOff={}",

                player.getGameProfile().getName(), hadClimb, hadTentaclesTraversal, hadFendOff);

    }



    private static LivingEntity findNearestLivingTarget(ServerPlayer player, double range) {

        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),

                        entity -> entity.isAlive() && !entity.isSpectator() && entity != player)

                .stream()

                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))

                .orElse(null);

    }



    private static LivingEntity getOrFindBerserkTarget(ServerPlayer player) {

        Integer targetId = BERSERK_TARGETS.get(player.getUUID());

        if (targetId != null) {

            Entity entity = player.level().getEntity(targetId);

            if (entity instanceof LivingEntity target && canUseBerserkTarget(player, target)) {

                return target;

            }

        }



        LivingEntity target = findNearestLivingTarget(player, BERSERK_TARGET_RANGE);

        if (target == null) {

            BERSERK_TARGETS.remove(player.getUUID());

            return null;

        }



        // lo stallo si dimentica solo se il bersaglio cambia davvero: capita di riagganciare

        // la stessa entita' piu' volte, e azzerando ogni volta il contatore non arriverebbe

        // mai alla soglia del salto

        Integer precedente = BERSERK_TARGETS.put(player.getUUID(), target.getId());

        if (precedente == null || precedente != target.getId()) {

            dimenticaStalloBerserk(player);

        }

        faceBerserkTarget(player, target, true);

        LOGGER.info("Venom berserk locked target {} for {}", EntityType.getKey(target.getType()), player.getGameProfile().getName());

        return target;

    }



    private static boolean canUseBerserkTarget(ServerPlayer player, LivingEntity target) {

        return target.isAlive()

                && !target.isSpectator()

                && target != player

                && target.distanceToSqr(player) <= BERSERK_TARGET_RANGE * BERSERK_TARGET_RANGE;

    }



    private static boolean isNonAggressiveBerserkTarget(ServerPlayer player, LivingEntity target) {

        if (target instanceof Player) {

            return false;

        }

        if (target instanceof Enemy) {

            return false;

        }

        return !(target instanceof Mob mob) || mob.getTarget() != player;

    }



    private static void chaseBerserkTarget(ServerPlayer player, LivingEntity target) {

        Vec3 targetCenter = getTargetCenter(target);

        faceBerserkTarget(player, target);

        Vec3 toTarget = targetCenter.subtract(player.position().add(0.0D, player.getBbHeight() * 0.45D, 0.0D));

        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);

        if (horizontal.lengthSqr() <= BERSERK_CHASE_STOP_RANGE * BERSERK_CHASE_STOP_RANGE) {

            return;

        }



        Vec3 direction = horizontal.normalize();

        Vec3 current = player.getDeltaMovement();

        double verticalAssist = target.getY() > player.getY() + 1.0D ? 0.12D : Math.min(current.y, 0.08D);

        if (shouldBerserkJumpForward(player, target, direction)) {

            player.setDeltaMovement(

                    direction.x * BERSERK_STUCK_JUMP_FORWARD_SPEED,

                    Math.max(current.y, BERSERK_STUCK_JUMP_UP_SPEED),

                    direction.z * BERSERK_STUCK_JUMP_FORWARD_SPEED

            );

            player.fallDistance = 0.0F;

            player.hasImpulse = true;

            player.hurtMarked = true;

            LOGGER.info("Venom berserk unstuck jump toward {} for {}", EntityType.getKey(target.getType()), player.getGameProfile().getName());

            return;

        }



        player.setDeltaMovement(

                current.x * 0.35D + direction.x * BERSERK_CHASE_SPEED,

                verticalAssist,

                current.z * 0.35D + direction.z * BERSERK_CHASE_SPEED

        );

        player.fallDistance = 0.0F;

        player.hasImpulse = true;

        player.hurtMarked = true;

    }



    private static void faceBerserkTarget(ServerPlayer player, LivingEntity target) {

        faceBerserkTarget(player, target, false);

    }



    private static void faceBerserkTarget(ServerPlayer player, LivingEntity target, boolean syncRotation) {

        Vec3 from = player.getEyePosition();

        Vec3 to = getTargetCenter(target);

        Vec3 delta = to.subtract(from);

        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        if (horizontal < 1.0E-4D) {

            return;

        }



        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);

        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        player.setYRot(yaw);

        player.setYHeadRot(yaw);

        player.setYBodyRot(yaw);

        player.setXRot(pitch);

        if (syncRotation) {

            player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);

        }

        player.hurtMarked = true;

    }



    /**

     * Accende la locomozione per scavalcare un ostacolo che i salti non hanno risolto.

     *

     * <p>Se il giocatore l'aveva gia' accesa per conto suo non la marchiamo come nostra, e

     * quindi non gliela spegneremo quando si sblocca.</p>

     */

    private static void accendiLocomozioneBerserk(ServerPlayer player, BerserkStuckState stato) {

        if (stato.locomozioneNostra

                || getScore(player, VenomTentaclesTraversalHandler.OBJECTIVE_NAME, false) > 0) {

            return;

        }

        setScore(player, VenomTentaclesTraversalHandler.OBJECTIVE_NAME, 1);

        stato.locomozioneNostra = true;

        LOGGER.info("Venom berserk tentacles_traversal engaged to clear an obstacle for {}",

                player.getGameProfile().getName());

    }



    /** Spegne la locomozione solo se l'avevamo accesa noi per lo stallo. */

    private static void spegniLocomozioneBerserk(ServerPlayer player, BerserkStuckState stato) {

        if (stato == null || !stato.locomozioneNostra) {

            return;

        }

        setScore(player, VenomTentaclesTraversalHandler.OBJECTIVE_NAME, 0);

        stato.locomozioneNostra = false;

        LOGGER.info("Venom berserk tentacles_traversal released for {}", player.getGameProfile().getName());

    }



    /** Dimentica lo stallo, spegnendo prima la locomozione se l'avevamo accesa noi. */

    private static void dimenticaStalloBerserk(ServerPlayer player) {

        spegniLocomozioneBerserk(player, BERSERK_STUCK_MEMORY.get(player.getUUID()));

        BERSERK_STUCK_MEMORY.remove(player.getUUID());

    }



    private static boolean shouldBerserkJumpForward(ServerPlayer player, LivingEntity target, Vec3 direction) {

        UUID playerId = player.getUUID();

        BerserkStuckState state = BERSERK_STUCK_MEMORY.computeIfAbsent(playerId,

                id -> new BerserkStuckState(target.getId(), player.position()));

        if (state.targetId != target.getId()) {

            state.targetId = target.getId();

            state.lastPosition = player.position();

            state.stuckTicks = 0;

            return false;

        }



        Vec3 delta = player.position().subtract(state.lastPosition);

        double horizontalMoveSqr = delta.x * delta.x + delta.z * delta.z;

        if (horizontalMoveSqr <= BERSERK_STUCK_DISTANCE_SQR && player.horizontalCollision) {

            state.stuckTicks++;

        } else {

            state.stuckTicks = 0;

            state.saltiFalliti = 0;

            state.lastPosition = player.position();

            spegniLocomozioneBerserk(player, state);   // sbloccato: torna a camminare

        }



        if (state.stuckTicks < BERSERK_STUCK_JUMP_TICKS || direction.lengthSqr() < 1.0E-4D) {

            return false;

        }



        state.stuckTicks = 0;

        state.lastPosition = player.position().add(direction.scale(0.75D));

        state.saltiFalliti++;

        if (state.saltiFalliti >= BERSERK_SALTI_PRIMA_DI_LOCOMOZIONE) {

            accendiLocomozioneBerserk(player, state);

        }

        return true;

    }



    private static Vec3 getTargetCenter(LivingEntity target) {

        return target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);

    }



    private static void spawnSymbioteNear(ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel level)) {

            return;

        }

        Entity symbiote = MyMod.SYMBIOTE_ENTITY.get().create(level);

        if (symbiote == null) {

            return;

        }



        Vec3 look = player.getLookAngle();

        Vec3 offset = new Vec3(look.x, 0.0D, look.z);

        if (offset.lengthSqr() < 1.0E-4D) {

            offset = new Vec3(1.0D, 0.0D, 0.0D);

        }

        Vec3 spawn = player.position().add(offset.normalize().scale(3.0D));

        symbiote.moveTo(spawn.x, player.getY(), spawn.z, player.getYRot(), 0.0F);

        level.addFreshEntity(symbiote);

    }



    private static boolean hasAirBelow(LivingEntity entity, int blocks) {

        BlockPos feet = entity.blockPosition();

        for (int i = 1; i <= blocks; i++) {

            if (isSolid(entity, feet.below(i))) {

                return false;

            }

        }

        return true;

    }



    private static boolean hasWallInFront(LivingEntity entity) {

        Vec3 horizontalLook = getHorizontalLook(entity);

        if (horizontalLook.lengthSqr() < 1.0E-4D) {

            return false;

        }



        double[] heights = {0.25D, Math.min(1.2D, entity.getBbHeight() * 0.5D), Math.max(0.35D, entity.getBbHeight() - 0.2D)};

        for (double height : heights) {

            Vec3 start = entity.position().add(0.0D, height, 0.0D);

            Vec3 end = start.add(horizontalLook.scale(1.35D));

            BlockHitResult hit = entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));

            if (hit.getType() != HitResult.Type.MISS && isValidClimbSurface(entity, hit.getBlockPos())) {

                return true;

            }

        }

        return false;

    }



    private static boolean hasWallAround(LivingEntity entity) {

        BlockPos base = entity.blockPosition();

        for (Direction direction : Direction.Plane.HORIZONTAL) {

            if (isValidClimbSurface(entity, base.relative(direction)) || isValidClimbSurface(entity, base.above().relative(direction))) {

                return true;

            }

        }

        return false;

    }



    private static boolean hasWallTouchingBoundingBox(LivingEntity entity) {

        AABB entityBox = entity.getBoundingBox();

        AABB box = entityBox.inflate(0.08D, 0.0D, 0.08D);

        int minX = (int) Math.floor(box.minX);

        int maxX = (int) Math.floor(box.maxX);

        int minY = (int) Math.floor(box.minY + 0.1D);

        int maxY = (int) Math.floor(box.maxY - 0.1D);

        int minZ = (int) Math.floor(box.minZ);

        int maxZ = (int) Math.floor(box.maxZ);



        for (int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {

                for (int z = minZ; z <= maxZ; z++) {

                    BlockPos pos = new BlockPos(x, y, z);

                    if (!isValidClimbSurface(entity, pos)) {

                        continue;

                    }



                    AABB blockBox = new AABB(pos);

                    boolean sideTouch = blockBox.maxX <= entityBox.minX + 0.12D

                            || blockBox.minX >= entityBox.maxX - 0.12D

                            || blockBox.maxZ <= entityBox.minZ + 0.12D

                            || blockBox.minZ >= entityBox.maxZ - 0.12D;

                    if (sideTouch && blockBox.intersects(box)) {

                        return true;

                    }

                }

            }

        }

        return false;

    }



    private static boolean hasCeiling(ServerPlayer player) {

        BlockPos above = player.blockPosition().above(2);

        for (Direction direction : Direction.Plane.HORIZONTAL) {

            if (isSolid(player, above.relative(direction))) {

                return true;

            }

        }

        return isSolid(player, above);

    }



    private static boolean canFitVenomVisualAt(ServerPlayer player, double y) {

        double halfWidth = VENOM_VISUAL_COLLISION_WIDTH / 2.0D;

        AABB visualBox = new AABB(

                player.getX() - halfWidth,

                y,

                player.getZ() - halfWidth,

                player.getX() + halfWidth,

                y + VENOM_VISUAL_COLLISION_HEIGHT,

                player.getZ() + halfWidth

        ).deflate(1.0E-6D);

        return player.level().noCollision(player, visualBox);

    }



    private static double findNearestVenomVisualFitY(ServerPlayer player) {

        double currentY = player.getY();

        for (int step = 1; step <= 12; step++) {

            double testY = currentY - step * 0.125D;

            if (canFitVenomVisualAt(player, testY)) {

                return testY;

            }

        }

        return currentY;

    }



    private static boolean isValidClimbSurface(LivingEntity entity, BlockPos pos) {

        BlockState state = entity.level().getBlockState(pos);

        return isSolid(entity, pos) && !state.is(BlockTags.STAIRS) && !state.is(BlockTags.SLABS);

    }



    private static boolean isSolid(LivingEntity entity, BlockPos pos) {

        BlockState state = entity.level().getBlockState(pos);

        return !state.isAir() && !state.getCollisionShape(entity.level(), pos).isEmpty();

    }



    private static boolean hasVenomPower(Player player) {

        return player.getTags().contains(PlayerPowerCapability.VENOM_TAG)

                || player.getCapability(PlayerPowerCapability.PLAYER_POWER)

                .map(power -> power.isTransformed()
                        && ("venom".equals(power.getForm())
                            || "venomspidey".equals(power.getForm())))

                .orElse(false);

    }



    private static boolean isClimbAbilityEnabled(Player player) {

        return getScore(player, CLIMB_ENABLED_OBJECTIVE, false) == 1;

    }



    public static void setClimbInput(ServerPlayer player, boolean forward, boolean ctrl, boolean shift) {

        if (forward) {

            CLIMB_FORWARD_INPUT.put(player.getUUID(), true);

        } else {

            CLIMB_FORWARD_INPUT.remove(player.getUUID());

        }



        if (ctrl) {

            CLIMB_CTRL_INPUT.put(player.getUUID(), true);

        } else {

            CLIMB_CTRL_INPUT.remove(player.getUUID());

        }



        if (shift) {

            CLIMB_SNEAK_INPUT.put(player.getUUID(), true);

        } else {

            CLIMB_SNEAK_INPUT.remove(player.getUUID());

        }

    }



    private static boolean isBodyActive(ServerPlayer player) {

        return getScore(player, CAMERA_OBJECTIVE, false) > 0;

    }



    /** Fa scendere di uno un contatore in tick, se e' ancora acceso. */

    private static void scalaContatore(ServerPlayer player, String objective) {

        int rimasti = getScore(player, objective, false);

        if (rimasti > 0) {

            setScore(player, objective, rimasti - 1);

        }

    }



    /**

     * Porta avanti la posa di scossa e, quando finisce, rimanda il giocatore in forma umana.

     *

     * <p>La scossa scaccia il simbionte: chi la subisce trasformato guarda l'animazione fino in

     * fondo e poi si ritrova uomo. Il rientro passa da un lucchetto di un secondo su "Unleash

     * the symbiote", perche' quella e' un'abilita' a interruttore di Palladium e spegnerla

     * scrivendo un punteggio non basterebbe: la riaccenderebbe da sola al tick dopo.</p>

     */

    /**

     * Sorveglia il momento in cui il corpo simbionte si spegne e apre la finestra del ritorno.

     *

     * <p>Per quella finestra la maschera del ritorno sale a 16 restando nascosta sotto il

     * modello che si ritira; quando la finestra si chiude il timer ridiscende, ed e' quella

     * discesa che si vede sulla pelle. Cosi' la maschera esce dopo l'animazione e non insieme,

     * come invece fa quella legata al timer della trasformazione.</p>

     */

    private static void tickRevert(ServerPlayer player) {

        boolean corpo = getScore(player, VenomPlayerSizeHandler.SIZE_OBJECTIVE, false) > 0;

        Boolean prima = CORPO_PRECEDENTE.put(player.getUUID(), corpo);



        if (Boolean.TRUE.equals(prima) && !corpo) {

            setScore(player, REVERT_OBJECTIVE, REVERT_TICKS);

            LOGGER.info("Il simbionte di {} si sta ritirando",

                    player.getGameProfile().getName());

        } else {

            scalaContatore(player, REVERT_OBJECTIVE);

        }

    }



    private static void tickVibrant(ServerPlayer player) {

        int rimasti = getScore(player, VIBRANT_OBJECTIVE, false);

        if (rimasti <= 0) {

            scalaContatore(player, FORCE_HUMAN_OBJECTIVE);

            return;

        }



        setScore(player, VIBRANT_OBJECTIVE, rimasti - 1);

        if (rimasti == 1 && getScore(player, VenomPlayerSizeHandler.SIZE_OBJECTIVE, false) > 0) {

            setScore(player, FORCE_HUMAN_OBJECTIVE, FORCE_HUMAN_TICKS);

            LOGGER.info("La scossa ha ricacciato dentro il simbionte di {}",

                    player.getGameProfile().getName());

        }

    }



    private static boolean isVulnerable(ServerPlayer player) {

        return getScore(player, VULNERABILITY_OBJECTIVE, false) > 0;

    }



    /**

     * Riporta la fame al massimo e fa sparire la barra. La chiamano la trasformazione e il

     * ritorno umano: ogni volta che il superpotere viene messo o tolto si riparte sazi.

     */

    public static void resetHunger(ServerPlayer player) {

        setScore(player, HUNGER_OBJECTIVE, MAX_HUNGER);

        setScore(player, BERSERK_OBJECTIVE, 0);

        setScore(player, BERSERK_PHASE_OBJECTIVE, 0);

        setScore(player, BERSERK_TICKS_OBJECTIVE, 0);

        setScore(player, AUTO_HEAD_OBJECTIVE, 0);

        player.getPersistentData().putBoolean("Klyntar.HungerInitialized", true);

        removeHungerBar(player);

    }



    /** La fame del simbionte in percentuale, da 0 a 100. */

    public static int getHunger(ServerPlayer player) {

        return Math.max(0, Math.min(MAX_HUNGER, getScore(player, HUNGER_OBJECTIVE, false)));

    }



    /** Indebolito da fuoco o suono? Lo leggono i passivi che vivono negli altri gestori. */

    public static boolean isPlayerVulnerable(ServerPlayer player) {

        return isVulnerable(player);

    }



    /** Fa comparire un simbionte a tre blocchi dal giocatore. */

    public static void spawnSymbioteNearPlayer(ServerPlayer player) {

        spawnSymbioteNear(player);

    }



    private static void ensureHungerScore(ServerPlayer player) {

        if (getScore(player, HUNGER_OBJECTIVE, true) <= 0 && !player.getPersistentData().getBoolean("Klyntar.HungerInitialized")) {

            setScore(player, HUNGER_OBJECTIVE, MAX_HUNGER);

            player.getPersistentData().putBoolean("Klyntar.HungerInitialized", true);

        }

    }



    private static ServerBossEvent createHungerBar(ServerPlayer player) {

        ServerBossEvent event = new ServerBossEvent(Component.literal("Symbiote Hunger"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

        event.setDarkenScreen(false);

        event.setCreateWorldFog(false);

        event.addPlayer(player);

        return event;

    }



    private static void removeHungerBar(ServerPlayer player) {

        ServerBossEvent event = HUNGER_BARS.remove(player.getUUID());

        if (event != null) {

            event.removePlayer(player);

        }

    }



    private static void startFallFlyingFlag(ServerPlayer player) {

        setFallFlyingFlag(player, true);

    }



    private static void stopFallFlyingFlag(ServerPlayer player) {

        if (player.isFallFlying()) {

            setFallFlyingFlag(player, false);

        }

    }



    private static void setFallFlyingFlag(ServerPlayer player, boolean value) {

        try {

            Method method = Entity.class.getDeclaredMethod("setSharedFlag", int.class, boolean.class);

            method.setAccessible(true);

            method.invoke(player, 7, value);

        } catch (ReflectiveOperationException ignored) {

        }

    }



    private static void addScore(ServerPlayer player, String objectiveName, int amount) {

        setScore(player, objectiveName, getScore(player, objectiveName, true) + amount);

    }



    private static int getScore(Player player, String objectiveName, boolean create) {

        Objective objective = getObjective(player, objectiveName, create);

        if (objective == null) {

            return 0;

        }

        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);

        return score.getScore();

    }



    private static void setScore(ServerPlayer player, String objectiveName, int value) {

        Objective objective = getObjective(player, objectiveName, true);

        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);

        score.setScore(value);

    }



    private static Objective getObjective(Player player, String objectiveName, boolean create) {

        Scoreboard scoreboard = player.getScoreboard();

        Objective objective = scoreboard.getObjective(objectiveName);

        if (objective == null && create) {

            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);

        }

        return objective;

    }



    private static final class BerserkStuckState {

        private int targetId;

        private Vec3 lastPosition;

        private int stuckTicks;

        /** salti tentati senza che il giocatore si sia mosso davvero */

        private int saltiFalliti;

        /** vero se la locomozione l'abbiamo accesa noi, e quindi tocca a noi spegnerla */

        private boolean locomozioneNostra;



        private BerserkStuckState(int targetId, Vec3 lastPosition) {

            this.targetId = targetId;

            this.lastPosition = lastPosition;

        }

    }



    private static final class FlightState {

        private final double launchStartY;

        private int launchTicks;

        private int propellerTicks;

        private boolean forwardPressedLastTick;

        private boolean launching = true;



        private FlightState(double launchStartY) {

            this.launchStartY = launchStartY;

        }

    }

}

