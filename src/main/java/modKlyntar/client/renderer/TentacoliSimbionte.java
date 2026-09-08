package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Il simbionte vivo: la corazza che gli ricopre il corpo e i tentacoli con cui prende.
 *
 * <p>Due cose diverse disegnate insieme perche' escono dallo stesso corpo. La <b>living
 * armor</b> sono i filamenti corti che spuntano tutt'intorno e respirano: ci sono sempre, e
 * servono a far sembrare il mob una cosa viva invece di un sasso nero. La <b>presa</b> sono i
 * filamenti lunghi che si protendono verso l'ospite agganciato mentre se lo tira addosso.</p>
 *
 * <p>Si disegna in spazio-mondo dentro {@code RenderLevelStageEvent}, come i bracci della
 * tentacles_traversal, e non dentro il renderer GeckoLib del mob: quella pila e' nello spazio
 * del modello, con le rotazioni e la scala che GeckoLib ci ha gia' messo dentro, e ogni punto
 * andrebbe riportato indietro a mano. Qui invece le coordinate sono quelle del mondo, le
 * stesse che il mob e il giocatore dichiarano, e non c'e' niente da convertire.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class TentacoliSimbionte {

    private static final ResourceLocation TEXTURE = new ResourceLocation(MyMod.MOD_ID,
            "textures/models/tentacles_traversal/venom_tentacle_segment.png");

    /** Oltre questa distanza non si disegna niente: sono filamenti sottili, non si vedono. */
    private static final double VISTA = 32.0D;

    /**
     * Il corpo del mob, misurato sul modello: 14 pixel di larghezza per 10 di altezza, per la
     * scala 0.8 del suo renderer. E' l'ovale su cui nascono i filamenti.
     */
    private static final double RAGGIO_X = 0.30D;
    private static final double RAGGIO_Y = 0.20D;
    private static final double RAGGIO_Z = 0.32D;
    /** L'altezza del centro dell'ovale da terra. */
    private static final double CENTRO_Y = 0.20D;

    /** Quanti filamenti di living armor. */
    private static final int CORAZZA = 16;
    /** Quanto salgono: piu' alti del corpo, cosi' si vedono sopra la sagoma del mob. */
    private static final double CORAZZA_ALTEZZA = 0.62D;
    /** Quanto si aprono verso l'esterno salendo: 0 e' un fascio dritto, 1 un ventaglio. */
    private static final double CORAZZA_APERTURA = 0.45D;
    private static final float CORAZZA_RAGGIO = 0.030F;
    private static final float CORAZZA_PUNTA = 0.005F;
    private static final int CORAZZA_SEGMENTI = 9;

    /** Quanti filamenti escono per la presa. */
    private static final int FILI_PRESA = 9;
    private static final float PRESA_RAGGIO = 0.045F;
    private static final float PRESA_PUNTA = 0.008F;
    private static final int PRESA_SEGMENTI = 22;
    /** Quanto puo' allungarsi un filamento a vuoto, prima di aver preso. */
    private static final double PRESA_A_VUOTO = 1.1D;
    /** Fin dove arriva quando ha preso: piu' della portata, cosi' non si stacca sul filo. */
    private static final double PRESA_ALLUNGO = 8.0D;
    /** Quanto ci mette a uscire o a rientrare: piu' basso, piu' lento. */
    private static final float INERZIA = 0.10F;

    private TentacoliSimbionte() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float parziale = evento.getPartialTick();
        Vec3 camera = evento.getCamera().getPosition();
        PoseStack pila = evento.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        long tempo = minecraft.level.getGameTime();

        boolean disegnato = false;
        pila.pushPose();
        pila.translate(-camera.x, -camera.y, -camera.z);
        for (Entity entita : minecraft.level.entitiesForRendering()) {
            if (!(entita instanceof SymbioteEntity simbionte) || simbionte.isInvisible()) {
                continue;
            }
            Vec3 piedi = simbionte.getPosition(parziale);
            if (piedi.distanceToSqr(camera) > VISTA * VISTA) {
                continue;
            }
            corazzaViva(simbionte, piedi, parziale, tempo, pila, buffer);
            presa(simbionte, piedi, parziale, tempo, pila, buffer);
            disegnato = true;
        }
        if (disegnato) {
            buffer.endBatch(RenderType.entityCutoutNoCull(TEXTURE));
        }
        pila.popPose();
    }

    /**
     * La living armor: filamenti che salgono dal pavimento tutt'intorno al mob, e respirano.
     *
     * <p><b>Le radici stanno per terra</b>, non sul corpo. Prima nascevano sulla superficie
     * dell'ovale e puntavano in fuori: meta' di loro partiva da mezz'aria, e il risultato era
     * un mucchio di schegge che fluttuavano staccate dal simbionte invece di sembrarne parte.
     * Nascendo dal suolo dentro la sua impronta hanno un attacco visibile, e salendo passano
     * <em>attraverso</em> il corpo: e' quello che li lega al mob.</p>
     *
     * <p>Le radici stanno su una spirale aurea, non su una griglia: una griglia si legge subito
     * come tale e il simbionte sembrerebbe pettinato. Il raggio varia da filamento a filamento
     * cosi' non finiscono tutti sulla stessa circonferenza.</p>
     */
    private static void corazzaViva(SymbioteEntity simbionte, Vec3 piedi, float parziale,
                                    long tempo, PoseStack pila, MultiBufferSource buffer) {
        double gira = Math.toRadians(-Mth.rotLerp(parziale, simbionte.yBodyRotO, simbionte.yBodyRot));
        double coseno = Math.cos(gira);
        double seno = Math.sin(gira);

        for (int i = 0; i < CORAZZA; i++) {
            double angolo = i * 2.39996D;                 // angolo aureo
            double quanto = 0.30D + 0.70D * ((i * 5 % CORAZZA) / (double) CORAZZA);
            double dx = Math.cos(angolo) * quanto;
            double dz = Math.sin(angolo) * quanto;

            // la radice segue il corpo quando il mob si gira, altrimenti i filamenti
            // scivolerebbero intorno a lui come se fossero appesi al mondo e non a lui
            double rx = dx * coseno - dz * seno;
            double rz = dx * seno + dz * coseno;

            // appena sopra il pavimento: a filo esatto la punta piu' bassa sparirebbe dentro
            Vec3 radice = piedi.add(rx * RAGGIO_X, 0.02D, rz * RAGGIO_Z);

            // salgono aprendosi in fuori, e piu' e' esterna la radice piu' il filamento e'
            // inclinato: al centro escono dritti, sul bordo si piegano come una corolla
            Vec3 su = new Vec3(rx * CORAZZA_APERTURA, 1.0D, rz * CORAZZA_APERTURA).normalize();

            double respiro = 0.55D + 0.45D * Math.sin(tempo * 0.09D + parziale * 0.09D + i * 1.7D);
            Vec3 punta = radice.add(su.scale(CORAZZA_ALTEZZA * (0.65D + 0.35D * respiro)));

            VenomTentaclesTraversalRenderer.disegnaFilamento(radice, punta, i, TEXTURE,
                    CORAZZA_RAGGIO, CORAZZA_PUNTA, CORAZZA_SEGMENTI, 0.05D, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }

    /**
     * I tentacoli della presa: escono, si protendono e restano addosso all'ospite.
     *
     * <p>Non compaiono di colpo. {@code uscitaTentacoli} insegue lo stato vero un pezzetto per
     * fotogramma, cosi' escono e rientrano con calma; e finche' rientrano continuano a puntare
     * l'ultimo agganciato, altrimenti scatterebbero in verticale nell'istante in cui la presa
     * finisce.</p>
     */
    private static void presa(SymbioteEntity simbionte, Vec3 piedi, float parziale, long tempo,
                              PoseStack pila, MultiBufferSource buffer) {
        Player agganciato = simbionte.ospiteAgganciato();
        Player preda = agganciato;
        if (agganciato != null) {
            simbionte.ultimoAgganciato = agganciato.getId();
        } else if (simbionte.ultimoAgganciato != 0
                && simbionte.level().getEntity(simbionte.ultimoAgganciato) instanceof Player ultimo) {
            preda = ultimo;
        }

        float obiettivo = agganciato != null ? 1.0F : 0.0F;
        simbionte.uscitaTentacoli += (obiettivo - simbionte.uscitaTentacoli) * INERZIA;
        if (simbionte.uscitaTentacoli < 0.02F || preda == null) {
            return;
        }
        float uscita = simbionte.uscitaTentacoli;

        // quanto stringono: sotto un terzo ondeggiano soltanto, sopra vanno a prenderlo
        double stretta = Math.max(0.0D, (uscita - 0.35D) / 0.65D);
        stretta = stretta * stretta * (3.0D - 2.0D * stretta);

        Vec3 suoiPiedi = preda.getPosition(parziale);
        double altezza = preda.getBbHeight();
        double fase = tempo * 0.12D + parziale * 0.12D;

        for (int i = 0; i < FILI_PRESA; i++) {
            double q = (i + 0.5D) / FILI_PRESA;
            double alto = 0.85D - 0.5D * q;               // solo la parte alta del corpo
            double anello = Math.sqrt(Math.max(0.0D, 1.0D - alto * alto));
            double angolo = i * 2.39996D;
            Vec3 radice = piedi.add(anello * Math.cos(angolo) * RAGGIO_X,
                    CENTRO_Y + alto * RAGGIO_Y,
                    anello * Math.sin(angolo) * RAGGIO_Z);

            // lo afferrano in punti sparsi su tutta la figura, non tutti in faccia: cosi'
            // sembra che lo avvolgano invece di puntarlo
            double quota = 0.15D + 0.75D * ((i * 7 % FILI_PRESA) / (double) FILI_PRESA);
            Vec3 appiglio = suoiPiedi.add(Math.cos(i * 2.39D) * 0.32D,
                    altezza * quota,
                    Math.sin(i * 2.39D) * 0.32D);

            // a vuoto: ondeggia verso l'alto senza arrivare da nessuna parte
            Vec3 versoAlto = new Vec3(Math.sin(fase + i * 1.3D) * 0.6D, 1.0D,
                    Math.cos(fase * 0.7D + i * 2.1D) * 0.6D).normalize();
            Vec3 puntaLibera = radice.add(versoAlto.scale(
                    PRESA_A_VUOTO * uscita * (0.55D + 0.45D * Math.sin(fase * 1.4D + i * 0.9D))));

            Vec3 versoLui = appiglio.subtract(radice);
            Vec3 puntaPresa = versoLui.length() > PRESA_ALLUNGO
                    ? radice.add(versoLui.normalize().scale(PRESA_ALLUNGO))
                    : appiglio;

            Vec3 punta = puntaLibera.add(puntaPresa.subtract(puntaLibera).scale(stretta));
            // piu' stringe, meno serpeggia: un filamento teso non ondeggia
            double ampiezza = (0.10D + 0.16D * uscita) * (1.0D - 0.75D * stretta);

            VenomTentaclesTraversalRenderer.disegnaFilamento(radice, punta, i, TEXTURE,
                    PRESA_RAGGIO, PRESA_PUNTA, PRESA_SEGMENTI, ampiezza, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }
}
