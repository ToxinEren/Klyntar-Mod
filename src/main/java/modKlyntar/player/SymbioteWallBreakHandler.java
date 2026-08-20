package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Il pugno del simbionte apre buchi tre per tre.
 *
 * <p>Passivo di tutte le forme, ma solo col corpo simbionte fuori: quando un giocatore cosi'
 * trasformato spacca un blocco, se ne portano via anche gli otto attorno, sul piano
 * perpendicolare a dove sta guardando — verticale se guarda davanti a se', orizzontale se
 * guarda in alto o in basso.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteWallBreakHandler {
    /** i blocchi che non cedono nemmeno al simbionte */
    private static final float DUREZZA_INFRANGIBILE = -1.0F;
    /** evita che le rotture a catena richiamino se stesse all'infinito */
    private static final ThreadLocal<Boolean> IN_CORSO = ThreadLocal.withInitial(() -> false);

    private SymbioteWallBreakHandler() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (IN_CORSO.get() || event.isCanceled()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer giocatore) || giocatore.isCreative()) {
            return;
        }
        if (!SymbioteState.haSimbionte(giocatore) || VenomSymbioteSystemsHandler.isPlayerVulnerable(giocatore)) {
            return;
        }
        // il pugno che sfonda e' quello del corpo simbionte: senza "Unleash the symbiote"
        // il giocatore ha ancora le sue mani e rompe un blocco per volta
        if (!SymbioteMiningHandler.corpoAttivo(giocatore)) {
            return;
        }
        if (!(event.getLevel() instanceof Level livello)) {
            return;
        }

        BlockPos centro = event.getPos();
        Direction.Axis asse = asseDiSguardo(giocatore);

        IN_CORSO.set(true);
        try {
            for (BlockPos posizione : intorno(centro, asse)) {
                rompi(livello, giocatore, posizione);
            }
        } finally {
            IN_CORSO.set(false);
        }
    }

    /**
     * L'asse lungo cui il buco NON si estende: e' quello che il giocatore sta guardando.
     * Guardando in alto o in basso il piano diventa orizzontale, altrimenti resta verticale.
     */
    private static Direction.Axis asseDiSguardo(ServerPlayer giocatore) {
        Vec3 sguardo = giocatore.getLookAngle();
        double x = Math.abs(sguardo.x);
        double y = Math.abs(sguardo.y);
        double z = Math.abs(sguardo.z);
        if (y >= x && y >= z) {
            return Direction.Axis.Y;
        }
        return x >= z ? Direction.Axis.X : Direction.Axis.Z;
    }

    /** Gli otto blocchi attorno al centro, sul piano perpendicolare all'asse dato. */
    private static Iterable<BlockPos> intorno(BlockPos centro, Direction.Axis asse) {
        java.util.List<BlockPos> posizioni = new java.util.ArrayList<>(8);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                posizioni.add(switch (asse) {
                    case Y -> centro.offset(a, 0, b);
                    case X -> centro.offset(0, a, b);
                    case Z -> centro.offset(a, b, 0);
                });
            }
        }
        return posizioni;
    }

    private static void rompi(Level livello, ServerPlayer giocatore, BlockPos posizione) {
        BlockState stato = livello.getBlockState(posizione);
        if (stato.isAir() || !livello.getFluidState(posizione).isEmpty()) {
            return;
        }
        if (stato.getDestroySpeed(livello, posizione) == DUREZZA_INFRANGIBILE) {
            return;
        }
        // destroyBlock con il giocatore fa cadere i drop giusti per l'attrezzo che ha in mano
        giocatore.gameMode.destroyBlock(posizione);
    }
}
