package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import modKlyntar.MyMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Symbiote Punch: il braccio diventa un tentacolo spesso che parte dal polsino del simbionte
 * (il layer {@code symbiote/symbiotepunch}), finisce in un pugno chiuso con nocche e artigli,
 * colpisce e rientra. Lo manda il server ({@code PugnoPacket}, da {@code PugnoTentacolo}) con il
 * bersaglio, o con il punto dove arriva se va a vuoto; la lunghezza e' quella che serve, fino alla
 * portata del server. Attorno al braccio si avvolgono tre filamenti sottili. I tubi sono quelli
 * dei bracci della tentacles_traversal e delle frustate
 * ({@link VenomTentaclesTraversalRenderer#disegnaFilamento}); il pugno e' fatto qui, a scatole.
 *
 * <p>Il pugno arriva al bersaglio in {@code USCITA * DURATA} tick, gli stessi che aspetta il server
 * prima di dare il colpo; poi resta teso un attimo e rientra. Si ferma sulla superficie del
 * bersaglio, non al suo centro.</p>
 *
 * <p>Braccio e tentacolo vanno insieme (11/10/2026). Nei poteri la spinta ({@code venompunch2})
 * si completa al tick 7 e resta fino al 16; il tentacolo parte all'8 e in {@code DURATA} tick e'
 * gia' rientrato, quindi esce e torna sempre da un braccio fermo in avanti. La radice e' la punta
 * del polsino letta mentre lo si disegna ({@link ManoDelPugno}): in terza persona dal corpo, in
 * prima dal braccio davanti alla camera. In prima persona, senza una lettura, il tentacolo quel
 * fotogramma non si disegna: una stima dalla camera lo faceva comparire sopra il polsino.</p>
 *
 * <p>Fino al 10/10/2026 c'era anche un braccio allungato di lunghezza fissa (un modello GeckoLib):
 * copriva il tentacolo, non arrivava mai al bersaglio e in prima persona metteva un pezzo piatto
 * davanti alla camera.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class TentacoloPugno {
    /** Tre tick per uscire (l'ARRIVO di PugnoTentacolo), uno fermo sul bersaglio, tre per rientrare. */
    private static final float DURATA = 7.0F;
    private static final double USCITA = 3.0D / 7.0D;
    private static final double TESA = 4.0D / 7.0D;
    private static final double VISTA = 64.0D;
    private static final int MASSIMI = 32;
    /** Lo spessore del braccio-tentacolo alla radice e al polso del pugno. */
    private static final float RAGGIO_BRACCIO = 0.15F;
    private static final float RAGGIO_POLSO = 0.1F;
    /**
     * In prima persona: la radice sta dietro al polsino (la mano si disegna sopra al mondo) e il
     * tentacolo ne esce largo quasi quanto lui, cosi' non si vede uno scalino.
     */
    private static final float RAGGIO_BRACCIO_VICINO = 0.13F;
    /**
     * La pelle del braccio: gli stessi toni del polsino (pelle_pugno.py della skill nuovo-simbionte),
     * non quella dei tentacoli della traversal, che in Riot era molto piu' scura del polsino.
     */
    private static final ResourceLocation PELLE_VENOM =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/punch_venom.png");
    private static final ResourceLocation PELLE_RIOT =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/punch_riot.png");
    /** I filamenti avvolti e gli artigli: il tono scuro del polsino (16 su 22), per fare rilievo. */
    private static final int TINTA_FILAMENTI = 185;
    /** Mezze misure del pugno: lungo il colpo, di lato, in alto. */
    private static final double PUGNO_LUNGO = 0.15D;
    private static final double PUGNO_LARGO = 0.17D;
    private static final double PUGNO_ALTO = 0.14D;

    private record Pugno(int da, int bersaglio, Vec3 punto, String forma, long inizio, int seme) {
    }

    /** I due punti del polsino letti da ManoDelPugno: la punta, e dove il tentacolo comincia dentro. */
    private record Mano(Vector3f punta, Vector3f dentro) {
    }

    private static final List<Pugno> ATTIVI = new ArrayList<>();
    /** Le mani lette da ManoDelPugno in questo fotogramma, nello spazio della camera. */
    private static final Map<Integer, Mano> MANI = new HashMap<>();
    /**
     * La mano in prima persona, nello spazio della mano. Il braccio si disegna dopo il mondo, quindi
     * qui c'e' quella del fotogramma prima: si aggiorna a ogni fotogramma in cui il braccio si
     * disegna (anche prima che il tentacolo parta) e si toglie a pugno finito.
     */
    private static final Map<Integer, Mano> MANI_PRIMA = new HashMap<>();
    /** Il campo visivo con cui Minecraft disegna la mano in prima persona, sempre lo stesso. */
    private static final double CAMPO_MANO = 70.0D;
    private static int semi;

    private TentacoloPugno() {
    }

    /** Arriva dal server: un tentacolo nuovo. */
    public static void aggiungi(int da, int bersaglio, double x, double y, double z, String forma) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (ATTIVI.size() >= MASSIMI) {
            ATTIVI.remove(0);
        }
        semi += 11;
        ATTIVI.add(new Pugno(da, bersaglio, new Vec3(x, y, z), forma, minecraft.level.getGameTime(), semi));
    }

    /** Per ManoDelPugno: questo giocatore ha un pugno in corso, e serve sapere dov'e' la sua mano. */
    public static boolean daSeguire(int da) {
        for (Pugno p : ATTIVI) {
            if (p.da() == da) {
                return true;
            }
        }
        return false;
    }

    /** Da ManoDelPugno: il polsino, nello spazio della camera, mentre il corpo si disegna. */
    public static void manoVista(int da, Vector3f punta, Vector3f dentro) {
        MANI.put(da, new Mano(punta, dentro));
    }

    /** Da ManoDelPugno: il polsino in prima persona, nello spazio della mano. */
    public static void manoPrimaPersona(int da, Vector3f punta, Vector3f dentro) {
        MANI_PRIMA.put(da, new Mano(punta, dentro));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evento) {
        // le mani valgono per il fotogramma in cui sono state lette: quelle di un giocatore
        // disegnato fuori dal mondo (l'inventario) non devono arrivare al fotogramma dopo
        if (evento.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            MANI.clear();
            return;
        }
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ATTIVI.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ATTIVI.clear();
            return;
        }
        float parziale = evento.getPartialTick();
        double ora = minecraft.level.getGameTime() + parziale;
        Camera camera = evento.getCamera();
        Vec3 occhio = camera.getPosition();
        PoseStack pila = evento.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Set<ResourceLocation> usate = new HashSet<>();
        // dallo spazio della camera al mondo: la pila qui ha solo la rotazione della camera,
        // la stessa con cui sono stati disegnati i corpi
        Matrix4f dallaCamera = new Matrix4f(pila.last().pose()).invert();

        pila.pushPose();
        pila.translate(-occhio.x, -occhio.y, -occhio.z);
        Iterator<Pugno> giro = ATTIVI.iterator();
        while (giro.hasNext()) {
            Pugno p = giro.next();
            double t = (ora - p.inizio()) / DURATA;
            Entity da = minecraft.level.getEntity(p.da());
            if (t >= 1.0D || t < -1.0D || da == null) {
                giro.remove();
                if (!daSeguire(p.da())) {
                    MANI_PRIMA.remove(p.da());
                }
                continue;
            }
            if (t < 0.0D || da.position().distanceToSqr(occhio) > VISTA * VISTA) {
                continue;
            }
            boolean vicino = da == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
            Mano letta = vicino ? MANI_PRIMA.get(p.da()) : MANI.get(p.da());
            Vec3 radice;
            Vec3 dentro = null;
            if (letta != null) {
                radice = nelMondo(letta.punta(), vicino, dallaCamera, occhio, minecraft);
                dentro = nelMondo(letta.dentro(), vicino, dallaCamera, occhio, minecraft);
            } else if (vicino) {
                continue;
            } else {
                radice = mano(da, parziale);
            }
            Entity bersaglio = p.bersaglio() >= 0 ? minecraft.level.getEntity(p.bersaglio()) : null;
            Vec3 arrivo = p.punto();
            if (bersaglio != null) {
                // il pugno si ferma sulla superficie del bersaglio, non dentro
                Vec3 centro = bersaglio.getPosition(parziale).add(0.0D, bersaglio.getBbHeight() * 0.6D, 0.0D);
                Vec3 verso = centro.subtract(radice);
                double ferma = bersaglio.getBbWidth() * 0.5D + PUGNO_LUNGO;
                arrivo = verso.length() > ferma * 2.0D ? centro.subtract(verso.normalize().scale(ferma)) : centro;
            }
            // la luce della mano, la stessa con cui Minecraft disegna il polsino
            int luce = minecraft.getEntityRenderDispatcher().getPackedLightCoords(da, parziale);
            disegna(p, dentro, radice, arrivo, Math.max(0.0D, t), (long) ora, vicino, luce, pila, buffer);
            usate.add(pelle(p.forma()));
        }
        for (ResourceLocation texture : usate) {
            buffer.endBatch(RenderType.entityCutoutNoCull(texture));
        }
        pila.popPose();
        MANI.clear();
    }

    /**
     * Un punto del polsino, dallo spazio in cui e' stato letto al mondo. In prima persona la mano si
     * disegna col campo visivo fisso di 70 gradi, il mondo con quello delle opzioni: perche' il
     * punto cada sullo schermo dove si vede il polsino, alla stessa profondita' si allarga di lato
     * quanto i due campi visivi sono diversi.
     */
    private static Vec3 nelMondo(Vector3f punto, boolean vicino, Matrix4f dallaCamera, Vec3 occhio,
                                 Minecraft minecraft) {
        Vector4f v = new Vector4f(punto, 1.0F);
        if (vicino) {
            double mondo = minecraft.options.fov().get();
            float k = (float) (Math.tan(Math.toRadians(mondo) / 2.0D) / Math.tan(Math.toRadians(CAMPO_MANO) / 2.0D));
            v.x *= k;
            v.y *= k;
        }
        v.mul(dallaCamera);
        return occhio.add(v.x(), v.y(), v.z());
    }

    private static ResourceLocation pelle(String forma) {
        return "riot".equals(forma) ? PELLE_RIOT : PELLE_VENOM;
    }

    /**
     * @param dentro dove il tentacolo comincia dentro il polsino (null se il polsino non e' stato
     *               letto): da li' alla radice, la punta del polsino, corre dritto lungo il braccio
     */
    private static void disegna(Pugno p, Vec3 dentro, Vec3 radice, Vec3 arrivo, double t, long tempo,
                                boolean vicino, int luce, PoseStack pila, MultiBufferSource buffer) {
        // quanto e' fuori: esce di scatto, resta teso, rientra
        double fuori;
        if (t < USCITA) {
            double q = 1.0D - t / USCITA;
            fuori = 1.0D - q * q * q;
        } else if (t < TESA) {
            fuori = 1.0D;
        } else {
            fuori = 1.0D - (t - TESA) / (1.0D - TESA);
        }
        Vec3 corsa = arrivo.subtract(radice);
        if (fuori < 0.02D || corsa.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 avanti = corsa.normalize();
        // il pugno cresce con la strada fatta, non col tempo: nasce piccolo dentro il polsino, e' grande
        // dopo un blocco e torna a sciogliersi nel polsino rientrando. In prima persona un pugno gia'
        // grande a mezzo blocco dalla camera copriva il polsino come un blocco grigio
        double grandezza = Mth.clamp((corsa.length() * fuori - 0.3D) / 0.9D, 0.0D, 1.0D);
        Vec3 pugno = radice.add(corsa.scale(fuori));
        Vec3 polso = pugno.subtract(avanti.scale(PUGNO_LUNGO * grandezza * 0.8D));
        ResourceLocation texture = pelle(p.forma());
        // la pelle ha gia' i colori del polsino: il braccio non si tinge, i filamenti si scuriscono
        int tubo = 255;
        int scuro = TINTA_FILAMENTI;
        // in volo serpeggia appena, teso e' dritto
        double ampiezza = fuori >= 1.0D ? 0.012D : 0.05D * (1.0D - fuori) + 0.015D;
        float raggio = vicino ? RAGGIO_BRACCIO_VICINO : RAGGIO_BRACCIO;
        // il tratto dentro il polsino: dritto lungo il braccio, sovrapposto al polsino, cosi' fra i
        // due non resta una fessura qualunque sia la direzione del colpo
        if (dentro != null) {
            VenomTentaclesTraversalRenderer.disegnaFilamento(dentro, radice, p.seme(), texture,
                    raggio, raggio, 3, 0.0D, pila, buffer, tempo, tubo, tubo, tubo, luce);
        }
        VenomTentaclesTraversalRenderer.disegnaFilamento(radice, polso, p.seme(), texture,
                raggio, RAGGIO_POLSO, 26, ampiezza, pila, buffer, tempo, tubo, tubo, tubo, luce);
        // i filamenti che gli si avvolgono attorno, piu' liberi: escono anche loro da dentro il polsino
        Vec3 partenza = dentro != null ? dentro : radice;
        for (int i = 1; i <= 3; i++) {
            VenomTentaclesTraversalRenderer.disegnaFilamento(partenza, polso, p.seme() + i * 5, texture,
                    0.045F, 0.012F, 24, ampiezza + 0.13D, pila, buffer, tempo, scuro, scuro, scuro, luce);
        }
        // il pugno prende la luce del posto dove arriva
        Minecraft minecraft = Minecraft.getInstance();
        int lucePugno = minecraft.level != null
                ? LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(pugno)) : luce;
        disegnaPugno(pugno, avanti, grandezza, p.seme(), texture, tubo, scuro, lucePugno, tempo, pila, buffer);
    }

    /**
     * Il pugno chiuso: il blocco della mano, quattro nocche sul davanti, il pollice di lato e
     * quattro artigli corti che escono dalle nocche, un poco piegati in giu'.
     */
    private static void disegnaPugno(Vec3 centro, Vec3 avanti, double g, int seme, ResourceLocation texture,
                                     int tinta, int scuro, int luce, long tempo, PoseStack pila,
                                     MultiBufferSource buffer) {
        Vec3 aiuto = Math.abs(avanti.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 destra = avanti.cross(aiuto).normalize();
        Vec3 su = destra.cross(avanti).normalize();
        VertexConsumer consumatore = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrice = pila.last().pose();
        Matrix3f normali = pila.last().normal();

        scatola(consumatore, matrice, normali, centro, avanti, destra, su,
                PUGNO_LUNGO * g, PUGNO_LARGO * g, PUGNO_ALTO * g, tinta, luce);
        // il pollice, sul lato verso il corpo, chiuso sotto le dita
        Vec3 pollice = centro.add(destra.scale(-PUGNO_LARGO * 0.9D * g)).add(su.scale(-PUGNO_ALTO * 0.35D * g))
                .add(avanti.scale(PUGNO_LUNGO * 0.25D * g));
        scatola(consumatore, matrice, normali, pollice, avanti, destra, su,
                PUGNO_LUNGO * 0.55D * g, 0.045D * g, 0.05D * g, tinta, luce);
        for (int k = 0; k < 4; k++) {
            double x = (-0.75D + k * 0.5D) * PUGNO_LARGO * g;
            Vec3 nocca = centro.add(avanti.scale(PUGNO_LUNGO * g)).add(destra.scale(x)).add(su.scale(PUGNO_ALTO * 0.4D * g));
            scatola(consumatore, matrice, normali, nocca, avanti, destra, su, 0.035D * g, 0.038D * g, 0.04D * g,
                    tinta, luce);
            if (g > 0.6D) {
                Vec3 punta = nocca.add(avanti.scale(0.13D * g)).add(su.scale(-0.06D * g));
                VenomTentaclesTraversalRenderer.disegnaFilamento(nocca, punta, seme + 31 + k, texture,
                        0.026F, 0.004F, 3, 0.0D, pila, buffer, tempo, scuro, scuro, scuro, luce);
            }
        }
    }

    /** Una scatola orientata, mezze misure lungo i tre assi. */
    private static void scatola(VertexConsumer c, Matrix4f m, Matrix3f n, Vec3 centro, Vec3 avanti, Vec3 destra,
                                Vec3 su, double lungo, double largo, double alto, int tinta, int luce) {
        Vec3 f = avanti.scale(lungo);
        Vec3 r = destra.scale(largo);
        Vec3 u = su.scale(alto);
        Vec3[] v = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            v[i] = centro.add(f.scale((i & 1) == 0 ? -1 : 1)).add(r.scale((i & 2) == 0 ? -1 : 1))
                    .add(u.scale((i & 4) == 0 ? -1 : 1));
        }
        faccia(c, m, n, v[1], v[3], v[7], v[5], avanti, tinta, luce);                // davanti
        faccia(c, m, n, v[2], v[0], v[4], v[6], avanti.scale(-1.0D), tinta, luce);   // dietro
        faccia(c, m, n, v[3], v[2], v[6], v[7], destra, tinta, luce);
        faccia(c, m, n, v[0], v[1], v[5], v[4], destra.scale(-1.0D), tinta, luce);
        faccia(c, m, n, v[4], v[5], v[7], v[6], su, tinta, luce);
        faccia(c, m, n, v[2], v[3], v[1], v[0], su.scale(-1.0D), tinta, luce);
    }

    private static void faccia(VertexConsumer c, Matrix4f m, Matrix3f n, Vec3 a, Vec3 b, Vec3 cc, Vec3 d,
                               Vec3 normale, int tinta, int luce) {
        float nx = (float) normale.x, ny = (float) normale.y, nz = (float) normale.z;
        c.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(tinta, tinta, tinta, 255).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luce).normal(n, nx, ny, nz).endVertex();
        c.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(tinta, tinta, tinta, 255).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luce).normal(n, nx, ny, nz).endVertex();
        c.vertex(m, (float) cc.x, (float) cc.y, (float) cc.z).color(tinta, tinta, tinta, 255).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luce).normal(n, nx, ny, nz).endVertex();
        c.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(tinta, tinta, tinta, 255).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luce).normal(n, nx, ny, nz).endVertex();
    }

    /**
     * La mano destra stimata davanti alla spalla, quando il corpo non e' stato disegnato in questo
     * fotogramma e ManoDelPugno non l'ha letta (per esempio fuori dall'inquadratura).
     */
    private static Vec3 mano(Entity da, float parziale) {
        float imbardata = da instanceof Player giocatore
                ? Mth.lerp(parziale, giocatore.yBodyRotO, giocatore.yBodyRot)
                : da.getViewYRot(parziale);
        double r = Math.toRadians(imbardata);
        Vec3 avanti = new Vec3(-Math.sin(r), 0.0D, Math.cos(r));
        Vec3 destra = new Vec3(-Math.cos(r), 0.0D, -Math.sin(r));
        double altezza = da.getBbHeight() * (da.isCrouching() ? 0.62D : 0.75D);
        return da.getPosition(parziale).add(0.0D, altezza, 0.0D).add(destra.scale(0.36D)).add(avanti.scale(0.5D));
    }
}
