package modKlyntar.client;

import modKlyntar.MyMod;
import net.minecraftforge.api.distmarker.Dist;
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
}
