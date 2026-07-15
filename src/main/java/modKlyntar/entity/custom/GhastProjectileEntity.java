package modKlyntar.entity.custom;

import modKlyntar.MeteorCommand;
import modKlyntar.MyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GhastProjectileEntity extends LargeFireball implements GeoEntity {
    private static final float METEOR_EXPLOSION_POWER = 6.0F;
    private static final int SMOKE_TRAIL_INTERVAL_TICKS = 5;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GhastProjectileEntity(EntityType<? extends GhastProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GhastProjectileEntity(Level level, double x, double y, double z, double targetX, double targetY, double targetZ) {
        super(MyMod.GHAST_PROJECTILE_ENTITY.get(), level);  // Usa il tuo EntityType registrato
        this.moveTo(x, y, z, 0, 0);
        this.xPower = targetX - x;
        this.yPower = targetY - y;
        this.zPower = targetZ - z;
        double length = Math.sqrt(this.xPower * this.xPower + this.yPower * this.yPower + this.zPower * this.zPower);
        this.xPower /= length;
        this.yPower /= length;
        this.zPower /= length;
        this.xPower *= 0.05;
        this.yPower *= 0.05;
        this.zPower *= 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround()) {
            this.remove(RemovalReason.DISCARDED);
        }
        if (this.getDeltaMovement().y() > -0.05) {
            this.setDeltaMovement(this.getDeltaMovement().x(), -0.05, this.getDeltaMovement().z());
        }
        
        Vec3 currentPos = this.position();
        if (this.tickCount % SMOKE_TRAIL_INTERVAL_TICKS == 0) {
            SmokeTrailEntity smokeTrail = new SmokeTrailEntity(this.level(), currentPos.x, currentPos.y, currentPos.z);
            this.level().addFreshEntity(smokeTrail);
        }

        // Aggiungi particelle di fumo
        this.level().addParticle(ParticleTypes.LARGE_SMOKE, currentPos.x, currentPos.y, currentPos.z, 0.0D, 0.0D, 0.0D);
        this.level().addParticle(ParticleTypes.FLAME, currentPos.x, currentPos.y, currentPos.z, 0.0D, 0.0D, 0.0D);
        this.level().addParticle(ParticleTypes.ASH, currentPos.x, currentPos.y, currentPos.z, 0.0D, 0.0D, 0.0D);
        this.level().addParticle(ParticleTypes.LAVA, currentPos.x, currentPos.y, currentPos.z, 0.0D, 0.0D, 0.0D);
    }
    
    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        BlockPos impactPos = blockHitResult.getBlockPos();
        // Crea l'esplosione
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), METEOR_EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
        
        MeteorCommand.handleProjectileImpact(this, blockHitResult);
        this.remove(RemovalReason.DISCARDED);
    }
    
    private <E extends GeoEntity> PlayState predicate(AnimationState<E> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("fall"));
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
