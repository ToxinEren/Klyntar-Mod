package modKlyntar.player;

import modKlyntar.MyMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * L'invisibilita' del simbionte, accesa dal tasto dedicato.
 *
 * <p>L'effetto lo mette il <b>server</b>, non il client: applicato di la' viene sincronizzato da
 * Minecraft a tutti quanti, quindi in multiplayer gli altri smettono davvero di vederti. Prima
 * lo scriveva solo il client, e sparivi soltanto ai tuoi occhi.</p>
 *
 * <p>Non serve un pacchetto per dire agli altri client di nascondere il corpo del simbionte:
 * l'effetto invisibilita' e' gia' sincronizzato, e i superpoteri li sincronizza Palladium per
 * disegnare i suoi render layer. Ogni client ha percio' tutto quello che gli serve per decidere
 * da solo, guardando {@link #invisibileDaSimbionte}.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SymbioteInvisibilityHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarInvisibilita");

    /** ricorda che l'invisibilita' addosso e' nostra e non di una pozione */
    private static final String CHIAVE = "klyntar.invisibilita";

    private SymbioteInvisibilityHandler() {
    }

    /** Porta Venom o la sua evoluzione Spidey? Vale da tutte e due le parti. */
    public static boolean portaSimbionte(Player giocatore) {
        Collection<?> poteri = SuperpowerUtil.getSuperpowerIds(giocatore);
        if (poteri == null) {
            return false;
        }
        for (Object potere : poteri) {
            String id = String.valueOf(potere);
            if (id.endsWith(":venom") || id.endsWith(":venomspidey")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Va nascosto il corpo del simbionte di questo giocatore?
     *
     * <p>Lo chiedono i renderer, che girano su ogni client anche per gli altri giocatori. Non
     * distingue l'invisibilita' nostra da quella di una pozione, ed e' voluto: se un simbionte
     * e' invisibile, il suo corpo non deve vedersi comunque ci sia riuscito.</p>
     */
    public static boolean invisibileDaSimbionte(Player giocatore) {
        return giocatore.hasEffect(MobEffects.INVISIBILITY) && portaSimbionte(giocatore);
    }

    /** Accende o spegne l'invisibilita'. La chiama il pacchetto che arriva dal tasto. */
    public static void alterna(ServerPlayer giocatore) {
        if (!portaSimbionte(giocatore)) {
            return;
        }
        if (giocatore.hasEffect(MobEffects.INVISIBILITY)) {
            giocatore.removeEffect(MobEffects.INVISIBILITY);
            giocatore.getPersistentData().remove(CHIAVE);
            giocatore.displayClientMessage(Component.literal("Invisibility disabled"), false);
        } else {
            // senza particelle e senza icona: il simbionte non brilla
            giocatore.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
            giocatore.getPersistentData().putBoolean(CHIAVE, true);
            giocatore.displayClientMessage(Component.literal("Invisibility enabled"), false);
        }
        LOGGER.debug("Invisibilita' del simbionte per {}: {}",
                giocatore.getGameProfile().getName(), giocatore.hasEffect(MobEffects.INVISIBILITY));
    }

    /**
     * Perdendo il simbionte si perde anche l'invisibilita'.
     *
     * <p>Il tasto richiede il potere, quindi senza questo chi si detrasforma da invisibile
     * resterebbe invisibile per sempre, senza piu' modo di spegnerla.</p>
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)) {
            return;
        }
        if (!giocatore.getPersistentData().getBoolean(CHIAVE)) {
            return;
        }
        if (!giocatore.hasEffect(MobEffects.INVISIBILITY)) {
            // l'ha tolta qualcos'altro, per esempio il latte: il ricordo non serve piu'
            giocatore.getPersistentData().remove(CHIAVE);
            return;
        }
        if (!portaSimbionte(giocatore)) {
            giocatore.removeEffect(MobEffects.INVISIBILITY);
            giocatore.getPersistentData().remove(CHIAVE);
            LOGGER.debug("Invisibilita' tolta a {}: non porta piu' il simbionte",
                    giocatore.getGameProfile().getName());
        }
    }
}
