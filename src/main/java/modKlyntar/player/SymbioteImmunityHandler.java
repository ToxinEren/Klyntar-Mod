package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.effect.ModEffects;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Il passivo che tiene puliti i portatori di simbionte.
 *
 * <p>Finche' il simbionte e' in forze scioglie i malus addosso al giocatore. Si spegne in due
 * casi soli: quando fuoco o suono lo hanno indebolito, e quando addosso c'e' l'Anti-Venom,
 * che e' fatto apposta per superarlo.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteImmunityHandler {
    /** ogni quanti tick si passa in rassegna il giocatore */
    private static final int INTERVALLO = 5;

    /**
     * I malus che il simbionte scioglie. E' un elenco chiuso e non l'intera categoria dannosa:
     * la levitazione, per dire, e' dannosa ma la usano tempest e altre abilita' nostre, e
     * cancellarla le romperebbe.
     */
    private static final List<MobEffect> DA_SCIOGLIERE = List.of(
            MobEffects.WEAKNESS,
            MobEffects.WITHER,
            MobEffects.POISON,
            MobEffects.HUNGER,
            MobEffects.CONFUSION,
            MobEffects.BLINDNESS,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.DARKNESS,
            MobEffects.UNLUCK);

    private SymbioteImmunityHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer giocatore) || giocatore.tickCount % INTERVALLO != 0) {
            return;
        }
        if (!SymbioteState.haSimbionte(giocatore)) {
            return;
        }
        // indebolito dal fuoco o dal suono: il simbionte non riesce a fare da filtro
        if (VenomSymbioteSystemsHandler.isPlayerVulnerable(giocatore)) {
            return;
        }
        // l'Anti-Venom passa oltre il passivo, altrimenti non morderebbe mai; ma su chi e'

        sciogliMalus(giocatore);
    }

    /** Toglie di dosso i malus che il simbionte sa sciogliere. */
    public static void sciogliMalus(LivingEntity portatore) {
        for (MobEffect malus : DA_SCIOGLIERE) {
            if (portatore.hasEffect(malus)) {
                portatore.removeEffect(malus);
            }
        }
    }
}
