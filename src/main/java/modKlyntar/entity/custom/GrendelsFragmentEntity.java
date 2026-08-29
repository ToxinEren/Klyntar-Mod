package modKlyntar.entity.custom;

import modKlyntar.symbiote.SymbioteState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Il simbionte che esce da un frammento di Knull.
 *
 * <p>Ha corpo e movenze del simbionte comune, ma non cerca un ospite: da' la caccia a chi un
 * simbionte ce l'ha gia', e solo a lui. Anti-Venom lo lascia in pace — o meglio, lui lascia in
 * pace Anti-Venom, che del suo veleno non sa che farsene.</p>
 */
public class GrendelsFragmentEntity extends SymbioteEntity {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarGrendel");

    /** entro quanto si accorge di una preda */
    private static final double RAGGIO_CACCIA = 16.0D;
    /** quanto fa male il contatto: come il simbionte comune */
    private static final float DANNO = 2.0F;

    public GrendelsFragmentEntity(EntityType<? extends Mob> tipo, Level livello) {
        super(tipo, livello);
    }

    /** Un bersaglio valido: porta un simbionte, e non e' Anti-Venom. */
    public static boolean ePreda(Player giocatore) {
        return !giocatore.isCreative() && !giocatore.isSpectator()
                && SymbioteState.haSimbionte(giocatore) && !SymbioteState.isAntiVenom(giocatore);
    }

    @Override
    protected boolean cercaOspite() {
        return false;   // non infetta nessuno: colpirlo non deve legarti a un simbionte
    }

    @Override
    protected boolean bersaglioValido(Player giocatore) {
        return ePreda(giocatore);   // il criterio opposto: solo chi e' gia' legato
    }

    @Override
    protected void registerGoals() {
        // niente goal del simbionte comune: quello cerca un ospite qualsiasi o un animale,
        // questo insegue solo chi gli interessa
        this.goalSelector.addGoal(1, new CacciaPortatoriGoal(this));
    }

    /**
     * Un colpo solo, poi sparisce dentro il giocatore lasciandogli il Knull's Bond.
     *
     * <p>Come il simbionte comune, che al contatto infetta e si dissolve: qui pero' l'ospite
     * un simbionte ce l'ha gia', e quello che gli resta e' il legame con Knull.</p>
     */
    @Override
    public boolean doHurtTarget(Entity bersaglio) {
        // il goal di caccia e la prossimita' in aiStep possono chiamarlo nello stesso
        // tick: senza questa guardia il frammento consegna due volte prima di sparire
        if (this.isRemoved()) {
            return false;
        }
        if (!(bersaglio instanceof Player giocatore) || !ePreda(giocatore)) {
            return false;
        }
        giocatore.hurt(this.damageSources().mobAttack(this), DANNO);

        if (giocatore instanceof ServerPlayer ospite) {
            SymbioteState.setScore(ospite, SymbioteState.KNULL_BOND_OBJECTIVE, 1);
            ospite.displayClientMessage(
                    Component.literal("Il frammento di Knull ti e' entrato dentro"), false);
            LOGGER.info("Knull's Bond consegnato a {}", ospite.getGameProfile().getName());
        }
        this.remove(RemovalReason.DISCARDED);
        return true;
    }

    /** Insegue il portatore di simbionte piu' vicino e lo colpisce a contatto. */
    private static class CacciaPortatoriGoal extends Goal {
        private final GrendelsFragmentEntity simbionte;
        private Player preda;

        CacciaPortatoriGoal(GrendelsFragmentEntity simbionte) {
            this.simbionte = simbionte;
        }

        private Player cerca() {
            Player vicino = null;
            double miglior = RAGGIO_CACCIA * RAGGIO_CACCIA;
            for (Player giocatore : this.simbionte.level().players()) {
                if (!ePreda(giocatore)) {
                    continue;
                }
                double distanza = giocatore.distanceToSqr(this.simbionte);
                if (distanza < miglior) {
                    miglior = distanza;
                    vicino = giocatore;
                }
            }
            return vicino;
        }

        @Override
        public boolean canUse() {
            this.preda = cerca();
            return this.preda != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.preda != null && this.preda.isAlive() && ePreda(this.preda)
                    && this.preda.distanceToSqr(this.simbionte) <= RAGGIO_CACCIA * RAGGIO_CACCIA;
        }

        @Override
        public void tick() {
            if (this.preda == null) {
                return;
            }
            this.simbionte.getNavigation().moveTo(this.preda, 1.2D);
            this.simbionte.getLookControl().setLookAt(this.preda);
            if (this.preda.distanceTo(this.simbionte) <= 1.5D) {
                this.simbionte.doHurtTarget(this.preda);
            }
        }
    }
}
