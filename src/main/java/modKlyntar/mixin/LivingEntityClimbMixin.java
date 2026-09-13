package modKlyntar.mixin;

import modKlyntar.player.VenomSymbioteSystemsHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbMixin {
    // Due nomi per lo stesso metodo: onClimbable in sviluppo, m_6147_ nel jar della release, dove
    // Minecraft e' offuscato. Il nostro jar non porta un refmap che faccia la traduzione, e col
    // solo nome leggibile questo mixin nella release non si e' mai applicato: l'arrampicata
    // funzionava in runClient e basta. La corrispondenza e' presa dalle mappature ufficiali
    // (onClimbable -> i_ -> m_6147_), non indovinata.
    @Inject(method = {"onClimbable", "m_6147_"}, at = @At("HEAD"), cancellable = true)
    private void klyntar$venomClimbsWallsLikeSpider(CallbackInfoReturnable<Boolean> callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (VenomSymbioteSystemsHandler.shouldUseVanillaClimb(entity)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
