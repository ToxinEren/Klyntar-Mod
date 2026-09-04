package modKlyntar.block;

import modKlyntar.block.entity.AlienoFusoBlockEntity;
import modKlyntar.player.RitualeAllBlack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * L'alieno fuso con All-Black, accasciato accanto al dio che ha ucciso.
 *
 * <p>E' il blocco d'ancoraggio: il corpo lo disegna la block entity, e attorno c'e' un quadrato
 * di {@link PezzoStrutturaBlock} perche' il cadavere e' steso e occupa in larghezza. Il simbionte
 * che ha addosso e' ancora vivo, ed e' da qui che un giorno passera' a chi lo raccoglie.</p>
 */
public class AlienoFusoBlock extends BaseEntityBlock {

    /**
     * Il sedime rispetto all'ancora: tre blocchi in larghezza per quattro in lunghezza.
     *
     * <p>Misurato sul modello, non stimato: il corpo va da -19 a +13 unita' su X e da -20 a
     * +31 su Z, e siccome l'origine del modello cade al centro dell'ancora questo copre le
     * caselle da -1 a +1 in larghezza e da -1 a +2 in lunghezza. La riga in piu' in fondo
     * e' quella dei piedi, che con un quadrato di tre per tre restavano fuori.</p>
     */
    public static final int[][] SEDIME = {
            {-1, 0, -1}, {0, 0, -1}, {1, 0, -1},
            {-1, 0, 0},              {1, 0, 0},
            {-1, 0, 1},  {0, 0, 1},  {1, 0, 1},
            {-1, 0, 2},  {0, 0, 2},  {1, 0, 2}};

    /** basso: e' un corpo disteso, ci si cammina sopra invece di sbatterci contro */
    private static final VoxelShape FORMA = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.3D, 1.0D);

    public AlienoFusoBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(14.0F, 900.0F)
                .sound(SoundType.SLIME_BLOCK)
                .noOcclusion()
                .noLootTable());
    }

    /** ogni quanto guarda se c'e' qualcuno a tiro */
    private static final int INTERVALLO = 10;

    @Override
    public void onPlace(BlockState stato, Level livello, BlockPos pos, BlockState vecchio,
                        boolean mosso) {
        if (!livello.isClientSide()) {
            livello.scheduleTick(pos, this, INTERVALLO);
        }
    }

    /**
     * Cerca chi avvicinarsi troppo, e lo prende.
     *
     * <p>Si controlla a intervalli e non a ogni tick: la scansione costa, e mezzo secondo di
     * ritardo su una presa che dura sei secondi non si nota.</p>
     */
    @Override
    public void tick(BlockState stato, ServerLevel livello, BlockPos pos, RandomSource caso) {
        livello.scheduleTick(pos, this, INTERVALLO);
        if (RitualeAllBlack.rivendicato(pos)) {
            return;
        }
        for (ServerPlayer giocatore : livello.players()) {
            if (giocatore.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                    > RitualeAllBlack.PORTATA * RitualeAllBlack.PORTATA) {
                continue;
            }
            // la regola non e' quella dei mob simbionte: All-Black prende anche chi ne ha
            // gia' uno, tranne l'anti-venom
            if (!RitualeAllBlack.puoPrendere(giocatore) || RitualeAllBlack.impegnato(giocatore)) {
                continue;
            }
            RitualeAllBlack.avvia(giocatore, livello, pos);
            return;
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState stato) {
        return new AlienoFusoBlockEntity(pos, stato);
    }

    @Override
    public RenderShape getRenderShape(BlockState stato) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState stato, BlockGetter mondo, BlockPos pos,
                               CollisionContext contesto) {
        return FORMA;
    }
}
