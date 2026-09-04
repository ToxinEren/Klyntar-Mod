package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.block.AlienoFusoBlock;
import modKlyntar.capability.PlayerPowerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Il simbionte sul cadavere afferra chi si avvicina, lo solleva e si fonde con lui.
 *
 * <p>Tre tempi: la presa, la salita, la fusione. Alla fine il potere passa e il corpo che lo
 * portava resta vuoto — succede una volta sola, perche' All-Black e' uno.</p>
 *
 * <p>Il cadavere viene <em>rivendicato</em> da chi lo innesca: senza, due giocatori vicini
 * partirebbero entrambi e il secondo si troverebbe a meta' rituale su un corpo gia' consumato.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class RitualeAllBlack {

    /** quanto dura la presa, prima che cominci a sollevarlo */
    private static final int PRESA = 25;
    /** quanto ci mette a portarlo in alto */
    private static final int SALITA = 45;
    /** quanto resta sospeso mentre il simbionte lo riveste */
    private static final int FUSIONE = 55;
    private static final int TOTALE = PRESA + SALITA + FUSIONE;

    /** quanto in alto lo porta, sopra il cadavere */
    private static final double ALTEZZA = 2.7D;
    /** entro quanto scatta */
    public static final double PORTATA = 3.5D;
    /** quanta parte dello scarto si copre in un tick: piu' alto, piu' secco */
    private static final double AVVICINAMENTO = 0.30D;
    /** il tetto alla spinta, perche' da lontano non parta come una fionda */
    private static final double VELOCITA_MASSIMA = 0.55D;

    private static final Map<UUID, Scena> IN_CORSO = new ConcurrentHashMap<>();
    private static final Set<BlockPos> RIVENDICATI = new HashSet<>();
    private static final Logger LOGGER = LogManager.getLogger("KlyntarRituale");

    private RitualeAllBlack() {
    }

    private static final class Scena {
        final BlockPos cadavere;
        final Vec3 partenza;
        int tick;

        Scena(BlockPos cadavere, Vec3 partenza) {
            this.cadavere = cadavere;
            this.partenza = partenza;
        }
    }

    /** Se questo cadavere ha gia' preso qualcuno. */
    public static boolean rivendicato(BlockPos pos) {
        return RIVENDICATI.contains(pos.immutable());
    }

    /**
     * Chi All-Black puo' prendersi.
     *
     * <p>A differenza degli altri simbionti non gli serve un corpo libero: prende anche chi ne
     * porta gia' uno, e glielo sostituisce. L'unica eccezione e' l'anti-venom, che di mestiere
     * distrugge i simbionti e quindi lo respinge.</p>
     */
    public static boolean puoPrendere(ServerPlayer giocatore) {
        if (giocatore.isCreative() || giocatore.isSpectator()) {
            return false;
        }
        String forma = PlayerPowerCapability.formaSuPalladium(giocatore);
        if (forma == null) {
            return true;                       // nessun simbionte: corpo libero
        }
        if ("antivenom".equals(forma)) {
            return false;                      // l'anti-venom lo rifiuta
        }
        return !"allblack".equals(forma);      // ce l'ha gia'
    }

    /** Se questo giocatore e' gia' in mezzo a un rituale. */
    public static boolean impegnato(ServerPlayer giocatore) {
        return IN_CORSO.containsKey(giocatore.getUUID());
    }

    /** Fa scattare la presa. */
    public static void avvia(ServerPlayer giocatore, ServerLevel livello, BlockPos cadavere) {
        if (impegnato(giocatore) || rivendicato(cadavere)) {
            return;
        }
        RIVENDICATI.add(cadavere.immutable());
        IN_CORSO.put(giocatore.getUUID(), new Scena(cadavere.immutable(), giocatore.position()));
        giocatore.setNoGravity(true);
        livello.playSound(null, cadavere, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS,
                1.6F, 0.6F);
        LOGGER.info("All-Black afferra {} sul cadavere a {}",
                giocatore.getGameProfile().getName(), cadavere);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END
                || !(evento.level instanceof ServerLevel livello)
                || IN_CORSO.isEmpty()) {
            return;
        }
        List<UUID> finiti = new ArrayList<>();
        for (Map.Entry<UUID, Scena> voce : IN_CORSO.entrySet()) {
            ServerPlayer giocatore = livello.getServer().getPlayerList().getPlayer(voce.getKey());
            if (giocatore == null || giocatore.serverLevel() != livello) {
                continue;
            }
            if (avanza(giocatore, livello, voce.getValue())) {
                finiti.add(voce.getKey());
            }
        }
        for (UUID id : finiti) {
            IN_CORSO.remove(id);
        }
    }

    /** Porta avanti una scena di un tick. Ritorna vero quando e' finita. */
    private static boolean avanza(ServerPlayer giocatore, ServerLevel livello, Scena scena) {
        scena.tick++;
        Vec3 centro = Vec3.atCenterOf(scena.cadavere);
        double baseY = scena.cadavere.getY() + 1.0D;

        // in tutte le fasi resta fermo dov'e' il simbionte, non dove vuole lui
        double x = centro.x;
        double z = centro.z;
        double y;
        if (scena.tick <= PRESA) {
            y = Math.max(baseY, scena.partenza.y);
            if (scena.tick % 6 == 0) {
                livello.playSound(null, scena.cadavere, SoundEvents.SLIME_BLOCK_PLACE,
                        SoundSource.BLOCKS, 1.2F, 0.5F);
            }
        } else if (scena.tick <= PRESA + SALITA) {
            double q = (scena.tick - PRESA) / (double) SALITA;
            // parte piano e frena in cima invece di salire a velocita' costante
            double morbido = q * q * (3.0D - 2.0D * q);
            y = baseY + ALTEZZA * morbido;
        } else {
            y = baseY + ALTEZZA;
            // sospeso: trema, e il tremito cresce mentre la fusione si compie
            double avanzamento = (scena.tick - PRESA - SALITA) / (double) FUSIONE;
            double scossa = 0.04D + 0.10D * avanzamento;
            x += (livello.random.nextDouble() - 0.5D) * scossa;
            z += (livello.random.nextDouble() - 0.5D) * scossa;
        }

        // Lo si porta con la VELOCITA', non riposizionandolo.
        //
        // Il teletrasporto a ogni tick era il modo piu' diretto ma sbagliato: rende il
        // movimento a scatti e, cosa peggiore, il pacchetto porta con se' la rotazione e
        // inchioda la telecamera. Spingendolo invece verso il punto voluto il moto e'
        // continuo e la testa resta libera, perche' la velocita' non tocca la rotazione.
        Vec3 bersaglio = new Vec3(x, y, z);
        Vec3 scarto = bersaglio.subtract(giocatore.position());
        Vec3 spinta = scarto.scale(AVVICINAMENTO);
        if (spinta.length() > VELOCITA_MASSIMA) {
            spinta = spinta.normalize().scale(VELOCITA_MASSIMA);
        }
        giocatore.setDeltaMovement(spinta);
        giocatore.hurtMarked = true;
        giocatore.fallDistance = 0.0F;
        // la lentezza gli toglie il passo: senza, camminando contrasterebbe la spinta
        giocatore.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6,
                false, false));

        if (scena.tick < TOTALE) {
            return false;
        }
        compi(giocatore, livello, scena);
        return true;
    }

    /** La fusione: il potere passa, il corpo che lo portava resta vuoto. */
    private static void compi(ServerPlayer giocatore, ServerLevel livello, Scena scena) {
        PlayerPowerCapability.infectPlayer(giocatore, "allblack");
        giocatore.setNoGravity(false);
        giocatore.fallDistance = 0.0F;
        giocatore.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
        giocatore.displayClientMessage(
                Component.literal("All-Black si e' fuso con te."), false);

        livello.playSound(null, giocatore.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.BLOCKS, 2.0F, 0.5F);

        svuota(livello, scena.cadavere);
        LOGGER.info("All-Black e' passato a {}", giocatore.getGameProfile().getName());
    }

    /** Toglie il cadavere: il simbionte se n'e' andato, non c'e' piu' niente da prendere. */
    private static void svuota(ServerLevel livello, BlockPos ancora) {
        livello.setBlock(ancora, Blocks.AIR.defaultBlockState(), 3);
        for (int[] cella : AlienoFusoBlock.SEDIME) {
            livello.setBlock(ancora.offset(cella[0], cella[1], cella[2]),
                    Blocks.AIR.defaultBlockState(), 3);
        }
        RIVENDICATI.remove(ancora);
    }
}
