package modKlyntar.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import modKlyntar.client.renderer.BraccioPrimaPersona;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.threetag.palladium.compat.geckolib.renderlayer.GeckoRenderLayer;
import net.threetag.palladium.util.context.DataContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Il punto fra le animazioni e il disegno del corpo in prima persona.
 *
 * <p>Palladium, in {@code renderArm}, applica le animazioni GeckoLib alle ossa e subito dopo le
 * disegna: prima non si puo' toccare niente (le animazioni sovrascrivono) e dopo e' tardi.
 * Qui si passa la mano a {@link BraccioPrimaPersona}, che alza le braccia attorno alla spalla
 * solo quando il disegno l'ha chiesto lei.</p>
 *
 * <p>{@code renderArm} e' un metodo di Palladium, non di Minecraft: il nome e' lo stesso in
 * sviluppo e nel jar della release, e il bersaglio {@code handleAnimations} e' di GeckoLib, che
 * non viene rimappato. Per questo, a differenza di {@link GeckoRenderLayerModelMixin}, non serve
 * il doppio nome SRG.</p>
 */
@Mixin(value = GeckoRenderLayer.class, remap = false)
public abstract class GeckoRenderLayerPrimaPersonaMixin {

    @Inject(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/model/GeoModel;handleAnimations(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;JLsoftware/bernie/geckolib/core/animation/AnimationState;)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void klyntar$dopoLeAnimazioni(DataContext contesto, HumanoidArm braccio, PlayerRenderer renderer,
                                          PoseStack pila, MultiBufferSource buffer, int luce, CallbackInfo info) {
        BraccioPrimaPersona.dopoLeAnimazioni((GeckoRenderLayer) (Object) this);
    }
}
