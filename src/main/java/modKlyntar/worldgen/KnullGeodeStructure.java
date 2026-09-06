package modKlyntar.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Il geode di Knull come struttura, per poterlo cercare con {@code /locate}.
 *
 * <p>Il comando di serie non vede le feature: guarda solo strutture, biomi e punti d'interesse.
 * Il geode resta identico a prima — il disegno e' quello di {@link KnullGeodeFeature} — ma
 * dichiarato come struttura diventa raggiungibile con
 * {@code /locate structure klyntars:knull_geode}, che genera anche i chunk mancanti pur di
 * trovarlo.</p>
 */
public class KnullGeodeStructure extends Structure {

    public static final Codec<KnullGeodeStructure> CODEC =
            RecordCodecBuilder.<KnullGeodeStructure>mapCodec(istanza -> istanza.group(
                    settingsCodec(istanza),
                    Codec.intRange(-64, 320).fieldOf("min_y").forGetter(s -> s.minimoY),
                    Codec.intRange(-64, 320).fieldOf("max_y").forGetter(s -> s.massimoY)
            ).apply(istanza, KnullGeodeStructure::new)).codec();

    private final int minimoY;
    private final int massimoY;

    public KnullGeodeStructure(StructureSettings impostazioni, int minimoY, int massimoY) {
        super(impostazioni);
        this.minimoY = minimoY;
        this.massimoY = massimoY;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext contesto) {
        ChunkPos chunk = contesto.chunkPos();
        RandomSource caso = contesto.random();

        int y = minimoY + caso.nextInt(massimoY - minimoY + 1);
        BlockPos centro = new BlockPos(chunk.getMiddleBlockX(), y, chunk.getMiddleBlockZ());

        return Optional.of(new GenerationStub(centro,
                costruttore -> costruttore.addPiece(new KnullGeodePiece(centro))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.KNULL_GEODE.get();
    }
}
