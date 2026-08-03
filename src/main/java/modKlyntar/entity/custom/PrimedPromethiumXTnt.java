package modKlyntar.entity.custom;

import javax.annotation.Nullable;

import modKlyntar.MyMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PrimedPromethiumXTnt extends PrimedTnt implements GeoEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	
    public PrimedPromethiumXTnt(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
    }

    public PrimedPromethiumXTnt(Level level, double x, double y, double z, @Nullable LivingEntity igniter) {
        // il costruttore di PrimedTnt userebbe EntityType.TNT, e il renderer sarebbe quello vanilla
        this(MyMod.PROMETHIUMX_TNT_ENTITY.get(), level);
        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * (Math.PI * 2D);
        this.setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
        this.setFuse(80); // Imposta il tempo di fuso della tua TNT personalizzata
        this.xo = x;
        this.yo = y;
        this.zo = z;
        assignOwner(igniter);
    }

    private void assignOwner(@Nullable LivingEntity igniter) {
        if (igniter == null) {
            return;
        }
        for (java.lang.reflect.Field field : PrimedTnt.class.getDeclaredFields()) {
            if (!LivingEntity.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(this, igniter);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // l'attribuzione dell'esplosione e' un extra: se fallisce la TNT funziona comunque
            }
            return;
        }
    }

    @Override
    protected void explode() {
        // Implementa qui la logica dell'esplosione personalizzata, se necessario
        this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 40.0F, Level.ExplosionInteraction.TNT);
    }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// TODO Auto-generated method stub
		
	}

	@Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
