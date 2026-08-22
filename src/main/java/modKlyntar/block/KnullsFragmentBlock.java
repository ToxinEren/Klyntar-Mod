package modKlyntar.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Il frammento di Knull: quello che resta di lui dentro un meteorite.
 *
 * <p>Duro come il blocco del pack da cui viene, e luminoso al massimo: nel buio di un geode
 * e' l'unica cosa che si vede.</p>
 */
public class KnullsFragmentBlock extends Block {
    /** quanto e' duro da scavare e quanto regge alle esplosioni, come nel pack */
    private static final float DUREZZA = 5.0F;
    private static final float RESISTENZA = 48.0F;

    public KnullsFragmentBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(DUREZZA, RESISTENZA)
                .requiresCorrectToolForDrops()
                .lightLevel(stato -> 15)
                .sound(SoundType.DEEPSLATE));
    }
}
