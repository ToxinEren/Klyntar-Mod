package modKlyntar.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Il pezzo che disegna il geode di Knull.
 *
 * <p>Non ridisegna niente di suo: chiama lo stesso codice della feature, cosi' il geode trovato
 * con {@code /locate} e' identico a quello che si incontra scavando.</p>
 */
public class KnullGeodePiece extends StructurePiece {

    /** raggio massimo piu' spessore del guscio, con un blocco di margine */
    private static final int MARGINE = 9;

    private final BlockPos centro;

    public KnullGeodePiece(BlockPos centro) {
        super(ModStructures.KNULL_GEODE_PIECE.get(), 0, scatolaAttorno(centro));
        this.centro = centro;
    }

    public KnullGeodePiece(CompoundTag tag) {
        super(ModStructures.KNULL_GEODE_PIECE.get(), tag);
        this.centro = new BlockPos(tag.getInt("CentroX"), tag.getInt("CentroY"), tag.getInt("CentroZ"));
    }

    private static BoundingBox scatolaAttorno(BlockPos centro) {
        return new BoundingBox(centro.getX() - MARGINE, centro.getY() - MARGINE, centro.getZ() - MARGINE,
                centro.getX() + MARGINE, centro.getY() + MARGINE, centro.getZ() + MARGINE);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext contesto, CompoundTag tag) {
        tag.putInt("CentroX", centro.getX());
        tag.putInt("CentroY", centro.getY());
        tag.putInt("CentroZ", centro.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel livello, StructureManager strutture, ChunkGenerator generatore,
                           RandomSource caso, BoundingBox scatola, ChunkPos chunk, BlockPos origine) {
        // scatola e' la porzione di chunk in lavorazione: il disegno si limita da solo a quella
        KnullGeodeFeature.disegna(livello, centro, scatola);
    }
}
