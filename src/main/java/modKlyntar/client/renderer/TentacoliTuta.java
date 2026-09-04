package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * I filamenti che strisciano sulla tuta di chi porta All-Black.
 *
 * <p>Non sono i bracci della tentacles_traversal, che escono per aggrapparsi a qualcosa: questi restano
 * corti e non arrivano da nessuna parte. Si contorcono sul posto, come se la corazza non fosse
 * un vestito ma una cosa viva appoggiata addosso.</p>
 *
 * <p>Gli ancoraggi sono in coordinate del corpo — destra, alto, avanti — e vengono girati con
 * la rotazione del busto, non con quella della testa: legandoli allo sguardo ruoterebbero
 * quando il giocatore si guarda intorno stando fermo.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class TentacoliTuta {

    /** Oltre questa distanza non si disegnano: a quel punto non si distinguono comunque. */
    private static final double VISIBILITA = 24.0D;
    /** Corti: devono strisciare sulla tuta, non allungarsi. */
    private static final double LUNGHEZZA = 0.34D;
    private static final float RAGGIO_BASE = 0.028F;
    private static final float RAGGIO_PUNTA = 0.005F;
    /** Pochi tratti: sono corti, e a questa scala una curva finissima non si vede. */
    private static final int SEGMENTI = 12;
    /** Quanto si contorcono. */
    private static final double CONTORSIONE = 0.16D;

    /** Il potere, per chiedere a Palladium quali abilita' sono accese. */
    private static final ResourceLocation POTERE = new ResourceLocation(MyMod.MOD_ID, "allblack");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/venom_tentacle_segment.png");

    /**
     * Dove nascono, in coordinate del corpo: destra, alto, avanti.
     *
     * <p>Le prime tre componenti sono il punto, le seconde tre la direzione in cui esce. Sono
     * distribuiti su petto, schiena, spalle, braccia e cosce, cosi' la tuta brulica dappertutto
     * e non solo davanti.</p>
     */
    private static final double[][] ANCORE = {
            // petto
            {0.10, 1.35, 0.16,   0.35, 0.30, 0.90},
            {-0.14, 1.22, 0.15,  -0.40, 0.20, 0.90},
            {0.02, 1.48, 0.14,   0.10, 0.75, 0.65},
            // schiena
            {-0.08, 1.38, -0.16, -0.30, 0.35, -0.90},
            {0.12, 1.20, -0.15,   0.40, 0.15, -0.90},
            {0.00, 1.52, -0.13,   0.00, 0.80, -0.60},
            // spalle
            {0.32, 1.46, 0.02,    0.85, 0.50, 0.15},
            {-0.32, 1.46, -0.02, -0.85, 0.50, -0.15},
            // braccia
            {0.36, 1.15, 0.04,    0.90, 0.05, 0.35},
            {-0.36, 1.05, -0.04, -0.90, -0.10, -0.35},
            {0.34, 0.92, -0.05,   0.80, -0.30, -0.45},
            {-0.34, 1.28, 0.05,  -0.80, 0.25, 0.45},
            // fianchi e cosce
            {0.20, 0.78, 0.10,    0.70, -0.35, 0.60},
            {-0.20, 0.70, -0.10, -0.70, -0.45, -0.55},
            {0.14, 0.52, -0.08,   0.55, -0.70, -0.45},
            {-0.14, 0.46, 0.08,  -0.55, -0.75, 0.40},
            // La Necrospada. I punti sono campionati lungo l'asse della lama in
            // necroswordback.geo.json e le direzioni sono la normale vera della faccia
            // piatta, presa dalla geometria: escono di taglio dalle due facce, alternati.
            //
            // La conversione modello -> argomenti di dalCorpo e' (m.x/16, m.y/16, -m.z/16).
            // Il primo argomento di dalCorpo finisce su +X a imbardata zero, cioe' e' la
            // SINISTRA nonostante il nome: verificato contro VenomTentaclesTraversalRenderer, dove
            // right = (-look.z, 0, look.x) vale -X. Fidarsi del nome mi ha fatto mettere i
            // filamenti prima davanti e poi sul fianco sbagliato.
            {-0.34, 0.73, 0.16,  -0.60, -0.72, 0.34},
            {-0.28, 0.80, 0.41,  0.60, 0.72, -0.34},
            {-0.22, 0.87, 0.66,  -0.60, -0.72, 0.34},
            {-0.16, 0.94, 0.91,  0.60, 0.72, -0.34},
            {-0.10, 1.01, 1.16,  -0.60, -0.72, 0.34},
            {-0.04, 1.08, 1.41,  0.60, 0.72, -0.34},
    };

    /**
     * Prova: disegna l'ingombro della Necrospada, sempre, senza guardare l'interruttore.
     *
     * <p>Serve a separare due cause. Il layer della spada si accende sul VALORE del timer di
     * {@code enablenecrosword} (1..8), i filamenti invece su {@code isEnabled}: se il toggle e'
     * spento ma il timer non e' ancora tornato a zero, la lama si vede e i filamenti no. Se la
     * gabbia avvolge la lama, le posizioni sono giuste e sbaglia il gate; se non la avvolge,
     * sbagliano le posizioni.</p>
     */
    private static final boolean CALIBRA_SPADA = false;

    /** Da questo indice in poi gli ancoraggi stanno sulla lama e seguono il braccio destro. */
    private static final int PRIMO_SULLA_LAMA = 16;

    /** La spalla destra, perno dell'oscillazione: nel modello sta a (-5, 22, 0). */
    private static final double SPALLA_DESTRA = -0.3125D;
    private static final double SPALLA_ALTEZZA = 1.375D;

    private TentacoliTuta() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Vec3 telecamera = evento.getCamera().getPosition();
        PoseStack pila = evento.getPoseStack();
        long tempo = minecraft.level.getGameTime();
        float parziale = minecraft.getFrameTime();

        boolean qualcuno = false;
        for (Player giocatore : minecraft.level.players()) {
            if (giocatore.isSpectator()
                    || giocatore.position().distanceTo(telecamera) > VISIBILITA
                    || !"allblack".equals(VenomTentaclesTraversalRenderer.formaDi(giocatore))) {
                continue;
            }
            // Il gate e' l'abilita' stessa, non la taglia. Prima guardavo Klyntar.VenomSize,
            // ma quell'objective per All-Black non lo scrive piu' nessuno: lo mettevano
            // venomcameraheight/reset, che ho tolto per avere la camera vanilla. Restava al
            // valore lasciato da un'altra forma, e i filamenti erano sempre accesi.
            // AbilityUtil legge lo stato che Palladium sincronizza gia' ai client per i
            // render layer, quindi non serve un pacchetto in piu'.
            if (!AbilityUtil.isEnabled(giocatore, POTERE, "enablevenom")) {
                continue;
            }
            // la Necrospada ha il suo interruttore: senza lama, niente filamenti sulla lama
            boolean spada = AbilityUtil.isEnabled(giocatore, POTERE, "enablenecrosword");
            if (giocatore == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                continue;                  // in prima persona il proprio corpo non si vede
            }
            if (!qualcuno) {
                pila.pushPose();
                pila.translate(-telecamera.x, -telecamera.y, -telecamera.z);
                qualcuno = true;
            }
            disegna(giocatore, parziale, tempo, spada, pila, buffer);
        }
        if (qualcuno) {
            pila.popPose();
            buffer.endBatch();
        }
    }

    private static void disegna(Player giocatore, float parziale, long tempo,
                                boolean spada, PoseStack pila, MultiBufferSource buffer) {
        Vec3 piedi = giocatore.getPosition(parziale);
        // il busto, non la testa: legarli allo sguardo li farebbe girare da fermo
        double imbardata = Math.toRadians(
                Mth.rotLerp(parziale, giocatore.yBodyRotO, giocatore.yBodyRot));
        double cos = Math.cos(imbardata), sin = Math.sin(imbardata);

        // la spada pende dal braccio destro e oscilla con lui: e' la stessa formula che
        // PlayerModel usa per il braccio, altrimenti camminando i filamenti restano indietro
        float passo = giocatore.walkAnimation.position(parziale);
        float ampiezza = giocatore.walkAnimation.speed(parziale);
        double bracciata = Math.cos(passo * 0.6662D + Math.PI) * ampiezza;
        double cosB = Math.cos(bracciata), sinB = Math.sin(bracciata);

        for (int i = 0; i < ANCORE.length; i++) {
            if (i >= PRIMO_SULLA_LAMA && !spada) {
                break;
            }
            double[] a = ANCORE[i];
            double pd = a[0], pa = a[1], pf = a[2];
            double dd = a[3], da = a[4], df = a[5];
            if (i >= PRIMO_SULLA_LAMA) {
                double[] p = attornoAllaSpalla(pd, pa, pf, cosB, sinB);
                pd = p[0]; pa = p[1]; pf = p[2];
                double[] d = ruotaAttornoADestra(dd, da, df, cosB, sinB);
                dd = d[0]; da = d[1]; df = d[2];
            }
            Vec3 radice = piedi.add(dalCorpo(pd, pa, pf, cos, sin));
            if (i == PRIMO_SULLA_LAMA && CALIBRA_SPADA) {
                gabbiaSpada(piedi, cos, sin, pila, buffer, tempo);
            }
            Vec3 fuori = dalCorpo(dd, da, df, cos, sin).normalize();

            // ognuno si contorce per conto suo, e ogni tanto si ritrae e riesce
            double t = tempo * 0.09D + parziale * 0.09D + i * 1.9D;
            double respiro = 0.55D + 0.45D * Math.sin(t * 0.6D);
            Vec3 sbandata = new Vec3(Math.sin(t * 1.3D), Math.cos(t * 0.9D), Math.sin(t * 1.1D))
                    .scale(CONTORSIONE);
            Vec3 punta = radice.add(fuori.scale(LUNGHEZZA * respiro)).add(sbandata);

            VenomTentaclesTraversalRenderer.disegnaFilamento(radice, punta, i, TEXTURE,
                    RAGGIO_BASE, RAGGIO_PUNTA, SEGMENTI, CONTORSIONE * 0.6D,
                    pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }

    /** Fa oscillare un punto della lama attorno alla spalla, come il braccio. */
    private static double[] attornoAllaSpalla(double destra, double alto, double avanti,
                                              double cos, double sin) {
        double[] r = ruotaAttornoADestra(destra - SPALLA_DESTRA, alto - SPALLA_ALTEZZA, avanti,
                                         cos, sin);
        return new double[]{r[0] + SPALLA_DESTRA, r[1] + SPALLA_ALTEZZA, r[2]};
    }

    /** Rotazione nel piano alto/avanti, cioe' attorno all'asse che punta a destra. */
    private static double[] ruotaAttornoADestra(double destra, double alto, double avanti,
                                                double cos, double sin) {
        return new double[]{destra, alto * cos - avanti * sin, alto * sin + avanti * cos};
    }

    /** L'ingombro della spada misurato sul geo, in coordinate del corpo. */
    private static void gabbiaSpada(Vec3 piedi, double cos, double sin,
                                    PoseStack pila, MultiBufferSource buffer, long tempo) {
        double a0 = -0.63D, a1 = 0.02D;
        double u0 = 0.47D, u1 = 1.15D;
        double f0 = -0.29D, f1 = 1.56D;
        double[][] p = {
                {a0, u0, f0}, {a1, u0, f0}, {a1, u0, f1}, {a0, u0, f1},
                {a0, u1, f0}, {a1, u1, f0}, {a1, u1, f1}, {a0, u1, f1},
        };
        int[][] sp = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        int n = 900;
        for (int[] e : sp) {
            Vec3 x = piedi.add(dalCorpo(p[e[0]][0], p[e[0]][1], p[e[0]][2], cos, sin));
            Vec3 y = piedi.add(dalCorpo(p[e[1]][0], p[e[1]][1], p[e[1]][2], cos, sin));
            VenomTentaclesTraversalRenderer.disegnaFilamento(x, y, n++, TEXTURE,
                    0.012F, 0.012F, 2, 0.0D, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }

    /** Da coordinate del corpo (destra, alto, avanti) a coordinate del mondo. */
    private static Vec3 dalCorpo(double destra, double alto, double avanti,
                                 double cos, double sin) {
        // con imbardata 0 il giocatore guarda verso Z positiva
        return new Vec3(destra * cos - avanti * sin, alto, destra * sin + avanti * cos);
    }
}
