package modKlyntar.entity.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.TentacleSegmentEntity;

public class TentacleSegmentModel<T extends TentacleSegmentEntity> extends EntityModel<T> {
    private final ModelPart segment;

    public TentacleSegmentModel(ModelPart root) {
        this.segment = root.getChild("segment");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartPose partpose = PartPose.ZERO;
        CubeListBuilder cubelistbuilder = CubeListBuilder.create()
            .addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 16.0F, new CubeDeformation(0.0F));
        meshdefinition.getRoot().addOrReplaceChild("segment", cubelistbuilder, partpose);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // No animation for now
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        segment.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
