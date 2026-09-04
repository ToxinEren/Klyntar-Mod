package modKlyntar.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * Il riempitivo di un multiblocco: invisibile, solido, e rimanda tutto all'ancora.
 *
 * <p>Un modello da sette blocchi non puo' stare in un blocco solo: il rendering puo' sforare,
 * la collisione e il "cosa ho colpito" no. Il corpo e' quindi disegnato per intero dalla block
 * entity dell'ancora, e il resto del sedime e' fatto di questi pezzi, che non si vedono ma si
 * toccano e che girano clic e rotture a chi di dovere.</p>
 *
 * <p>L'ancora non e' memorizzata: verrebbe uno stato per ogni scostamento possibile, o una
 * block entity per ognuno dei venti pezzi del dio. Si cerca invece nel raggio, che a queste
 * dimensioni costa una scansione di poche centinaia di posizioni e solo quando qualcuno tocca
 * la struttura.</p>
 */
public class PezzoStrutturaBlock extends Block {

    /** quanto lontano puo' stare l'ancora: il dio e' lungo sette blocchi, questo li copre */
    private static final int RAGGIO_ORIZZONTALE = 8;
    private static final int RAGGIO_VERTICALE = 3;

    private final Supplier<Block> ancora;
    private final int luce;

    public PezzoStrutturaBlock(Supplier<Block> ancora) {
        this(ancora, 0);
    }

    /**
     * @param luce quanta luce emette il pezzo: il corpo del dio brilla per intero, non solo
     *             dal blocco d'ancoraggio, altrimenti si illuminerebbe un pezzo su ventuno
     */
    public PezzoStrutturaBlock(Supplier<Block> ancora, int luce) {
        super(BlockBehaviour.Properties.of()
                .strength(24.0F, 1200.0F)
                .lightLevel(stato -> luce)
                .noOcclusion()
                .noLootTable());
        this.ancora = ancora;
        this.luce = luce;
    }

    @Override
    public void animateTick(BlockState stato, Level livello, BlockPos pos, RandomSource caso) {
        // solo i pezzi che gia' illuminano fumano oro: quelli dell'alieno restano spenti
        if (this.luce > 0 && caso.nextInt(3) == 0) {
            DioMortoBlock.particelle(livello, pos, caso);
        }
    }

    /** Dove sta l'ancora di questo pezzo, o {@code null} se e' rimasta orfana. */
    public BlockPos ancora(Level livello, BlockPos pos) {
        Block cercata = this.ancora.get();
        BlockPos.MutableBlockPos cursore = new BlockPos.MutableBlockPos();
        BlockPos migliore = null;
        double distanza = Double.MAX_VALUE;
        for (int dy = -RAGGIO_VERTICALE; dy <= RAGGIO_VERTICALE; dy++) {
            for (int dx = -RAGGIO_ORIZZONTALE; dx <= RAGGIO_ORIZZONTALE; dx++) {
                for (int dz = -RAGGIO_ORIZZONTALE; dz <= RAGGIO_ORIZZONTALE; dz++) {
                    cursore.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!livello.getBlockState(cursore).is(cercata)) {
                        continue;
                    }
                    double d = cursore.distSqr(pos);
                    if (d < distanza) {
                        distanza = d;
                        migliore = cursore.immutable();
                    }
                }
            }
        }
        return migliore;
    }

    @Override
    public RenderShape getRenderShape(BlockState stato) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState stato, Level livello, BlockPos pos, Player giocatore,
                                 InteractionHand mano, BlockHitResult colpo) {
        BlockPos capo = ancora(livello, pos);
        if (capo == null) {
            return InteractionResult.PASS;
        }
        BlockState statoAncora = livello.getBlockState(capo);
        return statoAncora.use(livello, giocatore, mano,
                new BlockHitResult(colpo.getLocation(), colpo.getDirection(), capo, false));
    }

    @Override
    public void playerWillDestroy(Level livello, BlockPos pos, BlockState stato, Player giocatore) {
        BlockPos capo = ancora(livello, pos);
        if (capo != null && !livello.isClientSide()) {
            // rompere un piede vale come rompere il petto: decide sempre l'ancora
            livello.getBlockState(capo).getBlock()
                    .playerWillDestroy(livello, capo, livello.getBlockState(capo), giocatore);
        }
        super.playerWillDestroy(livello, pos, stato, giocatore);
    }
}
