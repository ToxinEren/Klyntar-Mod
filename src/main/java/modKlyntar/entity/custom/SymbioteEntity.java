package modKlyntar.entity.custom;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import modKlyntar.capability.PlayerPowerCapability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import modKlyntar.power.PlayersPower;
import modKlyntar.power.PlayersPowerProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class SymbioteEntity extends Mob implements GeoEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Animal hostAnimal;

    public SymbioteEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new ApproachPlayerGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new WanderAroundGoal(this, 1.0D));
    }

    static class ApproachPlayerGoal extends Goal {
        private final SymbioteEntity symbiote;
        private Player targetPlayer;
        private final double speed;

        public ApproachPlayerGoal(SymbioteEntity symbiote, double speed) {
            this.symbiote = symbiote;
            this.speed = speed;
        }

        @Override
        public boolean canUse() {
            if (this.symbiote.hostAnimal == null) {
                this.targetPlayer = this.symbiote.level().getNearestPlayer(this.symbiote, 10.0D);
                return this.targetPlayer != null && !this.targetPlayer.isCreative() && this.targetPlayer.distanceTo(this.symbiote) > 1.0D;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPlayer != null && !this.targetPlayer.isCreative() && this.targetPlayer.distanceTo(this.symbiote) > 1.0D;
        }

        @Override
        public void tick() {
            if (this.targetPlayer != null) {
                this.symbiote.getNavigation().moveTo(this.targetPlayer, this.speed);
                if (this.targetPlayer.distanceTo(this.symbiote) <= 1.0D) {
                    this.symbiote.doHurtTarget(this.targetPlayer);
                }
            }
        }
    }

    static class WanderAroundGoal extends Goal {
        private final SymbioteEntity symbiote;
        private final double speed;
        private final TargetingConditions animalTargeting;

        public WanderAroundGoal(SymbioteEntity symbiote, double speed) {
            this.symbiote = symbiote;
            this.speed = speed;
            this.animalTargeting = TargetingConditions.forNonCombat().range(10.0D);
        }

        @Override
        public boolean canUse() {
            if (this.symbiote.hostAnimal == null) {
                this.symbiote.hostAnimal = this.symbiote.level().getNearestEntity(Animal.class, this.animalTargeting, this.symbiote, this.symbiote.getX(), this.symbiote.getY(), this.symbiote.getZ(), this.symbiote.getBoundingBox().inflate(10.0D));
                return this.symbiote.hostAnimal != null;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.symbiote.hostAnimal != null && !this.symbiote.hostAnimal.isDeadOrDying();
        }

        @Override
        public void tick() {
            if (this.symbiote.hostAnimal != null) {
                this.symbiote.getNavigation().moveTo(this.symbiote.hostAnimal, this.speed);
                if (this.symbiote.hostAnimal.distanceTo(this.symbiote) <= 1.0D) {
                    this.symbiote.despawnAndAttachToAnimal();
                }
                Player nearestPlayer = this.symbiote.level().getNearestPlayer(this.symbiote.hostAnimal, 10.0D);
                if (nearestPlayer != null && !nearestPlayer.isCreative()) {
                    this.symbiote.hostAnimal.getNavigation().moveTo(nearestPlayer, this.speed);
                }
            }
        }
    }

    private void despawnAndAttachToAnimal() {
        if (!this.level().isClientSide && this.hostAnimal != null) {
            this.hostAnimal.getPersistentData().putBoolean("InfectedBySymbiote", true);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    private void spawnAtAnimalLocation() {
        if (!this.level().isClientSide && this.hostAnimal != null) {
            SymbioteEntity newSymbiote = (SymbioteEntity) this.getType().create((ServerLevel) this.level());
            if (newSymbiote != null) {
                newSymbiote.moveTo(this.hostAnimal.getX(), this.hostAnimal.getY(), this.hostAnimal.getZ(), this.hostAnimal.getYRot(), this.hostAnimal.getXRot());
                this.level().addFreshEntity(newSymbiote);
            }
        }
    }

    public void doPlayerEffect(ServerPlayer player) {
        LOGGER.info("Symbiote infection triggered for {}", player.getGameProfile().getName());
        player.displayClientMessage(Component.literal("You have bonded with a symbiote."), false);
        PlayerPowerCapability.infectPlayer(player);
        // il livello di simbionte serve ad arrampicata e caduta; effetti e attributi
        // li mette gia' la trasformazione, che sa di che forma si tratta
        player.getCapability(PlayersPowerProvider.PLAYERS_POWER)
                .ifPresent(power -> power.addSymbiote(1));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || source.isCreativePlayer()) {
            return super.hurt(source, amount);
        } else if (source.getEntity() instanceof ServerPlayer player) {
            this.doPlayerEffect(player);
            this.remove(RemovalReason.DISCARDED);
            return false;
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (!this.level().isClientSide && target instanceof ServerPlayer player && !player.isCreative()) {
            this.doPlayerEffect(player);
            this.remove(RemovalReason.DISCARDED);
            return true;
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.hostAnimal == null) {
            for (Player player : this.level().players()) {
                if (!player.isCreative() && player.distanceTo(this) <= 1.0D) {
                    this.doHurtTarget(player);
                    break;
                }
            }
        } else if (this.hostAnimal.isDeadOrDying()) {
            this.spawnAtAnimalLocation();
            this.hostAnimal = null;
        }
    }

    private <E extends GeoEntity> PlayState predicate(AnimationState<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("tentacles"));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(RawAnimation.begin().thenLoop("tentacles"));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
