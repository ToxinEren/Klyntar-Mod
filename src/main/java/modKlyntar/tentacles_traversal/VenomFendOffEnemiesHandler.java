package modKlyntar.tentacles_traversal;

import modKlyntar.sound.SuoniKlyntar;
import modKlyntar.MyMod;
import modKlyntar.network.ModNetwork;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fend Off Enemies: il simbionte difende da solo chi lo porta.
 *
 * <p>Ricalcato sulle difese automatiche della mod di riferimento, che non sono una mossa sola:
 * la frusta del mantello, lo schiaffo di un braccio, lo schianto a terra, il contrattacco e lo
 * scontro fra due nemici. Qui sono tutte mosse della stessa abilita', scelte colpo per colpo:</p>
 * <ul>
 *   <li><b>frusta</b> - la mossa di base, su chiunque sia a portata;</li>
 *   <li><b>schiaffo</b> - chi e' troppo vicino viene colpito di lato e allontanato;</li>
 *   <li><b>schianto</b> - ogni tanto un nemico viene afferrato, alzato e sbattuto a terra;</li>
 *   <li><b>scontro</b> - ogni tanto due nemici vicini vengono alzati e sbattuti uno contro l'altro;</li>
 *   <li><b>contrattacco</b> - chi ci colpisce da vicino si prende subito uno schiaffo, senza aspettare.</li>
 * </ul>
 * <p>Schianto e scontro sono uno alla volta, e mentre uno e' in corso le altre braccia
 * continuano a frustare. I boss e i mob troppo grossi non si sollevano.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class VenomFendOffEnemiesHandler {
    private static final String OBJECTIVE_NAME = "Venom.FendOffEnemies";
    private static final double ATTACK_RANGE = 10.0D;
    private static final float ATTACK_DAMAGE = 10.0F;
    private static final int MAX_ARMS = 6;
    private static final int ATTACK_INTERVAL_TICKS = 10;
    /** Chi ci colpisce resta un bersaglio per trenta secondi, come nella mod di riferimento. */
    private static final long MEMORIA_AGGRESSORI = 600L;
    /** Margine sulla portata per un bersaglio che si allontana mentre il braccio e' in volo. */
    private static final double MARGINE_ARRIVO = 2.0D;

    /**
     * Le mosse, come le vede il client: cambia la forma del braccio mentre parte. L'ordine
     * conta, viaggia nel pacchetto come numero.
     */
    public enum Mossa { FRUSTA, SCHIAFFO, PRESA }

    // --- frusta: arriva, resta un attimo, rientra. Il colpo scende quando arriva ---
    private static final int FRUSTA_ARRIVA = 4;
    private static final int FRUSTA_RESTA = 2;
    private static final int FRUSTA_RIENTRA = 2;

    // --- schiaffo: da vicino, piu' veloce, e spinge via forte ---
    private static final double SCHIAFFO_PORTATA = 3.5D;
    private static final int SCHIAFFO_ARRIVA = 2;
    private static final int SCHIAFFO_RESTA = 1;
    private static final int SCHIAFFO_RIENTRA = 3;
    private static final double SCHIAFFO_SPINTA = 1.1D;
    private static final double SCHIAFFO_ALZA = 0.35D;
    /** Il contrattacco non aspetta la ricarica, ma lo stesso aggressore non ne prende piu' di uno al secondo. */
    private static final long CONTRATTACCO_PAUSA = 20L;

    // --- schianto a terra, dalla mod di riferimento: presa, su, sospeso, giu' ---
    private static final double SCHIANTO_PORTATA = 6.0D;
    private static final int SCHIANTO_ARRIVA = 6;
    private static final int SCHIANTO_SU_FINO = 16;
    private static final int SCHIANTO_TIENI_FINO = 24;
    private static final int SCHIANTO_GIU_FINO = 36;
    private static final double SCHIANTO_ALTEZZA = 2.5D;
    private static final double SCHIANTO_SALITA = 0.3D;
    private static final double SCHIANTO_DISCESA = -1.8D;
    private static final float SCHIANTO_DANNO = 1.75F;
    private static final long SCHIANTO_RICARICA = 200L;
    private static final float SCHIANTO_PROBABILITA = 0.5F;

    // --- scontro: due nemici su, poi l'uno contro l'altro ---
    private static final double SCONTRO_DISTANZA_MAX = 8.0D;
    private static final double SCONTRO_DISTANZA_MIN = 2.0D;
    private static final int SCONTRO_ARRIVA = 4;
    private static final int SCONTRO_SU_FINO = 12;
    private static final int SCONTRO_FINO = 32;
    private static final double SCONTRO_ALTEZZA = 2.5D;
    private static final double SCONTRO_SALITA = 0.4D;
    private static final double SCONTRO_PASSO = 0.9D;
    private static final double SCONTRO_IMPATTO = 1.5D;
    private static final float SCONTRO_DANNO = 1.5F;
    private static final double SCONTRO_SPINTA = 0.5D;
    private static final long SCONTRO_RICARICA = 240L;
    private static final float SCONTRO_PROBABILITA = 0.5F;

    // --- chi viene sollevato viene anche portato davanti a noi ---
    private static final double TRASCINA_DAVANTI = 2.5D;
    private static final double TRASCINA_GUADAGNO = 0.35D;
    private static final double TRASCINA_MAX = 0.6D;
    /** Oltre queste misure un mob non si solleva: un ravager appeso a un tentacolo non regge. */
    private static final float SOLLEVABILE_LARGHEZZA = 1.5F;
    private static final float SOLLEVABILE_ALTEZZA = 3.0F;
    /** Quanto dura il rientro del braccio che teneva qualcuno. */
    private static final int PRESA_RIENTRA = 4;

    private static final Map<UUID, Map<Integer, Long>> TARGET_COOLDOWNS = new ConcurrentHashMap<>();
    /** Chi ci ha colpito, qualunque cosa sia, e in che tick: id dell'entita' -> tick. */
    private static final Map<UUID, Map<Integer, Long>> AGGRESSORI = new ConcurrentHashMap<>();
    /** Quando ogni aggressore si e' preso l'ultimo contrattacco. */
    private static final Map<UUID, Map<Integer, Long>> CONTRATTACCHI = new ConcurrentHashMap<>();
    /** I colpi partiti che non sono ancora arrivati. */
    private static final Map<UUID, List<Colpo>> COLPI = new ConcurrentHashMap<>();
    /** L'ultimo tick in cui si e' sentito un colpo: sei colpi nello stesso tick fanno un rumore solo. */
    private static final Map<UUID, Long> ULTIMO_SUONO = new ConcurrentHashMap<>();
    /** Lo schianto o lo scontro in corso, uno per giocatore. */
    private static final Map<UUID, Presa> PRESE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PROSSIMO_SCHIANTO = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PROSSIMO_SCONTRO = new ConcurrentHashMap<>();

    private record Colpo(int bersaglio, long quando, Mossa mossa) {
    }

    /** Uno schianto (un bersaglio) o uno scontro (due), dal tick in cui il braccio e' partito. */
    private static final class Presa {
        final boolean scontro;
        final int[] bersagli;
        final double[] quota;
        final long inizio;

        Presa(boolean scontro, List<LivingEntity> presi, double altezza, long inizio) {
            this.scontro = scontro;
            this.bersagli = new int[presi.size()];
            this.quota = new double[presi.size()];
            for (int i = 0; i < presi.size(); i++) {
                this.bersagli[i] = presi.get(i).getId();
                this.quota[i] = presi.get(i).getY() + altezza;
            }
            this.inizio = inizio;
        }

        boolean tiene(int id) {
            for (int b : bersagli) {
                if (b == id) {
                    return true;
                }
            }
            return false;
        }
    }

    private VenomFendOffEnemiesHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        int active = getScore(player, OBJECTIVE_NAME, false);
        if (active <= 0) {
            dimentica(player.getUUID());
            return;
        }

        tickCombat(player);
    }

    /** Chi esce dal mondo non lascia niente nelle mappe, e non tiene nessuno appeso. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        dimentica(event.getEntity().getUUID());
    }

    private static void dimentica(UUID id) {
        TARGET_COOLDOWNS.remove(id);
        AGGRESSORI.remove(id);
        CONTRATTACCHI.remove(id);
        COLPI.remove(id);
        ULTIMO_SUONO.remove(id);
        PRESE.remove(id);
        PROSSIMO_SCHIANTO.remove(id);
        PROSSIMO_SCONTRO.remove(id);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (victim.level().isClientSide || getScore(victim, OBJECTIVE_NAME, false) <= 0) {
            return;
        }

        // qualunque cosa ci colpisca, non solo un giocatore: un lupo o un golem che ci attacca
        // va respinto anche se di suo sarebbe neutrale
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity aggressore) || attacker == victim) {
            return;
        }
        long ora = victim.level().getGameTime();
        AGGRESSORI.computeIfAbsent(victim.getUUID(), id -> new ConcurrentHashMap<>())
                .put(aggressore.getId(), ora);
        contrattacca(victim, aggressore, ora);
    }

    /**
     * Il contrattacco: chi ci colpisce da vicino si prende subito uno schiaffo, senza aspettare
     * che la sua ricarica scada. Da lontano (un arco, una pozione) no: li' basta che diventi un
     * bersaglio, e la frusta arriva al giro dopo.
     */
    private static void contrattacca(ServerPlayer player, LivingEntity aggressore, long ora) {
        if (aggressore.distanceToSqr(player) > SCHIAFFO_PORTATA * SCHIAFFO_PORTATA
                || !canAttackEntity(aggressore, player, SCHIAFFO_PORTATA) || tenuto(player, aggressore)) {
            return;
        }
        Map<Integer, Long> ultimi = CONTRATTACCHI.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        Long ultimo = ultimi.get(aggressore.getId());
        if (ultimo != null && ora - ultimo < CONTRATTACCO_PAUSA) {
            return;
        }
        ultimi.put(aggressore.getId(), ora);
        lancia(player, aggressore, Mossa.SCHIAFFO, ora);
    }

    /**
     * Un tick di difesa: prima arrivano i colpi partiti, poi va avanti la presa in corso, poi
     * partono i colpi nuovi.
     *
     * <p>Prima i tentacoli restavano puntati sui bersagli per tutto il tempo che erano a
     * portata, e il danno scendeva ogni mezzo secondo senza che si vedesse un colpo. Adesso
     * ogni colpo e' un braccio che parte, arriva, e rientra; il danno scende quando arriva.</p>
     */
    private static void tickCombat(ServerPlayer player) {
        long ora = player.level().getGameTime();
        colpisciQuelliArrivati(player, ora);
        tickPresa(player, ora);

        List<LivingEntity> targets = findTargets(player);
        Map<Integer, Long> cooldowns = TARGET_COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        // le mosse grandi una alla volta; si tira il dado una volta al secondo, non a ogni tick
        if (!targets.isEmpty() && !PRESE.containsKey(player.getUUID()) && ora % 20L == 0L) {
            if (!provaScontro(player, targets, cooldowns, ora)) {
                provaSchianto(player, targets, cooldowns, ora);
            }
        }
        for (LivingEntity target : targets) {
            if (tenuto(player, target) || ora < cooldowns.getOrDefault(target.getId(), 0L)) {
                continue;
            }
            Mossa mossa = target.distanceToSqr(player) <= SCHIAFFO_PORTATA * SCHIAFFO_PORTATA
                    ? Mossa.SCHIAFFO : Mossa.FRUSTA;
            lancia(player, target, mossa, ora);
        }

        cleanupCooldowns(cooldowns, ora);
        dimenticaAggressori(player, ora);
    }

    /** Fa partire un braccio verso il bersaglio: il client lo disegna, il colpo arriva dopo. */
    private static void lancia(ServerPlayer player, LivingEntity target, Mossa mossa, long ora) {
        boolean schiaffo = mossa == Mossa.SCHIAFFO;
        int arriva = schiaffo ? SCHIAFFO_ARRIVA : FRUSTA_ARRIVA;
        ModNetwork.syncVenomLash(player, target, getTargetCenter(target), mossa.ordinal(), arriva,
                schiaffo ? SCHIAFFO_RESTA : FRUSTA_RESTA, schiaffo ? SCHIAFFO_RIENTRA : FRUSTA_RIENTRA);
        COLPI.computeIfAbsent(player.getUUID(), id -> new ArrayList<>())
                .add(new Colpo(target.getId(), ora + arriva, mossa));
        TARGET_COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .put(target.getId(), ora + ATTACK_INTERVAL_TICKS);
    }

    /**
     * I colpi che arrivano adesso colpiscono, se il bersaglio c'e' ancora. Nel frattempo puo'
     * essere morto, uscito di portata o finito dietro un muro: in quel caso il braccio va a
     * vuoto, come si vede.
     */
    private static void colpisciQuelliArrivati(ServerPlayer player, long gameTime) {
        List<Colpo> coda = COLPI.get(player.getUUID());
        if (coda == null || coda.isEmpty()) {
            return;
        }
        Iterator<Colpo> iterator = coda.iterator();
        while (iterator.hasNext()) {
            Colpo colpo = iterator.next();
            if (colpo.quando() > gameTime) {
                continue;
            }
            iterator.remove();
            Entity entita = player.level().getEntity(colpo.bersaglio());
            if (!(entita instanceof LivingEntity target)
                    || !canAttackEntity(target, player, ATTACK_RANGE + MARGINE_ARRIVO)) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), ATTACK_DAMAGE);
            if (colpo.mossa() == Mossa.SCHIAFFO) {
                spingi(player, target, SCHIAFFO_SPINTA, SCHIAFFO_ALZA);
                suona(player, target, SuoniKlyntar.FEND_OFF_SLAP.get(), 0.8F, gameTime);
            } else {
                knockTargetBack(player, target);
                suona(player, target, SuoniKlyntar.FEND_OFF_LASH.get(), 0.7F, gameTime);
            }
        }
    }

    // ------------------------------------------------------------------ schianto e scontro

    /** Un nemico a portata e sollevabile, se il dado dice di si': lo afferra per sbatterlo a terra. */
    private static boolean provaSchianto(ServerPlayer player, List<LivingEntity> targets,
                                         Map<Integer, Long> cooldowns, long ora) {
        if (ora < PROSSIMO_SCHIANTO.getOrDefault(player.getUUID(), 0L)
                || player.getRandom().nextFloat() >= SCHIANTO_PROBABILITA) {
            return false;
        }
        for (LivingEntity target : targets) {    // gia' in ordine di distanza
            if (target.distanceToSqr(player) > SCHIANTO_PORTATA * SCHIANTO_PORTATA || !sollevabile(target)) {
                continue;
            }
            PRESE.put(player.getUUID(), new Presa(false, List.of(target), SCHIANTO_ALTEZZA, ora));
            ModNetwork.syncVenomLash(player, target, getTargetCenter(target), Mossa.PRESA.ordinal(),
                    SCHIANTO_ARRIVA, SCHIANTO_GIU_FINO - SCHIANTO_ARRIVA, PRESA_RIENTRA);
            PROSSIMO_SCHIANTO.put(player.getUUID(), ora + SCHIANTO_RICARICA);
            cooldowns.put(target.getId(), ora + SCHIANTO_GIU_FINO);
            return true;
        }
        return false;
    }

    /** Due nemici sollevabili, abbastanza vicini fra loro da farli incontrare a mezz'aria. */
    private static boolean provaScontro(ServerPlayer player, List<LivingEntity> targets,
                                        Map<Integer, Long> cooldowns, long ora) {
        if (targets.size() < 2 || ora < PROSSIMO_SCONTRO.getOrDefault(player.getUUID(), 0L)
                || player.getRandom().nextFloat() >= SCONTRO_PROBABILITA) {
            return false;
        }
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity a = targets.get(i);
            if (!sollevabile(a)) {
                continue;
            }
            for (int j = i + 1; j < targets.size(); j++) {
                LivingEntity b = targets.get(j);
                double fra = a.distanceTo(b);
                if (!sollevabile(b) || fra > SCONTRO_DISTANZA_MAX || fra < SCONTRO_DISTANZA_MIN) {
                    continue;
                }
                PRESE.put(player.getUUID(), new Presa(true, List.of(a, b), SCONTRO_ALTEZZA, ora));
                for (LivingEntity preso : List.of(a, b)) {
                    ModNetwork.syncVenomLash(player, preso, getTargetCenter(preso), Mossa.PRESA.ordinal(),
                            SCONTRO_ARRIVA, SCONTRO_FINO - SCONTRO_ARRIVA, PRESA_RIENTRA);
                    cooldowns.put(preso.getId(), ora + SCONTRO_FINO);
                }
                PROSSIMO_SCONTRO.put(player.getUUID(), ora + SCONTRO_RICARICA);
                return true;
            }
        }
        return false;
    }

    private static void tickPresa(ServerPlayer player, long ora) {
        Presa presa = PRESE.get(player.getUUID());
        if (presa == null) {
            return;
        }
        long t = ora - presa.inizio;
        LivingEntity[] presi = new LivingEntity[presa.bersagli.length];
        for (int i = 0; i < presi.length; i++) {
            Entity entita = player.level().getEntity(presa.bersagli[i]);
            presi[i] = entita instanceof LivingEntity vivo && vivo.isAlive() ? vivo : null;
        }
        boolean finita = presa.scontro ? tickScontro(player, presa, presi, t) : tickSchianto(player, presa, presi[0], t);
        if (finita) {
            PRESE.remove(player.getUUID());
        }
    }

    /**
     * Lo schianto: il braccio arriva, alza il bersaglio di {@link #SCHIANTO_ALTEZZA} blocchi
     * portandolo davanti a noi, lo tiene un attimo sospeso e poi lo sbatte giu'. Il danno scende
     * quando tocca terra. Restituisce true quando e' finito.
     */
    private static boolean tickSchianto(ServerPlayer player, Presa presa, LivingEntity preso, long t) {
        if (preso == null) {
            return true;
        }
        if (t < SCHIANTO_ARRIVA) {
            return false;
        }
        double quota = presa.quota[0];
        if (t < SCHIANTO_SU_FINO) {
            muovi(preso, trascina(player, preso), preso.getY() < quota ? SCHIANTO_SALITA : 0.0D);
            return false;
        }
        if (t < SCHIANTO_TIENI_FINO) {
            muovi(preso, trascina(player, preso), Math.max(-0.2D, Math.min(0.2D, (quota - preso.getY()) * 0.5D)));
            return false;
        }
        muovi(preso, Vec3.ZERO, SCHIANTO_DISCESA);
        if (!preso.onGround() && t < SCHIANTO_GIU_FINO) {
            return false;
        }
        // a terra: il colpo, la polvere del blocco sotto e il tonfo
        preso.fallDistance = 0.0F;
        preso.hurt(player.damageSources().playerAttack(player), ATTACK_DAMAGE * SCHIANTO_DANNO);
        ServerLevel livello = player.serverLevel();
        BlockState sotto = livello.getBlockState(preso.blockPosition().below());
        if (!sotto.isAir()) {
            livello.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sotto),
                    preso.getX(), preso.getY() + 0.1D, preso.getZ(), 40, 0.6D, 0.1D, 0.6D, 0.15D);
        }
        livello.playSound(null, preso.getX(), preso.getY(), preso.getZ(), SuoniKlyntar.FEND_OFF_SLAM_GROUND.get(),
                SoundSource.PLAYERS, 0.45F, 1.3F);
        livello.playSound(null, preso.getX(), preso.getY(), preso.getZ(), SuoniKlyntar.FEND_OFF_SLAM_HIT.get(),
                SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /**
     * Lo scontro: i due bersagli salgono, poi ognuno va verso l'altro finche' non si toccano.
     * Se uno dei due muore o sparisce, l'altro viene lasciato andare.
     */
    private static boolean tickScontro(ServerPlayer player, Presa presa, LivingEntity[] presi, long t) {
        LivingEntity a = presi[0];
        LivingEntity b = presi[1];
        if (a == null || b == null || t >= SCONTRO_FINO) {
            return true;
        }
        if (t < SCONTRO_ARRIVA) {
            return false;
        }
        if (t < SCONTRO_SU_FINO) {
            muovi(a, Vec3.ZERO, a.getY() < presa.quota[0] ? SCONTRO_SALITA : 0.0D);
            muovi(b, Vec3.ZERO, b.getY() < presa.quota[1] ? SCONTRO_SALITA : 0.0D);
            return false;
        }
        double fra = a.distanceTo(b);
        if (fra > SCONTRO_IMPATTO) {
            double passo = Math.min(SCONTRO_PASSO, fra / 2.0D);
            muovi(a, b.position().subtract(a.position()).normalize().scale(passo), 0.0D);
            muovi(b, a.position().subtract(b.position()).normalize().scale(passo), 0.0D);
            return false;
        }
        // si toccano: tutti e due si fanno male e rimbalzano via
        for (LivingEntity preso : presi) {
            preso.hurt(player.damageSources().playerAttack(player), ATTACK_DAMAGE * SCONTRO_DANNO);
            preso.fallDistance = 0.0F;
        }
        Vec3 via = a.position().subtract(b.position());
        via = via.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : via.normalize();
        muovi(a, via.scale(SCONTRO_SPINTA), 0.2D);
        muovi(b, via.scale(-SCONTRO_SPINTA), 0.2D);
        ServerLevel livello = player.serverLevel();
        Vec3 centro = a.position().add(b.position()).scale(0.5D).add(0.0D, a.getBbHeight() * 0.5D, 0.0D);
        livello.sendParticles(ParticleTypes.CRIT, centro.x, centro.y, centro.z, 20, 0.4D, 0.4D, 0.4D, 0.3D);
        livello.playSound(null, centro.x, centro.y, centro.z, SuoniKlyntar.FEND_OFF_CLASH_HIT.get(),
                SoundSource.PLAYERS, 1.0F, 0.7F);
        livello.playSound(null, centro.x, centro.y, centro.z, SuoniKlyntar.FEND_OFF_CLASH_SQUISH.get(),
                SoundSource.PLAYERS, 0.8F, 0.8F);
        return true;
    }

    /** Verso il punto davanti a noi dove il bersaglio va tenuto, con una velocita' massima. */
    private static Vec3 trascina(ServerPlayer player, LivingEntity preso) {
        Vec3 sguardo = player.getLookAngle();
        Vec3 davanti = new Vec3(sguardo.x, 0.0D, sguardo.z);
        davanti = davanti.lengthSqr() < 1.0E-4D ? Vec3.ZERO : davanti.normalize().scale(TRASCINA_DAVANTI);
        Vec3 punto = player.position().add(davanti);
        Vec3 verso = new Vec3(punto.x - preso.getX(), 0.0D, punto.z - preso.getZ()).scale(TRASCINA_GUADAGNO);
        return verso.length() > TRASCINA_MAX ? verso.normalize().scale(TRASCINA_MAX) : verso;
    }

    /**
     * La velocita' di chi e' tenuto, imposta a ogni tick. hurtMarked la manda al client: per un
     * giocatore e' l'unico modo di muoverlo, e anche ai mob evita che scattino avanti e indietro.
     */
    private static void muovi(LivingEntity preso, Vec3 orizzontale, double verticale) {
        preso.setDeltaMovement(orizzontale.x, verticale, orizzontale.z);
        preso.hasImpulse = true;
        preso.hurtMarked = true;
        if (verticale >= 0.0D) {
            preso.fallDistance = 0.0F;
        }
        if (preso instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static boolean sollevabile(LivingEntity entita) {
        return !entita.getType().is(Tags.EntityTypes.BOSSES)
                && entita.getBbWidth() <= SOLLEVABILE_LARGHEZZA
                && entita.getBbHeight() <= SOLLEVABILE_ALTEZZA
                && !entita.isPassenger() && !entita.isVehicle();
    }

    private static boolean tenuto(ServerPlayer player, LivingEntity entita) {
        Presa presa = PRESE.get(player.getUUID());
        return presa != null && presa.tiene(entita.getId());
    }

    private static void suona(ServerPlayer player, LivingEntity target, SoundEvent suono, float volume, long gameTime) {
        if (ULTIMO_SUONO.getOrDefault(player.getUUID(), -1L) == gameTime) {
            return;
        }
        ULTIMO_SUONO.put(player.getUUID(), gameTime);
        player.level().playSound(null, target.getX(), target.getY(0.55D), target.getZ(), suono,
                SoundSource.PLAYERS, volume, 0.85F + player.getRandom().nextFloat() * 0.3F);
    }

    // ------------------------------------------------------------------ bersagli

    private static void dimenticaAggressori(ServerPlayer player, long gameTime) {
        Map<Integer, Long> aggressori = AGGRESSORI.get(player.getUUID());
        if (aggressori != null) {
            aggressori.values().removeIf(quando -> gameTime - quando > MEMORIA_AGGRESSORI);
        }
        Map<Integer, Long> contrattacchi = CONTRATTACCHI.get(player.getUUID());
        if (contrattacchi != null) {
            contrattacchi.values().removeIf(quando -> gameTime - quando > CONTRATTACCO_PAUSA);
        }
    }

    private static List<LivingEntity> findTargets(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(ATTACK_RANGE);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> canAttackEntity(entity, player, ATTACK_RANGE));
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        if (targets.size() > MAX_ARMS) {
            return new ArrayList<>(targets.subList(0, MAX_ARMS));
        }
        return targets;
    }

    private static boolean canAttackEntity(LivingEntity entity, ServerPlayer player, double portata) {
        if (entity == null || !entity.isAlive() || entity.isSpectator() || entity == player) {
            return false;
        }
        if (entity.distanceToSqr(player) > portata * portata) {
            return false;
        }
        if (!player.hasLineOfSight(entity)) {
            return false;
        }
        if (entity instanceof Player targetPlayer) {
            // un avversario in PvP, oppure chi ci ha aggredito da poco, alleato compreso
            return modKlyntar.player.PvpRules.colpibile(player, targetPlayer)
                    || eAggressore(player, targetPlayer);
        }
        if (eAggressore(player, entity)) {
            return true;
        }
        // un neutrale si tocca solo se ce l'ha gia' con noi. Enderman e piglin zombie sono
        // anche mostri: prima bastava passargli accanto perche' partisse la frusta, e colpirne
        // uno scatena tutto il gruppo
        if (entity instanceof NeutralMob neutrale) {
            return neutrale.isAngryAt(player) || (entity instanceof Mob mob && mob.getTarget() == player);
        }
        if (entity instanceof Enemy) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() == player;
    }

    private static boolean eAggressore(ServerPlayer victim, LivingEntity entita) {
        Map<Integer, Long> aggressori = AGGRESSORI.get(victim.getUUID());
        if (aggressori == null) {
            return false;
        }
        Long quando = aggressori.get(entita.getId());
        return quando != null && victim.level().getGameTime() - quando <= MEMORIA_AGGRESSORI;
    }

    private static Vec3 getTargetCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
    }

    private static void knockTargetBack(ServerPlayer player, LivingEntity target) {
        spingi(player, target, 0.35D, 0.12D);
    }

    /** Via da noi, in orizzontale, con un po' di slancio verso l'alto. */
    private static void spingi(ServerPlayer player, LivingEntity target, double forza, double alza) {
        Vec3 push = target.position().subtract(player.position());
        if (push.lengthSqr() < 1.0E-4D) {
            push = player.getLookAngle();
        }
        push = new Vec3(push.x, 0.0D, push.z);
        push = push.lengthSqr() < 1.0E-4D ? Vec3.ZERO : push.normalize().scale(forza);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, alza, push.z));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static void cleanupCooldowns(Map<Integer, Long> cooldowns, long gameTime) {
        Iterator<Map.Entry<Integer, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > 40L) {
                iterator.remove();
            }
        }
    }

    private static int getScore(ServerPlayer player, String objectiveName, boolean create) {
        Objective objective = getObjective(player, objectiveName, create);
        if (objective == null) {
            return 0;
        }
        Score score = player.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static Objective getObjective(Player player, String objectiveName, boolean create) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null && create) {
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }
}
