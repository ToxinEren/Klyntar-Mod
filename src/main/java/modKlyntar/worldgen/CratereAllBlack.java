package modKlyntar.worldgen;

import modKlyntar.MyMod;
import modKlyntar.block.AlienoFusoBlock;
import modKlyntar.block.DioMortoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Il cratere dove All-Black e' caduto: una conca bruciata con dentro due cadaveri.
 *
 * <p>Al centro l'alieno fuso col simbionte, e accanto il dio d'oro che ha ucciso. Il cratere
 * e' poco profondo: quello grosso lo fara' il dio quando qualcuno lo tocchera'.</p>
 *
 * <p>Il posto non e' piu' il primo che capita: viene cercato finche' non se ne trova uno in
 * superficie, su terra o erba, senza acqua ne' lava e senza alberi. E i corpi non vengono piu'
 * appoggiati alla quota del centro della conca ma a quella del fondo sotto di loro, che e'
 * diversa perche' il fondo e' una calotta.</p>
 */
public final class CratereAllBlack {

    /** quanto e' larga la conca */
    private static final int RAGGIO = 17;
    /** quanto sprofonda al centro */
    private static final int PROFONDITA = 5;
    /** a che distanza dallo spawn viene cercato un posto */
    private static final int DISTANZA_MINIMA = 900;
    private static final int DISTANZA_MASSIMA = 2200;

    /**
     * Entro questo raggio il terreno viene preteso pulito: e' la parte che si vede da dentro
     * e dove finiscono i due corpi. Piu' in la', al bordo, qualche irregolarita' la conca la
     * mangia comunque scavando.
     */
    private static final int RAGGIO_INTERNO = 9;
    /**
     * Dove poggiano i corpi il suolo deve essere terra o erba senza eccezioni; nel resto della
     * parte interna basta che lo sia in grande maggioranza.
     *
     * <p>Pretendere terra o erba su tutti i punti campionati bocciava 26 posti su 29: in un
     * terreno naturale c'e' quasi sempre una chiazza di ghiaia o di sabbia da qualche parte, e
     * un posto per il resto perfetto veniva buttato via per quella.</p>
     */
    private static final int RAGGIO_CORPI = 6;
    private static final double FRAZIONE_SUOLO_BUONO = 0.80D;
    /** Quanto puo' essere mosso il terreno nella parte interna, in blocchi. */
    private static final int DISLIVELLO_MASSIMO = 4;
    /**
     * La tolleranza del filtro rapido, piu' larga di quella vera.
     *
     * <p>Un pre-filtro deve solo togliere di mezzo l'ovvio: se e' severo quanto il controllo
     * finale scarta anche i posti buoni prima ancora di guardarli. Con la tolleranza stretta
     * buttava via 34 candidati su 40 e non trovava mai niente.</p>
     */
    private static final int DISLIVELLO_FILTRO = DISLIVELLO_MASSIMO * 2;
    /**
     * Quanti posti si provano prima di rinunciare per questa volta.
     *
     * <p>Quello che costa non e' il numero di tentativi ma quanti ne passano il filtro rapido:
     * solo quelli generano i chunk della conca, e generarli e' sincrono sul thread del server.
     * Col filtro che scarta la gran parte dei candidati senza caricare niente, questo numero
     * puo' stare alto senza piantare il mondo.</p>
     */
    private static final int TENTATIVI = 120;
    /** Quanto spazio libero si pretende sopra il terreno: sotto una sporgenza non e' superficie. */
    private static final int CIELO_LIBERO = 4;

    private static final Logger LOGGER = LogManager.getLogger("KlyntarCratere");

    private CratereAllBlack() {
    }

