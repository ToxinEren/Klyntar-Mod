package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import modKlyntar.sound.SuoniKlyntar;
import modKlyntar.symbiote.SymbioteState;
import modKlyntar.symbiote.VoceSimbionte;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il berserk: il simbionte affamato prende il corpo e va a caccia da solo.
 *
 * <p>Parte quando la fame del simbionte arriva a zero e finisce quando e' di nuovo sazio. Prima
 * inseguiva il primo essere vivente nel raggio - animali domestici, villici, altri giocatori
 * compresi - spingendo il giocatore a colpi di velocita' dal server mentre i suoi tasti
 * continuavano a funzionare, e restava agganciato per sempre a una preda irraggiungibile.
 * Adesso, sul modello della mod di riferimento (Symbiote: A Bonding Experience):</p>
 * <ul>
 *   <li><b>sceglie la preda</b> - prima chi attacca l'ospite, poi i mostri, poi gli animali; i
 *   villici solo se non si fida dell'ospite (Bond 1); mai gli animali domestici, quelli con un
 *   nome, i golem, i boss o gli altri giocatori, a meno che non abbiano appena colpito
 *   l'ospite e il PvP lo permetta (e allora li morde, non li divora). Deve vederla, e stare al massimo sei blocchi sopra o sotto;</li>
 *   <li><b>prende il corpo</b> - sul client i tasti di movimento smettono di rispondere, la
 *   visuale si gira da sola verso la preda e il giocatore corre in avanti, saltando da solo gli
 *   ostacoli. Il percorso lo calcola il server col navigatore dei mob, e il client guarda verso
 *   il prossimo nodo: cosi' aggira muri e dislivelli invece di sbatterci contro;</li>
 *   <li><b>lascia perdere</b> - una preda che per sette secondi non si avvicina, o che dopo
 *   trenta non e' ancora raggiunta, viene scartata per cinque minuti e si passa alla prossima;
 *   una che sparisce alla vista per cinque secondi viene semplicemente dimenticata;</li>
 *   <li><b>mangia</b> - una preda piccola la trascina a se' e la divora, una grossa la morde sul
 *   posto finche' non cade. Senza prede il corpo torna al giocatore, che deve portarlo al
 *   cibo: la carne gettata a terra il simbionte la raccoglie da solo, ed e' il modo di calmarlo;</li>
 *   <li><b>la fiducia conta</b> - con un'affinita' alta si calma prima.</li>
 * </ul>
 *
 * <p>Da indebolito il simbionte non ha la forza di prendere il corpo: il berserk non parte, e se
 * era in corso si interrompe. Finche' dura, le mani sono sue: niente oggetti usati, blocchi
 * piazzati o porte aperte.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class BerserkSimbionte {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarBerserk");

    static final String BERSERK = "Venom.Berserk";
    static final String FASE = "Venom.Berserk.Phase";
    static final String TICK = "Venom.Berserk.Ticks";
    /** Acceso mentre divora: lo leggono le animazioni (assimilazione). */
    static final String PASTO = "Venom.Berserk.Regeneration";
    private static final String FEND_OFF = "Venom.FendOffEnemies";
    private static final String BLOCCO_MOVIMENTO = "Venom.LockMovement";

    private static final int FASE_CACCIA = 1;
    private static final int FASE_PASTO = 2;

    /** Oltre questa fame il simbionte e' sazio e lascia il corpo: prima, se si fida di piu'. */
    private static final int[] SAZIETA_PER_FIDUCIA = {70, 60, 50};

    private static final double RAGGIO_PREDA = 24.0D;
    /** Una preda piccola si comincia a divorare da qui... */
    private static final double PORTATA_PASTO = 4.5D;
    /** ...e se nel frattempo scappa oltre questa distanza, la presa si perde. */
    private static final double PORTATA_PRESA_PERSA = 7.0D;
    /** Una preda grossa si morde da vicino. */
    private static final double PORTATA_MORSO = 3.5D;
    /** Sotto questa distanza, e in vista, si punta la preda e non piu' il percorso. */
    private static final double VISTA_DIRETTA = 6.0D;
    /** Piu' in alto o in basso di cosi' una creatura non e' una preda: e' su un altro piano. */
    private static final double DISLIVELLO_MASSIMO = 6.0D;
    /** Dopo tanti tick senza vederla, la preda si lascia perdere. */
    private static final long PERSA_DI_VISTA = 100L;

    private static final int PASTO_TICK = 40;
    private static final int MORSO_OGNI = 20;
    private static final float MORSO_DANNO = 8.0F;
    private static final int MORSO_FAME = 4;
    /** Le prede che si divorano intere: poca vita e abbastanza piccole da trascinare. */
    private static final float PICCOLA_VITA = 40.0F;
    private static final float PICCOLA_LARGHEZZA = 1.5F;
    private static final float PICCOLA_ALTEZZA = 2.5F;

    private static final long SENZA_PROGRESSI = 140L;
    private static final long CACCIA_MASSIMA = 600L;
    private static final long SCARTO = 20L * 60L * 5L;
    private static final long OGNI_PERCORSO = 40L;
    /** Un nodo del percorso e' raggiunto a un blocco di distanza (al quadrato). */
    private static final double NODO_RAGGIUNTO = 1.0D;
    /** Quanti nodi avanti si puo' puntare, se la strada e' dritta e libera. */
    private static final int ANTICIPO = 4;
    /** Il client lascia il corpo se il server tace per piu' di tanto: vedi BerserkClient. */
    private static final long RINFRESCO = 10L;

    /** Letto dagli eventi di interazione sul client: lo scrive il pacchetto del berserk. */
    public static volatile boolean attivoSulClient;

    private static final Map<UUID, Caccia> CACCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> SCARTATE = new ConcurrentHashMap<>();

    private BerserkSimbionte() {
    }

    // ------------------------------------------------------------------ il ciclo

    /** Lo chiama la fame del simbionte quando e' a zero o il berserk e' gia' acceso. */
    static void tick(ServerPlayer ospite, int fame) {
        boolean acceso = SymbioteState.getScore(ospite, BERSERK) > 0;
        if (acceso && fame >= sazieta(ospite)) {
            ferma(ospite, "sazio");
            return;
        }
        if (SymbioteState.isVulnerabile(ospite)) {
            if (acceso) {
                ferma(ospite, "indebolito");
            }
            return;
        }
        if (!acceso) {
            if (fame > 0) {
                return;
            }
            inizia(ospite);
        }
        // il corpo lo fa uscire Palladium da solo, con Venom.Berserk (vedi enablevenom). Prima qui
        // si chiamava riapplicaForma finche' il corpo non era fuori: ma riapplicare la forma
        // rimette la fame a cento, e il berserk partito da umano si spegneva al primo tick
        // la carne gettata a terra la raccoglie sempre: e' la via d'uscita del giocatore
        VenomSymbioteSystemsHandler.pullNearbyFoodLoot(ospite);

        if (SymbioteState.getScore(ospite, BERSERK) <= 0) {
            // spento a meta' strada (ritorno umano, fame riportata al massimo): niente caccia
            // nuova, o il client riceverebbe una preda da un berserk che non c'e' piu'
            return;
        }
        long ora = ospite.level().getGameTime();
        Caccia caccia = CACCE.computeIfAbsent(ospite.getUUID(), id -> new Caccia());
        int tick = SymbioteState.getScore(ospite, TICK) + 1;
        SymbioteState.setScore(ospite, TICK, tick);

        LivingEntity preda = caccia.preda(ospite);
        if (preda != null && valida(ospite, preda) && persaDiVista(ospite, caccia, preda, ora)) {
            LOGGER.info("Venom berserk lost sight of {} for {}", EntityType.getKey(preda.getType()),
                    ospite.getGameProfile().getName());
            caccia.dimenticaPreda();
            preda = null;
        }
        if (preda == null || !valida(ospite, preda)) {
            preda = nuovaPreda(ospite, caccia, ora);
        }
        if (preda == null) {
            senzaPreda(ospite, caccia, ora);
            return;
        }
        if (SymbioteState.getScore(ospite, FASE) == FASE_PASTO) {
            divora(ospite, caccia, preda, tick, ora);
            return;
        }
        double distanza = Math.sqrt(preda.distanceToSqr(ospite));
        boolean piccola = piccola(preda);
        if (piccola && distanza <= PORTATA_PASTO && ospite.hasLineOfSight(preda)) {
            SymbioteState.setScore(ospite, FASE, FASE_PASTO);
            SymbioteState.setScore(ospite, TICK, 0);
            SymbioteState.setScore(ospite, FEND_OFF, 0);
            divora(ospite, caccia, preda, 0, ora);
            return;
        }
        if (!piccola && distanza <= PORTATA_MORSO) {
            mordi(ospite, caccia, preda, ora);
            return;
        }
        insegui(ospite, caccia, preda, distanza, ora);
    }

    private static void inizia(ServerPlayer ospite) {
        SymbioteState.setScore(ospite, BERSERK, 1);
        SymbioteState.setScore(ospite, FASE, FASE_CACCIA);
        SymbioteState.setScore(ospite, TICK, 0);
        // averlo lasciato a digiuno fino alla furia costa fiducia
        SymbioteAffinityHandler.penalizza(ospite, SymbioteAffinityHandler.PENALITA_BERSERK);
        CACCE.put(ospite.getUUID(), new Caccia());
        ospite.displayClientMessage(Component.translatable("klyntars.berserk.hint"), true);
        ospite.level().playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                SuoniKlyntar.ABILITY_UNLEASH.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
        ModNetwork.syncBerserk(ospite, true, false, -1, null, ModNetwork.BERSERK_LAMPO_INIZIO);
        LOGGER.info("Venom berserk started for {}", ospite.getGameProfile().getName());
    }

    /** Spegne tutto e ridà il corpo al giocatore. */
    private static void ferma(ServerPlayer ospite, String motivo) {
        SymbioteState.setScore(ospite, BERSERK, 0);
        SymbioteState.setScore(ospite, FASE, 0);
        SymbioteState.setScore(ospite, TICK, 0);
        SymbioteState.setScore(ospite, PASTO, 0);
        SymbioteState.setScore(ospite, FEND_OFF, 0);
        dimentica(ospite);
        LOGGER.info("Venom berserk stopped for {} ({})", ospite.getGameProfile().getName(), motivo);
    }

    /**
     * Dimentica la caccia in corso e avvisa il client che il corpo e' di nuovo del giocatore.
     * La chiamano anche il ritorno umano e l'uscita dal gioco.
     */
    public static void dimentica(ServerPlayer ospite) {
        Caccia caccia = CACCE.remove(ospite.getUUID());
        VenomSymbioteSystemsHandler.dimenticaStalloBerserk(ospite);
        ModNetwork.syncVenomCombatTargets(ospite, List.of());
        if (caccia != null) {
            ModNetwork.syncBerserk(ospite, false, false, -1, null, 0);
        }
    }

    private static int sazieta(ServerPlayer ospite) {
        return SAZIETA_PER_FIDUCIA[VoceSimbionte.fiducia(ospite)];
    }

    // ------------------------------------------------------------------ le fasi

    /** Niente da mangiare in vista: il corpo torna al giocatore, che deve portarlo al cibo. */
    private static void senzaPreda(ServerPlayer ospite, Caccia caccia, long ora) {
        SymbioteState.setScore(ospite, FASE, FASE_CACCIA);
        SymbioteState.setScore(ospite, PASTO, 0);
        SymbioteState.setScore(ospite, FEND_OFF, 1);
        caccia.dimenticaPreda();
        VoceSimbionte.di(ospite, "berserk_niente", Tono.AGGRESSIVO, false, 20L * 20L);
        sincronizza(ospite, caccia, false, -1, null, ora);
    }

    private static void insegui(ServerPlayer ospite, Caccia caccia, LivingEntity preda, double distanza, long ora) {
        SymbioteState.setScore(ospite, FEND_OFF, 1);
        SymbioteState.setScore(ospite, PASTO, 0);
        if (distanza < caccia.migliore - 1.0D) {
            caccia.migliore = distanza;
            caccia.ultimoProgresso = ora;
        }
        if (ora - caccia.ultimoProgresso > SENZA_PROGRESSI || ora - caccia.inizio > CACCIA_MASSIMA) {
            scarta(ospite, preda, ora);
            VoceSimbionte.di(ospite, "berserk_scappata", Tono.AGGRESSIVO);
            LOGGER.info("Venom berserk gave up on {} for {}", EntityType.getKey(preda.getType()),
                    ospite.getGameProfile().getName());
            caccia.dimenticaPreda();
            VenomSymbioteSystemsHandler.dimenticaStalloBerserk(ospite);
            return;
        }
        Vec3 guida = guida(ospite, caccia, preda, distanza, ora);
        Vec3 verso = (guida != null ? guida : preda.position()).subtract(ospite.position());
        Vec3 orizzontale = new Vec3(verso.x, 0.0D, verso.z);
        if (orizzontale.lengthSqr() > 1.0E-4D
                && VenomSymbioteSystemsHandler.sbloccaBerserk(ospite, preda, orizzontale.normalize())) {
            // ha dovuto saltare: il percorso vecchio probabilmente non vale piu'
            caccia.percorso = null;
        }
        // i tentacoli del morso o del pasto si ritirano una volta sola, non a ogni tick: il
        // pacchetto va a tutti i giocatori che vedono l'ospite
        if (caccia.tentacoliFuori) {
            ModNetwork.syncVenomCombatTargets(ospite, List.of());
            caccia.tentacoliFuori = false;
        }
        sincronizza(ospite, caccia, true, preda.getId(), guida, ora);
    }

    /** Una preda grossa non si trascina: si morde sul posto, e la frusta di Fend Off aiuta. */
    private static void mordi(ServerPlayer ospite, Caccia caccia, LivingEntity preda, long ora) {
        SymbioteState.setScore(ospite, FEND_OFF, 1);
        SymbioteState.setScore(ospite, PASTO, 0);
        caccia.ultimoProgresso = ora;
        caccia.migliore = Math.min(caccia.migliore, Math.sqrt(preda.distanceToSqr(ospite)));
        if (ora - caccia.ultimoMorso >= MORSO_OGNI) {
            caccia.ultimoMorso = ora;
            caccia.inizio = ora;
            ModNetwork.syncVenomCombatTargets(ospite, List.of(centro(preda)));
            caccia.tentacoliFuori = true;
            preda.hurt(ospite.damageSources().playerAttack(ospite), MORSO_DANNO);
            VenomSymbioteSystemsHandler.addHunger(ospite, MORSO_FAME);
            ospite.level().playSound(null, preda.getX(), preda.getY(0.6D), preda.getZ(),
                    SuoniKlyntar.FEND_OFF_CLASH_SQUISH.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
        }
        sincronizza(ospite, caccia, false, preda.getId(), null, ora);
    }

    /** La preda piccola: trascinata contro il corpo e divorata. */
    private static void divora(ServerPlayer ospite, Caccia caccia, LivingEntity preda, int tick, long ora) {
        if (preda.distanceToSqr(ospite) > PORTATA_PRESA_PERSA * PORTATA_PRESA_PERSA) {
            // e' sfuggita alla presa: si torna a cacciare
            SymbioteState.setScore(ospite, FASE, FASE_CACCIA);
            SymbioteState.setScore(ospite, TICK, 0);
            SymbioteState.setScore(ospite, PASTO, 0);
            return;
        }
        SymbioteState.setScore(ospite, BLOCCO_MOVIMENTO, 2);
        ospite.setDeltaMovement(0.0D, Math.min(ospite.getDeltaMovement().y, 0.0D), 0.0D);
        ospite.hurtMarked = true;
        SymbioteState.setScore(ospite, PASTO, 1);
        ModNetwork.syncVenomCombatTargets(ospite, List.of(centro(preda)));
        caccia.tentacoliFuori = true;
        Vec3 tira = ospite.position().add(0.0D, ospite.getBbHeight() * 0.85D, 0.0D).subtract(preda.position());
        if (tira.lengthSqr() > 1.0D) {
            preda.setDeltaMovement(tira.normalize().scale(0.42D));
            preda.hasImpulse = true;
            preda.hurtMarked = true;
            preda.fallDistance = 0.0F;
        }
        if (preda instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        preda.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 8, false, false, false));
        sincronizza(ospite, caccia, false, preda.getId(), null, ora);
        if (tick < PASTO_TICK && tira.lengthSqr() > 1.3D) {
            return;
        }
        int sazia = valoreDelPasto(preda);
        preda.hurt(ospite.damageSources().playerAttack(ospite), 1000.0F);
        if (preda.isAlive() && !(preda instanceof Player)) {
            // il colpo di riserva, senza attaccante, passa sopra alle regole del PvP: solo per i mob
            preda.hurt(ospite.damageSources().magic(), 1000.0F);
        }
        ospite.heal(8.0F);
        VenomSymbioteSystemsHandler.addHunger(ospite, sazia);
        ServerLevel livello = ospite.serverLevel();
        livello.playSound(null, preda.getX(), preda.getY(), preda.getZ(), SuoniKlyntar.SYMBIOTE_GRAB.get(),
                SoundSource.PLAYERS, 1.0F, 0.75F);
        livello.playSound(null, preda.getX(), preda.getY(), preda.getZ(), SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        VoceSimbionte.di(ospite, "berserk_pasto", Tono.AGGRESSIVO);
        SymbioteState.setScore(ospite, FASE, FASE_CACCIA);
        SymbioteState.setScore(ospite, TICK, 0);
        SymbioteState.setScore(ospite, PASTO, 0);
        caccia.dimenticaPreda();
        VenomSymbioteSystemsHandler.dimenticaStalloBerserk(ospite);
        ModNetwork.syncVenomCombatTargets(ospite, List.of());
        ModNetwork.syncBerserk(ospite, true, false, -1, null, ModNetwork.BERSERK_LAMPO_PASTO);
        caccia.ultimoInvio = ora;
        LOGGER.info("Venom berserk devoured {} (+{} hunger) for {}", EntityType.getKey(preda.getType()), sazia,
                ospite.getGameProfile().getName());
    }

    private static int valoreDelPasto(LivingEntity preda) {
        if (preda instanceof AbstractVillager || preda instanceof Animal && !(preda instanceof Allay)) {
            return 40;
        }
        return preda instanceof Enemy ? 25 : 15;
    }

    // ------------------------------------------------------------------ il percorso

    /**
     * Il punto verso cui guardare e correre: il prossimo nodo del percorso, o nessuno quando la
     * preda e' vicina e in vista (allora si punta lei).
     */
    private static Vec3 guida(ServerPlayer ospite, Caccia caccia, LivingEntity preda, double distanza, long ora) {
        if (distanza <= VISTA_DIRETTA && ospite.hasLineOfSight(preda)) {
            return null;
        }
        // un percorso nuovo quando manca, e' finito o la preda si e' spostata; mai piu' di uno
        // ogni due secondi, perche' il navigatore costa
        BlockPos meta = preda.blockPosition();
        boolean daRifare = caccia.percorso == null || caccia.nodo >= caccia.percorso.size()
                || meta.distSqr(caccia.meta) > 9.0D;
        if (daRifare && ora - caccia.ultimoPercorso >= OGNI_PERCORSO) {
            caccia.percorso = percorso(ospite, meta);
            caccia.nodo = 0;
            caccia.meta = meta;
            caccia.ultimoPercorso = ora;
        }
        List<BlockPos> nodi = caccia.percorso;
        if (nodi == null) {
            return null;
        }
        // un nodo e' fatto quando ci si passa vicino, ma anche quando lo si e' gia' superato:
        // correndo si tagliano le curve, e prima un nodo mancato di un soffio restava il
        // bersaglio - il giocatore si girava per tornarci e finiva a girargli intorno
        while (caccia.nodo < nodi.size()) {
            BlockPos corrente = nodi.get(caccia.nodo);
            if (orizzontale(ospite, corrente) < NODO_RAGGIUNTO) {
                caccia.nodo++;
                continue;
            }
            if (caccia.nodo + 1 < nodi.size()) {
                BlockPos dopo = nodi.get(caccia.nodo + 1);
                if (orizzontale(ospite, dopo) <= fra(corrente, dopo)) {
                    caccia.nodo++;
                    continue;
                }
            }
            break;
        }
        if (caccia.nodo >= nodi.size()) {
            return null;
        }
        // si guarda avanti: il nodo piu' lontano, fra i prossimi, raggiungibile in linea retta
        // sullo stesso piano. Puntare sempre il nodo successivo, a un blocco di distanza,
        // faceva sterzare di continuo
        BlockPos corrente = nodi.get(caccia.nodo);
        int ultimo = Math.min(caccia.nodo + ANTICIPO, nodi.size() - 1);
        int piano = caccia.nodo;
        while (piano < ultimo && nodi.get(piano + 1).getY() == corrente.getY()) {
            piano++;
        }
        for (int j = piano; j > caccia.nodo; j--) {
            if (lineaLibera(ospite, nodi.get(j))) {
                return centroDi(nodi.get(j));
            }
        }
        return centroDi(corrente);
    }

    private static Vec3 centroDi(BlockPos nodo) {
        return new Vec3(nodo.getX() + 0.5D, nodo.getY(), nodo.getZ() + 0.5D);
    }

    private static double fra(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** Niente blocchi fra il giocatore e il nodo, all'altezza delle gambe e del petto. */
    private static boolean lineaLibera(ServerPlayer ospite, BlockPos nodo) {
        Vec3 arrivo = centroDi(nodo);
        for (double altezza : new double[]{0.5D, 1.4D}) {
            Vec3 da = ospite.position().add(0.0D, altezza, 0.0D);
            Vec3 a = arrivo.add(0.0D, altezza, 0.0D);
            if (ospite.level().clip(new ClipContext(da, a, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ospite))
                    .getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Il percorso fino alla preda, calcolato con il navigatore di un mob che non entra mai nel
     * mondo: e' la stessa strada che farebbe uno zombie, porte chiuse escluse.
     */
    private static List<BlockPos> percorso(ServerPlayer ospite, BlockPos meta) {
        Zombie guida = new Zombie(EntityType.ZOMBIE, ospite.level());
        guida.moveTo(ospite.getX(), ospite.getY(), ospite.getZ(), ospite.getYRot(), 0.0F);
        guida.setOnGround(true);
        Path strada = guida.getNavigation().createPath(meta, 1);
        guida.discard();
        if (strada == null || strada.getNodeCount() == 0) {
            return null;
        }
        List<BlockPos> nodi = new ArrayList<>(strada.getNodeCount());
        for (int i = 0; i < strada.getNodeCount(); i++) {
            nodi.add(strada.getNodePos(i));
        }
        return nodi;
    }

    private static double orizzontale(ServerPlayer ospite, BlockPos nodo) {
        double dx = nodo.getX() + 0.5D - ospite.getX();
        double dz = nodo.getZ() + 0.5D - ospite.getZ();
        return dx * dx + dz * dz;
    }

    /** Manda lo stato al client quando cambia qualcosa, e comunque ogni secondo. */
    private static void sincronizza(ServerPlayer ospite, Caccia caccia, boolean marcia, int preda, Vec3 guida, long ora) {
        boolean cambiato = marcia != caccia.marciaInviata || preda != caccia.predaInviata
                || !stessoPunto(guida, caccia.guidaInviata);
        if (!cambiato && ora - caccia.ultimoInvio < RINFRESCO) {
            return;
        }
        caccia.marciaInviata = marcia;
        caccia.predaInviata = preda;
        caccia.guidaInviata = guida;
        caccia.ultimoInvio = ora;
        ModNetwork.syncBerserk(ospite, true, marcia, preda, guida, 0);
    }

    private static boolean stessoPunto(Vec3 a, Vec3 b) {
        return a == null ? b == null : b != null && a.distanceToSqr(b) < 0.01D;
    }

    // ------------------------------------------------------------------ le prede

    private static LivingEntity nuovaPreda(ServerPlayer ospite, Caccia caccia, long ora) {
        LivingEntity scelta = scegli(ospite, ora);
        caccia.dimenticaPreda();
        if (scelta == null) {
            return null;
        }
        caccia.predaId = scelta.getId();
        caccia.inizio = ora;
        caccia.ultimoProgresso = ora;
        caccia.migliore = Math.sqrt(scelta.distanceToSqr(ospite));
        caccia.ultimaVista = ora;
        SymbioteState.setScore(ospite, FASE, FASE_CACCIA);
        SymbioteState.setScore(ospite, TICK, 0);
        VenomSymbioteSystemsHandler.dimenticaStalloBerserk(ospite);
        VoceSimbionte.di(ospite, "berserk_preda", Tono.AGGRESSIVO);
        LOGGER.info("Venom berserk hunts {} for {}", EntityType.getKey(scelta.getType()),
                ospite.getGameProfile().getName());
        return scelta;
    }

    /** La preda migliore in vista: per rango, poi la piu' vicina. */
    private static LivingEntity scegli(ServerPlayer ospite, long ora) {
        int fiducia = VoceSimbionte.fiducia(ospite);
        AABB zona = ospite.getBoundingBox().inflate(RAGGIO_PREDA);
        LivingEntity migliore = null;
        int rangoMigliore = Integer.MAX_VALUE;
        double distanzaMigliore = Double.MAX_VALUE;
        for (LivingEntity candidata : ospite.level().getEntitiesOfClass(LivingEntity.class, zona,
                e -> e != ospite && e.isAlive() && !e.isSpectator())) {
            int rango = rango(ospite, candidata, fiducia);
            if (rango < 0 || rango > rangoMigliore || scartata(ospite, candidata, ora)
                    || Math.abs(candidata.getY() - ospite.getY()) > DISLIVELLO_MASSIMO) {
                continue;
            }
            double distanza = candidata.distanceToSqr(ospite);
            if (rango == rangoMigliore && distanza >= distanzaMigliore) {
                continue;
            }
            if (!ospite.hasLineOfSight(candidata)) {
                continue;
            }
            migliore = candidata;
            rangoMigliore = rango;
            distanzaMigliore = distanza;
        }
        return migliore;
    }

    /**
     * Quanto il simbionte affamato vuole quella creatura: 0 chi lo attacca, 1 i mostri, 2 gli
     * animali, 3 i villici. Negativo: non si tocca.
     */
    private static int rango(ServerPlayer ospite, LivingEntity e, int fiducia) {
        if (e instanceof Player altro) {
            // un altro giocatore solo se ha appena colpito l'ospite, e solo dove il PvP lo permette:
            // su un server PvP il berserk non deve mettersi a dare la caccia a chiunque passi
            return aggressore(ospite, e) && PvpRules.colpibile(ospite, altro) ? 0 : -1;
        }
        if (!(e instanceof Mob mob) || e.getType().is(Tags.EntityTypes.BOSSES) || e.hasCustomName()
                || e.isPassenger() || e instanceof Allay) {
            return -1;
        }
        if (e instanceof OwnableEntity animaleDiQualcuno && animaleDiQualcuno.getOwnerUUID() != null) {
            return -1;
        }
        boolean ceLaConNoi = mob.getTarget() == ospite || aggressore(ospite, e);
        if (ceLaConNoi) {
            return 0;
        }
        // i neutrali (lupi, api, enderman, golem, piglin zombie) si lasciano stare finche'
        // non si arrabbiano: colpirne uno solleva tutto il gruppo
        if (e instanceof NeutralMob || e instanceof AbstractGolem) {
            return -1;
        }
        if (e instanceof Enemy) {
            return 1;
        }
        if (e instanceof Animal || e instanceof WaterAnimal || e instanceof AmbientCreature) {
            return 2;
        }
        if (e instanceof AbstractVillager) {
            return fiducia == VoceSimbionte.DIFFIDENTE ? 3 : -1;
        }
        return -1;
    }

    private static boolean aggressore(ServerPlayer ospite, LivingEntity e) {
        return ospite.getLastHurtByMob() == e && ospite.tickCount - ospite.getLastHurtByMobTimestamp() < 600;
    }

    /**
     * Una preda che non si vede da un po' si lascia perdere: se si infila in una grotta o dietro
     * una collina il simbionte non sa piu' dov'e'. Prima continuava a inseguirla anche sottoterra,
     * guidato dal percorso. Chi la sta gia' divorando o mordendo ce l'ha sotto il naso.
     */
    private static boolean persaDiVista(ServerPlayer ospite, Caccia caccia, LivingEntity preda, long ora) {
        if (ospite.hasLineOfSight(preda) || preda.distanceToSqr(ospite) <= PORTATA_PASTO * PORTATA_PASTO) {
            caccia.ultimaVista = ora;
            return false;
        }
        return ora - caccia.ultimaVista > PERSA_DI_VISTA;
    }

    private static boolean valida(ServerPlayer ospite, LivingEntity preda) {
        return preda.isAlive() && !preda.isRemoved() && preda.level() == ospite.level()
                && preda.distanceToSqr(ospite) <= RAGGIO_PREDA * RAGGIO_PREDA * 1.5D;
    }

    /**
     * Le prede che si trascinano e si divorano intere. Mai un giocatore: sarebbe un'uccisione
     * istantanea a dispetto dell'armatura. Un giocatore si morde, col danno normale del PvP.
     */
    private static boolean piccola(LivingEntity preda) {
        return !(preda instanceof Player) && preda.getMaxHealth() <= PICCOLA_VITA && preda.getBbWidth() <= PICCOLA_LARGHEZZA
                && preda.getBbHeight() <= PICCOLA_ALTEZZA && !preda.isVehicle();
    }

    private static void scarta(ServerPlayer ospite, LivingEntity preda, long ora) {
        SCARTATE.computeIfAbsent(ospite.getUUID(), id -> new ConcurrentHashMap<>()).put(preda.getId(), ora + SCARTO);
    }

    private static boolean scartata(ServerPlayer ospite, LivingEntity preda, long ora) {
        Map<Integer, Long> scartate = SCARTATE.get(ospite.getUUID());
        if (scartate == null) {
            return false;
        }
        scartate.values().removeIf(fino -> fino <= ora);
        return scartate.containsKey(preda.getId());
    }

    private static Vec3 centro(LivingEntity e) {
        return e.position().add(0.0D, e.getBbHeight() * 0.55D, 0.0D);
    }

    // ------------------------------------------------------------------ le mani sono sue

    public static boolean inBerserk(Player giocatore) {
        return giocatore.level().isClientSide ? attivoSulClient
                : SymbioteState.getScore(giocatore, BERSERK) > 0;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        nega(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        nega(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        nega(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        nega(event);
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player giocatore && inBerserk(giocatore)) {
            event.setCanceled(true);
        }
    }

    private static void nega(PlayerInteractEvent event) {
        if (event.isCancelable() && inBerserk(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CACCE.remove(event.getEntity().getUUID());
        SCARTATE.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ lo stato

    private static final class Caccia {
        int predaId = -1;
        long inizio;
        long ultimoProgresso;
        double migliore = Double.MAX_VALUE;
        long ultimoMorso;
        long ultimaVista;
        /** Vero finche' i tentacoli sono puntati su una preda e vanno ritirati. */
        boolean tentacoliFuori = true;
        List<BlockPos> percorso;
        int nodo;
        BlockPos meta = BlockPos.ZERO;
        long ultimoPercorso = Long.MIN_VALUE / 2;
        boolean marciaInviata;
        int predaInviata = Integer.MIN_VALUE;
        Vec3 guidaInviata;
        long ultimoInvio = Long.MIN_VALUE / 2;

        LivingEntity preda(ServerPlayer ospite) {
            if (predaId < 0) {
                return null;
            }
            return ospite.level().getEntity(predaId) instanceof LivingEntity vivente ? vivente : null;
        }

        void dimenticaPreda() {
            predaId = -1;
            migliore = Double.MAX_VALUE;
            percorso = null;
            nodo = 0;
            ultimoPercorso = Long.MIN_VALUE / 2;
            ultimoMorso = 0L;
        }
    }
}
