package modKlyntar.item;

import java.util.function.Consumer;

import modKlyntar.MyMod;
import modKlyntar.client.renderer.item.CapsuleItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Capsula renderizzata con un modello GeckoLib invece della sprite piatta. */
public class CapsuleItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.klyntars.capsule.idle");
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final String guiModelName;
    private final boolean animated;

    public CapsuleItem(Properties properties, String modelName, boolean animated) {
        super(properties);
        this.model = new ResourceLocation(MyMod.MOD_ID, "geo/" + modelName + ".geo.json");
        this.texture = new ResourceLocation(MyMod.MOD_ID, "textures/item/capsule_model.png");
        this.guiModelName = modelName + "_gui";
        this.animated = animated;
    }

    public String getGuiModelName() {
        return this.guiModelName;
    }

    public ResourceLocation getModelResource() {
        return this.model;
    }

    public ResourceLocation getTextureResource() {
        return this.texture;
    }

    /** solo la capsula piena ha il simbionte che si muove dentro */
    public ResourceLocation getAnimationResource() {
        return this.animated ? new ResourceLocation(MyMod.MOD_ID, "animations/capsule.animation.json") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (!this.animated) {
            return;
        }
        controllers.add(new AnimationController<>(this, "capsule", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CapsuleItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new CapsuleItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
