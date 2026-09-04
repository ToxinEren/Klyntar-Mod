package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * I tentacoli che il simbionte sul cadavere allunga verso chi si avvicina.
 *
 * <p>Stanno ritratti finche' nessuno e' nei paraggi; dentro {@link #PORTATA} escono, ondeggiano
 * e si protendono verso il giocatore piu' vicino, tanto piu' quanto lui e' vicino. Non toccano
 * nessuno e non fanno danno: e' un avvertimento, non un attacco.</p>
 *
 * <p>Tutto lato client e senza entita': il disegno avviene dentro il renderer della block
 * entity, che sa gia' dov'e' il blocco e ha una {@code poseStack} nel suo spazio locale. Il
 * giocatore piu' vicino lo si guarda direttamente, senza pacchetti, perche' il risultato e'
 * solo visivo e ognuno vede i tentacoli tendersi verso se stesso.</p>
 */
public final class TentacoliCadavere {

    /** Oltre questa distanza i tentacoli restano rientrati. */
    private static final double PORTATA = 9.0D;
    /** Sotto questa distanza sono tesi al massimo. */
    private static final double PORTATA_PIENA = 2.5D;
    /** Quanti ne escono: tanti e sottili, come nel simbionte di riferimento. */
    private static final int QUANTI = 15;
    /** Quanto puo' allungarsi un tentacolo mentre ondeggia a vuoto. */
    private static final double LUNGHEZZA = 2.4D;
    /** Fin dove puo' arrivare quando ha preso qualcuno: deve seguirlo mentre lo alza. */
    private static final double ALLUNGO_MASSIMO = 7.0D;
    /** Il raggio alla radice e in punta: molto piu' fini dei bracci della tentacles_traversal. */
    private static final float RAGGIO_BASE = 0.045F;
    private static final float RAGGIO_PUNTA = 0.008F;
    /** Quanti tratti per filamento: piu' ce ne sono, piu' la curva e' morbida. */
    private static final int SEGMENTI = 26;
    /** Quanto ci mette a uscire o a rientrare: piu' alto, piu' lento. */
    private static final double INERZIA = 0.08D;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/venom_tentacle_segment.png");

    /**
     * Da dove esce ogni filamento, in blocchi rispetto all'angolo del blocco d'ancoraggio.
     *
     * <p>Distribuiti lungo il corpo invece che elencati a mano: il cadavere e' steso lungo Z
     * per un paio di blocchi, e cosi' i filamenti nascono su tutta la sua lunghezza, alternati
     * ai due lati del busto.</p>
     */
    private static Vec3 radice(int i) {
        double q = (i + 0.5D) / QUANTI;                 // 0 alla testa, 1 ai piedi
        double lato = ((i % 3) - 1) * 0.42D;            // sinistra, centro, destra
        double serpeggio = Math.sin(i * 2.4D) * 0.14D;
        return new Vec3(0.1D + lato + serpeggio, 0.26D + (i % 2) * 0.10D, -0.35D + q * 2.3D);
    }

    /**
     * Quanto sono usciti i tentacoli, per non farli scattare di colpo.
     *
     * <p>Uno per blocco e non uno solo: nel mondo il cadavere e' unico, ma il comando di prova
     * ne posa altri, e con un contatore condiviso due cadaveri vicini si contenderebbero lo
     * stesso valore facendo pulsare i tentacoli dell'uno al ritmo dell'altro.</p>
     */
    private static final java.util.Map<BlockPos, Float> USCITA =
            new java.util.concurrent.ConcurrentHashMap<>();

    private TentacoliCadavere() {
    }

    /**
     * Disegna i tentacoli del cadavere.
     *
     * @param pila deve essere nello spazio locale del blocco, com'e' dentro un renderer di
     *             block entity prima di qualunque traslazione
     */
    public static void disegna(Level livello, BlockPos pos, float parziale,
                               PoseStack pila, MultiBufferSource buffer) {
        Player giocatore = Minecraft.getInstance().player;
        if (giocatore == null) {
            return;
        }
        Vec3 occhi = giocatore.getEyePosition(parziale);
        Vec3 centro = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 1.0D);
        double distanza = occhi.distanceTo(centro);

        // Se il rituale e' in corso i filamenti stringono al massimo, senza dipendere dalla
        // distanza. Il segnale non costa un pacchetto: durante la presa il server toglie la
        // gravita' al giocatore, e quel flag arriva al client da solo.
        boolean rituale = giocatore.isNoGravity() && distanza < 14.0D;

        float bersaglio = rituale ? 1.0F
                : (distanza >= PORTATA ? 0.0F
                : (float) Math.min(1.0D, (PORTATA - distanza) / (PORTATA - PORTATA_PIENA)));
        float uscita = USCITA.getOrDefault(pos, 0.0F);
        uscita += (bersaglio - uscita) * INERZIA;
        USCITA.put(pos.immutable(), uscita);
        if (uscita < 0.02F) {
            return;
        }

        long tempo = livello.getGameTime();
        double fase = tempo * 0.12D + parziale * 0.12D;
        // Il riferimento e' il CENTRO del blocco, non il suo angolo: GeckoLib, prima di
        // chiamare postRender, ha gia' spostato la pila di mezzo blocco in X e in Z.
        Vec3 angolo = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        Vec3 piedi = giocatore.getPosition(parziale).subtract(angolo);
        double altezza = giocatore.getBbHeight();

        // quanto stringono: sotto meta' ondeggiano soltanto, sopra vanno a prenderlo
        double presa = rituale ? 1.0D : Math.max(0.0D, (uscita - 0.45D) / 0.55D);
        presa = presa * presa * (3.0D - 2.0D * presa);

        for (int i = 0; i < QUANTI; i++) {
            Vec3 radice = radice(i);

            // dove lo afferra questo filamento: punti sparsi su tutta la sua figura,
            // non tutti in faccia, cosi' sembra che lo avvolgano invece di puntarlo
            double quota = 0.15D + 0.75D * ((i * 7 % QUANTI) / (double) QUANTI);
            double giroX = Math.cos(i * 2.39D) * 0.32D;
            double giroZ = Math.sin(i * 2.39D) * 0.32D;
            Vec3 appiglio = piedi.add(giroX, altezza * quota, giroZ);

            Vec3 versoLui = appiglio.subtract(radice);
            if (versoLui.lengthSqr() < 1.0E-4D) {
                continue;
            }

            // a riposo: ondeggia verso l'alto senza arrivare da nessuna parte
            double oscilla = Math.sin(fase + i * 1.3D);
            double oscilla2 = Math.cos(fase * 0.7D + i * 2.1D);
            Vec3 riposo = new Vec3(oscilla * 0.5D, 1.0D, oscilla2 * 0.5D).normalize();
            double lungoRiposo = LUNGHEZZA * uscita * (0.55D + 0.45D * Math.sin(fase * 1.4D + i * 0.9D));
            Vec3 puntaRiposo = radice.add(riposo.scale(lungoRiposo));

            // stretto: la punta sta addosso a lui, e lo segue mentre viene sollevato
            Vec3 puntaPresa = appiglio;
            if (versoLui.length() > ALLUNGO_MASSIMO) {
                puntaPresa = radice.add(versoLui.normalize().scale(ALLUNGO_MASSIMO));
            }

            Vec3 punta = puntaRiposo.add(puntaPresa.subtract(puntaRiposo).scale(presa));
            // piu' stringe, meno serpeggia: un filamento teso non ondeggia
            double ampiezza = (0.10D + 0.16D * uscita) * (1.0D - 0.75D * presa);
            VenomTentaclesTraversalRenderer.disegnaFilamento(radice, punta, i, TEXTURE,
                    RAGGIO_BASE, RAGGIO_PUNTA, SEGMENTI, ampiezza, pila, buffer, tempo,
                    VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE, VenomTentaclesTraversalRenderer.NERO_SIMBIONTE);
        }
    }
}
