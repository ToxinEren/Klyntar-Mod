package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Difesa dello scudo simbiotico: con lo scudo alzato il giocatore non viene spostato dai colpi e
 * non subisce danno da quelli che arrivano di fronte.
 *
 * <p>Lo stato dello scudo e' l'obiettivo {@code Venom.Anim.Shield}, acceso da venomshieldstateflag
 * finche' venomblock resta attivo. Il fronte si calcola come nello scudo vanilla: se il vettore che
 * va dalla sorgente al giocatore punta in verso opposto allo sguardo, il colpo arriva davanti.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomShieldDefenseHandler {
    private static final String SHIELD_OBJECTIVE = "Venom.Anim.Shield";

    private VenomShieldDefenseHandler() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isShieldActive(player)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            // veleno, caduta nel vuoto e simili passano comunque, come per lo scudo vanilla
            return;
        }
        Vec3 origin = source.getSourcePosition();
        if (origin != null && isFromFront(player, origin)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isShieldActive(player)) {
            // qui non si guarda la direzione: con lo scudo alzato non lo sposta nessun colpo
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        // le esplosioni spingono scrivendo la velocita' a mano, senza passare da LivingKnockBackEvent
        Vec3 center = event.getExplosion().getPosition();
        event.getAffectedEntities().removeIf(entity -> entity instanceof ServerPlayer player
                && isShieldActive(player)
                && isFromFront(player, center));
    }

    /** stesso criterio dello scudo vanilla: sorgente davanti se punta contro lo sguardo */
    private static boolean isFromFront(Player player, Vec3 sourcePosition) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 toPlayer = sourcePosition.vectorTo(player.position()).normalize();
        return new Vec3(toPlayer.x, 0.0D, toPlayer.z).dot(look) < 0.0D;
    }

    private static boolean isShieldActive(Player player) {
        Objective objective = player.getScoreboard().getObjective(SHIELD_OBJECTIVE);
        if (objective == null) {
            return false;
        }
        return player.getScoreboard()
                .getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore() > 0;
    }
}
