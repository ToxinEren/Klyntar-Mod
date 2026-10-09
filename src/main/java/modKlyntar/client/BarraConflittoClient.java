package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.symbiote.RegistroSimbionti;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La barra del conflitto fra due simbionti nello stesso corpo, solo per l'ospite.
 *
 * <p>Due tentacoli, uno per lato, che si spingono fino al punto dello scontro e li' si
 * attorcigliano: a sinistra quello che era gia' nel corpo, a destra quello entrato da fuori, ognuno
 * con la pelle della sua famiglia (il disegno sta in {@link DisegnoTentacoli}). Chi tiene piu'
 * corpo ha il tentacolo piu' lungo: se sta vincendo Riot, il grigio arriva quasi in fondo. Sopra i
 * nomi, sotto una riga sottile che e' la tensione: quando arriva in fondo, uno dei due prevale.</p>
 *
 * <p>Sta sotto le barre dei boss (la fame del simbionte e' una di quelle), e scende se ce ne sono
 * altre. Il confine insegue il valore vero con dolcezza invece di saltare a ogni secondo; finita la
 * lotta resta un attimo a mostrare com'e' andata, poi sfuma.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class BarraConflittoClient {
    private static final int LARGHEZZA = 182;
    private static final long TENUTA_MS = 1500L;
    private static final long SFUMA_MS = 700L;
    /** Quanto in fretta il confine insegue il valore vero, al secondo. */
    private static final float INSEGUE = 4.0F;

    private static boolean visibile;
    private static boolean attiva;
    private static String dentro = "";
    private static String intruso = "";
    private static float bersaglio = 0.5F;
    private static float mostrato = 0.5F;
    private static float tensione;
    private static long fine;
    private static long ultimoFotogramma;
    /** Fin dove arrivano le barre dei boss in questo fotogramma. */
    private static int fondoBoss;
    /** Dove finisce la barra, per chi disegna sotto di lei (le battute del simbionte); 0 se non c'e'. */
    private static int fondo;

    private BarraConflittoClient() {
    }

    /** Il bordo basso della barra del conflitto, o 0 quando non si vede. */
    public static int fondo() {
        return visibile ? fondo : 0;
    }

    /** Arriva dal server: l'andamento, o la fine della lotta. */
    public static void ricevi(boolean attivaOra, String suoDentro, String suoIntruso, float equilibrio, float suaTensione) {
        if (attivaOra && (!visibile || !attiva)) {
            // una lotta nuova parte dal centro
            mostrato = 0.5F;
        }
        visibile = true;
        attiva = attivaOra;
        dentro = suoDentro;
        intruso = suoIntruso;
        bersaglio = equilibrio;
        tensione = suaTensione;
        if (!attivaOra) {
            fine = Util.getMillis();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        fondoBoss = 0;
    }

    /** Dopo la barra organica della fame, che si allarga, e anche quando ha annullato il disegno standard. */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW, receiveCanceled = true)
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        fondoBoss = Math.max(fondoBoss, event.getY() + event.getIncrement());
    }

    public static final IGuiOverlay OVERLAY = (gui, grafica, partialTick, larghezza, altezza) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visibile || minecraft.player == null || minecraft.options.hideGui) {
            if (minecraft.player == null) {
                visibile = false;
            }
            return;
        }
        long adesso = Util.getMillis();
        float alfa = 1.0F;
        if (!attiva) {
            long dopo = adesso - fine;
            if (dopo > TENUTA_MS + SFUMA_MS) {
                visibile = false;
                return;
            }
            if (dopo > TENUTA_MS) {
                alfa = 1.0F - (dopo - TENUTA_MS) / (float) SFUMA_MS;
            }
        }
        float dt = Math.min(0.1F, Math.max(0.0F, (adesso - ultimoFotogramma) / 1000.0F));
        ultimoFotogramma = adesso;
        mostrato += (bersaglio - mostrato) * Math.min(1.0F, dt * INSEGUE);

        Font font = minecraft.font;
        int x = (larghezza - LARGHEZZA) / 2;
        int yNomi = Math.max(fondoBoss, 12) + 2;
        int y = yNomi + font.lineHeight + 1;
        int a = Math.max(4, Math.round(alfa * 255.0F)) << 24;

        grafica.drawString(font, nome(dentro), x, yNomi, a | 0xFFFFFF, true);
        String destra = nome(intruso);
        grafica.drawString(font, destra, x + LARGHEZZA - font.width(destra), yNomi, a | 0xFFFFFF, true);

        // i due tentacoli, uno per lato, che si attorcigliano nel punto dello scontro
        int alta = BarreOrganicheClient.altezzaConflitto();
        int yTentacoli = y - 5;
        BarreOrganicheClient.disegnaConflitto(grafica, x, yTentacoli, dentro, intruso,
                Math.max(0.0F, Math.min(1.0F, mostrato)), alfa);
        // la tensione: quando la riga arriva in fondo, uno dei due prevale
        int yRiga = yTentacoli + alta - 3;
        int lunga = Math.round(LARGHEZZA * Math.max(0.0F, Math.min(1.0F, tensione)));
        grafica.fill(x, yRiga, x + lunga, yRiga + 1, (Math.round(alfa * 170.0F) << 24) | 0xFFFFFF);
        fondo = yRiga + 1;
    };

    private static String nome(String forma) {
        RegistroSimbionti.Simbionte s = RegistroSimbionti.di(forma);
        return s == null ? forma : s.nome();
    }
}
