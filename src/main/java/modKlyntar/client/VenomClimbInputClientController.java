package modKlyntar.client;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomClimbInputClientController {
    private static boolean lastForward;
    private static boolean lastCtrl;
    private static boolean lastShift;

    private VenomClimbInputClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            syncInput(false, false, false);
            return;
        }

        syncInput(minecraft.options.keyUp.isDown(), minecraft.options.keySprint.isDown(), minecraft.options.keyShift.isDown());
    }

    private static void syncInput(boolean forward, boolean ctrl, boolean shift) {
        if (forward == lastForward && ctrl == lastCtrl && shift == lastShift) {
            return;
        }

        lastForward = forward;
        lastCtrl = ctrl;
        lastShift = shift;
        ModNetwork.syncVenomClimbInput(forward, ctrl, shift);
    }
}
