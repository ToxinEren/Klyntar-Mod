package modKlyntar.entity.client.model;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.PrimedPromethiumXTnt;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PrimedPromethiumXTntModel extends GeoModel<PrimedPromethiumXTnt> {

	@Override
	public ResourceLocation getAnimationResource(PrimedPromethiumXTnt animatable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceLocation getModelResource(PrimedPromethiumXTnt object) {
		return new ResourceLocation(MyMod.MOD_ID, "geo/primed_promethiumx_tnt.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PrimedPromethiumXTnt object) {
		return new ResourceLocation(MyMod.MOD_ID, "textures/entity/primed_promethiumx_tnt.png");
	}

}