    /**
     * Cerca un posto adatto lontano dallo spawn e ci scava la struttura.
     *
     * @return il centro del fondo, oppure {@code null} se in questo giro non si e' trovato
     *         niente di adatto: in quel caso il cratere non va segnato come posato, cosi' si
     *         riprova piu' avanti invece di perderlo per sempre.
     */
    public static BlockPos posa(ServerLevel livello, Random sorte) {
        int scartatiSubito = 0, scartatiFluido = 0, scartatiSuolo = 0, scartatiDislivello = 0, scartatiCielo = 0;

        for (int tentativo = 0; tentativo < TENTATIVI; tentativo++) {
            double angolo = sorte.nextDouble() * Math.PI * 2.0D;
            int distanza = DISTANZA_MINIMA + sorte.nextInt(DISTANZA_MASSIMA - DISTANZA_MINIMA);
            BlockPos spawn = livello.getSharedSpawnPos();
            int x = spawn.getX() + (int) (Math.cos(angolo) * distanza);
            int z = spawn.getZ() + (int) (Math.sin(angolo) * distanza);

            // Filtro a buon mercato PRIMA di generare qualsiasi cosa: l'altezza di base si
            // ricava dal rumore senza costruire il chunk. Serve a buttare via subito oceani e
            // pareti di montagna, che sono la stragrande maggioranza degli scarti: senza,
            // ogni tentativo genererebbe una decina di chunk sul thread del server.
            if (!promettente(livello, x, z)) {
                scartatiSubito++;
                continue;
            }

            // solo ora vale la pena generare il terreno per misurarlo davvero
            for (int cx = (x - RAGGIO) >> 4; cx <= (x + RAGGIO) >> 4; cx++) {
                for (int cz = (z - RAGGIO) >> 4; cz <= (z + RAGGIO) >> 4; cz++) {
                    livello.getChunk(cx, cz);
                }
            }

            int esito = esamina(livello, x, z);
            if (esito != OK) {
                if (esito == FLUIDO) scartatiFluido++;
                else if (esito == SUOLO) scartatiSuolo++;
                else if (esito == DISLIVELLO) scartatiDislivello++;
                else scartatiCielo++;
                continue;
            }

            int y = quotaSuolo(livello, x, z);
            BlockPos centro = new BlockPos(x, y, z);
            scava(livello, centro);
            piazzaCorpi(livello, centro);

            BlockPos fondo = centro.below(PROFONDITA);
            LOGGER.info("Cratere di All-Black posato a {} dopo {} tentativi", fondo, tentativo + 1);
            verifica(livello, centro);
            return fondo;
        }

        LOGGER.warn("Nessun posto adatto per il cratere in {} tentativi (scartati subito {}, "
                        + "acqua o lava {}, suolo non adatto {}, terreno mosso {}, niente cielo {}). Si riprova.",
                TENTATIVI, scartatiSubito, scartatiFluido, scartatiSuolo, scartatiDislivello,
                scartatiCielo);
        return null;
    }

    private static final int OK = 0, FLUIDO = 1, SUOLO = 2, DISLIVELLO = 3, CIELO = 4;

    /**
     * Scarto rapido senza generare chunk.
     *
     * <p>{@code getBaseHeight} ricava la quota dal rumore del generatore, quindi risponde anche
     * per terreno che non esiste ancora. Non sa dire di che blocco e' fatto il suolo — quello
     * resta a {@link #esamina} — ma basta a togliere di mezzo oceani e pendii ripidi, che sono
     * quasi tutti gli scarti, senza pagarne il costo.</p>
     */
    private static boolean promettente(ServerLevel livello, int x, int z) {
        var generatore = livello.getChunkSource().getGenerator();
        var casualita = livello.getChunkSource().randomState();
        int minima = Integer.MAX_VALUE, massima = Integer.MIN_VALUE;
        int[][] punti = {{0, 0}, {-RAGGIO_INTERNO, 0}, {RAGGIO_INTERNO, 0},
                         {0, -RAGGIO_INTERNO}, {0, RAGGIO_INTERNO}};
        for (int[] p : punti) {
            int h = generatore.getBaseHeight(x + p[0], z + p[1],
                    Heightmap.Types.OCEAN_FLOOR_WG, livello, casualita);
            if (h <= livello.getSeaLevel()) {
                return false;   // oceano o depressione allagata
            }
            minima = Math.min(minima, h);
            massima = Math.max(massima, h);
        }
        return massima - minima <= DISLIVELLO_FILTRO;
    }

