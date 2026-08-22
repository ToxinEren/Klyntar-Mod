package modKlyntar.entity.custom;

import modKlyntar.MyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/** Il simbionte che porta la forma antivenom. */
public class AntivenomSymbioteEntity extends SimbionteColoratoEntity {
    public AntivenomSymbioteEntity(EntityType<? extends Mob> tipo, Level livello) {
        super(tipo, livello);
    }

    @Override
    public String forma() {
        return "antivenom";
    }

    @Override
    public ResourceLocation texture() {
        return new ResourceLocation(MyMod.MOD_ID, "textures/entity/antivenom/antivenom_symbiote.png");
    }
}
