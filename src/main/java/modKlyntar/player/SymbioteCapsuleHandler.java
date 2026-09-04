package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.entity.custom.ThrownCapsuleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Le due meta' della vita di una capsula: catturare un simbionte e rilanciarlo fuori.
 *
 * <p>Sta in un gestore di eventi invece che dentro gli oggetti perche' le quattro capsule
 * registrate non condividono una classe: due sono {@code CapsuleItem} con modello GeckoLib,
 * due sono oggetti semplici.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteCapsuleHandler {
    /** quanto forte parte la capsula lanciata */
    private static final float VELOCITA = 1.5F;
    private static final float IMPRECISIONE = 1.0F;

    private SymbioteCapsuleHandler() {
    }

    /** Capsula vuota in mano su un simbionte: il simbionte finisce dentro. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player giocatore = event.getEntity();
        ItemStack inMano = giocatore.getItemInHand(event.getHand());
        if (!inMano.is(MyMod.CAPSULE.get()) || !(event.getTarget() instanceof SymbioteEntity simbionte)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (giocatore.level().isClientSide) {
            return;
        }

        simbionte.discard();
        giocatore.level().playSound(null, giocatore.blockPosition(),
                SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0F, 1.0F);

        ItemStack piena = new ItemStack(capsulaPer(simbionte));
        if (!giocatore.isCreative()) {
            inMano.shrink(1);
        }
        if (!giocatore.addItem(piena)) {
            giocatore.drop(piena, false);
        }
    }

    /**
     * La capsula che spetta al simbionte catturato.
     *
     * <p>Solo le forme colorate dichiarano una forma propria; il mob {@code symbiote} di base
     * e' venom, e resta il ripiego anche per chi ne eredita senza essere una forma, come il
     * frammento di Grendel.</p>
     */
    private static Item capsulaPer(SymbioteEntity simbionte) {
        // in questa release esiste solo venom: ogni simbionte catturato da la sua capsula
        return MyMod.VENOM_CAPSULE.get();
    }

    /** Capsula piena in mano: si lancia come una palla di neve. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player giocatore = event.getEntity();
        ItemStack inMano = event.getItemStack();
        if (!inMano.is(MyMod.VENOM_CAPSULE.get())) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(giocatore.level() instanceof ServerLevel livello)) {
            return;
        }

        ThrownCapsuleEntity capsula = new ThrownCapsuleEntity(livello, giocatore);
        capsula.setItem(inMano.copyWithCount(1));
        capsula.shootFromRotation(giocatore, giocatore.getXRot(), giocatore.getYRot(),
                0.0F, VELOCITA, IMPRECISIONE);
        livello.addFreshEntity(capsula);
        livello.playSound(null, giocatore.blockPosition(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.4F);

        if (!giocatore.isCreative()) {
            inMano.shrink(1);
        }
        giocatore.getCooldowns().addCooldown(inMano.getItem(), 10);
    }
}
