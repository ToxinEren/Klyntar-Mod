package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Riporta al server ogni clic sinistro, cosi' che il moveset di attack barrage possa avanzare di
 * un colpo per clic. L'evento scatta su ogni pressione distinta del tasto attacco, quindi tenerlo
 * premuto non fa scorrere la catena da solo.
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomAttackInputClientController {
    private VenomAttackInputClientController() {
    }

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        // filtro grossolano lato client: le condizioni vere le ricontrolla il server
        if (!ClientEventHandler.isVenomModelActive() || minecraft.player.isShiftKeyDown()) {
            return;
        }

        ModNetwork.syncVenomAttackClick();
    }
}
