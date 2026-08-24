package modKlyntar.entity.custom;

import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.power.PlayersPowerProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Un simbionte comune che porta una forma diversa da Venom.
 *
 * <p>Corpo, movenze e comportamento sono quelli del simbionte di sempre: cerca un ospite e al
 * contatto ci si infila dentro. Cambia solo di chi e' il legame che lascia.</p>
 */
public abstract class SimbionteColoratoEntity extends SymbioteEntity {
    protected SimbionteColoratoEntity(EntityType<? extends Mob> tipo, Level livello) {
        super(tipo, livello);
    }

    /** La forma che questo simbionte impianta: venom, carnage, antivenom, toxin. */
    public abstract String forma();

    /** La pelle con cui va disegnato. */
    public abstract ResourceLocation texture();

    @Override
    public void doPlayerEffect(ServerPlayer giocatore) {
        giocatore.displayClientMessage(
                Component.literal("Un simbionte si e' legato a te."), false);
        PlayerPowerCapability.infectPlayer(giocatore, forma());
        giocatore.getCapability(PlayersPowerProvider.PLAYERS_POWER)
                .ifPresent(potere -> potere.addSymbiote(1));
    }
}
