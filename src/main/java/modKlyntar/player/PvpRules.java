package modKlyntar.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * Chi, fra i giocatori, i tentacoli possono prendere di mira.
 *
 * <p>Le abilita' di combattimento sceglievano i bersagli fra i mostri e i mob che ce l'avevano
 * col giocatore, e gli altri giocatori li scartavano a priori: in multiplayer il grab, il fend
 * off, il pull e lo strike passavano oltre chiunque avesse un nome. Questa e' l'unica regola,
 * condivisa, per decidere quando un giocatore vale come bersaglio: cosi' le abilita' non
 * divergono fra loro sui casi limite.</p>
 *
 * <p>Vale la stessa etichetta di Minecraft per il PvP: niente se il server lo vieta, niente
 * contro i compagni di squadra, mai contro chi e' in creativa o spettatore.</p>
 */
public final class PvpRules {

    private PvpRules() {
    }

    public static boolean colpibile(Player attaccante, Player bersaglio) {
        if (bersaglio == null || bersaglio == attaccante || !bersaglio.isAlive()) {
            return false;
        }
        if (bersaglio.isSpectator() || bersaglio.isCreative()) {
            return false;
        }
        MinecraftServer server = bersaglio.getServer();
        if (server != null && !server.isPvpAllowed()) {
            return false;
        }
        return !attaccante.isAlliedTo(bersaglio);
    }
}
