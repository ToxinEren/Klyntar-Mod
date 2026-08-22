package modKlyntar.worldgen;

import modKlyntar.MyMod;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, MyMod.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KNULL_GEODE =
            FEATURES.register("knull_geode",
                    () -> new KnullGeodeFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }
}
