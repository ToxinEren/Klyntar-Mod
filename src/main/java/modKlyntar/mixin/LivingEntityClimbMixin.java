package modKlyntar.mixin;

import modKlyntar.player.VenomSymbioteSystemsHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbMixin {
    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void klyntar$venomClimbsWallsLikeSpider(CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (VenomSymbioteSystemsHandler.shouldUseVanillaClimb(entity)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
