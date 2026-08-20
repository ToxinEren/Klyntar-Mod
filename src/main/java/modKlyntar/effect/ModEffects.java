package modKlyntar.effect;

import modKlyntar.MyMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MyMod.MOD_ID);

    public static final RegistryObject<MobEffect> ANTI_VENOM =
            EFFECTS.register("anti_venom", AntiVenomEffect::new);

    private ModEffects() {
    }
}
