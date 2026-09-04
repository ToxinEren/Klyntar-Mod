package modKlyntar.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import modKlyntar.MyMod;
import modKlyntar.block.AlienoFusoBlock;
import modKlyntar.block.DioMortoBlock;
import modKlyntar.worldgen.DatiCratere;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandi di servizio per il cratere di All-Black.
 *
 * <p>{@code /klyntarcratere qui} posa i due cadaveri davanti al giocatore, per guardarli senza
 * dover cercare la struttura vera; {@code /klyntarcratere dove} dice se e dove il mondo l'ha
 * gia' generata; {@code /klyntarcratere dimentica} cancella il promemoria, cosi' il mondo la
 * rigenera come se fosse la prima volta.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class CratereCommand {

    private CratereCommand() {
    }

    @SubscribeEvent
    public static void onRegistraComandi(RegisterCommandsEvent evento) {
        LiteralArgumentBuilder<CommandSourceStack> radice = Commands.literal("klyntarcratere")
                .requires(sorgente -> sorgente.hasPermission(2));

        radice.then(Commands.literal("qui").executes(contesto -> {
            ServerPlayer giocatore = contesto.getSource().getPlayerOrException();
            BlockPos dove = giocatore.blockPosition();
            posa(giocatore.serverLevel(), dove);
            contesto.getSource().sendSuccess(
                    () -> Component.literal("Cadaveri posati a " + dove.toShortString()), false);
            return 1;
        }));

        radice.then(Commands.literal("dove").executes(contesto -> {
            ServerLevel livello = contesto.getSource().getLevel();
            DatiCratere dati = DatiCratere.di(livello);
            String testo = dati.posato() && dati.dove() != null
                    ? "Il cratere sta a " + dati.dove().toShortString()
                    : "Il cratere non e' ancora comparso in questo mondo";
            contesto.getSource().sendSuccess(() -> Component.literal(testo), false);
            return 1;
        }));

        radice.then(Commands.literal("dimentica").executes(contesto -> {
            ServerLevel livello = contesto.getSource().getLevel();
            DatiCratere.di(livello).dimentica();
            contesto.getSource().sendSuccess(
                    () -> Component.literal("Promemoria cancellato: il cratere verra' rigenerato"),
                    false);
            return 1;
        }));

        evento.getDispatcher().register(radice);
    }

    /** I due corpi soli, senza scavare la conca: serve a guardarli, non a rifare la struttura. */
    private static void posa(ServerLevel livello, BlockPos dove) {
        Block pezzoDio = MyMod.PEZZO_DIO.get();
        Block pezzoAlieno = MyMod.PEZZO_ALIENO.get();

        BlockPos ancoraDio = dove.offset(3, 0, -3);
        livello.setBlock(ancoraDio, MyMod.DIO_MORTO.get().defaultBlockState(), 3);
        for (int[] cella : DioMortoBlock.SEDIME) {
            BlockPos p = ancoraDio.offset(cella[0], cella[1], cella[2]);
            if (!p.equals(ancoraDio)) {
                livello.setBlock(p, pezzoDio.defaultBlockState(), 3);
            }
        }

        BlockPos ancoraAlieno = dove.offset(-4, 0, 0);
        livello.setBlock(ancoraAlieno, MyMod.ALIENO_FUSO.get().defaultBlockState(), 3);
        for (int[] cella : AlienoFusoBlock.SEDIME) {
            livello.setBlock(ancoraAlieno.offset(cella[0], cella[1], cella[2]),
                    pezzoAlieno.defaultBlockState(), 3);
        }
    }
}
