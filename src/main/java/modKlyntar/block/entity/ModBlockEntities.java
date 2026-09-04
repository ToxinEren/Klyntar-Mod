package modKlyntar.block.entity;

import modKlyntar.MyMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Le block entity della mod: servono solo a dare un renderer ai due cadaveri del cratere. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MyMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<DioMortoBlockEntity>> DIO_MORTO =
            BLOCK_ENTITIES.register("dio_morto",
                    () -> BlockEntityType.Builder.of(DioMortoBlockEntity::new,
                            MyMod.DIO_MORTO.get()).build(null));

    public static final RegistryObject<BlockEntityType<AlienoFusoBlockEntity>> ALIENO_FUSO =
            BLOCK_ENTITIES.register("alieno_fuso",
                    () -> BlockEntityType.Builder.of(AlienoFusoBlockEntity::new,
                            MyMod.ALIENO_FUSO.get()).build(null));

    private ModBlockEntities() {
    }
}
