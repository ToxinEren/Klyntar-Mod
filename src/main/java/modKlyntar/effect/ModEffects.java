package modKlyntar.effect;

import modKlyntar.MyMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MyMod.MOD_ID);

    /** Il simbionte dentro un animale: lo consuma un punto ogni dieci secondi. */
    public static final RegistryObject<MobEffect> SYMBIOTE_PARASITE =
            EFFECTS.register("symbiote_parasite", SymbioteParasiteEffect::new);

    /** Il Necrosword: niente rigenerazione per un minuto. Segnalibro di All-Black. */
    public static final RegistryObject<MobEffect> NEGAZIONE_IMMORTALITA =
            EFFECTS.register("immortality_negation", NegazioneImmortalitaEffect::new);

    /** La corruzione del bersaglio, simile al wither. Segnalibro di All-Black. */
    public static final RegistryObject<MobEffect> CORRUZIONE_BERSAGLIO =
            EFFECTS.register("corruption", CorruzioneBersaglioEffect::new);

    private ModEffects() {
    }
}
