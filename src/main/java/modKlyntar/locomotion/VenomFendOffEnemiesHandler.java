package modKlyntar.locomotion;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomFendOffEnemiesHandler {
    private static final String OBJECTIVE_NAME = "Venom.FendOffEnemies";
    private static final double ATTACK_RANGE = 10.0D;
    private static final float ATTACK_DAMAGE = 10.0F;
    private static final int MAX_ARMS = 6;
    private static final int ATTACK_INTERVAL_TICKS = 10;
    private static final Map<UUID, Map<Integer, Long>> TARGET_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> PLAYER_AGGRESSORS = new ConcurrentHashMap<>();
    /** chi ha braccia in scena adesso: serve a non calpestare i poteri simbionte */
    private static final Set<UUID> SHOWING_ARMS = ConcurrentHashMap.newKeySet();

    private VenomFendOffEnemiesHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        int active = getScore(player, OBJECTIVE_NAME, false);
        if (active <= 0) {
            TARGET_COOLDOWNS.remove(player.getUUID());
            PLAYER_AGGRESSORS.remove(player.getUUID());
            // il canale dei tentacoli e' in comune con i poteri simbionte: va svuotato una
            // volta sola allo spegnimento, non a ogni tick, o cancella i loro bersagli
            if (SHOWING_ARMS.remove(player.getUUID())) {
                ModNetwork.syncVenomCombatTargets(player, List.of());
            }
            return;
        }

        tickCombat(player);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (victim.level().isClientSide || getScore(victim, OBJECTIVE_NAME, false) <= 0) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player attackingPlayer) || attacker == victim) {
            return;
        }

        PLAYER_AGGRESSORS.computeIfAbsent(victim.getUUID(), id -> ConcurrentHashMap.newKeySet()).add(attackingPlayer.getUUID());
    }

    private static void tickCombat(ServerPlayer player) {
        List<LivingEntity> targets = findTargets(player);
        List<Vec3> targetCenters = new ArrayList<>();
        long gameTime = player.level().getGameTime();
        Map<Integer, Long> cooldowns = TARGET_COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());

        for (LivingEntity target : targets) {
            targetCenters.add(getTargetCenter(target));
            long nextAttackTick = cooldowns.getOrDefault(target.getId(), 0L);
            if (gameTime < nextAttackTick) {
                continue;
            }

            target.hurt(player.damageSources().playerAttack(player), ATTACK_DAMAGE);
            knockTargetBack(player, target);
            cooldowns.put(target.getId(), gameTime + ATTACK_INTERVAL_TICKS);
        }

        cleanupCooldowns(cooldowns, gameTime);
        if (targetCenters.isEmpty()) {
            SHOWING_ARMS.remove(player.getUUID());
        } else {
            SHOWING_ARMS.add(player.getUUID());
        }
        ModNetwork.syncVenomCombatTargets(player, targetCenters);
    }

    private static List<LivingEntity> findTargets(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(ATTACK_RANGE);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> canAttackEntity(entity, player));
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        if (targets.size() > MAX_ARMS) {
            return new ArrayList<>(targets.subList(0, MAX_ARMS));
        }
        return targets;
    }

    private static boolean canAttackEntity(LivingEntity entity, ServerPlayer player) {
        if (entity == null || !entity.isAlive() || entity.isSpectator() || entity == player) {
            return false;
        }
        if (entity.distanceToSqr(player) > ATTACK_RANGE * ATTACK_RANGE) {
            return false;
        }
        if (!player.hasLineOfSight(entity)) {
            return false;
        }
        if (entity instanceof Player targetPlayer) {
            return isRememberedAggressor(player, targetPlayer);
        }
        if (entity instanceof Enemy) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() == player;
    }

    private static boolean isRememberedAggressor(ServerPlayer victim, Player targetPlayer) {
        Set<UUID> aggressors = PLAYER_AGGRESSORS.get(victim.getUUID());
        return aggressors != null && aggressors.contains(targetPlayer.getUUID());
    }

    private static Vec3 getTargetCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
    }

    private static void knockTargetBack(ServerPlayer player, LivingEntity target) {
        Vec3 push = target.position().subtract(player.position());
        if (push.lengthSqr() < 1.0E-4D) {
            push = player.getLookAngle();
        }
        push = push.normalize().scale(0.35D);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.12D, push.z));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static void cleanupCooldowns(Map<Integer, Long> cooldowns, long gameTime) {
        Iterator<Map.Entry<Integer, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > 40L) {
                iterator.remove();
            }
        }
    }

    private static int getScore(ServerPlayer player, String objectiveName, boolean create) {
        Objective objective = getObjective(player, objectiveName, create);
        if (objective == null) {
            return 0;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static Objective getObjective(Player player, String objectiveName, boolean create) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null && create) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }
}
