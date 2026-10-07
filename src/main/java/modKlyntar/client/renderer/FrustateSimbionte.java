package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Le frustate del duello fra simbionti: un tentacolo che parte, colpisce e rientra.
 *
 * <p>Come le "whip" della mod Symbiote, ma senza un'entita' per ogni frustata: il server manda
 * chi frusta, chi viene frustato e la forma ({@code FrustataPacket}), e qui si disegna per poco
 * piu' di mezzo secondo. Il filamento e' lo stesso dei bracci della tentacles_traversal e della
 * presa del mob ({@link VenomTentaclesTraversalRenderer#disegnaFilamento}), con la pelle e la
 * tinta della forma: nero per Venom, grigio per Riot.</p>
 *
 * <p>Parte dal petto di chi frusta (dal corpo, se e' un mob) e arriva al petto del bersaglio.
 * Esce in fretta, resta teso un attimo e rientra piu' piano; finche' e' in volo serpeggia,
 * teso no.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class FrustateSimbionte {
    private static final float DURATA = 12.0F;
    private static final double USCITA = 0.35D;
    private static final double TESA = 0.6D;
    private static final int FILI = 2;
    private static final float RAGGIO = 0.05F;
    private static final float PUNTA = 0.008F;
    private static final int SEGMENTI = 18;
    private static final double VISTA = 48.0D;
    private static final int MASSIME = 64;

    private record Frustata(int da, int a, String forma, long inizio, int seme) {
    }

    private static final List<Frustata> ATTIVE = new ArrayList<>();
    private static int semi;

    private FrustateSimbionte() {
    }

    /** Arriva dal server: una frustata nuova. */
    public static void aggiungi(int da, int a, String forma) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (ATTIVE.size() >= MASSIME) {
            ATTIVE.remove(0);
        }
        semi += 7;
        ATTIVE.add(new Frustata(da, a, forma, minecraft.level.getGameTime(), semi));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ATTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ATTIVE.clear();
            return;
        }
        float parziale = evento.getPartialTick();
        double ora = minecraft.level.getGameTime() + parziale;
        Vec3 camera = evento.getCamera().getPosition();
        PoseStack pila = evento.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Set<ResourceLocation> usate = new HashSet<>();

        pila.pushPose();
        pila.translate(-camera.x, -camera.y, -camera.z);
        Iterator<Frustata> giro = ATTIVE.iterator();
        while (giro.hasNext()) {
            Frustata f = giro.next();
            double t = (ora - f.inizio()) / DURATA;
            Entity da = minecraft.level.getEntity(f.da());
            Entity a = minecraft.level.getEntity(f.a());
            if (t >= 1.0D || t < -1.0D || da == null || a == null) {
                giro.remove();
                continue;
            }
            if (t < 0.0D || da.position().distanceToSqr(camera) > VISTA * VISTA) {
                continue;
            }
            disegna(f, da, a, Math.max(0.0D, t), parziale, (long) ora, pila, buffer);
            usate.add(VenomTentaclesTraversalRenderer.textureDellaForma(f.forma()));
        }
        for (ResourceLocation texture : usate) {
            buffer.endBatch(RenderType.entityCutoutNoCull(texture));
        }
        pila.popPose();
    }

    private static void disegna(Frustata f, Entity da, Entity a, double t, float parziale, long tempo,
                                PoseStack pila, MultiBufferSource buffer) {
        // quanto e' fuori: esce in fretta, resta teso, rientra
        double fuori;
        if (t < USCITA) {
            double q = 1.0D - t / USCITA;
            fuori = 1.0D - q * q;
        } else if (t < TESA) {
            fuori = 1.0D;
        } else {
            fuori = 1.0D - (t - TESA) / (1.0D - TESA);
        }
        if (fuori < 0.02D) {
            return;
        }
        Vec3 bersaglio = a.getPosition(parziale).add(0.0D, a.getBbHeight() * 0.55D, 0.0D);
        Vec3 radice = radice(da, bersaglio, parziale);
        Vec3 verso = bersaglio.subtract(radice);
        Vec3 lato = new Vec3(-verso.z, 0.0D, verso.x);
        lato = lato.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : lato.normalize();
        ResourceLocation texture = VenomTentaclesTraversalRenderer.textureDellaForma(f.forma());
        int tinta = VenomTentaclesTraversalRenderer.tintaFilamentiDi(f.forma());
        for (int i = 0; i < FILI; i++) {
            // due filamenti un po' discosti, che colpiscono in due punti del petto
            double scarto = (i - (FILI - 1) / 2.0D) * 0.22D;
            Vec3 punto = bersaglio.add(lato.scale(scarto)).add(0.0D, scarto * 0.4D, 0.0D);
            Vec3 punta = radice.add(punto.subtract(radice).scale(fuori));
            double ampiezza = 0.05D + 0.25D * (1.0D - fuori);
            VenomTentaclesTraversalRenderer.disegnaFilamento(radice, punta, f.seme() + i, texture,
                    RAGGIO, PUNTA, SEGMENTI, ampiezza, pila, buffer, tempo, tinta, tinta, tinta);
        }
    }

    /** Da dove parte: il petto di un giocatore, verso il bersaglio; il corpo di un mob. */
    private static Vec3 radice(Entity da, Vec3 bersaglio, float parziale) {
        Vec3 piedi = da.getPosition(parziale);
        Vec3 centro = da instanceof Player
                ? piedi.add(0.0D, da.getBbHeight() * 0.62D, 0.0D)
                : piedi.add(0.0D, 0.25D, 0.0D);
        Vec3 verso = bersaglio.subtract(centro).multiply(1.0D, 0.0D, 1.0D);
        if (verso.lengthSqr() < 1.0E-4D) {
            return centro;
        }
        return centro.add(verso.normalize().scale(da instanceof Player ? 0.3D : 0.2D));
    }
}
