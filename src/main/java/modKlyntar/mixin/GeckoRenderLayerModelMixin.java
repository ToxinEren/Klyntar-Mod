package modKlyntar.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.compat.geckolib.renderlayer.GeckoLayerState;
import net.threetag.palladium.compat.geckolib.renderlayer.GeckoRenderLayerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.RenderUtils;

@Mixin(value = GeckoRenderLayerModel.class, remap = false)
public abstract class GeckoRenderLayerModelMixin {
    private static final Logger KLYNTAR_LOGGER = LoggerFactory.getLogger("KlyntarVenomRender");
    private static boolean klyntar$loggedHook;
    private static boolean klyntar$loggedLegHook;
    private static boolean klyntar$loggedLeftHook;
    private static boolean klyntar$loggedLeftLegHook;
    private static boolean klyntar$loggedWalkRunReapply;
    private static int klyntar$lastOverrideLogTick = -200;
    private static int klyntar$lastLegOverrideLogTick = -200;
    private static int klyntar$lastLeftOverrideLogTick = -200;
    private static int klyntar$lastLeftLegOverrideLogTick = -200;

    @Shadow(remap = false) protected Entity currentEntity;
    @Shadow(remap = false) protected GeckoLayerState currentState;
    @Shadow(remap = false) protected HumanoidModel<?> baseModel;
    @Shadow(remap = false) protected GeoBone rightArm;
    @Shadow(remap = false) protected GeoBone leftArm;
    @Shadow(remap = false) protected GeoBone rightLeg;
    @Shadow(remap = false) protected GeoBone leftLeg;
    @Shadow(remap = false) public String rightArmBone;
    @Shadow(remap = false) public String leftArmBone;
    @Shadow(remap = false) public String rightLegBone;
    @Shadow(remap = false) public String leftLegBone;

    @Inject(
            method = "renderToBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/model/GeoModel;handleAnimations(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;JLsoftware/bernie/geckolib/core/animation/AnimationState;)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void klyntar$reapplyBaseWalkRunPose(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                                float red, float green, float blue, float alpha, CallbackInfo callbackInfo) {
        AnimationController<GeckoLayerState> controller = klyntar$getVenomActionsController();
        boolean walkOrRun = klyntar$isWalkOrRunAnimation(controller);
        // le varianti senza gambe lasciano il passo al player: vanno riapplicate anche loro,
        // ma solo sulle gambe, perche' braccia e busto devono restare nella posa del colpo
        boolean legFree = klyntar$isLegFreeAnimation(controller) || klyntar$legsShouldFollowPlayer(controller);
        if ((!walkOrRun && !legFree) || this.baseModel == null) {
            return;
        }
        if (!klyntar$loggedWalkRunReapply) {
            klyntar$loggedWalkRunReapply = true;
            KLYNTAR_LOGGER.info("Venom walk/run render reapply active. animation={}", klyntar$getAnimationName(controller.getCurrentRawAnimation()));
        }

        if (walkOrRun && "rightArm_player_anchor".equals(this.rightArmBone) && this.rightArm != null) {
            ModelPart modelPart = this.baseModel.rightArm;
            RenderUtils.matchModelPartRot(modelPart, this.rightArm);
            GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, this.rightArm);
            this.rightArm.updatePosition(modelPart.x + 5.0F, 2.0F - modelPart.y, modelPart.z);
        }

