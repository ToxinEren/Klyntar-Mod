package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomCameraHeightHandler {
    public static final String OBJECTIVE_NAME = "Venom.CameraHeight";

    private VenomCameraHeightHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    }
}
