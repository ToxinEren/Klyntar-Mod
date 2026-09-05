package modKlyntar.client;

import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static String transformedForm = "";

    /** Acceso mentre l'invisibilita' del simbionte e' attiva, dal tasto dedicato. */
    private static boolean invisibilitaSimbionte = false;

    public static boolean isVenomModelActive() {
        return !transformedForm.isEmpty();
    }

    public static void setVenomModelActive(boolean active) {
        transformedForm = active ? "venom" : "";
    }

    public static void setTransformedForm(String form) {
        transformedForm = form == null ? "" : form.trim().toLowerCase();
    }

    public static boolean isInvisibilitaSimbionte() {
        return invisibilitaSimbionte;
    }

    public static void setInvisibilitaSimbionte(boolean attiva) {
        invisibilitaSimbionte = attiva;
    }

    /**
     * Sparire vuol dire sparire tutto: nascondere il modello vanilla non basta.
     *
     * <p>Il corpo del simbionte non e' il modello del giocatore, sono i render layer che
     * Palladium disegna dentro {@code PlayerRenderer}: ignorano l'invisibilita' e resterebbero
     * in vista da soli. Annullando l'evento all'inizio del rendering cade tutto insieme,
     * modello e strati.</p>
     */
    @SubscribeEvent
    public static void nascondiRenderSimbionte(RenderPlayerEvent.Pre evento) {
        if (!invisibilitaSimbionte) {
            return;
        }
        if (evento.getEntity() == Minecraft.getInstance().player) {
            evento.setCanceled(true);
        }
    }
}
