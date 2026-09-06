package modKlyntar.worldgen;

import modKlyntar.MyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MyMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MyMod.MOD_ID);

    public static final RegistryObject<StructureType<KnullGeodeStructure>> KNULL_GEODE =
            STRUCTURE_TYPES.register("knull_geode", () -> () -> KnullGeodeStructure.CODEC);

    public static final RegistryObject<StructurePieceType> KNULL_GEODE_PIECE =
            STRUCTURE_PIECES.register("knull_geode",
                    () -> (StructurePieceType.ContextlessType) KnullGeodePiece::new);

    private ModStructures() {
    }
}
