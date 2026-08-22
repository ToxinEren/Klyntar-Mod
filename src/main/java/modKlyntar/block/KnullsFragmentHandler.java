package modKlyntar.block;

import modKlyntar.MyMod;
import modKlyntar.player.SymbioteMiningHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Chi puo' rompere un frammento di Knull, e cosa ne esce.
 *
 * <p>La roccia comune non basta: serve un simbionte fuori o il piccone della sua ruota. E non
 * si raccoglie niente — quello che c'era dentro esce da solo, e non e' contento.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class KnullsFragmentHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarFragment");

    private KnullsFragmentHandler() {
    }

    private static boolean puoRomperlo(Player giocatore) {
        return giocatore.isCreative() || SymbioteMiningHandler.scavaComeNetherite(giocatore);
    }

    /** Senza simbionte il blocco non cede: la velocita' di scavo resta a zero. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(MyMod.KNULLS_FRAGMENT_BLOCK.get()) && !puoRomperlo(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(MyMod.KNULLS_FRAGMENT_BLOCK.get())) {
            return;
        }
        Player giocatore = event.getPlayer();
        if (!puoRomperlo(giocatore)) {
            event.setCanceled(true);
            giocatore.displayClientMessage(
                    Component.literal("Solo un simbionte puo' aprire questo frammento"), true);
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel livello)) {
            return;
        }

        BlockPos posizione = event.getPos();
        Entity fuoriuscito = MyMod.GRENDELS_FRAGMENT_ENTITY.get().create(livello);
        if (fuoriuscito == null) {
            return;
        }
        fuoriuscito.moveTo(posizione.getX() + 0.5D, posizione.getY(), posizione.getZ() + 0.5D,
                giocatore.getYRot(), 0.0F);
        livello.addFreshEntity(fuoriuscito);
        livello.playSound(null, posizione, SoundEvents.WARDEN_AGITATED, SoundSource.HOSTILE, 1.0F, 0.8F);
        LOGGER.info("Un frammento aperto da {} ha liberato un Grendel's Fragment",
                giocatore.getGameProfile().getName());
    }
}