        if (walkOrRun && "leftArm_player_anchor".equals(this.leftArmBone) && this.leftArm != null) {
            ModelPart modelPart = this.baseModel.leftArm;
            RenderUtils.matchModelPartRot(modelPart, this.leftArm);
            GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, this.leftArm);
            this.leftArm.updatePosition(modelPart.x - 5.0F, 2.0F - modelPart.y, modelPart.z);
        }

        if ("rightLeg_player_anchor".equals(this.rightLegBone) && this.rightLeg != null) {
            ModelPart modelPart = this.baseModel.rightLeg;
            RenderUtils.matchModelPartRot(modelPart, this.rightLeg);
            GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, this.rightLeg);
            this.rightLeg.updatePosition(modelPart.x + 2.0F, 12.0F - modelPart.y, modelPart.z);
        }

        if ("leftLeg_player_anchor".equals(this.leftLegBone) && this.leftLeg != null) {
            ModelPart modelPart = this.baseModel.leftLeg;
            RenderUtils.matchModelPartRot(modelPart, this.leftLeg);
            GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, this.leftLeg);
            this.leftLeg.updatePosition(modelPart.x - 2.0F, 12.0F - modelPart.y, modelPart.z);
        }
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/util/RenderUtils;matchModelPartRot(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 2
            ),
            remap = false
    )
    private void klyntar$venomRightArmAnimationOverridesRotation(ModelPart modelPart, CoreGeoBone bone) {
        if (klyntar$shouldSkipVenomRightArmBaseRotation()) {
            bone.updateRotation(0.0F, 0.0F, 0.0F);
        } else {
            RenderUtils.matchModelPartRot(modelPart, bone);
        }
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/threetag/palladium/compat/geckolib/renderlayer/GeckoRenderLayerModel;copyScaleAndVisibility(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 2
            ),
            remap = false
    )
    private void klyntar$venomRightArmAnimationOverridesScale(ModelPart modelPart, CoreGeoBone bone) {
        GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, bone);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/cache/object/GeoBone;updatePosition(FFF)V",
                    ordinal = 2
            ),
            remap = false
    )
    private void klyntar$venomRightArmAnimationOverridesPosition(GeoBone bone, float x, float y, float z) {
        bone.updatePosition(x, y, z);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/util/RenderUtils;matchModelPartRot(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 3
            ),
            remap = false
    )
    private void klyntar$venomLeftArmAnimationOverridesRotation(ModelPart modelPart, CoreGeoBone bone) {
        if (klyntar$shouldSkipVenomLeftArmBaseRotation()) {
            bone.updateRotation(0.0F, 0.0F, 0.0F);
        } else {
            RenderUtils.matchModelPartRot(modelPart, bone);
        }
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/threetag/palladium/compat/geckolib/renderlayer/GeckoRenderLayerModel;copyScaleAndVisibility(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 3
            ),
            remap = false
    )
    private void klyntar$venomLeftArmAnimationOverridesScale(ModelPart modelPart, CoreGeoBone bone) {
        GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, bone);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/cache/object/GeoBone;updatePosition(FFF)V",
                    ordinal = 3
            ),
            remap = false
    )
    private void klyntar$venomLeftArmAnimationOverridesPosition(GeoBone bone, float x, float y, float z) {
        bone.updatePosition(x, y, z);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/util/RenderUtils;matchModelPartRot(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 4
            ),
            remap = false
    )
    private void klyntar$venomRightLegAnimationOverridesRotation(ModelPart modelPart, CoreGeoBone bone) {
        if (klyntar$shouldSkipVenomRightLegBaseRotation()) {
            bone.updateRotation(0.0F, 0.0F, 0.0F);
        } else {
            RenderUtils.matchModelPartRot(modelPart, bone);
        }
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/threetag/palladium/compat/geckolib/renderlayer/GeckoRenderLayerModel;copyScaleAndVisibility(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 4
            ),
            remap = false
    )
    private void klyntar$venomRightLegAnimationOverridesScale(ModelPart modelPart, CoreGeoBone bone) {
        GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, bone);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/cache/object/GeoBone;updatePosition(FFF)V",
                    ordinal = 4
            ),
            remap = false
    )
    private void klyntar$venomRightLegAnimationOverridesPosition(GeoBone bone, float x, float y, float z) {
        bone.updatePosition(x, y, z);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/util/RenderUtils;matchModelPartRot(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 5
            ),
            remap = false
    )
    private void klyntar$venomLeftLegAnimationOverridesRotation(ModelPart modelPart, CoreGeoBone bone) {
        if (klyntar$shouldSkipVenomLeftLegBaseRotation()) {
            bone.updateRotation(0.0F, 0.0F, 0.0F);
        } else {
            RenderUtils.matchModelPartRot(modelPart, bone);
        }
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/threetag/palladium/compat/geckolib/renderlayer/GeckoRenderLayerModel;copyScaleAndVisibility(Lnet/minecraft/client/model/geom/ModelPart;Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;)V",
                    ordinal = 5
            ),
            remap = false
    )
    private void klyntar$venomLeftLegAnimationOverridesScale(ModelPart modelPart, CoreGeoBone bone) {
        GeckoRenderLayerModel.copyScaleAndVisibility(modelPart, bone);
    }

    @Redirect(
            method = "applyBaseTransformations",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/cache/object/GeoBone;updatePosition(FFF)V",
                    ordinal = 5
            ),
            remap = false
    )
    private void klyntar$venomLeftLegAnimationOverridesPosition(GeoBone bone, float x, float y, float z) {
        bone.updatePosition(x, y, z);
    }

    private boolean klyntar$shouldSkipVenomRightArmBaseRotation() {
        if (!"rightArm_player_anchor".equals(this.rightArmBone) || this.currentState == null) {
            return false;
        }
        if (!klyntar$loggedHook) {
            klyntar$loggedHook = true;
            KLYNTAR_LOGGER.info("Venom right arm render hook active for {}", this.currentEntity == null ? "unknown entity" : this.currentEntity.getName().getString());
        }

        AnimationController<GeckoLayerState> controller = klyntar$getVenomActionsController();
        if (controller == null) {
            return false;
        }
        if (klyntar$isWalkOrRunAnimation(controller)) {
            return false;
        }

        boolean triggered = controller.isPlayingTriggeredAnimation() || controller.getTriggeredAnimation() != null;
        if (triggered && this.currentEntity != null && this.currentEntity.tickCount - klyntar$lastOverrideLogTick >= 40) {
            klyntar$lastOverrideLogTick = this.currentEntity.tickCount;
            KLYNTAR_LOGGER.info("Venom right arm vanilla rotation suppressed during triggered animation. state={}, raw={}",
                    controller.getAnimationState(), klyntar$getAnimationName(controller.getTriggeredAnimation()));
        }

        return triggered;
    }

    private boolean klyntar$shouldSkipVenomRightLegBaseRotation() {
        if (!"rightLeg_player_anchor".equals(this.rightLegBone) || this.currentState == null) {
            return false;
        }
        if (!klyntar$loggedLegHook) {
            klyntar$loggedLegHook = true;
            KLYNTAR_LOGGER.info("Venom right leg render hook active for {}", this.currentEntity == null ? "unknown entity" : this.currentEntity.getName().getString());
        }

        AnimationController<GeckoLayerState> controller = klyntar$getVenomActionsController();
        if (controller == null) {
            return false;
        }
        if (klyntar$isWalkOrRunAnimation(controller) || klyntar$isLegFreeAnimation(controller)
                || klyntar$legsShouldFollowPlayer(controller)) {
            return false;
        }

        boolean triggered = controller.isPlayingTriggeredAnimation() || controller.getTriggeredAnimation() != null;
        if (triggered && this.currentEntity != null && this.currentEntity.tickCount - klyntar$lastLegOverrideLogTick >= 40) {
            klyntar$lastLegOverrideLogTick = this.currentEntity.tickCount;
            KLYNTAR_LOGGER.info("Venom right leg vanilla rotation suppressed during triggered animation. state={}, raw={}",
                    controller.getAnimationState(), klyntar$getAnimationName(controller.getTriggeredAnimation()));
        }

        return triggered;
    }

    private boolean klyntar$shouldSkipVenomLeftArmBaseRotation() {
        if (!"leftArm_player_anchor".equals(this.leftArmBone) || this.currentState == null) {
            return false;
        }
        if (!klyntar$loggedLeftHook) {
            klyntar$loggedLeftHook = true;
            KLYNTAR_LOGGER.info("Venom left arm render hook active for {}", this.currentEntity == null ? "unknown entity" : this.currentEntity.getName().getString());
        }

        AnimationController<GeckoLayerState> controller = klyntar$getVenomActionsController();
        if (controller == null || klyntar$isWalkOrRunAnimation(controller)) {
            return false;
        }

        boolean triggered = controller.isPlayingTriggeredAnimation() || controller.getTriggeredAnimation() != null;
        if (triggered && this.currentEntity != null && this.currentEntity.tickCount - klyntar$lastLeftOverrideLogTick >= 40) {
            klyntar$lastLeftOverrideLogTick = this.currentEntity.tickCount;
            KLYNTAR_LOGGER.info("Venom left arm vanilla rotation suppressed during triggered animation. state={}, raw={}",
                    controller.getAnimationState(), klyntar$getAnimationName(controller.getTriggeredAnimation()));
        }

        return triggered;
    }

    private boolean klyntar$shouldSkipVenomLeftLegBaseRotation() {
        if (!"leftLeg_player_anchor".equals(this.leftLegBone) || this.currentState == null) {
            return false;
        }
        if (!klyntar$loggedLeftLegHook) {
            klyntar$loggedLeftLegHook = true;
            KLYNTAR_LOGGER.info("Venom left leg render hook active for {}", this.currentEntity == null ? "unknown entity" : this.currentEntity.getName().getString());
        }

        AnimationController<GeckoLayerState> controller = klyntar$getVenomActionsController();
        if (controller == null || klyntar$isWalkOrRunAnimation(controller) || klyntar$isLegFreeAnimation(controller)
                || klyntar$legsShouldFollowPlayer(controller)) {
            return false;
        }

        boolean triggered = controller.isPlayingTriggeredAnimation() || controller.getTriggeredAnimation() != null;
        if (triggered && this.currentEntity != null && this.currentEntity.tickCount - klyntar$lastLeftLegOverrideLogTick >= 40) {
            klyntar$lastLeftLegOverrideLogTick = this.currentEntity.tickCount;
            KLYNTAR_LOGGER.info("Venom left leg vanilla rotation suppressed during triggered animation. state={}, raw={}",
                    controller.getAnimationState(), klyntar$getAnimationName(controller.getTriggeredAnimation()));
        }

        return triggered;
    }

    private AnimationController<GeckoLayerState> klyntar$getVenomActionsController() {
        return this.currentState == null ? null : this.currentState.getController("venom_actions");
    }

    private boolean klyntar$isWalkOrRunAnimation(AnimationController<GeckoLayerState> controller) {
        if (controller == null) {
            return false;
        }

        String currentName = klyntar$getAnimationName(controller.getCurrentRawAnimation());
        String triggeredName = klyntar$getAnimationName(controller.getTriggeredAnimation());
        return klyntar$isWalkOrRunAnimationName(currentName) || klyntar$isWalkOrRunAnimationName(triggeredName);
    }

    /**
     * Le copie ".nolegs" delle animazioni d'attacco non toccano le ossa delle gambe apposta: qui
     * gli anchor devono continuare a seguire il player, altrimenti il passo sparisce comunque.
     */
    /**
     * Le gambe non vanno bloccate mentre il player cammina: la lista bianca per nome non basta,
     * perche' nei tick fra il clic e l'arrivo del trigger al client resta in scena l'idle, che
     * congelerebbe il passo proprio all'inizio del colpo. Vale solo per attacchi e idle: le
     * animazioni di arrampicata e presa le gambe devono poterle piantare davvero.
     */
    private boolean klyntar$legsShouldFollowPlayer(AnimationController<GeckoLayerState> controller) {
        if (controller == null || !(this.currentEntity instanceof LivingEntity living)) {
            return false;
        }
        if (living.walkAnimation.speed() <= 0.01F) {
            return false;
        }

        return klyntar$isAttackOrIdle(klyntar$getAnimationName(controller.getCurrentRawAnimation()))
                || klyntar$isAttackOrIdle(klyntar$getAnimationName(controller.getTriggeredAnimation()));
    }

    private boolean klyntar$isAttackOrIdle(String animationName) {
        return animationName.contains(".attack.") || animationName.startsWith("animation.venom.idle");
    }

    private boolean klyntar$isLegFreeAnimation(AnimationController<GeckoLayerState> controller) {
        if (controller == null) {
            return false;
        }

        return klyntar$getAnimationName(controller.getCurrentRawAnimation()).endsWith(".nolegs")
                || klyntar$getAnimationName(controller.getTriggeredAnimation()).endsWith(".nolegs");
    }

    private boolean klyntar$isWalkOrRunAnimationName(String animationName) {
        return "animation.venom.walk".equals(animationName) || "animation.venom.run.java_combined".equals(animationName);
    }

    private String klyntar$getAnimationName(RawAnimation animation) {
        if (animation == null || animation.getAnimationStages().isEmpty()) {
            return "";
        }

        return animation.getAnimationStages().get(0).animationName();
    }
}
