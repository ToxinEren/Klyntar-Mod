package modKlyntar.worldgen;

import com.mojang.serialization.Codec;
import modKlyntar.MyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Il geode di Knull: una bolla vuota sepolta nella roccia, come quelli di ametista.
 *
 * <p>Guscio esterno di deepslate, parete interna di ossidiana, e al centro della cavita' un
 * solo frammento di Knull. Non usa la feature {@code minecraft:geode} di serie perche' quella
 * riempie tutto lo strato interno con lo stesso blocco, e qui il frammento deve essere uno.</p>
 *
 * <p>Il disegno sta in {@link #disegna} e non nel metodo {@code place}, perche' lo usa anche la
 * struttura omonima: cosi' il geode che {@code /locate} porta a trovare e' identico a quello in
 * cui ci si imbatte scavando.</p>
 */
public class KnullGeodeFeature extends Feature<NoneFeatureConfiguration> {
    /** raggio della cavita' vuota */
    private static final int RAGGIO_MINIMO = 3;
    private static final int RAGGIO_MASSIMO = 5;
    /** spessore del guscio, ossidiana e deepslate mescolate meta' e meta' */
    private static final double SPESSORE_GUSCIO = 3.0D;
    /** la prima parete si posa comunque: e' quella che tiene chiusa la bolla */
    private static final double PARETE_SIGILLANTE = 1.0D;
    /** quanto il bordo puo' essere irregolare, per non ottenere una sfera perfetta */
    private static final double IRREGOLARITA = 0.6D;

    public KnullGeodeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contesto) {
        disegna(contesto.level(), contesto.origin(), null);
        return true;
    }

    /**
     * Scava e riveste il geode attorno a un centro.
     *
     * <p>Il caso non arriva da fuori ma si semina dal centro stesso. Serve alla struttura, che
     * viene disegnata un chunk per volta: con un generatore condiviso ogni chiamata estrarrebbe
     * un raggio diverso e il geode uscirebbe tagliato male a meta'. Seminandolo dalla posizione,
     * tutte le chiamate ridisegnano la stessa identica bolla.</p>
     *
     * @param limite porzione da riempire, o {@code null} per non porre limiti
     */
    public static void disegna(WorldGenLevel livello, BlockPos centro, BoundingBox limite) {
        RandomSource caso = RandomSource.create(centro.asLong());

        int raggio = RAGGIO_MINIMO + caso.nextInt(RAGGIO_MASSIMO - RAGGIO_MINIMO + 1);
        double bordoSigillante = raggio + PARETE_SIGILLANTE;
        double bordoGuscio = raggio + SPESSORE_GUSCIO;
        int estensione = (int) Math.ceil(bordoGuscio);

        BlockState ossidiana = Blocks.OBSIDIAN.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState aria = Blocks.CAVE_AIR.defaultBlockState();

        for (BlockPos posizione : BlockPos.betweenClosed(centro.offset(-estensione, -estensione, -estensione),
                centro.offset(estensione, estensione, estensione))) {
            double distanza = Math.sqrt(posizione.distSqr(centro));
            // un pizzico di rumore sul bordo: senza, il geode e' una sfera da manuale.
            // va estratto sempre, anche per i blocchi fuori dal limite, se no il rumore
            // cambierebbe da un chunk all'altro e il bordo non combacerebbe
            double scarto = (caso.nextDouble() - 0.5D) * IRREGOLARITA;
            BlockState pietra = caso.nextBoolean() ? ossidiana : deepslate;

            if (limite != null && !limite.isInside(posizione)) {
                continue;
            }

            if (distanza + scarto <= raggio) {
                livello.setBlock(posizione, aria, 2);
                continue;
            }
            if (distanza + scarto > bordoGuscio) {
                continue;
            }

            // il guscio non e' a strati: ogni blocco esce ossidiana o deepslate a testa o croce,
            // cosi' dall'interno se ne vedono meta' e meta' invece di una parete sola
            if (distanza + scarto <= bordoSigillante) {
                livello.setBlock(posizione, pietra, 2);
            } else if (livello.getBlockState(posizione).isSolidRender(livello, posizione)) {
                // piu' in fuori solo dove c'e' gia' roccia: niente tappi sulle grotte
                livello.setBlock(posizione, pietra, 2);
            }
        }

        // l'unico frammento, sospeso al centro della cavita'
        if (limite == null || limite.isInside(centro)) {
            livello.setBlock(centro, MyMod.KNULLS_FRAGMENT_BLOCK.get().defaultBlockState(), 2);
        }
    }
}
