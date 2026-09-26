package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.player.SymbioteInvisibilityHandler;
import modKlyntar.player.VenomPlayerSizeHandler;
import modKlyntar.player.VenomSymbioteSystemsHandler;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quando il simbionte ha qualcosa da dire.
 *
 * <p>Invece di mettere una chiamata alla voce in ogni gestore - fame, debolezze, legame, forme -
 * si guarda lo stato del giocatore ogni {@link #OGNI} tick e si confronta con quello di prima:
 * ogni cambio che conta e' una situazione. I gestori restano come sono, e la voce sta tutta
 * qui. La prima osservazione dopo l'ingresso nel mondo fa solo da base, altrimenti chi entra
 * trasformato si sentirebbe salutare come se si fosse appena legato.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VoceSimbionteOsservatore {
    private static final int OGNI = 5;
    /** Sotto questa frazione della vita il simbionte si preoccupa. */
    private static final float SALUTE_BASSA = 0.3F;
    /** Le uccisioni sono tante: il simbionte ne commenta solo una parte. */
    private static final float PROBABILITA_UCCISIONE = 0.35F;
    /** Di quanto deve salire la fame del simbionte in un'osservazione perche' sia un pasto. */
    private static final int PASTO_MINIMO = 5;

    private static final String FAME = "Venom.SymbioteHunger";
    private static final String BERSERK = "Venom.Berserk";
    private static final String SCOTTATURA = "Venom.Burn";
    private static final String INDEBOLITO = "Venom.VulnerabilityLock";
    private static final String COLPI_SONORI = "Venom.SonicHits";
    private static final String TESTA_AUTO = "Venom.AutoHead";
    /** Sopra questa fame la testa smette di mangiare da sola. */
    private static final int TESTA_SAZIA = 90;

    /** Quello che conta dello stato di un ospite, per accorgersi di cosa e' cambiato. */
    private record Stato(String forma, boolean corpo, int fame, boolean berserk, int affinita,
                         int scottatura, int indebolito, int colpi, boolean knull,
                         boolean invisibile, boolean saluteBassa, boolean testa, boolean testaAuto) {
    }

    private static final Map<UUID, Stato> PRIMA = new ConcurrentHashMap<>();

    private VoceSimbionteOsservatore() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0) {
            return;
        }
        Stato ora = leggi(ospite);
        Stato prima = PRIMA.put(ospite.getUUID(), ora);
        if (prima != null) {
            confronta(ospite, prima, ora);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PRIMA.remove(event.getEntity().getUUID());
        VoceSimbionte.dimentica(event.getEntity().getUUID());
    }

    /** Un nemico ucciso col corpo fuori: il simbionte ogni tanto commenta. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer ospite)
                || !(event.getEntity() instanceof Enemy)
                || !SymbioteState.haSimbionte(ospite)
                || SymbioteState.getScore(ospite, VenomPlayerSizeHandler.SIZE_OBJECTIVE) <= 0
                || ospite.getRandom().nextFloat() >= PROBABILITA_UCCISIONE) {
            return;
        }
        UmoreSimbionte.cambia(ospite, 3);
        VoceSimbionte.di(ospite, "uccisione", Tono.AGGRESSIVO);
    }

    private static Stato leggi(ServerPlayer ospite) {
        return new Stato(
                SymbioteState.forma(ospite),
                SymbioteState.getScore(ospite, VenomPlayerSizeHandler.SIZE_OBJECTIVE) > 0,
                SymbioteState.getScore(ospite, FAME),
                SymbioteState.getScore(ospite, BERSERK) > 0,
                SymbioteState.affinita(ospite),
                SymbioteState.getScore(ospite, SCOTTATURA),
                SymbioteState.getScore(ospite, INDEBOLITO),
                SymbioteState.getScore(ospite, COLPI_SONORI),
                SymbioteState.getScore(ospite, SymbioteState.KNULL_BOND_OBJECTIVE) > 0,
                SymbioteInvisibilityHandler.invisibileDaSimbionte(ospite),
                ospite.getHealth() <= ospite.getMaxHealth() * SALUTE_BASSA,
                testaFuori(ospite),
                SymbioteState.getScore(ospite, TESTA_AUTO) > 0);
    }

    /** La testa del simbionte e' fuori? Lo dice l'abilita' stessa, che Palladium tiene aggiornata. */
    private static boolean testaFuori(ServerPlayer ospite) {
        String forma = SymbioteState.forma(ospite);
        return !forma.isEmpty()
                && AbilityUtil.isEnabled(ospite, new ResourceLocation(MyMod.MOD_ID, forma), "enablevenomhead");
    }

    private static void confronta(ServerPlayer ospite, Stato prima, Stato ora) {
        // --- il legame: arriva, cambia forma, se ne va
        if (prima.forma().isEmpty() && !ora.forma().isEmpty()) {
            VoceSimbionte.di(ospite, "legame_primo", Tono.LEGAME_SU, true);
            return;
        }
        if (!prima.forma().isEmpty() && ora.forma().isEmpty()) {
            // strappato dal suono: i colpi erano gia' contati prima dello strappo
            if (prima.colpi() > 0) {
                VoceSimbionte.di(ospite, "strappato", Tono.LEGAME_GIU, true);
            }
            return;
        }
        if (ora.forma().isEmpty()) {
            return;
        }
        boolean formaCambiata = !prima.forma().equals(ora.forma());
        if (formaCambiata && "venomspidey".equals(ora.forma())) {
            VoceSimbionte.di(ospite, "ragno", Tono.LEGAME_SU, true);
        }

        // --- le debolezze, prima di tutto il resto: sono quelle che il giocatore deve sentire
        // e l'umore ne risente: il suono e il fuoco sono quello che il simbionte odia di piu'
        if (ora.colpi() > prima.colpi()) {
            boolean ultimo = ora.colpi() >= VenomSymbioteSystemsHandler.colpiPerStrappo(ospite) - 1;
            UmoreSimbionte.cambia(ospite, -10);
            VoceSimbionte.di(ospite, ultimo ? "colpo_sonoro_ultimo" : "colpo_sonoro", Tono.AVVISO, ultimo);
        } else if (prima.indebolito() <= 0 && ora.indebolito() > 0) {
            UmoreSimbionte.cambia(ospite, -15);
            VoceSimbionte.di(ospite, "indebolito", Tono.LEGAME_GIU);
        } else if (prima.scottatura() <= 0 && ora.scottatura() > 0 && ora.indebolito() <= 0) {
            UmoreSimbionte.cambia(ospite, -5);
            VoceSimbionte.di(ospite, "scottatura", Tono.AVVISO);
        }

        // --- il corpo: fuori e dentro. Se rientra perche' indebolito, ha gia' parlato l'indebolimento
        if (!prima.corpo() && ora.corpo()) {
            VoceSimbionte.di(ospite, "trasformazione", Tono.NEUTRO);
        } else if (prima.corpo() && !ora.corpo() && ora.indebolito() <= 0) {
            VoceSimbionte.di(ospite, "ritorno", Tono.NEUTRO);
        }

        // --- la fame: le soglie in discesa, il berserk, i pasti
        if (!prima.berserk() && ora.berserk()) {
            UmoreSimbionte.cambia(ospite, -20);
            VoceSimbionte.di(ospite, "berserk_inizio", Tono.AGGRESSIVO, true);
        } else if (prima.berserk() && !ora.berserk()) {
            VoceSimbionte.di(ospite, "berserk_fine", Tono.NEUTRO);
        } else if (ora.berserk()) {
            // in berserk comanda lui, e ogni tanto lo fa sapere
            VoceSimbionte.di(ospite, "berserk_durante", Tono.AGGRESSIVO);
        } else if (prima.fame() > 10 && ora.fame() <= 10) {
            UmoreSimbionte.cambia(ospite, -10);
            VoceSimbionte.di(ospite, "fame_10", Tono.AGGRESSIVO);
        } else if (prima.fame() > 25 && ora.fame() <= 25) {
            UmoreSimbionte.cambia(ospite, -5);
            VoceSimbionte.di(ospite, "fame_25", Tono.AVVISO);
        } else if (prima.fame() > 50 && ora.fame() <= 50) {
            VoceSimbionte.di(ospite, "fame_50", Tono.NEUTRO);
        } else if (ora.fame() - prima.fame() >= PASTO_MINIMO) {
            UmoreSimbionte.cambia(ospite, 10);
            DesideriSimbionte.pastoDelSimbionte(ospite);
            // da umano e' la testa che mangia: commenta lei, a modo suo
            VoceSimbionte.di(ospite, ora.corpo() ? "pasto" : "testa_mangia", Tono.LEGAME_SU);
        }

        // --- la testa: la fame che la fa uscire da sola, la sazieta', e le uscite a comando
        if (!prima.testaAuto() && ora.testaAuto()) {
            VoceSimbionte.di(ospite, "testa_fame", Tono.NEUTRO);
        } else if (prima.testaAuto() && !ora.testaAuto() && ora.fame() >= TESTA_SAZIA) {
            VoceSimbionte.di(ospite, "testa_sazia", Tono.LEGAME_SU);
        } else if (!prima.testa() && ora.testa() && !ora.testaAuto()) {
            VoceSimbionte.di(ospite, "testa_fuori", Tono.NEUTRO);
        } else if (prima.testa() && !ora.testa() && !ora.corpo() && !prima.testaAuto()) {
            VoceSimbionte.di(ospite, "testa_dentro", Tono.NEUTRO);
        }

        // --- l'affinita': Bond 2 e Bond 3. Cambiando forma l'affinita' letta salta a quella
        // della forma nuova, e non e' un legame che cresce
        if (!formaCambiata) {
            if (prima.affinita() < 100 && ora.affinita() >= 100) {
                VoceSimbionte.di(ospite, "bond3", Tono.LEGAME_SU, true);
            } else if (prima.affinita() < 50 && ora.affinita() >= 50) {
                VoceSimbionte.di(ospite, "bond2", Tono.LEGAME_SU, true);
            }
        }

        // --- il resto
        if (!prima.knull() && ora.knull()) {
            VoceSimbionte.di(ospite, "knull", Tono.LEGAME_SU, true);
        }
        if (!prima.invisibile() && ora.invisibile()) {
            VoceSimbionte.di(ospite, "invisibile", Tono.NEUTRO);
        }
        if (!prima.saluteBassa() && ora.saluteBassa() && ospite.isAlive()) {
            VoceSimbionte.di(ospite, "salute_bassa", Tono.AVVISO);
        }
    }
}
