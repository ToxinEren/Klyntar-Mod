package modKlyntar.input;

import modKlyntar.MyMod;
import net.minecraft.world.phys.EntityHitResult;
import modKlyntar.input.KeyBindings;
import modKlyntar.power.PlayersPower;
import modKlyntar.power.PlayersPowerProvider;
import modKlyntar.entity.custom.WebProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "mymod", value = Dist.CLIENT)
public class KeyInputHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean isSwinging = false;
    private static Vec3 targetPosition = null;
    private static Entity targetEntityToMove = null;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key  event) {
        LOGGER.debug("Key input event received: " + event.getKey());

        if (KeyBindings.toggleAbility1.consumeClick()) {
            LOGGER.debug("Toggle Ability 1 key pressed");
            handleToggleAbility1();
        }

        if (KeyBindings.toggleAbility2.consumeClick()) {
            LOGGER.debug("Toggle Ability 2 key pressed");
            handleToggleAbility2();
        }
        
        if (KeyBindings.toggleAbility3.consumeClick()) {
            LOGGER.debug("Toggle Ability 3 key pressed");
            handleTogglePlayerScale();
        }

    }

    private static void handleToggleAbility1() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                if (power.getSymbioteLevel() == 1) {
                    LOGGER.debug("Player has symbiote level 1, toggling invisibility");
                    // Toggle dell'invisibilità
                    if (player.hasEffect(MobEffects.INVISIBILITY)) {
                        player.removeEffect(MobEffects.INVISIBILITY);
                        player.displayClientMessage(Component.literal("Invisibility disabled"), false);
                        setPlayerModelVisible(player, true);
                    } else {
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, true));
                        player.displayClientMessage(Component.literal("Invisibility enabled"), false);
                        setPlayerModelVisible(player, false);
                    }
                }
            });
        } else {
            LOGGER.debug("Player is null");
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            setPlayerModelVisible(player, false);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        }

        if (player.level().isClientSide && isSwinging && targetPosition != null) {
            Vec3 playerPos = player.position();
            Vec3 direction;

            if (targetEntityToMove != null) {
                // Movimento del target verso il player
                Vec3 targetPos = targetEntityToMove.position();
                direction = playerPos.subtract(targetPos).normalize().scale(1.0); // Adjust the speed as needed
                targetEntityToMove.setDeltaMovement(direction);

                if (targetPos.distanceTo(playerPos) < 1.0) {
                    isSwinging = false;
                    targetPosition = null;
                    targetEntityToMove.setDeltaMovement(Vec3.ZERO);
                    targetEntityToMove = null; // Reset target entity
                }
            } else {
                // Movimento del player verso il target
                direction = targetPosition.subtract(playerPos).normalize().scale(1.0); // Adjust the speed as needed
                player.setDeltaMovement(direction);

                if (playerPos.distanceTo(targetPosition) < 1.0) {
                    isSwinging = false;
                    targetPosition = null;
                    player.setDeltaMovement(Vec3.ZERO);
                }
            }
        }
    }

    private static void handleToggleAbility2() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                if (power.getSymbioteLevel() == 1) {
                    LOGGER.debug("Player has symbiote level 1, toggling ability 2");
                    shootWeb(player);
                }
            });
        } else {
            LOGGER.debug("Player is null");
        }
    }
    
    private static EntityHitResult getEntityHitResult(Player player, Vec3 startVec, Vec3 endVec, double padding) {
        Level level = player.level();
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(level, player, startVec, endVec, player.getBoundingBox().expandTowards(endVec).inflate(padding), e -> !e.isSpectator() && e.isPickable());
        return entityHitResult;
    } 
    
    private static void handleTogglePlayerScale() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                if (power.getSymbioteLevel() == 1) {
                    LOGGER.debug("Player has symbiote level 1, toggling player scale");
                    
                    // Invertiamo la scala del giocatore
                    float currentScale = player.getEntityData().get(MyMod.PLAYER_SCALE);
                    float newScale = (currentScale == 1.0f) ? 1.5f : 1.0f;

                    player.getEntityData().set(MyMod.PLAYER_SCALE, newScale);
                    player.displayClientMessage(Component.literal("Player scale set to " + newScale), false);
                }
            });
        }
    }

    private static void shootWeb(Player player) {
        // Ray trace per i blocchi
        HitResult blockHitResult = player.pick(70.0, 0.0f, false);

        // Ray trace per le entità
        Vec3 startVec = player.getEyePosition(0.0f);
        Vec3 endVec = startVec.add(player.getViewVector(0.0f).scale(70.0));
        EntityHitResult entityHitResult = getEntityHitResult(player, startVec, endVec, 1.0);

        HitResult finalHitResult = null;

        // Confronta i risultati dei ray trace
        if (entityHitResult != null && (blockHitResult == null || startVec.distanceTo(entityHitResult.getLocation()) < startVec.distanceTo(blockHitResult.getLocation()))) {
            finalHitResult = entityHitResult;
        } else {
            finalHitResult = blockHitResult;
        }

        if (finalHitResult != null) {
            Level level = player.level();
            WebProjectileEntity webProjectile = new WebProjectileEntity(MyMod.WEB_PROJECTILE_ENTITY.get(), level);
            webProjectile.setPos(player.getX(), player.getEyeY(), player.getZ());

            if (finalHitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = ((BlockHitResult) finalHitResult).getBlockPos();
                webProjectile.setTargetPos(hitPos.getX() + 0.5, hitPos.getY() + 1, hitPos.getZ() + 0.5);
                targetPosition = new Vec3(hitPos.getX() + 0.5, hitPos.getY() + 1, hitPos.getZ() + 0.5);
            } else if (finalHitResult.getType() == HitResult.Type.ENTITY) {
                Entity targetEntity = ((EntityHitResult) finalHitResult).getEntity();
                webProjectile.setTargetPos(targetEntity.getX(), targetEntity.getY() + targetEntity.getEyeHeight(), targetEntity.getZ());
                targetPosition = targetEntity.position();
                targetEntityToMove = targetEntity;
            }

            player.displayClientMessage(Component.literal("DEBUG: Web Line spawn"), false);
            level.addFreshEntity(webProjectile);
            isSwinging = true;
            player.displayClientMessage(Component.literal("Web shot!"), false);
        } else {
            player.displayClientMessage(Component.literal("No target found!"), false);
        }
    }


    private static void spawnWebParticles(Vec3 startPos, Vec3 endPos) {
        Vec3 direction = endPos.subtract(startPos);
        double distance = startPos.distanceTo(endPos);
        int particleCount = (int) (distance * 5); // Number of particles along the line

        for (int i = 0; i < particleCount; i++) {
            double factor = (double) i / (particleCount - 1);
            Vec3 particlePos = startPos.add(direction.scale(factor));
            Minecraft.getInstance().level.addParticle(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
    }
    
    private static void setPlayerModelVisible(Player player, boolean visible) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(player.getUUID())) {
                Minecraft.getInstance().player.setInvisible(!visible);
                LOGGER.debug("Set player model visibility to: " + visible);
            }
        });
    }
}