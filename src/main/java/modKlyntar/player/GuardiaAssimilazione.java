package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rimette a posto quello che l'assimilazione lascia in giro quando non finisce come deve.
 *
 * <p>L'abilita' (nei JSON dei poteri) congela la preda con NoAI e NoGravity, la marca con
 * {@code venom_feed_target} e la toglie di mezzo all'ultimo tick. Se l'ultimo tick non arriva -
 * il giocatore esce dal server, muore, il server si ferma a meta' - la preda restava congelata
 * a mezz'aria per sempre, e il giocatore restava marcato e nella squadra senza collisioni. In
 * multiplayer capitava anche senza uscire: la pulizia di un Venom toglieva il segno alla preda
 * di un altro Venom vicino, e quella non veniva piu' liberata.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class GuardiaAssimilazione {
    private static final String PREDA = "venom_feed_target";
    private static final String MANGIA = "venom_feeding";
    private static final String SQUADRA = "venom_feed_nocollision";
    /** Lo stesso raggio entro cui l'abilita' cerca la sua preda. */
    private static final double RAGGIO = 16.0D;
    private static final int OGNI = 20;

    private GuardiaAssimilazione() {
    }

    /** Una preda che nessun Venom vicino sta mangiando torna libera. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity preda = event.getEntity();
        if (preda.tickCount % OGNI != 0 || preda.level().isClientSide || !preda.getTags().contains(PREDA)) {
            return;
        }
        boolean qualcunoMangia = !preda.level().getEntitiesOfClass(Player.class, preda.getBoundingBox().inflate(RAGGIO),
                p -> p.isAlive() && p.getTags().contains(MANGIA)).isEmpty();
        if (!qualcunoMangia) {
            preda.removeTag(PREDA);
            if (preda instanceof Mob mob) {
                mob.setNoAi(false);
            }
            preda.setNoGravity(false);
            esciDallaSquadra(preda);
        }
    }

    /** Chi rientra non si porta dietro il segno e la squadra di un'assimilazione interrotta. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        pulisci(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pulisci(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        pulisci(event.getEntity());
    }

    private static void pulisci(Player giocatore) {
        if (giocatore.level().isClientSide) {
            return;
        }
        giocatore.removeTag(MANGIA);
        esciDallaSquadra(giocatore);
    }

    /** Solo dalla squadra dell'assimilazione: quella di un server o di una fazione non si tocca. */
    private static void esciDallaSquadra(LivingEntity entita) {
        Scoreboard tabellone = entita.level().getScoreboard();
        PlayerTeam squadra = tabellone.getPlayerTeam(SQUADRA);
        if (squadra != null && entita.getTeam() == squadra) {
            tabellone.removePlayerFromTeam(entita.getScoreboardName(), squadra);
        }
    }
}
