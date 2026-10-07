package modKlyntar.effect;

import modKlyntar.MyMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La negazione dell'immortalita' del Necrosword: chi viene colpito non si rigenera.
 *
 * <p>Dal documento di design: la spada di All-Black blocca tutti i tipi di rigenerazione per un
 * minuto sul bersaglio colpito. Non fa danno da sola: annulla ogni cura, che venga dal cibo,
 * dalla pozione, dall'effetto di rigenerazione o da un'abilita'.</p>
 *
 * <p><b>Segnalibro.</b> La applichera' il Necrosword di All-Black, per 1200 tick.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class NegazioneImmortalitaEffect extends MobEffect {
    /** il nero violaceo del simbionte di Knull */
    private static final int COLORE = 0x1B0F24;
    /** quanto dura quando la applica la spada: un minuto */
    public static final int DURATA = 20 * 60;

    public NegazioneImmortalitaEffect() {
        super(MobEffectCategory.HARMFUL, COLORE);
    }

    @SubscribeEvent
    public static void onCura(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModEffects.NEGAZIONE_IMMORTALITA.get())) {
            event.setCanceled(true);
        }
    }
}
