package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

/**
 * I filamenti della tuta visti dagli occhi di chi la porta.
 *
 * <p>In prima persona il corpo non viene disegnato: esistono solo il braccio e cio' che
 * Palladium gli attacca sopra con {@code renderArm}, quindi anche la Necrospada. Gli
 * ancoraggi di {@link TentacoliTuta} qui non servono — sono in coordinate del mondo e
 * pescano petto, schiena e cosce, che in prima persona non ci sono e finirebbero a
 * ondeggiare in mezzo allo schermo.</p>
 *
 * <p>Si aggancia a {@code RenderArmEvent}, la cui pila e' nello stesso stato che riceve
 * {@code PlayerRenderer.renderHand}: prima della translateAndRotate del braccio. Le
 * coordinate sono quindi quelle del modello vanilla divise per 16 — le stesse con cui
 * vengono disegnati i cubi del braccio — e non numeri trovati a occhio davanti allo
 * schermo. Il modello e' Bedrock (+Y in su, piedi a zero) mentre il vanilla ha +Y in giu'
 * partendo dall'alto, da cui la conversione {@code vy = 24 - by} gia' applicata ai valori
 * qui sotto.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class TentacoliPrimaPersona {

    private static final ResourceLocation POTERE = new ResourceLocation(MyMod.MOD_ID, "allblack");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/venom_tentacle_segment.png");

    /**
     * Molto piu' corti di quelli in terza persona.
     *
     * <p>Qui il braccio sta a mezzo blocco dall'occhio: a 0.22 i filamenti erano lunghi quanto
     * meta' braccio e si proiettavano nel cielo aperto. Restavano attaccati — la radice e'
     * fissa, {@code smorza} vale zero a q=0 — ma si leggevano come cose sospese accanto al
     * braccio invece che come simbionte che striscia sulla tuta.</p>
     */
    private static final double LUNGHEZZA = 0.085D;
    private static final float RAGGIO_BASE = 0.006F;
    private static final float RAGGIO_PUNTA = 0.0012F;
    private static final int SEGMENTI = 10;
    private static final double CONTORSIONE = 0.009D;

    /** Punto e direzione d'uscita, in coordinate del braccio. */
    private static final double[][] ANCORE = {
            // Tutti sulla faccia esterna del braccio (x = -8 nel modello), che in prima
            // persona e' quella grande rivolta alla telecamera, e ben dentro i suoi bordi.
            //
            // Le versioni precedenti ne mettevano anche sulle facce davanti e dietro
            // (z = -+2), che pero' in prima persona si vedono di taglio: stando sul filo del
            // bordo, qualunque scostamento le portava fuori dalla sagoma e sembravano
            // sospese. Non era la lunghezza ne' la scala: era la faccia sbagliata.
            //
            // Radice 0.01 dentro la superficie, direzione quasi tutta lungo l'asse del
            // braccio con 0.15 di uscita: emergono dalla tuta.
            //
            // La y cresce verso la MANO, che in prima persona e' la punta visibile in alto a
            // sinistra (la spalla sta fuori schermo in basso a destra), e il braccio finisce a
            // y = 0.756. Con ancoraggi fino a 0.70 e direzione +y, la corsa di 0.085 arrivava
            // a 0.785: oltre la fine del braccio, e il filamento usciva dalla punta. Ora ogni
            // radice piu' un intero passo resta dentro 0.24..0.67, ben lontano da entrambi
            // gli estremi.
            {-0.490, 0.320, -0.070,  -0.15, 0.99, 0.00},
            {-0.490, 0.400, 0.060,   -0.15, -0.99, 0.00},
            {-0.490, 0.460, -0.045,  -0.15, 0.99, 0.00},
            {-0.490, 0.520, 0.070,   -0.15, -0.99, 0.00},
            {-0.490, 0.580, -0.060,  -0.15, 0.99, 0.00},
            {-0.490, 0.620, 0.040,   -0.15, -0.99, 0.00},
            // Sulla Necrospada, che in prima persona VIENE disegnata: e' l'oggetto lungo che
            // attraversa in basso a sinistra. Avevo concluso il contrario guardando ritagli
            // della sola meta' destra dello schermo, dove c'e' il braccio e la lama non entra.
            //
            // La lama va dall'impugnatura (-0.389, 0.830, 0.062) alla punta
            // (-0.023, 0.393, -1.505): questi sei stanno fra z -0.22 e -1.16, lontani da
            // entrambi gli estremi.
            //
            // Qui, al contrario del braccio, la direzione e' la NORMALE della faccia piatta
            // (-0.60, 0.72, -0.34), alternata sulle due facce: sul braccio i filamenti devono
            // aderire, sulla lama devono sporgere. Correndo lungo l'asse restavano dentro la
            // sagoma e, essendo neri su una lama nera, non si distinguevano affatto.
            //
            // La y porta gia' una correzione di -0.18 rispetto a quanto calcolato dal geo.
            // E' un valore MISURATO, non spiegato. Senza correzione i filamenti restavano
            // 0.21 sopra la lama; con -0.30 finivano 0.14 sotto: i due scarti danno -0.18 per
            // interpolazione. Direzione e scala erano gia' corrette — l'asse disegnato
            // risultava parallelo alla lama — quindi lo scarto e' una traslazione pura fra il
            // geo e cio' che Palladium disegna sul braccio. Se un giorno se ne trova la causa,
            // questa costante sparisce.
            {-0.317, 0.564, -0.217,  -0.60, 0.72, -0.34},
            {-0.286, 0.526, -0.412,   0.60, -0.72, 0.34},
            {-0.229, 0.459, -0.593,  -0.60, 0.72, -0.34},
            {-0.198, 0.421, -0.788,   0.60, -0.72, 0.34},
            {-0.141, 0.354, -0.969,  -0.60, 0.72, -0.34},
            {-0.110, 0.316, -1.164,   0.60, -0.72, 0.34},
    };

    /** Da qui in poi si sta sulla lama, che ha un interruttore suo. */
    private static final int PRIMO_SULLA_LAMA = 6;

    /** Passaggio di taratura: disegna la gabbia del braccio invece dei filamenti. */
    private static final boolean CALIBRA = false;

    /** Le quote provate dalla taratura, dall'alto in basso. La prima e' quella di prima. */
    private static final double[] QUOTE_PROVA = {0.0D, -0.25D, -0.50D, -0.75D};

    private TentacoliPrimaPersona() {
    }

    /**
     * Spento finche' la trasformazione della prima persona non e' misurata davvero.
     *
     * <p>Quello che si e' accertato, per chi ci tornera' sopra:</p>
     * <ul>
     *   <li>{@code RenderArmEvent} scatta in {@code PlayerRenderer.renderRightHand} con la
     *       stessa pila che riceve {@code renderHand} — verificato sul bytecode.</li>
     *   <li>Palladium non aggiunge nessuna trasformazione sua: {@code PackRenderLayer.renderArm}
     *       prende il {@code rightArm} del modello del layer e chiama {@code ModelPart.render}
     *       sulla pila com'e', agganciandosi dentro {@code renderHand}. Non c'e' quindi una
     *       trasformazione di Palladium da ereditare.</li>
     *   <li>Eppure una gabbia disegnata sull'ingombro del braccio — 0.25 x 0.75 x 0.25 blocchi
     *       con la conversione {@code (x/16, (24-y)/16, z/16)} — riempie tutto lo schermo.
     *       L'unita' di quello spazio non e' il blocco, e il fattore non e' uno.</li>
     * </ul>
     *
     * <p>Manca quindi una scala, non un segno: e' li' che va ripreso il lavoro. Finche' non e'
     * misurata, meglio niente che filamenti sospesi nel vuoto.</p>
     */
    private static final boolean ATTIVO = true;

    @SubscribeEvent
    public static void suBraccio(RenderArmEvent evento) {
        if (!ATTIVO) {
            return;
        }
        // il modello e la spada pendono dal braccio destro: sul sinistro non c'e' niente
        if (evento.getArm() != HumanoidArm.RIGHT) {
            return;
        }
        Player giocatore = evento.getPlayer();
        if (!"allblack".equals(VenomTentaclesTraversalRenderer.formaDi(giocatore))
                || !AbilityUtil.isEnabled(giocatore, POTERE, "enablevenom")) {
            return;
        }
        boolean spada = AbilityUtil.isEnabled(giocatore, POTERE, "enablenecrosword");

        PoseStack pila = evento.getPoseStack();
        MultiBufferSource buffer = evento.getMultiBufferSource();
        Minecraft minecraft = Minecraft.getInstance();
        long tempo = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        float parziale = minecraft.getFrameTime();

        if (CALIBRA) {
            calibra(pila, buffer, tempo);
            return;
        }
        for (int i = 0; i < ANCORE.length; i++) {
            if (i >= PRIMO_SULLA_LAMA && !spada) {
                break;
            }
            double[] a = ANCORE[i];
            Vec3 radice = new Vec3(a[0], a[1], a[2]);
            Vec3 fuori = new Vec3(a[3], a[4], a[5]).normalize();

            // sulla lama servono piu' lunghi: sono neri su una lama nera, e a lunghezza da
            // braccio restano dentro la sagoma e non si distinguono. La lama e' lunga 1.5
            // blocchi e li regge senza sembrare sproporzionati.
            boolean sullaLama = i >= PRIMO_SULLA_LAMA;
            double lunghezza = sullaLama ? LUNGHEZZA * 2.4D : LUNGHEZZA;
            double contorsione = sullaLama ? CONTORSIONE * 2.2D : CONTORSIONE;

            double t = tempo * 0.09D + parziale * 0.09D + i * 1.9D;
            double respiro = 0.55D + 0.45D * Math.sin(t * 0.6D);
            Vec3 sbandata = new Vec3(Math.sin(t * 1.3D), Math.cos(t * 0.9D), Math.sin(t * 1.1D))
                    .scale(contorsione);
            Vec3 punta = radice.add(fuori.scale(lunghezza * respiro)).add(sbandata);

            VenomTentaclesTraversalRenderer.disegnaFilamento(radice, punta, i, TEXTURE,
                    RAGGIO_BASE, RAGGIO_PUNTA, SEGMENTI, contorsione * 0.6D,
                    pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }

    /**
     * Disegna il contorno della scatola del braccio destro vanilla, nelle coordinate in cui
     * credo di stare.
     *
     * <p>Serve a smettere di indovinare: se la gabbia avvolge il braccio, lo spazio e' quello
     * giusto e sbagliano solo gli ancoraggi; se sta altrove, si vede di quanto e in che
     * direzione. La scatola e' {@code addBox(-3,-2,-2, 4,12,4)} sul perno {@code (-5,2,0)},
     * quindi in assoluto x da -8 a -4, y da 0 a 12, z da -2 a 2, tutto diviso 16.</p>
     */
    /**
     * Quattro gabbie del braccio a quote diverse, per trovare la traslazione mancante.
     *
     * <p>La scatola e' quella vanilla — {@code addBox(-3,-2,-2, 4,12,4)} sul perno
     * {@code (-5,2,0)} — e la prima e' esattamente dove la mettevo prima, cioe' troppo in
     * alto. Le altre scendono di un quarto di blocco per volta. Quella che avvolge il
     * braccio da' l'offset da applicare a tutti gli ancoraggi, senza altri tentativi.</p>
     */
    /**
     * Una gabbia sola, attorno alla scatola della Necrospada.
     *
     * <p>Il passaggio precedente ha mostrato una cosa che cambia il problema: in prima
     * persona il braccio non viene disegnato, si vede solo la spada. Sette dei tredici
     * ancoraggi stavano quindi su un braccio inesistente, ed erano quelli che fluttuavano.
     * Qui si prova solo la conversione: se la gabbia avvolge la lama, lo spazio e' giusto
     * e restano da tenere i soli ancoraggi sulla spada.</p>
     *
     * <p>I numeri vengono dall'ingombro vero del geo, convertito con
     * {@code (x/16, (24-y)/16, z/16)}.</p>
     */
    /**
     * Una terna di assi dall'origine dello spazio di RenderArmEvent.
     *
     * <p>Le gabbie non bastavano: dicevano che sbagliavo ma non in cosa. Qui si disegnano i
     * tre assi con lunghezze diverse — X corto, Y medio, Z lungo — cosi' dallo schermo si
     * legge dove va ciascuno, e la trasformazione si ricava per calcolo invece che per
     * tentativi. Il quarto segmento, il piu' lungo di tutti, e' la diagonale della spada
     * secondo la conversione attuale: serve a vedere quanto e' fuori rispetto alla lama
     * vera, che in prima persona e' l'unico riferimento visibile.</p>
     */
    /**
     * Le due scatole insieme: spessa il braccio, sottile la spada.
     *
     * <p>Palladium non applica trasformazioni proprie — {@code PackRenderLayer.renderArm}
     * chiama {@code ModelPart.render} sulla pila com'e' — e si aggancia dentro
     * {@code renderHand}, la stessa pila di RenderArmEvent. Quindi la conversione
     * {@code (x/16, (24-y)/16, z/16)} dovrebbe essere giusta. Se lo e', la gabbia spessa
     * avvolge l'oggetto scuro in basso, e allora quello e' il braccio e la spada in prima
     * persona semplicemente non viene disegnata. Se invece nessuna delle due lo avvolge,
     * sbaglia la conversione e non l'oggetto.</p>
     */
    /**
     * La gabbia del braccio a cinque scale insieme: 1, 1/2, 1/4, 1/8, 1/16.
     *
     * <p>La misura precedente ha spostato il problema: la scatola del braccio, 0.25 x 0.75 x
     * 0.25 blocchi, riempiva lo schermo. Non e' fuori posto, e' fuori scala. Disegnarle tutte
     * insieme fa cadere la domanda su un solo numero, leggibile in una schermata sola: quella
     * che avvolge il braccio da' il fattore.</p>
     */
    /**
     * L'ingombro della Necrospada, per misurare lo scarto costante.
     *
     * <p>I filamenti sulla lama compaiono paralleli ad essa ma spostati di circa 0.12
     * blocchi: direzione giusta, posizione no. Con la gabbia si legge di quanto e in che
     * verso, invece di provare correzioni a caso.</p>
     */
    /**
     * L'asse della lama: un solo segmento dall'impugnatura alla punta.
     *
     * <p>La gabbia dell'ingombro non serviva: e' un parallelepipedo che arriva vicinissimo
     * alla telecamera, quindi i suoi spigoli vicini si proiettano enormi e non dicono nulla
     * sulla lama, che dentro quel volume e' un oggetto sottile in diagonale. Il segmento
     * invece si sovrappone alla lama se le coordinate sono giuste, e mostra direzione e
     * scarto se non lo sono.</p>
     */
    /**
     * L'asse della lama a quattro quote, per leggere lo scarto in una schermata sola.
     *
     * <p>Il segmento sull'asse calcolato risulta parallelo alla lama ma spostato verso l'alto
     * di circa 0.21 blocchi. In questo spazio y cresce verso la mano, che sullo schermo sta in
     * alto a sinistra: la correzione e' quindi una diminuzione di y. Il piu' sottile e' senza
     * correzione, e poi si scende di 0.10 per volta ingrossando: quello che si sovrappone alla
     * lama da' il numero.</p>
     */
    private static void calibra(PoseStack pila, MultiBufferSource buffer, long tempo) {
        int n = 0;
        float[] spessori = {0.004F, 0.008F, 0.013F, 0.019F};
        double[] correzioni = {0.0D, -0.10D, -0.20D, -0.30D};
        for (int k = 0; k < correzioni.length; k++) {
            Vec3 a = new Vec3(-0.389D, 0.830D + correzioni[k], 0.062D);
            Vec3 b = new Vec3(-0.023D, 0.393D + correzioni[k], -1.505D);
            VenomTentaclesTraversalRenderer.disegnaFilamento(a, b, n++, TEXTURE,
                    spessori[k], spessori[k], 2, 0.0D, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }

    /** Una scatola in coordinate Bedrock, convertita e disegnata a spigoli. */
    private static void gabbia(double bx0, double bx1, double by0, double by1,
                               double bz0, double bz1, float raggio, int seme,
                               PoseStack pila, MultiBufferSource buffer, long tempo) {
        double x0 = bx0 / 16.0D, x1 = bx1 / 16.0D;
        double y0 = (24.0D - by1) / 16.0D, y1 = (24.0D - by0) / 16.0D;
        double z0 = bz0 / 16.0D, z1 = bz1 / 16.0D;
        double[][] a = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1},
        };
        int[][] sp = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        int n = seme;
        for (int[] e : sp) {
            VenomTentaclesTraversalRenderer.disegnaFilamento(
                    new Vec3(a[e[0]][0], a[e[0]][1], a[e[0]][2]),
                    new Vec3(a[e[1]][0], a[e[1]][1], a[e[1]][2]),
                    n++, TEXTURE, raggio, raggio, 2, 0.0D, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }

    private static void disegnaDritto(Vec3 a, Vec3 b, int indice,
                                      PoseStack pila, MultiBufferSource buffer, long tempo) {
        VenomTentaclesTraversalRenderer.disegnaFilamento(a, b, indice, TEXTURE,
                0.010F, 0.010F, 2, 0.0D, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
    }
}