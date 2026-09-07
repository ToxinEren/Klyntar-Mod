package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.player.SymbioteInvisibilityHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static String transformedForm = "";

    public static boolean isVenomModelActive() {
        return !transformedForm.isEmpty();
    }

    public static void setVenomModelActive(boolean active) {
        transformedForm = active ? "venom" : "";
    }

    public static void setTransformedForm(String form) {
        transformedForm = form == null ? "" : form.trim().toLowerCase();
    }

    /**
     * Sparire vuol dire sparire tutto: nascondere il modello vanilla non basta.
     *
     * <p>Il corpo del simbionte non e' il modello del giocatore, sono i render layer che
     * Palladium disegna dentro {@code PlayerRenderer}: ignorano l'invisibilita' e resterebbero
     * in vista da soli. Annullando l'evento all'inizio del rendering cade tutto insieme,
     * modello e strati.</p>
     *
     * <p>Vale per <b>qualunque</b> giocatore, non solo per il proprio: l'evento gira su ogni
     * client anche per gli altri, ed e' cosi' che l'invisibilita' funziona in multiplayer.</p>
     */
    @SubscribeEvent
    public static void nascondiRenderSimbionte(RenderPlayerEvent.Pre evento) {
        if (SymbioteInvisibilityHandler.invisibileDaSimbionte(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }
}
