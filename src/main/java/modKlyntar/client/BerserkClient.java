package modKlyntar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import modKlyntar.MyMod;
import modKlyntar.player.BerserkSimbionte;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Il berserk visto da chi lo subisce: il corpo non risponde piu'.
 *
 * <p>Finche' il simbionte ha una preda i tasti di movimento sono suoi: il giocatore corre in
 * avanti da solo, salta gli ostacoli da solo, e la visuale si gira verso il prossimo punto del
 * percorso o verso la preda (il mouse la sposta appena, poi torna li'). Senza preda il corpo
 * torna al giocatore, che deve portarlo al cibo. Sullo schermo una vignetta rossa pulsa col
 * battito, e lampeggia quando prende il corpo e a ogni preda divorata.</p>
 *
 * <p>Il movimento resta del client, come sempre in Minecraft: il server dice dove andare, e qui
 * si preme "avanti". Prima il server spingeva il giocatore a colpi di velocita' mentre i suoi
 * tasti continuavano a funzionare, e il risultato era un tira e molla a scatti.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class BerserkClient {
    private static final ResourceLocation VIGNETTA = new ResourceLocation(MyMod.MOD_ID, "textures/gui/berserk_vignette.png");
    /** Senza notizie dal server per un secondo e mezzo il corpo torna al giocatore. */
    private static final int SCADENZA = 30;
    /** Quanto tira la visuale verso la preda, per tick. */
    private static final float PRESA_VISUALE = 0.28F;
    /** Fino a tanti gradi fuori rotta si corre a piena velocita'... */
    private static final float GIRO_LIBERO = 25.0F;
    /** ...e oltre questi ci si ferma a girare. */
    private static final float GIRO_FERMO = 75.0F;
    /** Sotto questa distanza (al quadrato) dal punto non si sterza piu'. */
    private static final double SOPRA_IL_PUNTO = 0.64D;
    private static final int BATTITO_CACCIA = 16;
    private static final int BATTITO_ATTESA = 26;

    private static boolean attivo;
    private static boolean marcia;
    private static int preda = -1;
    private static Vec3 guida;
    private static int silenzio;
    /** 0..1: quanto e' forte la vignetta, segue lo stato senza scatti. */
    private static float presa;
    private static float presaPrima;
    private static float lampo;
    private static float pulsazione;
    private static int battito;

    private BerserkClient() {
    }

    /** Arriva dal server. */
    public static void ricevi(boolean attivoNuovo, boolean marciaNuova, int predaNuova, Vec3 guidaNuova, int lampoNuovo) {
        Minecraft mc = Minecraft.getInstance();
        if (attivoNuovo && !attivo && mc.player != null && mc.screen instanceof AbstractContainerScreen<?>) {
            // il simbionte non aspetta che il giocatore finisca di sistemare l'inventario
            mc.player.closeContainer();
        }
        if (!attivoNuovo && attivo && mc.player != null) {
            mc.player.setSprinting(false);
        }
        attivo = attivoNuovo;
        marcia = attivoNuovo && marciaNuova;
        preda = attivoNuovo ? predaNuova : -1;
        guida = attivoNuovo ? guidaNuova : null;
        silenzio = 0;
        BerserkSimbionte.attivoSulClient = attivoNuovo;
        if (lampoNuovo > 0) {
            lampo = 1.0F;
        }
    }

    private static boolean comanda() {
        return attivo && preda >= 0;
    }

    private static Entity preda(Minecraft mc) {
        if (mc.level == null || preda < 0) {
            return null;
        }
        Entity entita = mc.level.getEntity(preda);
        return entita != null && entita.isAlive() ? entita : null;
    }

    // ------------------------------------------------------------------ il corpo

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!comanda()) {
            return;
        }
        Input input = event.getInput();
        LocalPlayer giocatore = (LocalPlayer) event.getEntity();
        // correre sempre in avanti mentre la visuale gira piano e' la ricetta per girare in
        // tondo attorno al punto: quando il punto e' di lato si rallenta, quando e' alle spalle
        // ci si ferma e si gira sul posto, e si riparte appena lo si ha davanti
        float avanti = 0.0F;
        if (marcia) {
            Float scarto = scartoDiRotta(Minecraft.getInstance(), giocatore);
            avanti = scarto == null || scarto < GIRO_LIBERO ? 1.0F
                    : scarto > GIRO_FERMO ? 0.0F
                    : 1.0F - (scarto - GIRO_LIBERO) / (GIRO_FERMO - GIRO_LIBERO);
        }
        input.forwardImpulse = avanti;
        input.leftImpulse = 0.0F;
        input.up = avanti > 0.0F;
        input.down = false;
        input.left = false;
        input.right = false;
        input.shiftKeyDown = false;
        // salta da solo quello che ha davanti, come fa un mob
        input.jumping = avanti > 0.0F && giocatore.horizontalCollision && (giocatore.onGround() || giocatore.isInWater());
        if (avanti < 0.5F) {
            giocatore.setSprinting(false);
        }
    }

    /** Il punto verso cui il simbionte vuole andare: il nodo del percorso, o la preda. */
    private static Vec3 puntoVoluto(Minecraft mc) {
        if (marcia && guida != null) {
            return guida;
        }
        Entity bersaglio = preda(mc);
        return bersaglio == null ? null : bersaglio.position().add(0.0D, bersaglio.getBbHeight() * 0.55D, 0.0D);
    }

    /**
     * Di quanti gradi la visuale e' fuori rotta; null se il punto e' troppo vicino per avere
     * una direzione (ci si sta passando sopra: si tira dritto).
     */
    private static Float scartoDiRotta(Minecraft mc, LocalPlayer giocatore) {
        Vec3 punto = puntoVoluto(mc);
        if (punto == null) {
            return null;
        }
        double dx = punto.x - giocatore.getX();
        double dz = punto.z - giocatore.getZ();
        if (dx * dx + dz * dz < SOPRA_IL_PUNTO) {
            return null;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        return Math.abs(Mth.wrapDegrees(yaw - giocatore.getYRot()));
    }

    /** La visuale va dove vuole il simbionte: un poco a ogni fotogramma, cosi' non scatta. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || !comanda() || mc.player == null || mc.isPaused()) {
            return;
        }
        LocalPlayer giocatore = mc.player;
        Entity bersaglio = preda(mc);
        Vec3 centro = bersaglio == null ? null : bersaglio.position().add(0.0D, bersaglio.getBbHeight() * 0.55D, 0.0D);
        Vec3 verso = puntoVoluto(mc);
        if (verso == null) {
            return;
        }
        double dx = verso.x - giocatore.getX();
        double dz = verso.z - giocatore.getZ();
        if (dx * dx + dz * dz < SOPRA_IL_PUNTO) {
            // ci si sta passando sopra: la direzione verso il punto impazzisce, meglio tenerla
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch;
        if (centro != null && (guida == null || !marcia)) {
            double dy = centro.y - giocatore.getEyeY();
            pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0D / Math.PI));
        } else {
            // lungo il percorso guarda un po' in basso, dove mette i piedi
            pitch = 12.0F;
        }
        float k = Math.min(1.0F, PRESA_VISUALE * mc.getDeltaFrameTime());
        giocatore.setYRot(giocatore.getYRot() + Mth.wrapDegrees(yaw - giocatore.getYRot()) * k);
        giocatore.setXRot(Mth.clamp(giocatore.getXRot() + (pitch - giocatore.getXRot()) * k, -90.0F, 90.0F));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.isPaused()) {
            return;
        }
        presaPrima = presa;
        float voluta = attivo ? (comanda() ? 1.0F : 0.6F) : 0.0F;
        presa += (voluta - presa) * 0.15F;
        lampo = Math.max(0.0F, lampo - 0.06F);
        pulsazione *= 0.78F;
        if (!attivo) {
            return;
        }
        if (++silenzio > SCADENZA) {
            // il server non si fa piu' sentire: meglio ridare il corpo che tenerlo per sempre
            ricevi(false, false, -1, null, 0);
            return;
        }
        if (marcia) {
            Float scarto = scartoDiRotta(mc, mc.player);
            if (scarto == null || scarto < GIRO_LIBERO) {
                mc.player.setSprinting(true);
            }
        }
        if (--battito <= 0) {
            battito = comanda() ? BATTITO_CACCIA : BATTITO_ATTESA;
            pulsazione = 1.0F;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.9F, 0.55F));
        }
    }

    // ------------------------------------------------------------------ lo schermo

    /** La vignetta rossa: pulsa col battito, lampeggia alla presa e ai pasti. */
    public static final IGuiOverlay OVERLAY = (gui, grafica, partialTick, larghezza, altezza) -> {
        float intensita = Mth.lerp(partialTick, presaPrima, presa) * (0.42F + 0.22F * pulsazione) + lampo * 0.55F;
        if (intensita < 0.01F) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        grafica.setColor(0.62F, 0.02F, 0.04F, Math.min(0.92F, intensita));
        grafica.blit(VIGNETTA, 0, 0, larghezza, altezza, 0.0F, 0.0F, 256, 256, 256, 256);
        grafica.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    };
}
