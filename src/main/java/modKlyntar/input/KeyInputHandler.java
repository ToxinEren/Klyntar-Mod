package modKlyntar.input;

import modKlyntar.client.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = "klyntars", value = Dist.CLIENT)
public class KeyInputHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.toggleAbility1.consumeClick()) {
            LOGGER.debug("Toggle Ability 1 key pressed");
            handleToggleAbility1();
        }
    }

    /**
     * Il giocatore porta Venom o la sua evoluzione Spidey?
     *
     * <p>Il potere si chiede a Palladium e non alla capability della mod: quella vive solo sul
     * server e non viene sincronizzata, quindi qui sul client sarebbe sempre a zero e il tasto
     * non funzionerebbe mai.</p>
     */
    private static boolean portaVenom(Player giocatore) {
        Collection<?> poteri = SuperpowerUtil.getSuperpowerIds(giocatore);
        if (poteri == null) {
            return false;
        }
        for (Object potere : poteri) {
            String id = String.valueOf(potere);
            if (id.endsWith(":venom") || id.endsWith(":venomspidey")) {
                return true;
            }
        }
        return false;
    }

    /** L'invisibilita' e' del simbionte: senza Venom addosso il tasto non fa niente. */
    private static void handleToggleAbility1() {
        Player player = Minecraft.getInstance().player;
        if (player == null || !portaVenom(player)) {
            return;
        }
        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.removeEffect(MobEffects.INVISIBILITY);
            ClientEventHandler.setInvisibilitaSimbionte(false);
            player.displayClientMessage(Component.literal("Invisibility disabled"), false);
            setPlayerModelVisible(player, true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, true));
            ClientEventHandler.setInvisibilitaSimbionte(true);
            player.displayClientMessage(Component.literal("Invisibility enabled"), false);
            setPlayerModelVisible(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player != Minecraft.getInstance().player || !ClientEventHandler.isInvisibilitaSimbionte()) {
            return;
        }

        // perdendo il simbionte l'invisibilita' se ne va con lui: altrimenti, visto che il
        // tasto ora richiede Venom, resterebbe accesa per sempre senza modo di spegnerla
        if (!portaVenom(player)) {
            player.removeEffect(MobEffects.INVISIBILITY);
            ClientEventHandler.setInvisibilitaSimbionte(false);
            setPlayerModelVisible(player, true);
            return;
        }

        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            setPlayerModelVisible(player, false);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        }
    }

    private static void setPlayerModelVisible(Player player, boolean visible) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(player.getUUID())) {
                Minecraft.getInstance().player.setInvisible(!visible);
                LOGGER.debug("Set player model visibility to: " + visible);
            }
        });
    }
}
