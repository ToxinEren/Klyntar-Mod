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
 * Il dio d'oro steso a terra: la block entity esiste solo per disegnare il corpo intero.
 *
 * <p>Non ha animazioni ne' dati da salvare. Il riquadro di rendering va allargato a mano,
 * altrimenti il gioco smette di disegnarlo appena l'angolo del suo blocco esce dallo schermo,
 * e un corpo lungo sette blocchi sparirebbe quasi sempre.</p>
 */
public class DioMortoBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DioMortoBlockEntity(BlockPos pos, BlockState stato) {
        super(ModBlockEntities.DIO_MORTO.get(), pos, stato);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // un cadavere non si muove
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(9.0D);
    }
}
