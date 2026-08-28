package modKlyntar.client;

import modKlyntar.player.VenomPlayerSizeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Riceve dal server l'altezza del simbionte e la applica sul client.
 *
 * <p>Registrare il valore non basta: le dimensioni di un'entita' sono ricalcolate solo quando
 * qualcuno lo chiede, quindi dopo averlo memorizzato va chiamato {@code refreshDimensions()},
 * altrimenti la telecamera in prima persona resta dove stava fino al primo altro motivo di
 * ricalcolo.</p>
 */
public final class ClientVenomSize {

    private ClientVenomSize() {
    }

    public static void applica(int idEntita, int stato) {
        int precedente = VenomPlayerSizeHandler.statoDalServer(idEntita);
        VenomPlayerSizeHandler.aggiornaStatoDalServer(idEntita, stato);
        if (precedente == stato) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entita = minecraft.level.getEntity(idEntita);
        if (entita != null) {
            entita.refreshDimensions();
        }
    }
}
