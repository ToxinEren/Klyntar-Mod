package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.effect.ModEffects;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contagio e conseguenze dell'effetto Anti-Venom.
 *
 * <p>Chi porta un simbionte e viene toccato da Anti-Venom o dalle sue abilita' si becca il
 * veleno per due minuti, e per tutta la durata resta indebolito come se fosse stato preso dal
 * fuoco o dal suono. Presa tre volte in venti minuti, la terza strappa via il simbionte, che
 * ricompare a tre blocchi di distanza.</p>
 *
 * <p>Il conteggio sta sull'evento che aggiunge l'effetto, non su chi lo infligge: cosi' vale
 * qualunque strada — abilita', colpo in mischia o {@code /effect give} a mano.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class AntiVenomEffectHandler {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarAntiVenom");

    /** due minuti di gioco, il minimo chiesto */
    public static final int DURATA = 20 * 60 * 2;
    /** quante prese servono per perdere il simbionte */
    private static final int PRESE_PER_ESPULSIONE = 3;
    /** la finestra entro cui le prese si sommano: cinque minuti dalla prima */
    private static final int FINESTRA = 20 * 60 * 5;

    private static final String PRESE = "Klyntar.AntiVenom.Hits";
    private static final String FINESTRA_OBJ = "Klyntar.AntiVenom.Window";
    /** 1 finche' il giocatore e' Anti-Venom: serve a purificare una volta sola per volta */
    private static final String PURIFICATO = "Klyntar.AntiVenom.Purified";

    private AntiVenomEffectHandler() {
    }

    /**
     * Applica il veleno a chi porta un simbionte. Il conteggio lo fa
     * {@link #onEffectAdded}, che scatta comunque l'effetto arrivi.
     *
     * @return true se il bersaglio poteva essere colpito
     */
    public static boolean colpisci(LivingEntity bersaglio) {
        if (!SymbioteState.haSimbionte(bersaglio) || SymbioteState.isAntiVenom(bersaglio)) {
            return false;
        }
        bersaglio.addEffect(new MobEffectInstance(ModEffects.ANTI_VENOM.get(), DURATA, 0, false, true));
        return true;
    }

    /** Colpisce tutti i portatori di simbionte in una lista, saltando chi lancia l'abilita'. */
    public static void colpisci(Iterable<? extends LivingEntity> bersagli, LivingEntity lanciatore) {
        if (!SymbioteState.isAntiVenom(lanciatore)) {
            return;
        }
        for (LivingEntity bersaglio : bersagli) {
            if (bersaglio != lanciatore) {
                colpisci(bersaglio);
            }
        }
    }

    /**
     * Guarda il superpotere che il giocatore ha davvero addosso, e purifica quando diventa
     * Anti-Venom.
     *
     * <p>Non basta agganciarsi al comando di trasformazione: il potere puo' arrivare anche
     * dalla barra di Palladium o da un suo comando, strade che la trasformazione nostra non
     * attraversa mai. Qui si guarda lo stato, non chi lo ha cambiato.</p>
     *
     * <p>Il segnaposto evita di ripulire a ogni tick: si purifica all'ingresso nella forma,
     * cosi' fuoco e suono continuano a poter indebolire un Anti-Venom gia' trasformato.</p>
     */
    private static void controllaPurificazione(ServerPlayer giocatore) {
        boolean antiVenom = SymbioteState.isAntiVenom(giocatore);
        boolean gia = SymbioteState.getScore(giocatore, PURIFICATO) > 0;

        if (antiVenom && !gia) {
            SymbioteState.setScore(giocatore, PURIFICATO, 1);
            VenomSymbioteSystemsHandler.clearWeakness(giocatore);
            purifica(giocatore);
            SymbioteImmunityHandler.sciogliMalus(giocatore);
            LOGGER.info("Anti-Venom ha ripulito ogni indebolimento di {}",
                    giocatore.getGameProfile().getName());
        } else if (!antiVenom && gia) {
            SymbioteState.setScore(giocatore, PURIFICATO, 0);
        }
    }

    /**
     * Toglie di dosso l'Anti-Venom e azzera le prese contate.
     * Il suo stesso veleno non tocca chi lo indossa.
     */
    public static void purifica(ServerPlayer giocatore) {
        giocatore.removeEffect(ModEffects.ANTI_VENOM.get());
        SymbioteState.setScore(giocatore, PRESE, 0);
        SymbioteState.setScore(giocatore, FINESTRA_OBJ, 0);
    }

    /** Ogni arrivo dell'effetto e' una presa, da qualunque parte venga. */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() != ModEffects.ANTI_VENOM.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer giocatore)) {
            return;
        }
        if (!SymbioteState.haSimbionte(giocatore) || SymbioteState.isAntiVenom(giocatore)) {
            return;
        }

        // l'Anti-Venom indebolisce il simbionte come il fuoco e il suono: stessa durata
        VenomSymbioteSystemsHandler.applyAntiVenomWeakness(giocatore);

        // la finestra parte alla prima presa e non si allunga: dopo cinque minuti il
        // conteggio si azzera comunque, quante che siano state le prese nel frattempo
        int prese = SymbioteState.getScore(giocatore, PRESE) + 1;
        SymbioteState.setScore(giocatore, PRESE, prese);
        if (SymbioteState.getScore(giocatore, FINESTRA_OBJ) <= 0) {
            SymbioteState.setScore(giocatore, FINESTRA_OBJ, FINESTRA);
        }
        LOGGER.info("Anti-Venom preso da {}: {}/{}",
                giocatore.getGameProfile().getName(), prese, PRESE_PER_ESPULSIONE);

        if (prese < PRESE_PER_ESPULSIONE) {
            giocatore.displayClientMessage(Component.literal(
                    "Il simbionte si contorce (" + prese + "/" + PRESE_PER_ESPULSIONE + ")"), true);
            return;
        }

        SymbioteState.setScore(giocatore, PRESE, 0);
        SymbioteState.setScore(giocatore, FINESTRA_OBJ, 0);
        LOGGER.info("Anti-Venom ha espulso il simbionte di {}", giocatore.getGameProfile().getName());
        PlayerPowerCapability.revertPlayer(giocatore);
        VenomSymbioteSystemsHandler.spawnSymbioteNearPlayer(giocatore);
        giocatore.displayClientMessage(Component.literal("Il simbionte ti ha abbandonato"), true);
    }

    /** Un colpo in mischia di Anti-Venom vale come tocco. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attaccante
                && SymbioteState.isAntiVenom(attaccante)) {
            colpisci(event.getEntity());
        }
    }

    /** Sorveglia il passaggio ad Anti-Venom e fa scadere la finestra delle prese. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)) {
            return;
        }

        controllaPurificazione(giocatore);

        int rimasti = SymbioteState.getScore(giocatore, FINESTRA_OBJ);
        if (rimasti <= 0) {
            return;
        }
        rimasti--;
        SymbioteState.setScore(giocatore, FINESTRA_OBJ, rimasti);
        if (rimasti == 0) {
            SymbioteState.setScore(giocatore, PRESE, 0);
        }
    }
}