    /**
     * La quota del terreno solido.
     *
     * <p>Due scelte, entrambe importanti. {@code OCEAN_FLOOR} e non {@code WORLD_SURFACE}:
     * quest'ultima si ferma al primo blocco non vuoto, quindi sul pelo dell'acqua o sulla
     * chioma di un albero, ed e' il motivo per cui il cratere poteva nascere in mezzo al mare.
     * E senza il suffisso {@code _WG}: le heightmap col suffisso sono quelle usate durante la
     * generazione e non vengono conservate nel chunk finito, quindi lette da un mondo gia'
     * generato danno numeri che non c'entrano — e il cratere finisce sottoterra.</p>
     */
    private static int quotaSuolo(ServerLevel livello, int x, int z) {
        return livello.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    /** Terra o erba: e' l'unico suolo su cui la struttura puo' posarsi. */
    private static boolean suoloAmmesso(Block blocco) {
        return blocco == Blocks.GRASS_BLOCK || blocco == Blocks.DIRT;
    }

    /**
     * Dice se il posto va bene, e se no perche'.
     *
     * <p>Si campiona la conca a passo di due blocchi. Nella parte interna si pretende terra o
     * erba, terreno poco mosso e cielo libero sopra; in tutta la conca, invece, non ci devono
     * essere fluidi — ne' in superficie ne' dentro il volume che verra' scavato, altrimenti
     * l'acqua ci rientra appena finito di scavare.</p>
     *
     * <p>Gli alberi cadono da soli: il tronco non e' ne' terra ne' erba, e le fronde occupano
     * il cielo che si pretende libero.</p>
     */
    private static int esamina(ServerLevel livello, int x, int z) {
        int minima = Integer.MAX_VALUE, massima = Integer.MIN_VALUE;
        int interni = 0, buoni = 0;
        BlockPos.MutableBlockPos cursore = new BlockPos.MutableBlockPos();

        for (int dx = -RAGGIO; dx <= RAGGIO; dx += 2) {
            for (int dz = -RAGGIO; dz <= RAGGIO; dz += 2) {
                double distanza = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distanza > RAGGIO) {
                    continue;
                }
                int px = x + dx, pz = z + dz;
                int py = quotaSuolo(livello, px, pz);

                for (int y = py - PROFONDITA - 1; y <= py + CIELO_LIBERO; y++) {
                    cursore.set(px, y, pz);
                    if (!livello.getFluidState(cursore).isEmpty()) {
                        return FLUIDO;
                    }
                }

                if (distanza > RAGGIO_INTERNO) {
                    continue;
                }

                cursore.set(px, py - 1, pz);
                boolean buono = suoloAmmesso(livello.getBlockState(cursore).getBlock());
                if (!buono && distanza <= RAGGIO_CORPI) {
                    return SUOLO;   // proprio dove finiscono i corpi non si transige
                }
                interni++;
                if (buono) {
                    buoni++;
                }
                // sopra ci deve essere aria o al piu' erba alta e fiori: dentro una grotta o
                // sotto una sporgenza non si e' in superficie
                for (int y = py; y < py + CIELO_LIBERO; y++) {
                    cursore.set(px, y, pz);
                    BlockState stato = livello.getBlockState(cursore);
                    if (!stato.isAir() && !stato.canBeReplaced()) {
                        return CIELO;
                    }
                }
                minima = Math.min(minima, py);
                massima = Math.max(massima, py);
            }
        }
        if (interni > 0 && (double) buoni / interni < FRAZIONE_SUOLO_BUONO) {
            return SUOLO;
        }
        return massima - minima <= DISLIVELLO_MASSIMO ? OK : DISLIVELLO;
    }

