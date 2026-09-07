package modKlyntar.input;

import modKlyntar.network.ModNetwork;
import modKlyntar.player.SymbioteInvisibilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "klyntars", value = Dist.CLIENT)
public class KeyInputHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.toggleAbility1.consumeClick()) {
            LOGGER.debug("Toggle Ability 1 key pressed");
            chiediInvisibilita();
        }
    }

    /**
     * Chiede al server di accendere o spegnere l'invisibilita' del simbionte.
     *
     * <p>L'effetto qui non si tocca: applicato dal client resterebbe locale, e in multiplayer
     * gli altri continuerebbero a vederti. Il controllo sul potere e' solo un filtro per non
     * mandare pacchetti a vuoto — quello che conta lo rifa' il server, che e' l'unico di cui
     * fidarsi.</p>
     */
    private static void chiediInvisibilita() {
        Player giocatore = Minecraft.getInstance().player;
        if (giocatore == null || !SymbioteInvisibilityHandler.portaSimbionte(giocatore)) {
            return;
        }
        ModNetwork.alternaInvisibilitaSimbionte();
    }
}
