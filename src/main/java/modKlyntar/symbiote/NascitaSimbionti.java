package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.symbiote.RegistroSimbionti.Simbionte;
import modKlyntar.symbiote.RegistroSimbionti.Tratto;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La nascita di Carnage e Toxin.
 *
 * <p>Dal documento di design: il figlio nasce quando il giocatore e' gravemente ferito e il
 * padre ha inglobato due simbionti, cosi' la nascita e' una conseguenza dello stile di gioco e
 * non un semplice sblocco.</p>
 * <ol>
 *   <li>ogni simbionte inglobato dal padre fa salire il suo contatore ({@link ProfiliSimbionti}):
 *   a due il padre e' pronto;</li>
 *   <li>quando la vita del giocatore scende sotto il 30% con il padre pronto addosso, il figlio
 *   spawna accanto;</li>
 *   <li>il contatore del padre si azzera;</li>
 *   <li>il figlio eredita una parte delle abilita' del padre; e siccome il padre e' ancora sul
 *   corpo, il figlio lo attacca a vista e scatta il conflitto ({@link ConflittoSimbionti}).</li>
 * </ol>
 * <p>Il figlio non nasce se il padre e' gia' in conflitto con un altro simbionte.</p>
 *
 * <p><b>Segnalibro.</b> Carnage e Toxin non sono ancora nella mod: il padre arriva pronto, ma
 * finche' il figlio nel registro non e' disponibile non nasce niente e il contatore resta
 * com'e', pronto per quando ci sara'.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class NascitaSimbionti {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarNascita");
    /** Sotto questa frazione di vita il padre pronto genera. Da calibrare in test. */
    private static final float SOGLIA_VITA = 0.30F;
    private static final int OGNI = 10;
    /** Per il segnalibro: lo si scrive nel log una volta per giocatore, non a ogni controllo. */
    private static final Set<UUID> GIA_SEGNALATO = ConcurrentHashMap.newKeySet();

    private NascitaSimbionti() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0 || !ospite.isAlive()) {
            return;
        }
        String padre = SymbioteState.forma(ospite);
        if (padre.isEmpty()) {
            return;
        }
        String famiglia = RegistroSimbionti.famiglia(padre);
        Optional<Simbionte> figlio = RegistroSimbionti.figlioDi(famiglia);
        if (figlio.isEmpty()
                || ProfiliSimbionti.inglobati(ospite, famiglia) < ProfiliSimbionti.INGLOBATI_PER_NASCERE
                || ospite.getHealth() >= ospite.getMaxHealth() * SOGLIA_VITA
                || ConflittoSimbionti.inCorso(ospite)) {
            return;
        }
        if (!figlio.get().disponibile()) {
            // SEGNALIBRO: qui nascera' Carnage (da Venom) e poi Toxin (da Carnage)
            if (GIA_SEGNALATO.add(ospite.getUUID())) {
                LOGGER.info("{}'s {} is ready to give birth to {}, which is not in the mod yet",
                        ospite.getGameProfile().getName(), famiglia, figlio.get().id());
            }
            return;
        }
        nasce(ospite, famiglia, figlio.get());
    }

    private static void nasce(ServerPlayer ospite, String padre, Simbionte figlio) {
        if (!(ospite.level() instanceof ServerLevel livello)) {
            return;
        }
        SymbioteEntity mob = MyMod.SYMBIOTE_ENTITY.get().create(livello);
        if (mob == null) {
            return;
        }
        List<Tratto> ereditati = ProfiliSimbionti.eredita(ospite, padre, figlio.id(), ospite.getRandom());
        ProfiliSimbionti.azzeraInglobati(ospite, padre);
        mob.setForma(figlio.id());
        Vec3 dove = ospite.position().add(ospite.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale(1.5D));
        mob.moveTo(dove.x, ospite.getY(), dove.z, ospite.getYRot() + 180.0F, 0.0F);
        livello.addFreshEntity(mob);
        livello.playSound(null, mob.blockPosition(), modKlyntar.sound.SuoniKlyntar.SYMBIOTE_EMERGE.get(),
                SoundSource.HOSTILE, 1.2F, 0.5F);
        VoceSimbionte.di(ospite, "nascita", Tono.AVVISO, true);
        LOGGER.info("{}: {} was born from {} (inherits {})", ospite.getGameProfile().getName(),
                figlio.id(), padre, ereditati);
        // il padre e' ancora sul corpo: il figlio lo attacca a vista, e il conflitto scatta subito.
        // Se per qualche ragione non puo' entrare, resta li' come mob da catturare
        if (ConflittoSimbionti.entra(ospite, figlio.id(), mob.position().add(0.0D, 0.5D, 0.0D))) {
            mob.discard();
        }
    }
}