    /**
     * Quanto sprofonda il fondo a una certa distanza dal centro.
     *
     * <p>La stessa formula la usano lo scavo e l'appoggio dei corpi: se ognuno avesse la sua,
     * basterebbe cambiarne una per ritrovarsi i cadaveri sepolti o a mezz'aria.</p>
     */
    private static int scavoA(double dx, double dz) {
        double distanza = Math.sqrt(dx * dx + dz * dz);
        if (distanza > RAGGIO) {
            return 0;
        }
        double quota = 1.0D - distanza / RAGGIO;
        return (int) Math.round(PROFONDITA * quota * quota);
    }

    /** La conca: una calotta rovesciata, con il fondo vetrificato. */
    private static void scava(ServerLevel livello, BlockPos centro) {
        BlockState aria = Blocks.AIR.defaultBlockState();
        BlockState crosta = Blocks.BLACKSTONE.defaultBlockState();
        BlockState cenere = Blocks.BASALT.defaultBlockState();
        BlockPos.MutableBlockPos cursore = new BlockPos.MutableBlockPos();

        for (int dx = -RAGGIO; dx <= RAGGIO; dx++) {
            for (int dz = -RAGGIO; dz <= RAGGIO; dz++) {
                double distanza = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distanza > RAGGIO) {
                    continue;
                }
                int scavo = scavoA(dx, dz);
                int cima = centro.getY() + 6;
                for (int y = cima; y > centro.getY() - scavo; y--) {
                    cursore.set(centro.getX() + dx, y, centro.getZ() + dz);
                    if (!livello.getBlockState(cursore).isAir()) {
                        livello.setBlock(cursore, aria, 2);
                    }
                }
                cursore.set(centro.getX() + dx, centro.getY() - scavo, centro.getZ() + dz);
                if (!livello.getBlockState(cursore).isAir()) {
                    livello.setBlock(cursore,
                            livello.random.nextInt(4) == 0 ? cenere : crosta, 2);
                }
            }
        }
    }

    /**
     * Ricontrolla il risultato e lo scrive nel log.
     *
     * <p>Il render del client non e' un testimone affidabile — dopo un teletrasporto lungo puo'
     * restare fermo su geometria vecchia — quindi la prova che i corpi poggiano davvero la da'
     * il mondo, non lo schermo: sotto ogni ancora ci deve essere un blocco solido e nell'ancora
     * il corpo.</p>
     */
    private static void verifica(ServerLevel livello, BlockPos centro) {
        controlla(livello, "dio", appoggiaSolaQuota(centro, 2, -3, DioMortoBlock.SEDIME),
                MyMod.DIO_MORTO.get());
        controlla(livello, "alieno", appoggiaSolaQuota(centro, -4, 0, AlienoFusoBlock.SEDIME),
                MyMod.ALIENO_FUSO.get());
        LOGGER.info("  suolo al centro: {}, quota superficie {}",
                livello.getBlockState(centro.below()).getBlock(), centro.getY());
    }

    private static void controlla(ServerLevel livello, String chi, BlockPos ancora, Block atteso) {
        Block qui = livello.getBlockState(ancora).getBlock();
        Block sotto = livello.getBlockState(ancora.below()).getBlock();
        boolean solido = !livello.getBlockState(ancora.below()).isAir();
        LOGGER.info("  {} a {}: blocco {} ({}), sotto {} -> {}",
                chi, ancora, qui, qui == atteso ? "giusto" : "SBAGLIATO",
                sotto, solido ? "appoggiato" : "SOSPESO");
    }

    /** La sola quota calcolata da {@link #appoggia}, senza toccare il mondo. */
    private static BlockPos appoggiaSolaQuota(BlockPos centro, int offX, int offZ, int[][] sedime) {
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (int[] cella : sedime) {
            minX = Math.min(minX, cella[0]);
            maxX = Math.max(maxX, cella[0]);
            minZ = Math.min(minZ, cella[2]);
            maxZ = Math.max(maxZ, cella[2]);
        }
        int cima = Integer.MIN_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cima = Math.max(cima, centro.getY() - scavoA(offX + x, offZ + z));
            }
        }
        return new BlockPos(centro.getX() + offX, cima + 1, centro.getZ() + offZ);
    }

    /** Il dio steso e, accanto, l'alieno che lo ha ammazzato. */
    private static void piazzaCorpi(ServerLevel livello, BlockPos centro) {
        Block pezzoDio = MyMod.PEZZO_DIO.get();
        Block pezzoAlieno = MyMod.PEZZO_ALIENO.get();

        // il dio: l'ancora sta al capo, il corpo si stende verso Z positiva
        BlockPos ancoraDio = appoggia(livello, centro, 2, -3, DioMortoBlock.SEDIME);
        livello.setBlock(ancoraDio, MyMod.DIO_MORTO.get().defaultBlockState(), 3);
        for (int[] cella : DioMortoBlock.SEDIME) {
            BlockPos p = ancoraDio.offset(cella[0], cella[1], cella[2]);
            if (!p.equals(ancoraDio)) {
                livello.setBlock(p, pezzoDio.defaultBlockState(), 3);
            }
        }

        // l'alieno, qualche blocco di fianco
        BlockPos ancoraAlieno = appoggia(livello, centro, -4, 0, AlienoFusoBlock.SEDIME);
        livello.setBlock(ancoraAlieno, MyMod.ALIENO_FUSO.get().defaultBlockState(), 3);
        for (int[] cella : AlienoFusoBlock.SEDIME) {
            livello.setBlock(ancoraAlieno.offset(cella[0], cella[1], cella[2]),
                    pezzoAlieno.defaultBlockState(), 3);
        }
    }

    /**
     * Spiana il fondo sotto l'impronta di un corpo e dice dove appoggiarlo.
     *
     * <p>Il fondo della conca e' una calotta, quindi sotto un corpo lungo sette blocchi sta a
     * quote diverse. Appoggiandolo alla quota del centro — com'era prima — da una parte restava
     * sepolto e dall'altra sospeso. Qui si prende la quota piu' alta del fondo sotto tutta
     * l'impronta, si riempie fino a quella e si libera lo spazio sopra: cosi' il corpo poggia
     * su una piazzola piana, tutto fuori e senza vuoti sotto.</p>
     */
    private static BlockPos appoggia(ServerLevel livello, BlockPos centro,
                                     int offX, int offZ, int[][] sedime) {
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0, altezza = 0;
        for (int[] cella : sedime) {
            minX = Math.min(minX, cella[0]);
            maxX = Math.max(maxX, cella[0]);
            minZ = Math.min(minZ, cella[2]);
            maxZ = Math.max(maxZ, cella[2]);
            altezza = Math.max(altezza, cella[1]);
        }

        int cima = Integer.MIN_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cima = Math.max(cima, centro.getY() - scavoA(offX + x, offZ + z));
            }
        }

        BlockState crosta = Blocks.BLACKSTONE.defaultBlockState();
        BlockState aria = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursore = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int px = centro.getX() + offX + x;
                int pz = centro.getZ() + offZ + z;
                for (int y = centro.getY() - scavoA(offX + x, offZ + z); y <= cima; y++) {
                    cursore.set(px, y, pz);
                    if (livello.getBlockState(cursore).isAir()) {
                        livello.setBlock(cursore, crosta, 2);
                    }
                }
                for (int y = cima + 1; y <= cima + 1 + altezza; y++) {
                    cursore.set(px, y, pz);
                    if (!livello.getBlockState(cursore).isAir()) {
                        livello.setBlock(cursore, aria, 2);
                    }
                }
            }
        }
        return new BlockPos(centro.getX() + offX, cima + 1, centro.getZ() + offZ);
    }
}
