package modKlyntar.input;

import com.mojang.blaze3d.platform.InputConstants;
import modKlyntar.MyMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CATEGORY = "key.categories.klyntars";
    public static final String TOGGLE_ABILITY_1 = "key.klyntars.toggle_ability_1";

    public static final KeyMapping toggleAbility1 = new KeyMapping(TOGGLE_ABILITY_1, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        LOGGER.debug("Registering key mappings");
        event.register(toggleAbility1);
    }
}
