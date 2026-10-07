package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.GrendelsFragmentEntity;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il simbionte sente gli altri simbionti prima che arrivino.
 *
 * <p>Viene dalla mod Symbiote (WildHostSense): chi porta un simbionte sa quando un altro e' nei
 * paraggi, anche dietro un muro, e il suo glielo dice. Non gli serve vederlo: lo sente attraverso
 * tutto quello che c'e' in mezzo. Cosi' il conflitto non arriva mai a tradimento.</p>
 *
 * <ul>
 *   <li>il primo avvertimento, entro 18 blocchi: {@code senso_primo} se si vede,
 *   {@code senso_primo_cieco} se no, {@code senso_bracca} se gli sta gia' girando intorno;</li>
 *   <li>uno della stessa famiglia: {@code senso_parente}. Non lotteranno, ma non si amano;</li>
 *   <li>quando arriva sotto gli 8 blocchi: {@code senso_vicino};</li>
 *   <li>quando smette di girare intorno e gli viene addosso: {@code senso_arriva}, urgente.</li>
 * </ul>
 *
 * <p>Lo stesso mob si riannuncia solo dopo un minuto. Durante un conflitto il simbionte ha
 * altro a cui pensare, e tace.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SensoSimbionti {
    private static final int OGNI = 30;
    private static final double LONTANO = 18.0D;
    private static final double VICINO = 8.0D;
    private static final long RIANNUNCIO = 20L * 60L;

    /** Cosa ha gia' detto il simbionte di un giocatore, mob per mob. */
    private static final class Memoria {
        final Map<Integer, Long> annunciati = new HashMap<>();
        final Set<Integer> vicini = new HashSet<>();
        final Set<Integer> inArrivo = new HashSet<>();
    }

    private static final Map<UUID, Memoria> MEMORIE = new ConcurrentHashMap<>();

    private SensoSimbionti() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0) {
            return;
        }
        String forma = SymbioteState.forma(ospite);
        if (forma.isEmpty() || ospite.isSpectator()) {
            MEMORIE.remove(ospite.getUUID());
            return;
        }
        if (ConflittoSimbionti.inCorso(ospite)) {
            return;
        }
        Memoria memoria = MEMORIE.computeIfAbsent(ospite.getUUID(), k -> new Memoria());
        long ora = ospite.level().getGameTime();
        String famiglia = RegistroSimbionti.famiglia(forma);
        List<SymbioteEntity> attorno = ospite.level().getEntitiesOfClass(SymbioteEntity.class,
                ospite.getBoundingBox().inflate(LONTANO),
                // il frammento di Grendel non e' un simbionte libero: porta il Knull's Bond, e va per conto suo
                e -> e.isAlive() && !e.inFuga() && !(e instanceof GrendelsFragmentEntity)
                        && e.distanceTo(ospite) <= LONTANO);
        attorno.sort(Comparator.comparingDouble(e -> e.distanceToSqr(ospite)));
        Set<Integer> presenti = new HashSet<>();
        for (SymbioteEntity mob : attorno) {
            int id = mob.getId();
            presenti.add(id);
            boolean parente = famiglia.equals(RegistroSimbionti.famiglia(mob.forma()));
            boolean vede = ospite.hasLineOfSight(mob);
            if (!parente && mob.staColpendo(ospite)) {
                if (!memoria.inArrivo.contains(id)
                        && VoceSimbionte.di(ospite, vede ? "senso_arriva" : "senso_arriva_cieco", Tono.AVVISO, true)) {
                    memoria.inArrivo.add(id);
                    memoria.annunciati.put(id, ora);
                }
                continue;
            }
            Long quando = memoria.annunciati.get(id);
            if (quando == null || ora - quando >= RIANNUNCIO) {
                String situazione = parente ? "senso_parente"
                        : mob.bracca(ospite) ? "senso_bracca"
                        : vede ? "senso_primo" : "senso_primo_cieco";
                if (VoceSimbionte.di(ospite, situazione, parente ? Tono.NEUTRO : Tono.AVVISO)) {
                    memoria.annunciati.put(id, ora);
                }
                continue;
            }
            if (!parente && ospite.distanceTo(mob) <= VICINO && !memoria.vicini.contains(id)
                    && VoceSimbionte.di(ospite, "senso_vicino", Tono.AVVISO)) {
                memoria.vicini.add(id);
            }
        }
        // chi esce dal raggio, quando torna, puo' di nuovo avvicinarsi e venire addosso
        memoria.vicini.retainAll(presenti);
        memoria.inArrivo.retainAll(presenti);
        memoria.annunciati.entrySet().removeIf(e -> ora - e.getValue() >= RIANNUNCIO && !presenti.contains(e.getKey()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MEMORIE.remove(event.getEntity().getUUID());
    }
}
