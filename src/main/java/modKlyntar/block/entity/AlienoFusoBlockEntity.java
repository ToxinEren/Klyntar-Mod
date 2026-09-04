package modKlyntar.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * L'alieno fuso con All-Black: il corpo e la corazza li disegna questa block entity.
 *
 * <p>La Necrospada sulla schiena e' un modello a parte con la sua texture, quindi la disegna
 * il renderer come secondo passaggio: un modello GeckoLib ha una texture sola.</p>
 */
public class AlienoFusoBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AlienoFusoBlockEntity(BlockPos pos, BlockState stato) {
        super(ModBlockEntities.ALIENO_FUSO.get(), pos, stato);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // sta li' e basta
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * Largo, perche' i filamenti si allungano fino a sette blocchi per seguire chi hanno preso.
     *
     * <p>Con un riquadro stretto il gioco smette di disegnare la block entity appena il suo
     * cubo esce dall'inquadratura, e i tentacoli spariscono proprio quando sono piu' tesi.</p>
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(10.0D);
    }
}
