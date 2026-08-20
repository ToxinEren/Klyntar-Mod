package modKlyntar.symbiote;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Chi viene davvero toccato dal modello, invece di chiunque stia nel raggio.
 *
 * <p>Le braccia e i tendini del simbionte partono dal petto e si allungano in avanti: qui
 * quella portata diventa un segmento nello spazio, e prende solo chi ci finisce contro.</p>
 *
 * <p>E' una ricostruzione geometrica, non la posizione vera delle ossa: quelle le calcola
 * GeckoLib sul client, mentre il danno si assegna sul server. La forma del volume ricalca
 * pero' l'allungo reale delle animazioni, quindi in pratica un mob alle spalle o troppo in
 * alto non viene piu' colpito.</p>
 */
public final class ColpoLocalizzato {
    /** ogni quanti blocchi si campiona il segmento del braccio */
    private static final double PASSO = 0.35D;
    /** altezza della spalla rispetto ai piedi, da cui parte l'allungo */
    private static final double ALTEZZA_SPALLA = 1.35D;

    private ColpoLocalizzato() {
    }

    /**
     * Il bersaglio e' dentro l'allungo del modello?
     *
     * @param portata  quanto lontano arriva la parte che colpisce, in blocchi
     * @param spessore quanto e' grossa quella parte: tendine sottile o pugno pieno
     */
    public static boolean toccato(Player attaccante, LivingEntity bersaglio, double portata, double spessore) {
        AABB corpo = bersaglio.getBoundingBox().inflate(spessore);
        Vec3 spalla = attaccante.position().add(0.0D, ALTEZZA_SPALLA, 0.0D);
        Vec3 direzione = attaccante.getLookAngle();

        for (double d = 0.0D; d <= portata; d += PASSO) {
            if (corpo.contains(spalla.add(direzione.scale(d)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Il bersaglio e' raggiunto da un tendine teso verso di lui?
     *
     * <p>I tendini non vanno dritti davanti come il pugno: si piegano verso ogni bersaglio.
     * Basta quindi che il bersaglio sia entro la lunghezza del tendine e non oltre un muro.</p>
     */
    public static boolean raggiuntoDaTendine(Player attaccante, LivingEntity bersaglio, double lunghezza) {
        Vec3 spalla = attaccante.position().add(0.0D, ALTEZZA_SPALLA, 0.0D);
        Vec3 punto = bersaglio.getBoundingBox().getCenter();
        return spalla.distanceToSqr(punto) <= lunghezza * lunghezza && attaccante.hasLineOfSight(bersaglio);
    }

    /** Tiene della lista solo chi il modello sta toccando davvero. */
    public static List<LivingEntity> soloToccati(Player attaccante, Collection<LivingEntity> candidati,
                                                 double portata, double spessore) {
        List<LivingEntity> toccati = new ArrayList<>(candidati.size());
        for (LivingEntity candidato : candidati) {
            if (candidato.isAlive() && toccato(attaccante, candidato, portata, spessore)) {
                toccati.add(candidato);
            }
        }
        return toccati;
    }

    /** Tiene della lista solo chi un tendine arriva a prendere. */
    public static List<LivingEntity> soloRaggiunti(Player attaccante, Collection<LivingEntity> candidati,
                                                   double lunghezza) {
        List<LivingEntity> raggiunti = new ArrayList<>(candidati.size());
        for (LivingEntity candidato : candidati) {
            if (candidato.isAlive() && raggiuntoDaTendine(attaccante, candidato, lunghezza)) {
                raggiunti.add(candidato);
            }
        }
        return raggiunti;
    }
}
