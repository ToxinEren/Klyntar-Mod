package modKlyntar;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import modKlyntar.power.PlayersPower;
import modKlyntar.power.PlayersPowerProvider;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public class ModEventSubscriber {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayersPower.class);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            player.getCapability(PlayersPowerProvider.PLAYERS_POWER).ifPresent(power -> {
                power.tick(player);
            });
        }
    }
}
