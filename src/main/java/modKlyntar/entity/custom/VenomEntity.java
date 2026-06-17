package modKlyntar.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

import modKlyntar.MyMod;

public class VenomEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean isInvulnerable = true;
    private boolean isGrabbing = false;
    private Entity grabbedEntity = null;
    private boolean isImmobilized = false;
    private int grabCooldown = 0;

    public VenomEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.JUMP_STRENGTH, 4.0D);
    }

    @Override
    public void tick() {
        super.tick();

        // Effetto di rigenerazione
        if (!this.isOnFire()) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false, false));
        }

        // Gestione invulnerabilità
        if (this.isOnFire()) {
            this.isInvulnerable = false;
        } else {
            this.isInvulnerable = true;
        }

        // Gestione entità afferrata
        if (isGrabbing && grabbedEntity != null) {
            Vec3 lookDirection = this.getLookAngle().normalize();
            Vec3 grabPosition = this.position().add(lookDirection.scale(1.5)).add(0, 1.5, 0); // Posiziona l'entità afferrata di fronte a VenomEntity
            grabbedEntity.setPos(grabPosition.x, grabPosition.y, grabPosition.z);
            grabbedEntity.setDeltaMovement(Vec3.ZERO); // Ferma il movimento dell'entità afferrata
        }

        // Immobilizzazione durante l'attacco
        if (isImmobilized) {
            this.setDeltaMovement(Vec3.ZERO); // Ferma il movimento dell'entità
        }

        // Gestione cooldown del grab
        if (grabCooldown > 0) {
            grabCooldown--;
        }

        // Arrampicarsi come gli spider
        if (this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.2, 0.0));
        }

        // Distruzione dei blocchi solo se non riesce a muoversi o raggiungere il target
        if (!this.level().isClientSide() && (this.getNavigation().isDone() || !this.canReachTarget())) {
            BlockPos pos = this.blockPosition();
            for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
                Block block = this.level().getBlockState(blockPos).getBlock();
                if (block != Blocks.OBSIDIAN && block != Blocks.STONE && block != Blocks.DEEPSLATE && block != Blocks.DEEPSLATE_TILES && block != Blocks.DEEPSLATE_BRICKS && block != Blocks.GRASS && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT && block != Blocks.DIRT_PATH) {
                    this.level().destroyBlock(blockPos, true, this);
                }
            }
        }
    }

    private boolean canReachTarget() {
        Entity target = this.getTarget();
        return target != null && this.distanceTo(target) <= this.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerable) {
            return false;
        } else {
            return super.hurt(source, amount);
        }
    }

    private <E extends GeoEntity> PlayState predicate(AnimationState<E> event) {
        AnimationController<VenomEntity> controller = (AnimationController<VenomEntity>) event.getController();

        if (this.isInLava() || this.isOnFire()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("hurt"));
            return PlayState.CONTINUE;
        }

        if (event.isMoving()) {
            controller.setAnimation(RawAnimation.begin().thenLoop("walk"));
            return PlayState.CONTINUE;
        }

        controller.setAnimation(RawAnimation.begin().thenLoop("growl"));
        return PlayState.CONTINUE;
    }

    private <E extends GeoEntity> PlayState attackPredicate(AnimationState<E> event) {
        if (this.swinging) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("attack"));
            this.swinging = false;
        }
        return PlayState.CONTINUE;
    }

    private <E extends GeoEntity> PlayState grabPredicate(AnimationState<E> event) {
        if (this.isGrabbing) {
            event.getController().setAnimation(RawAnimation.begin().thenPlay("grab"));
            this.isImmobilized = true;
        } else {
            this.isImmobilized = false;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
        controllers.add(new AnimationController<>(this, "attackController", 0, this::attackPredicate));
        controllers.add(new AnimationController<>(this, "grabController", 0, this::grabPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Goal personalizzato per trovare e attaccare entità
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(3, new FindAndKillEntitiesGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new LeapOverBlocksGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new ShootArrowsGoal(this, 1.0D, 20, 20.0F));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, false));
    }

    public void grabEntity(Entity target) {
        if (target != null && !isGrabbing && grabbedEntity == null && grabCooldown == 0) {
            this.isGrabbing = true;
            this.grabbedEntity = target;
            this.grabCooldown = 20; // Imposta il cooldown, ad esempio 5 secondi (100 tick)
        }
    }

    public void throwEntity(double strength) {
        if (isGrabbing && grabbedEntity != null) {
            Vec3 throwDirection = this.getLookAngle().scale(strength);
            grabbedEntity.setDeltaMovement(throwDirection);
            grabbedEntity = null;
            isGrabbing = false;
        }
    }

    class FindAndKillEntitiesGoal extends Goal {
        private final VenomEntity entity;
        private int searchCooldown = 0;

        public FindAndKillEntitiesGoal(VenomEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            if (searchCooldown > 0) {
                searchCooldown--;
                return;
            }

            Level level = entity.level();
            List<Monster> monsters = level.getEntitiesOfClass(Monster.class, entity.getBoundingBox().inflate(60)); // Increased range

            for (Monster monster : monsters) {
                if (!(monster instanceof Creeper) && !(monster instanceof VenomEntity)) {
                    if (!entity.isGrabbing && entity.grabbedEntity == null) {
                        startGrab(monster);
                    }
                    return;
                }
            }
            
            List<Player> players = level.getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(60)); // Increased range

            for (Player player : players) {
                if (!entity.isGrabbing && entity.grabbedEntity == null) {
                    startGrab(player);
                }
                return;
            }

            List<Animal> animals = level.getEntitiesOfClass(Animal.class, entity.getBoundingBox().inflate(60)); // Increased range

            for (Animal animal : animals) {
                if (!entity.isGrabbing && entity.grabbedEntity == null) {
                    startGrab(animal);
                }
                return;
            }
            
            List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class, entity.getBoundingBox().inflate(60)); // Increased range

            for (AbstractVillager villager : villagers) {
                if (!entity.isGrabbing && entity.grabbedEntity == null) {
                    startGrab(villager);
                }
                return;
            }

            List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, entity.getBoundingBox().inflate(60)); // Increased range

            for (Creeper creeper : creepers) {
                if (!entity.isGrabbing && entity.grabbedEntity == null) {
                    startGrab(creeper);
                } else {
                    entity.throwEntity(7.5); // Puoi regolare la forza del lancio
                }
                return;
            }

            searchCooldown = 20; // Cerca ogni secondo
        }

        private void startGrab(Entity target) {
            if (target != null && !target.isInWater() && entity.grabCooldown == 0) {
                entity.grabEntity(target);
                entity.getNavigation().moveTo(target, 1.0);
                entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }

        private void throwGrabbedEntity() {
            if (entity.isGrabbing && entity.grabbedEntity != null) {
                entity.throwEntity(7.5); // Puoi regolare la forza del lancio
            }
        }
    }

    // Goal per far saltare VenomEntity sopra ostacoli di 3 blocchi
    class LeapOverBlocksGoal extends Goal {
        private final VenomEntity entity;
        private final double speedModifier;

        public LeapOverBlocksGoal(VenomEntity entity, double speedModifier) {
            this.entity = entity;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return entity.getTarget() != null && !entity.canReachTarget();
        }

        @Override
        public void start() {
            Vec3 vec3 = this.entity.getDeltaMovement();
            Vec3 vec31 = new Vec3(this.entity.getX() + vec3.x, this.entity.getY() + 1.5D, this.entity.getZ() + vec3.z);
            this.entity.setDeltaMovement(vec31);
        }
    }

    // Goal per sparare frecce
    class ShootArrowsGoal extends Goal {
        private final VenomEntity entity;
        private final double speedModifier;
        private final int attackInterval;
        private final float attackRadius;
        private int attackCooldown = 0;

        public ShootArrowsGoal(VenomEntity entity, double speedModifier, int attackInterval, float attackRadius) {
            this.entity = entity;
            this.speedModifier = speedModifier;
            this.attackInterval = attackInterval;
            this.attackRadius = attackRadius;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return entity.getTarget() != null && entity.distanceTo(entity.getTarget()) > 10.0D;
        }

        @Override
        public void start() {
            entity.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (attackCooldown > 0) {
                attackCooldown--;
                return;
            }

            Entity target = entity.getTarget();
            if (target != null && entity.distanceTo(target) <= attackRadius) {
                entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

                CustomArrowEntity  arrow = new CustomArrowEntity(MyMod.CUSTOM_ARROW.get(),entity.level(), entity);
                double dx = target.getX() - entity.getX();
                double dy = target.getY(0.3333333333333333D) - arrow.getY();
                double dz = target.getZ() - entity.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                arrow.shoot(dx, dy + distance * 0.20000000298023224D, dz, 1.6F, (float) (14 - entity.level().getDifficulty().getId() * 4));
                entity.level().addFreshEntity(arrow);

                attackCooldown = attackInterval;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }
    }
}
