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

    public static final String CATEGORY = "key.categories.mymod";
    public static final String TOGGLE_ABILITY_1 = "key.mymod.toggle_ability_1";
    public static final String TOGGLE_ABILITY_2 = "key.mymod.toggle_ability_2";
    public static final String TOGGLE_ABILITY_3 = "key.mymod.toggle_ability_3";

    public static final KeyMapping toggleAbility1 = new KeyMapping(TOGGLE_ABILITY_1, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping toggleAbility2 = new KeyMapping(TOGGLE_ABILITY_2, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);
    public static final KeyMapping toggleAbility3 = new KeyMapping(TOGGLE_ABILITY_3, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        LOGGER.debug("Registering key mappings");
        event.register(toggleAbility1);
        event.register(toggleAbility2);
        event.register(toggleAbility3);
    }
}
