package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.worldgen.CratereAllBlack;
import modKlyntar.worldgen.DatiCratere;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Fa comparire il cratere di All-Black una volta sola per mondo.
 *
 * <p>Non al primo ingresso di un giocatore ma qualche secondo dopo, perche' posare la struttura
 * costringe a generare una manciata di chunk lontani: farlo mentre il giocatore sta ancora
 * entrando si sente come uno scatto.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class CratereHandler {

    /** quanto aspettare, in tick, prima di posare */
    private static final int ATTESA = 20 * 8;

    private static final Random SORTE = new Random();
    private static final Logger LOGGER = LogManager.getLogger("KlyntarCratere");

    private static int attesa = -1;

    private CratereHandler() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END
                || !(evento.level instanceof ServerLevel livello)
                || livello.dimension() != Level.OVERWORLD) {
            return;
        }
        if (livello.players().isEmpty()) {
            attesa = -1;
            return;
        }

        DatiCratere dati = DatiCratere.di(livello);
        if (dati.posato()) {
            return;
        }
        if (attesa < 0) {
            attesa = ATTESA;
            return;
        }
        if (--attesa > 0) {
            return;
        }

        BlockPos dove = CratereAllBlack.posa(livello, SORTE);
        if (dove == null) {
            // nessun posto adatto in questo giro: non si segna niente, cosi' si riprova piu'
            // avanti invece di dare il cratere per posato e perderlo per sempre
            attesa = ATTESA;
            return;
        }
        dati.segna(dove);
        LOGGER.info("Il cratere di All-Black esiste ora a {} e non verra' piu' rigenerato", dove);
    }
}
