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

/**
 * Il geode di Knull: una bolla vuota sepolta nella roccia, come quelli di ametista.
 *
 * <p>Guscio esterno di deepslate, parete interna di ossidiana, e al centro della cavita' un
 * solo frammento di Knull. Non usa la feature {@code minecraft:geode} di serie perche' quella
 * riempie tutto lo strato interno con lo stesso blocco, e qui il frammento deve essere uno.</p>
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
        WorldGenLevel livello = contesto.level();
        BlockPos centro = contesto.origin();
        RandomSource caso = contesto.random();

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
            // un pizzico di rumore sul bordo: senza, il geode e' una sfera da manuale
            double scarto = (caso.nextDouble() - 0.5D) * IRREGOLARITA;

            if (distanza + scarto <= raggio) {
                livello.setBlock(posizione, aria, 2);
                continue;
            }
            if (distanza + scarto > bordoGuscio) {
                continue;
            }

            // il guscio non e' a strati: ogni blocco esce ossidiana o deepslate a testa o croce,
            // cosi' dall'interno se ne vedono meta' e meta' invece di una parete sola
            BlockState pietra = caso.nextBoolean() ? ossidiana : deepslate;
            if (distanza + scarto <= bordoSigillante) {
                livello.setBlock(posizione, pietra, 2);
            } else if (livello.getBlockState(posizione).isSolidRender(livello, posizione)) {
                // piu' in fuori solo dove c'e' gia' roccia: niente tappi sulle grotte
                livello.setBlock(posizione, pietra, 2);
            }
        }

        // l'unico frammento, sospeso al centro della cavita'
        livello.setBlock(centro, MyMod.KNULLS_FRAGMENT_BLOCK.get().defaultBlockState(), 2);
        return true;
    }
}
