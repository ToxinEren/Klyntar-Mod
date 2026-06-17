package modKlyntar.entity.client.model;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.VenomEntity;
import modKlyntar.entity.custom.VenomPlayerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VenomPlayerModel extends GeoModel<VenomPlayerEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(MyMod.MOD_ID, "venom"), "main");

    @Override
    public ResourceLocation getAnimationResource(VenomPlayerEntity animatable) {
        return new ResourceLocation(MyMod.MOD_ID, "animations/venom.animation.json");
    }
    @Override
    public ResourceLocation getModelResource(VenomPlayerEntity object) {
        return new ResourceLocation(MyMod.MOD_ID, "geo/venom.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(VenomPlayerEntity object) {
        return new ResourceLocation(MyMod.MOD_ID, "textures/entity/venom/venom.png");
    }

}
