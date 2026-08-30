package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lo schianto di Grab &amp; Crush: chi viene lasciato andare si fa male atterrando.
 *
 * <p>Il tag lo mette lo script dell'abilita' quando molla la presa; qui si aspetta solo che il
 * corpo tocchi terra. Il danno non puo' essere applicato al rilascio perche' in quel momento
 * l'entita' e' ancora per aria, e nemmeno affidato alla caduta normale, che la presa azzera a
 * ogni tick.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class GrabCrushLandingHandler {

    /** Il segno lasciato dallo script del grab su chi e' stato mollato. */
    public static final String TAG_CADUTA = "venom_grab_dropped";
    /** Quanto costa l'atterraggio. */
    private static final float DANNO = 20.0F;
    /** Oltre questi tick il segno decade: se non atterra, non resta marcato per sempre. */
    private static final int ATTESA_MASSIMA = 20 * 15;

    private static final java.util.Map<java.util.UUID, Integer> ATTESA =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger("KlyntarGrabCrush");

    private GrabCrushLandingHandler() {
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entita = event.getEntity();
        if (entita.level().isClientSide() || !entita.getTags().contains(TAG_CADUTA)) {
            return;
        }

        java.util.UUID id = entita.getUUID();
        if (!entita.onGround()) {
            int attesa = ATTESA.merge(id, 1, Integer::sum);
            if (attesa > ATTESA_MASSIMA) {
                entita.removeTag(TAG_CADUTA);
                ATTESA.remove(id);
            }
            return;
        }

        entita.removeTag(TAG_CADUTA);
        ATTESA.remove(id);
        entita.hurt(entita.damageSources().fall(), DANNO);
        LOGGER.info("Grab & Crush: {} atterra e incassa {}",
                net.minecraft.world.entity.EntityType.getKey(entita.getType()), DANNO);
    }
}
