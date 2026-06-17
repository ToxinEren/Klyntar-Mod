package modKlyntar.entity.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.model.GeoModel;


public class Symbiote extends GeoModel<SymbioteEntity> {

	@Override
	public ResourceLocation getAnimationResource(SymbioteEntity animatable) {
		// TODO Auto-generated method stub
		return new ResourceLocation(MyMod.MOD_ID,"animations/symbiote.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SymbioteEntity object) {
		// TODO Auto-generated method stub
		return new ResourceLocation(MyMod.MOD_ID,"geo/symbiote.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SymbioteEntity object) {
		// TODO Auto-generated method stub
		return new ResourceLocation(MyMod.MOD_ID,"textures/entity/symbiote/symbiote.png");
	}
	
}

/**/
