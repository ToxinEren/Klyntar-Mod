package modKlyntar.block;

import modKlyntar.block.entity.DioMortoBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

/**
 * Il dio d'oro morto, steso sulla schiena al centro del cratere.
 *
 * <p>Questo e' il blocco d'ancoraggio: disegna il corpo intero, lungo sette blocchi e largo tre,
 * mentre il resto del sedime e' fatto di {@link PezzoStrutturaBlock}. Toccarlo — con un clic o
 * con un piccone — libera quello che gli e' rimasto dentro, ed e' molto piu' di quanto convenga
 * a chi sta li' vicino.</p>
 */
public class DioMortoBlock extends BaseEntityBlock {

    /** Il sedime, in blocchi, rispetto all'ancora: tre di larghezza per sette di lunghezza. */
    public static final int[][] SEDIME = costruisciSedime();

    /** il colore del pulviscolo: lo stesso oro della pelle del dio */
    private static final DustParticleOptions POLVERE_ORO =
            new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.80F, 0.30F), 1.1F);

    /** appena piu' basso di due blocchi: il corpo e' steso, non in piedi */
    private static final VoxelShape FORMA = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    public DioMortoBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(28.0F, 1800.0F)
                .sound(SoundType.NETHERITE_BLOCK)
                // l'oro di un dio non si spegne: tutto il corpo illumina come la glowstone
                .lightLevel(stato -> 15)
                .noOcclusion()
                .noLootTable());
    }

    private static int[][] costruisciSedime() {
        int[][] celle = new int[3 * 2 * 7][];
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 6; dz++) {
                    celle[n++] = new int[]{dx, dy, dz};
                }
            }
        }
        return celle;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState stato) {
        return new DioMortoBlockEntity(pos, stato);
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

    /**
     * Il pulviscolo dorato che sale dal corpo.
     *
     * <p>Lo emettono anche i {@link PezzoStrutturaBlock} del sedime, altrimenti le particelle
     * uscirebbero da un punto solo di un cadavere lungo sette blocchi.</p>
     */
    @Override
    public void animateTick(BlockState stato, Level livello, BlockPos pos, RandomSource caso) {
        particelle(livello, pos, caso);
    }

    /** Il pulviscolo d'oro: granelli lenti che salgono, e ogni tanto una scintilla. */
    public static void particelle(Level livello, BlockPos pos, RandomSource caso) {
        double x = pos.getX() + caso.nextDouble();
        double y = pos.getY() + 0.9D + caso.nextDouble() * 0.6D;
        double z = pos.getZ() + caso.nextDouble();
        livello.addParticle(POLVERE_ORO, x, y, z,
                (caso.nextDouble() - 0.5D) * 0.01D, 0.012D + caso.nextDouble() * 0.02D,
                (caso.nextDouble() - 0.5D) * 0.01D);
        if (caso.nextInt(9) == 0) {
            livello.addParticle(ParticleTypes.END_ROD, x, y, z,
                    (caso.nextDouble() - 0.5D) * 0.008D, 0.02D,
                    (caso.nextDouble() - 0.5D) * 0.008D);
        }
    }

    @Override
    public InteractionResult use(BlockState stato, Level livello, BlockPos pos, Player giocatore,
                                 InteractionHand mano, BlockHitResult colpo) {
        if (livello instanceof ServerLevel server) {
            EsplosioneNucleare.innesca(server, pos);
        }
        return InteractionResult.sidedSuccess(livello.isClientSide());
    }

    @Override
    public void playerWillDestroy(Level livello, BlockPos pos, BlockState stato, Player giocatore) {
        if (livello instanceof ServerLevel server) {
            EsplosioneNucleare.innesca(server, pos);
        }
        super.playerWillDestroy(livello, pos, stato, giocatore);
    }

    @Override
    public void wasExploded(Level livello, BlockPos pos, Explosion esplosione) {
        if (livello instanceof ServerLevel server) {
            EsplosioneNucleare.innesca(server, pos);
        }
    }
}
