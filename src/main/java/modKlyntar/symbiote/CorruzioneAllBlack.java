package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

/**
 * La corruzione di All-Black: la sua affinita', che sale uccidendo.
 *
 * <p>Dal documento di design. Una barra da 0 a 100 sale a ogni uccisione: +1 per un mob normale,
 * +2 per un mostro, +15 per un boss (+20 il drago dell'End), +10 per un giocatore in PvP. Non
 * scende finche' il giocatore resta attivo: dopo circa 20 giorni di gioco senza uccidere comincia a
 * calare, uno stadio al giorno.</p>
 *
 * <table>
 *   <tr><th>Stadio</th><th>Soglia</th><th>Cosa sblocca</th><th>Prezzo</th></tr>
 *   <tr><td>0 Simbiosi instabile</td><td>0-20</td><td>passive, Necrosword</td><td>sussurri che spingono a combattere</td></tr>
 *   <tr><td>1 Risveglio</td><td>20-40</td><td>onda d'ombra, piccolo bonus danno</td><td>venature nere visibili</td></tr>
 *   <tr><td>2 Sussurro del Necrosword</td><td>40-65</td><td>corruzione del bersaglio, volo</td><td>sotto 40 si spengono</td></tr>
 *   <tr><td>3 Servo di Knull</td><td>65-90</td><td>fusione totale, bonus danno alto</td><td>senza uccidere, l'arma colpisce chi e' vicino</td></tr>
 *   <tr><td>4 Avatar del Necrosword</td><td>90-100</td><td>tutto, cooldown ridotti, niente knockback</td><td>fame piu' rapida, rischio di berserk</td></tr>
 * </table>
 *
 * <p>Gli sblocchi stanno nei JSON del potere, che leggono {@link #STADIO}. Le debolezze (fuoco e
 * suono) crescono con la corruzione, e il simbionte si stacca dall'ospite solo se le prende
 * insieme.</p>
 *
 * <p><b>Segnalibro.</b> All-Black non e' ancora nella mod: tutto questo dorme finche' nessuno
 * indossa la forma {@code allblack}.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class CorruzioneAllBlack {
    public static final String FORMA = "allblack";
    public static final String CORRUZIONE = "Klyntar.Corruption";
    public static final String STADIO = "Klyntar.Corruption.Stage";
    private static final String GIORNO_UCCISIONE = "Klyntar.Corruption.LastKillDay";
    private static final String GIORNO_CALO = "Klyntar.Corruption.LastDecayDay";
    /** Dove comincia ogni stadio. */
    private static final int[] SOGLIE = {0, 20, 40, 65, 90};
    private static final int MASSIMO = 100;
    private static final int GIORNI_PRIMA_DEL_CALO = 20;
    /** Allo stadio 3, dopo tanti giorni senza uccidere, l'arma comincia a colpire da sola. */
    private static final int GIORNI_DI_SETE = 3;
    private static final long TICK_GIORNO = 24000L;
    private static final int OGNI = 100;
    private static final int SETE_OGNI = 20 * 30;
    private static final double SETE_PORTATA = 5.0D;
    private static final float SETE_DANNO = 4.0F;
    private static final int FAME_EXTRA_OGNI = 20 * 10;

    private CorruzioneAllBlack() {
    }

    public static boolean attivo(Player giocatore) {
        return FORMA.equals(SymbioteState.forma(giocatore));
    }

    public static int corruzione(Player giocatore) {
        return Math.max(0, Math.min(MASSIMO, SymbioteState.getScore(giocatore, CORRUZIONE)));
    }

    public static int stadio(Player giocatore) {
        int c = corruzione(giocatore);
        for (int i = SOGLIE.length - 1; i > 0; i--) {
            if (c >= SOGLIE[i]) {
                return i;
            }
        }
        return 0;
    }

    private static long giorno(Player giocatore) {
        return giocatore.level().getDayTime() / TICK_GIORNO;
    }

    // ------------------------------------------------------------------ le uccisioni

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer uccisore) || !attivo(uccisore)) {
            return;
        }
        LivingEntity vittima = event.getEntity();
        int guadagno;
        if (vittima instanceof Player) {
            guadagno = 10;
        } else if (vittima.getType().is(Tags.EntityTypes.BOSSES)) {
            guadagno = vittima instanceof EnderDragon ? 20 : 15;
        } else if (vittima instanceof Enemy) {
            guadagno = 2;
        } else {
            guadagno = 1;
        }
        SymbioteState.setScore(uccisore, CORRUZIONE, Math.min(MASSIMO, corruzione(uccisore) + guadagno));
        SymbioteState.setScore(uccisore, GIORNO_UCCISIONE, (int) giorno(uccisore));
        aggiornaStadio(uccisore);
    }

    // ------------------------------------------------------------------ il tempo

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0 || !attivo(ospite)) {
            return;
        }
        long oggi = giorno(ospite);
        if (ospite.getScoreboard().getObjective(GIORNO_UCCISIONE) == null) {
            // appena raccolto: il conto dei giorni senza uccidere parte da qui
            SymbioteState.setScore(ospite, GIORNO_UCCISIONE, (int) oggi);
            SymbioteState.assicuraObiettivo(ospite, CORRUZIONE);
        }
        long senzaUccidere = oggi - SymbioteState.getScore(ospite, GIORNO_UCCISIONE);
        // il calo: uno stadio al giorno, dopo venti giorni senza uccisioni
        if (senzaUccidere > GIORNI_PRIMA_DEL_CALO && SymbioteState.getScore(ospite, GIORNO_CALO) < oggi) {
            int stadio = stadio(ospite);
            SymbioteState.setScore(ospite, CORRUZIONE, stadio == 0 ? 0 : SOGLIE[stadio - 1]);
            SymbioteState.setScore(ospite, GIORNO_CALO, (int) oggi);
        }
        aggiornaStadio(ospite);
        // l'affinita' di All-Black e' la sua corruzione: i livelli di fiducia della voce la seguono
        SymbioteState.setScore(ospite, "Klyntar.Affinity." + FORMA, corruzione(ospite));
        prezzo(ospite, stadio(ospite), senzaUccidere);
    }

    private static void aggiornaStadio(ServerPlayer ospite) {
        int prima = SymbioteState.getScore(ospite, STADIO);
        int ora = stadio(ospite);
        if (prima == ora && ospite.getScoreboard().getObjective(STADIO) != null) {
            return;
        }
        SymbioteState.setScore(ospite, STADIO, ora);
        VoceSimbionte.di(ospite, ora > prima ? "corruzione_sale" : "corruzione_scende", Tono.AGGRESSIVO, true);
        // la dominanza di All-Black cresce con la corruzione: la doppia unione va ricalcolata
        UnioneDoppia.ricalcola(ospite);
    }

    /** Quello che ogni stadio fa pagare. */
    private static void prezzo(ServerPlayer ospite, int stadio, long senzaUccidere) {
        if (stadio == 0) {
            // sussurri ambigui che spingono a combattere
            VoceSimbionte.di(ospite, "corruzione_sussurro", Tono.AGGRESSIVO, false, 20L * 60L * 4L);
        }
        // stadio 1: le venature nere visibili. SEGNALIBRO: le disegnera' il render layer di All-Black,
        // con una condizione su Klyntar.Corruption.Stage
        if (stadio >= 3 && senzaUccidere >= GIORNI_DI_SETE && ospite.tickCount % SETE_OGNI == 0) {
            seteDiSangue(ospite);
        }
        if (stadio >= 4 && ospite.tickCount % FAME_EXTRA_OGNI == 0) {
            // fame e sete piu' rapide: il berserk arriva prima
            modKlyntar.player.VenomSymbioteSystemsHandler.togliFame(ospite, 1);
        }
    }

    /** Dopo troppo tempo senza uccidere, l'arma colpisce da sola l'essere vivente piu' vicino. */
    private static void seteDiSangue(ServerPlayer ospite) {
        AABB zona = ospite.getBoundingBox().inflate(SETE_PORTATA);
        ospite.level().getEntitiesOfClass(LivingEntity.class, zona,
                        e -> e != ospite && e.isAlive() && !e.isSpectator()
                                && (!(e instanceof Player altro) || modKlyntar.player.PvpRules.colpibile(ospite, altro)))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(ospite)))
                .ifPresent(vittima -> {
                    vittima.hurt(ospite.damageSources().playerAttack(ospite), SETE_DANNO);
                    VoceSimbionte.di(ospite, "corruzione_sete", Tono.AGGRESSIVO, true);
                });
    }

    // ------------------------------------------------------------------ le debolezze

    /** Fuoco e suono pesano di piu' a corruzione alta: un quarto in piu' per stadio. */
    public static float moltiplicatoreDebolezza(Player giocatore) {
        return attivo(giocatore) ? 1.0F + 0.25F * stadio(giocatore) : 1.0F;
    }

    /**
     * Si puo' staccare dall'ospite? Gli altri simbionti si strappano col suono; All-Black solo
     * se fuoco e suono lo colpiscono insieme.
     */
    public static boolean strappabile(Player giocatore) {
        return !attivo(giocatore) || giocatore.isOnFire()
                || SymbioteState.getScore(giocatore, "Venom.Burn") > 0;
    }
}
