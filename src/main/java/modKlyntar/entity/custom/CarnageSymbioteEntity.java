package modKlyntar.entity.custom;

import modKlyntar.MyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/** Il simbionte che porta la forma carnage. */
public class CarnageSymbioteEntity extends SimbionteColoratoEntity {
    public CarnageSymbioteEntity(EntityType<? extends Mob> tipo, Level livello) {
        super(tipo, livello);
    }

    @Override
    public String forma() {
        return "carnage";
    }

    @Override
    public ResourceLocation texture() {
        return new ResourceLocation(MyMod.MOD_ID, "textures/entity/carnage/carnage_symbiote.png");
    }
}
