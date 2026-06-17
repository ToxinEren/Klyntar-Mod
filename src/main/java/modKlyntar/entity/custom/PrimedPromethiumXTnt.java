package modKlyntar.entity.custom;

import javax.annotation.Nullable;

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
        super(level, x, y, z, igniter);
        this.setFuse(80); // Imposta il tempo di fuso della tua TNT personalizzata
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
