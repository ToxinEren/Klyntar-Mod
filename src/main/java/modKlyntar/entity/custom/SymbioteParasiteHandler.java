package modKlyntar.entity.custom;

import modKlyntar.MyMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Il simbionte che esce dall'animale in cui era entrato.
 *
 * <p>Quando il mob simbionte raggiunge un animale gli entra dentro: l'entita' sparisce,
 * l'animale riceve il marchio e l'effetto {@code symbiote_parasite}, che lo consuma un punto
 * ogni dieci secondi. Qui si chiude il cerchio: alla morte dell'ospite - per l'effetto, per
 * mano di qualcuno, per qualunque causa - un simbionte nuovo esce dal cadavere.</p>
 *
 * <p>Prima questo pezzo non c'era. Il marchio veniva scritto e nessuno lo leggeva, e il codice
 * che avrebbe fatto uscire il simbionte stava nell'entita' che si era appena cancellata:
 * ogni simbionte che raggiungeva un animale era perso per sempre.</p>
 *
 * <p>Il marchio nei dati persistenti e' la fonte di verita', non l'effetto: un effetto lo puo'
 * togliere qualcosa d'altro, il marchio no. Si ascolta a priorita' minima, cosi' chi annulla
 * la morte - un totem, una mod - passa prima, e un evento annullato qui non arriva.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteParasiteHandler {

    /** Il marchio sull'animale che porta il simbionte dentro. */
    public static final String MARCHIO = "InfectedBySymbiote";

    private static final Logger LOGGER = LogManager.getLogger("KlyntarParasite");

    private SymbioteParasiteHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMorte(LivingDeathEvent event) {
        LivingEntity ospite = event.getEntity();
        if (!(ospite.level() instanceof ServerLevel livello)
                || !ospite.getPersistentData().getBoolean(MARCHIO)) {
            return;
        }
        // tolto subito: l'evento puo' arrivare piu' volte per la stessa morte
        ospite.getPersistentData().remove(MARCHIO);

        SymbioteEntity simbionte = MyMod.SYMBIOTE_ENTITY.get().create(livello);
        if (simbionte == null) {
            return;
        }
        simbionte.moveTo(ospite.getX(), ospite.getY(), ospite.getZ(), ospite.getYRot(), 0.0F);
        livello.addFreshEntity(simbionte);
        livello.playSound(null, ospite.blockPosition(), modKlyntar.sound.SuoniKlyntar.SYMBIOTE_EMERGE.get(),
                SoundSource.HOSTILE, 1.0F, 0.6F);
        LOGGER.info("A symbiote left the body of {}", ospite.getType().getDescription().getString());
    }
}
