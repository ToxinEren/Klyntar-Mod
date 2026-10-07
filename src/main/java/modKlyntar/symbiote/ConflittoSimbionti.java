package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.player.VenomSymbioteSystemsHandler;
import modKlyntar.symbiote.RegistroSimbionti.Simbionte;
import modKlyntar.symbiote.RegistroSimbionti.Temperamento;
import modKlyntar.symbiote.RegistroSimbionti.Tratto;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Due simbionti nello stesso corpo.
 *
 * <p>Regola base del documento di design: se due simbionti finiscono nello stesso corpo, a
 * seconda della personalita' uno fugge oppure attacca l'altro fino a inglobarlo. Il secondo
 * entra quando un mob simbionte raggiunge chi ne porta gia' uno (i dominanti lo braccano
 * apposta), quando il giocatore stesso ne colpisce uno, o quando nasce un figlio dal padre
 * indossato.</p>
 *
 * <p>La reazione la decide il temperamento di chi e' dentro:</p>
 * <ul>
 *   <li><b>protettivo</b> (Venom) - difende l'ospite e combatte solo se l'altro attacca; con un
 *   bond basso e' freddo e sospettoso, e a volte se ne va lui;</li>
 *   <li><b>dominante, psicopatico</b> (Riot, Carnage) - attaccano sempre;</li>
 *   <li><b>cacciatore</b> (Toxin) - combatte se l'altro e' piu' debole, altrimenti lo lascia andare;</li>
 *   <li><b>purificatore</b> (Anti-Venom) - espelle l'altro, se non e' molto piu' forte;</li>
 *   <li><b>timido</b> (Life Foundation) - scappa;</li>
 *   <li><b>sovrano</b> (All-Black) - domina sempre: vedi {@link UnioneDoppia}.</li>
 * </ul>
 *
 * <p>Il ritmo viene dal "graft" della mod Symbiote, il secondo simbionte innestato nel corpo:</p>
 * <ol>
 *   <li><b>l'ingresso</b> - quello di dentro si accorge dell'altro e si oppone. Due secondi e
 *   mezzo in cui l'ospite resta quasi fermo e i filamenti dei due, ognuno del suo colore, si
 *   frustano a turno fra il corpo e il punto da cui l'altro e' entrato. Alla fine la reazione:
 *   lo respinge, gli cede il posto o lo affronta;</li>
 *   <li><b>la lotta</b> - una tensione da 0 a 100 che sale ogni secondo, piu' in fretta coi
 *   temperamenti violenti e con un simbionte affamato. L'ospite subisce danni e nausea, i due
 *   parlano a turno (l'intruso col suo nome davanti), e quello di dentro avvisa a 60 e a 85.
 *   Mangiare a meta' lotta nutre quello di dentro e gli da' un piccolo vantaggio;</li>
 *   <li><b>la resa</b> - a 100 uno dei due prevale. Chi perde implora, chi vince lo strappa
 *   via: assorbimento o fuga.</li>
 * </ol>
 *
 * <p>La lotta non uccide l'ospite: i due si contendono un corpo, non vogliono un cadavere. I
 * danni del conflitto si fermano a mezzo cuore, come nella mod Symbiote.</p>
 *
 * <p>Gli esiti: la <b>fuga</b> (chi perde o chi e' timido esce e torna un mob, da ricatturare) e
 * l'<b>assorbimento</b> (il vincitore digerisce l'altro e ne prende le abilita'). Il bond di chi
 * esce o viene inglobato resta salvato: e' nel suo objective di affinita'.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class ConflittoSimbionti {
    private static final Logger LOGGER = LogManager.getLogger("KlyntarConflitto");

    // ---- l'ingresso
    /** Quanto dura la scena dell'ingresso; alla fine scatta la reazione. */
    private static final int INGRESSO = 50;
    // ---- la lotta
    private static final int OGNI_TENSIONE = 20;
    /** Quanto sale la tensione al secondo: a 4, venticinque secondi di lotta. */
    private static final double PRESSIONE_BASE = 4.0D;
    private static final double PRESSIONE_PSICOPATICO = 1.0D;
    private static final double PRESSIONE_DOMINANTE = 0.5D;
    private static final double PRESSIONE_FAME = 0.5D;
    /** Sotto questa fame (su 100) il simbionte di dentro e' affamato: si agita e lotta peggio. */
    private static final int FAME_BASSA = 25;
    private static final double MALUS_FAME = 0.05D;
    private static final int SOGLIA_AVVISO = 60;
    private static final int SOGLIA_ULTIMO = 85;
    private static final int TENSIONE_MASSIMA = 100;
    private static final int OGNI_DANNO = 60;
    private static final float DANNO = 1.0F;
    private static final int MALUS = 100;
    /** Le due voci si alternano, ma non ogni tre secondi: otto fra una battuta e l'altra della stessa. */
    private static final long RICARICA_BATTUTE = 20L * 8L;
    // ---- la resa
    private static final int RESA = 70;
    private static final int RESA_SUPPLICA = 15;
    private static final int RESA_COLPO = 55;
    private static final float DANNO_RESA = 4.0F;
    // ---- il pasto
    private static final double SPINTA_PASTO = 0.05D;
    private static final double SPINTA_MASSIMA = 0.15D;
    /** Un Venom col bond basso, davanti a chi non lo minaccia, a volte preferisce andarsene. */
    private static final double CEDE_COL_BOND_BASSO = 0.35D;
    /** Nelle scene l'ospite resta quasi fermo mentre i due si strappano il corpo. */
    private static final int LENTEZZA = 4;

    private enum Reazione { LOTTA, RESPINGE, CEDE }

    private enum Fase { INGRESSO, LOTTA, RESA }

    private static final class Lotta {
        final String dentro;
        final String intruso;
        final Reazione reazione;
        /** Da dove e' entrato l'intruso: i filamenti dell'ingresso partono da qui. */
        final Vec3 da;
        Fase fase = Fase.INGRESSO;
        long dal;
        int tensione;
        /** Il vantaggio che l'ospite da' a quello di dentro mangiando a meta' lotta. */
        double spinta;
        /** Quanta forza ha l'intruso dopo il duello: 1 intatto, 0.5 allo stremo. */
        double ferite = 1.0D;
        String vincitore;
        String perdente;

        Lotta(String dentro, String intruso, Reazione reazione, Vec3 da, long dal) {
            this.dentro = dentro;
            this.intruso = intruso;
            this.reazione = reazione;
            this.da = da;
            this.dal = dal;
        }
    }

    private static final Map<UUID, Lotta> LOTTE = new ConcurrentHashMap<>();

    private ConflittoSimbionti() {
    }

    public static boolean inCorso(Player giocatore) {
        return LOTTE.containsKey(giocatore.getUUID());
    }

    /** Il simbionte con cui si sta lottando, per la barra della fame; null se nessuno. */
    public static String avversario(Player giocatore) {
        Lotta l = LOTTE.get(giocatore.getUUID());
        return l == null ? null : l.intruso;
    }

    /** La tensione della lotta, da 0 a 100, per la barra; -1 se non c'e' lotta. */
    public static int tensione(Player giocatore) {
        Lotta l = LOTTE.get(giocatore.getUUID());
        if (l == null) {
            return -1;
        }
        return switch (l.fase) {
            case INGRESSO -> 0;
            case LOTTA -> l.tensione;
            case RESA -> TENSIONE_MASSIMA;
        };
    }

    // ------------------------------------------------------------------ l'ingresso

    /** Come sotto, con l'intruso che arriva da davanti all'ospite (il figlio appena nato). */
    public static boolean entra(ServerPlayer ospite, String intruso) {
        Vec3 davanti = ospite.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (davanti.lengthSqr() < 1.0E-4D) {
            davanti = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return entra(ospite, intruso, ospite.position().add(davanti.normalize().scale(1.5D)).add(0.0D, 0.6D, 0.0D));
    }

    /**
     * Un secondo simbionte entra nel corpo di chi ne ha gia' uno.
     *
     * @param da da dove arriva, per i filamenti della scena
     * @return true se il simbionte che entrava e' stato preso in carico: il mob va tolto dal
     * mondo. False se non c'e' niente da fare (stessa famiglia, lotta gia' in corso, ospite vuoto).
     */
    public static boolean entra(ServerPlayer ospite, String intruso, Vec3 da) {
        return entra(ospite, intruso, da, 1.0D);
    }

    /**
     * Come sopra, con la vita che all'intruso e' rimasta dopo il duello coi tentacoli (da 0 a 1):
     * un simbionte malconcio entra piu' debole, fino a meta' della sua forza. Logorarlo prima e
     * poi lasciarlo entrare e' il modo di assorbirlo piu' facilmente.
     */
    public static boolean entra(ServerPlayer ospite, String intruso, Vec3 da, double salute) {
        String dentro = SymbioteState.forma(ospite);
        // in un corpo che sta morendo non entra nessuno: la lotta continuerebbe dopo la rinascita
        if (dentro.isEmpty() || !ospite.isAlive() || ospite.isDeadOrDying() || LOTTE.containsKey(ospite.getUUID())
                || RegistroSimbionti.famiglia(dentro).equals(RegistroSimbionti.famiglia(intruso))) {
            return false;
        }
        Simbionte sDentro = RegistroSimbionti.di(dentro);
        Simbionte sIntruso = RegistroSimbionti.di(intruso);
        if (sDentro == null || sIntruso == null) {
            return false;
        }
        // All-Black non entra in conflitto: domina, o vive in doppia unione
        if (sIntruso.temperamento() == Temperamento.SOVRANO) {
            UnioneDoppia.entraAllBlack(ospite);
            return true;
        }
        if (sDentro.temperamento() == Temperamento.SOVRANO) {
            assorbe(ospite, dentro, intruso);
            return true;
        }
        RandomSource caso = ospite.getRandom();
        int forzaDentro = ProfiliSimbionti.forza(ospite, dentro);
        double ferite = 0.5D + 0.5D * Math.max(0.0D, Math.min(1.0D, salute));
        int forzaIntruso = (int) Math.round(ProfiliSimbionti.forza(ospite, intruso) * ferite);
        Reazione reazione = reazione(sDentro, sIntruso, forzaDentro, forzaIntruso,
                ProfiliSimbionti.livelloBond(ospite, dentro), caso);
        LOGGER.info("{} carries {} ({}), {} ({}) enters: {}", ospite.getGameProfile().getName(),
                dentro, forzaDentro, intruso, forzaIntruso, reazione);
        Lotta lotta = new Lotta(dentro, intruso, reazione, da, ospite.level().getGameTime());
        lotta.ferite = ferite;
        LOTTE.put(ospite.getUUID(), lotta);
        apriIngresso(ospite, lotta);
        return true;
    }

    private static Reazione reazione(Simbionte dentro, Simbionte intruso, int forzaDentro, int forzaIntruso,
                                     int bondDentro, RandomSource caso) {
        boolean minacciato = intruso.temperamento().attacca()
                || (intruso.temperamento() == Temperamento.CACCIATORE && forzaIntruso > forzaDentro);
        return switch (dentro.temperamento()) {
            case PROTETTIVO -> {
                if (bondDentro <= 1 && caso.nextDouble() < CEDE_COL_BOND_BASSO) {
                    // freddo e sospettoso: l'ospite non vale ancora la pena
                    yield Reazione.CEDE;
                }
                yield minacciato ? Reazione.LOTTA : Reazione.RESPINGE;
            }
            case DOMINANTE, PSICOPATICO -> Reazione.LOTTA;
            case CACCIATORE -> forzaIntruso < forzaDentro ? Reazione.LOTTA : Reazione.RESPINGE;
            case PURIFICATORE -> forzaIntruso > forzaDentro * 3 / 2 ? Reazione.LOTTA : Reazione.RESPINGE;
            case TIMIDO -> intruso.temperamento() == Temperamento.TIMIDO && forzaDentro >= forzaIntruso
                    ? Reazione.RESPINGE : Reazione.CEDE;
            case SOVRANO -> Reazione.LOTTA;
        };
    }

    /** Quello di dentro si accorge dell'altro: si oppone, e i filamenti dell'intruso arrivano. */
    private static void apriIngresso(ServerPlayer ospite, Lotta lotta) {
        VoceSimbionte.di(ospite, "conflitto_obietta", Tono.AVVISO, true);
        blocca(ospite, INGRESSO + 10);
        ospite.level().playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                modKlyntar.sound.SuoniKlyntar.SYMBIOTE_GRAB.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        if (ospite.level() instanceof ServerLevel livello) {
            for (int i = 0; i < 3; i++) {
                Vec3 partenza = lotta.da.add((livello.random.nextDouble() - 0.5D) * 0.6D,
                        (livello.random.nextDouble() - 0.5D) * 0.4D, (livello.random.nextDouble() - 0.5D) * 0.6D);
                frusta(livello, partenza, corpo(ospite), RegistroSimbionti.colore(lotta.intruso));
            }
        }
    }

    // ------------------------------------------------------------------ il tick

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || !(ospite.level() instanceof ServerLevel livello)) {
            return;
        }
        Lotta lotta = LOTTE.get(ospite.getUUID());
        if (lotta == null || !ospite.isAlive()) {
            return;
        }
        // il simbionte di dentro e' andato via per conto suo (strappato, tolto): la lotta finisce
        if (!lotta.dentro.equals(SymbioteState.forma(ospite))) {
            LOTTE.remove(ospite.getUUID());
            liberaMob(ospite, lotta.intruso, true);
            return;
        }
        long ora = livello.getGameTime();
        int t = (int) (ora - lotta.dal);
        switch (lotta.fase) {
            case INGRESSO -> tickIngresso(ospite, livello, lotta, t, ora);
            case LOTTA -> tickLotta(ospite, livello, lotta, t, ora);
            case RESA -> tickResa(ospite, livello, lotta, t);
        }
    }

    private static void tickIngresso(ServerPlayer ospite, ServerLevel livello, Lotta lotta, int t, long ora) {
        // i due si frustano a turno: uno dal corpo verso l'intruso, l'altro di ritorno
        if (t >= 20 && t < 45 && t % 5 == 0) {
            boolean frustaDentro = (t / 5) % 2 == 0;
            Vec3 corpo = corpo(ospite);
            frusta(livello, frustaDentro ? corpo : lotta.da, frustaDentro ? lotta.da : corpo,
                    RegistroSimbionti.colore(frustaDentro ? lotta.dentro : lotta.intruso));
        }
        if (t == 25) {
            livello.playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                    modKlyntar.sound.SuoniKlyntar.SYMBIOTE_GRAB.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
        }
        if (t < INGRESSO) {
            return;
        }
        switch (lotta.reazione) {
            case RESPINGE -> {
                LOTTE.remove(ospite.getUUID());
                scoppio(livello, corpo(ospite), RegistroSimbionti.colore(lotta.dentro));
                fugge(ospite, lotta.intruso, lotta.dentro);
            }
            case CEDE -> {
                LOTTE.remove(ospite.getUUID());
                if (RegistroSimbionti.di(lotta.intruso).temperamento().nonLasciaScappare()) {
                    // a Riot non si cede il posto e basta: chi si arrende viene inghiottito
                    assorbe(ospite, lotta.intruso, lotta.dentro);
                } else {
                    fugge(ospite, lotta.dentro, lotta.intruso);
                }
            }
            case LOTTA -> {
                lotta.fase = Fase.LOTTA;
                lotta.dal = ora;
                VoceSimbionte.di(ospite, "conflitto_inizio", Tono.AGGRESSIVO, true);
                ospite.displayClientMessage(Component.translatable("klyntars.conflitto.inizio",
                        nome(lotta.dentro), nome(lotta.intruso)), true);
            }
        }
    }

    private static void tickLotta(ServerPlayer ospite, ServerLevel livello, Lotta lotta, int t, long ora) {
        if (t > 0 && t % OGNI_TENSIONE == 0) {
            int prima = lotta.tensione;
            double pressione = pressione(ospite, lotta);
            int sale = Mth.floor(pressione) + (livello.random.nextDouble() < pressione - Math.floor(pressione) ? 1 : 0);
            lotta.tensione = Math.min(TENSIONE_MASSIMA, lotta.tensione + sale);
            if (prima < SOGLIA_AVVISO && lotta.tensione >= SOGLIA_AVVISO) {
                VoceSimbionte.di(ospite, "conflitto_avviso", Tono.AVVISO, true);
            } else if (prima < SOGLIA_ULTIMO && lotta.tensione >= SOGLIA_ULTIMO) {
                VoceSimbionte.di(ospite, "conflitto_ultimo", Tono.AVVISO, true);
            }
        }
        if (t > 0 && t % OGNI_DANNO == 0) {
            ferisci(ospite, DANNO);
            ospite.addEffect(new MobEffectInstance(MobEffects.CONFUSION, MALUS, 0, false, false));
            ospite.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MALUS, 0, false, false));
            Vec3 corpo = corpo(ospite);
            livello.sendParticles(ParticleTypes.SQUID_INK, corpo.x, corpo.y, corpo.z, 10, 0.35D, 0.5D, 0.35D, 0.02D);
            for (int i = 0; i < 2; i++) {
                String chi = i == 0 ? lotta.dentro : lotta.intruso;
                frusta(livello, corpo, intorno(livello, corpo), RegistroSimbionti.colore(chi));
            }
            // parlano a turno: quello di dentro e l'intruso, col suo nome davanti
            if ((t / OGNI_DANNO) % 2 == 1) {
                VoceSimbionte.di(ospite, "conflitto_lotta", Tono.AGGRESSIVO, false, RICARICA_BATTUTE);
            } else {
                VoceSimbionte.diCome(ospite, lotta.intruso, "conflitto_intruso", Tono.AGGRESSIVO, false,
                        RICARICA_BATTUTE);
            }
        }
        if (lotta.tensione >= TENSIONE_MASSIMA) {
            apriResa(ospite, livello, lotta, ora);
        }
    }

    /**
     * Quanto sale la tensione in un secondo: i temperamenti violenti la fanno salire prima, e
     * cosi' un simbionte di dentro affamato, che si agita.
     */
    private static double pressione(ServerPlayer ospite, Lotta lotta) {
        Temperamento dentro = RegistroSimbionti.di(lotta.dentro).temperamento();
        Temperamento intruso = RegistroSimbionti.di(lotta.intruso).temperamento();
        double pressione = PRESSIONE_BASE;
        if (dentro == Temperamento.PSICOPATICO || intruso == Temperamento.PSICOPATICO) {
            pressione += PRESSIONE_PSICOPATICO;
        }
        if (dentro == Temperamento.DOMINANTE || intruso == Temperamento.DOMINANTE) {
            pressione += PRESSIONE_DOMINANTE;
        }
        if (VenomSymbioteSystemsHandler.getHunger(ospite) < FAME_BASSA) {
            pressione += PRESSIONE_FAME;
        }
        return pressione;
    }

    // ------------------------------------------------------------------ la resa

    /** La tensione e' al massimo: si decide chi prevale, e chi vince apre la scena finale. */
    private static void apriResa(ServerPlayer ospite, ServerLevel livello, Lotta lotta, long ora) {
        RandomSource caso = ospite.getRandom();
        Simbionte dentro = RegistroSimbionti.di(lotta.dentro);
        int forzaDentro = ProfiliSimbionti.forza(ospite, lotta.dentro);
        int forzaIntruso = (int) Math.round(ProfiliSimbionti.forza(ospite, lotta.intruso) * lotta.ferite);
        double vinceDentro = forzaDentro / (double) (forzaDentro + forzaIntruso);
        if (dentro.temperamento() == Temperamento.PROTETTIVO) {
            // un Venom col bond alto tende a restare e difendere l'ospite
            int bond = ProfiliSimbionti.livelloBond(ospite, lotta.dentro);
            vinceDentro += bond >= 2 ? 0.15D : bond <= 1 ? -0.10D : 0.0D;
        }
        if (VenomSymbioteSystemsHandler.getHunger(ospite) < FAME_BASSA) {
            vinceDentro -= MALUS_FAME;
        }
        vinceDentro += lotta.spinta;
        vinceDentro = Math.max(0.1D, Math.min(0.9D, vinceDentro));
        boolean dentroVince = caso.nextDouble() < vinceDentro;
        lotta.vincitore = dentroVince ? lotta.dentro : lotta.intruso;
        lotta.perdente = dentroVince ? lotta.intruso : lotta.dentro;
        lotta.fase = Fase.RESA;
        lotta.dal = ora;
        LOGGER.info("Conflict of {}: {} prevails over {} ({}%)", ospite.getGameProfile().getName(),
                lotta.vincitore, lotta.perdente, Math.round(vinceDentro * 100.0D));
        blocca(ospite, RESA + 10);
        VoceSimbionte.diCome(ospite, lotta.vincitore, "conflitto_resa", Tono.AGGRESSIVO, true, 0L);
        scoppio(livello, corpo(ospite), RegistroSimbionti.colore(lotta.vincitore));
    }

    private static void tickResa(ServerPlayer ospite, ServerLevel livello, Lotta lotta, int t) {
        if (t == RESA_SUPPLICA) {
            VoceSimbionte.diCome(ospite, lotta.perdente, "conflitto_supplica", Tono.AVVISO, true, 0L);
        }
        // chi vince strappa l'altro dal corpo, un filamento alla volta
        if (t >= 20 && t < 50 && t % 5 == 0) {
            Vec3 corpo = corpo(ospite);
            frusta(livello, corpo, intorno(livello, corpo), RegistroSimbionti.colore(lotta.vincitore));
        }
        if (t == 25 || t == 45) {
            livello.playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                    modKlyntar.sound.SuoniKlyntar.SYMBIOTE_GRAB.get(), SoundSource.PLAYERS, 0.9F, t == 45 ? 0.8F : 0.95F);
        }
        if (t < RESA_COLPO) {
            return;
        }
        LOTTE.remove(ospite.getUUID());
        ferisci(ospite, DANNO_RESA);
        scoppio(livello, corpo(ospite), RegistroSimbionti.colore(lotta.perdente));
        esito(ospite, lotta);
    }

    /** Chi ha perso scappa o viene inglobato. */
    private static void esito(ServerPlayer ospite, Lotta lotta) {
        RandomSource caso = ospite.getRandom();
        Simbionte sVincitore = RegistroSimbionti.di(lotta.vincitore);
        Simbionte sPerdente = RegistroSimbionti.di(lotta.perdente);
        double fuga = sPerdente.temperamento() == Temperamento.TIMIDO ? 0.6D : 0.35D;
        if (sVincitore.temperamento().vuoleAssorbire()) {
            fuga *= 0.6D;
        }
        if (sVincitore.temperamento().nonLasciaScappare()) {
            fuga = 0.0D;
        }
        if (sPerdente.temperamento() == Temperamento.PROTETTIVO
                && ProfiliSimbionti.livelloBond(ospite, lotta.perdente) <= 1) {
            fuga += 0.15D;
        }
        LOGGER.info("Conflict of {}: {} wins over {}", ospite.getGameProfile().getName(), lotta.vincitore, lotta.perdente);
        if (caso.nextDouble() < fuga) {
            fugge(ospite, lotta.perdente, lotta.vincitore);
        } else {
            assorbe(ospite, lotta.vincitore, lotta.perdente);
        }
    }

    // ------------------------------------------------------------------ il pasto

    /**
     * L'ospite mangia a meta' lotta: nutre quello di dentro, che ne esce un po' piu' forte. E'
     * l'unica cosa che il giocatore puo' fare per il suo simbionte mentre l'altro glielo contende.
     */
    @SubscribeEvent
    public static void onMangia(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer ospite) || !event.getItem().isEdible()) {
            return;
        }
        Lotta lotta = LOTTE.get(ospite.getUUID());
        if (lotta == null || lotta.fase != Fase.LOTTA || lotta.spinta >= SPINTA_MASSIMA) {
            return;
        }
        lotta.spinta = Math.min(SPINTA_MASSIMA, lotta.spinta + SPINTA_PASTO);
        VoceSimbionte.di(ospite, "conflitto_nutrito", Tono.LEGAME_SU, true, 100L);
    }

    // ------------------------------------------------------------------ gli esiti

    /** Il vincitore digerisce il perdente e ne prende le abilita'; se era l'intruso, ora e' lui l'ospite. */
    public static void assorbe(ServerPlayer ospite, String vincitore, String perdente) {
        List<Tratto> nuovi = ProfiliSimbionti.digerisci(ospite, vincitore, perdente);
        if (!vincitore.equals(SymbioteState.forma(ospite))) {
            cambiaForma(ospite, vincitore);
        }
        VoceSimbionte.di(ospite, "conflitto_assorbe", Tono.AGGRESSIVO, true);
        ospite.displayClientMessage(Component.translatable("klyntars.conflitto.assorbe",
                nome(vincitore), nome(perdente)), false);
        for (Tratto t : nuovi) {
            ospite.displayClientMessage(Component.translatable("klyntars.conflitto.tratto",
                    Component.translatable("klyntars.tratto." + t.name().toLowerCase(java.util.Locale.ROOT))), false);
        }
        ospite.level().playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.5F);
        LOGGER.info("{}: {} absorbed {} (+{})", ospite.getGameProfile().getName(), vincitore, perdente, nuovi);
    }

    /** Chi scappa esce dal corpo e torna un mob; se era quello di dentro, l'altro prende il suo posto. */
    private static void fugge(ServerPlayer ospite, String chiScappa, String chiResta) {
        boolean eraDentro = chiScappa.equals(SymbioteState.forma(ospite));
        if (eraDentro) {
            cambiaForma(ospite, chiResta);
            VoceSimbionte.di(ospite, "conflitto_subentra", Tono.AGGRESSIVO, true);
        } else {
            VoceSimbionte.di(ospite, "conflitto_respinto", Tono.NEUTRO, true);
        }
        ospite.displayClientMessage(Component.translatable("klyntars.conflitto.fuga", nome(chiScappa)), true);
        liberaMob(ospite, chiScappa, true);
    }

    /** Il simbionte di dentro lascia il posto a un altro: la trasformazione rifa' il corpo. */
    public static void cambiaForma(ServerPlayer ospite, String forma) {
        PlayerPowerCapability.infectPlayer(ospite, forma);
    }

    /** Fa uscire un simbionte come mob accanto all'ospite, in fuga se serve. */
    private static void liberaMob(ServerPlayer ospite, String forma, boolean inFuga) {
        if (!(ospite.level() instanceof ServerLevel livello)) {
            return;
        }
        String formaMob = RegistroSimbionti.formaDelMob(forma) ? forma : RegistroSimbionti.famiglia(forma);
        if (!RegistroSimbionti.formaDelMob(formaMob)) {
            return;
        }
        SymbioteEntity mob = MyMod.SYMBIOTE_ENTITY.get().create(livello);
        if (mob == null) {
            return;
        }
        mob.setForma(formaMob);
        Vec3 sguardo = ospite.getLookAngle();
        Vec3 lato = new Vec3(-sguardo.z, 0.0D, sguardo.x);
        if (lato.lengthSqr() < 1.0E-4D) {
            lato = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 dove = ospite.position().add(lato.normalize().scale(2.0D));
        mob.moveTo(dove.x, ospite.getY(), dove.z, ospite.getYRot(), 0.0F);
        livello.addFreshEntity(mob);
        if (inFuga) {
            mob.scappaDa(ospite);
        }
        livello.playSound(null, mob.blockPosition(), modKlyntar.sound.SuoniKlyntar.SYMBIOTE_EMERGE.get(),
                SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    private static Component nome(String forma) {
        Simbionte s = RegistroSimbionti.di(forma);
        return Component.literal(s == null ? forma : s.nome());
    }

    // ------------------------------------------------------------------ il corpo e i filamenti

    /** I danni del conflitto non uccidono: si fermano a mezzo cuore. */
    private static void ferisci(ServerPlayer ospite, float danno) {
        float margine = ospite.getHealth() - 1.0F;
        if (margine > 0.0F) {
            ospite.hurt(ospite.damageSources().magic(), Math.min(danno, margine));
        }
    }

    /** L'ospite resta quasi fermo: la lentezza forte, non un blocco, per non lasciarlo inerme. */
    private static void blocca(ServerPlayer ospite, int durata) {
        ospite.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durata, LENTEZZA, false, false));
    }

    private static Vec3 corpo(ServerPlayer ospite) {
        return ospite.position().add(0.0D, 1.0D, 0.0D);
    }

    /** Un punto a caso a un metro dal corpo: dove arriva un filamento che si stacca. */
    private static Vec3 intorno(ServerLevel livello, Vec3 corpo) {
        double angolo = livello.random.nextDouble() * Math.PI * 2.0D;
        return corpo.add(Math.cos(angolo) * 1.2D, (livello.random.nextDouble() - 0.4D) * 0.8D, Math.sin(angolo) * 1.2D);
    }

    /** Un filamento di particelle del colore del simbionte, da un punto all'altro, un po' curvo. */
    private static void frusta(ServerLevel livello, Vec3 da, Vec3 a, int colore) {
        DustParticleOptions polvere = polvere(colore, 1.1F);
        Vec3 tratto = a.subtract(da);
        double lunghezza = tratto.length();
        if (lunghezza < 1.0E-3D) {
            return;
        }
        Vec3 lato = new Vec3(-tratto.z, 0.0D, tratto.x);
        lato = lato.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : lato.normalize();
        double curva = (livello.random.nextDouble() - 0.5D) * 0.8D;
        int punti = Math.max(4, (int) (lunghezza * 5.0D));
        for (int i = 0; i <= punti; i++) {
            double f = i / (double) punti;
            Vec3 p = da.add(tratto.scale(f)).add(lato.scale(Math.sin(f * Math.PI) * curva));
            livello.sendParticles(polvere, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Uno sbuffo di particelle del colore del simbionte attorno al corpo. */
    private static void scoppio(ServerLevel livello, Vec3 centro, int colore) {
        livello.sendParticles(polvere(colore, 1.4F), centro.x, centro.y, centro.z, 30, 0.4D, 0.6D, 0.4D, 0.05D);
        livello.sendParticles(ParticleTypes.SQUID_INK, centro.x, centro.y, centro.z, 8, 0.3D, 0.4D, 0.3D, 0.03D);
    }

    private static DustParticleOptions polvere(int colore, float grandezza) {
        return new DustParticleOptions(new Vector3f(((colore >> 16) & 0xFF) / 255.0F,
                ((colore >> 8) & 0xFF) / 255.0F, (colore & 0xFF) / 255.0F), grandezza);
    }

    // ------------------------------------------------------------------ la pulizia

    /** Chi esce dal gioco o muore a meta' lotta: l'intruso torna un mob, da ricatturare. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer ospite) {
            Lotta lotta = LOTTE.remove(ospite.getUUID());
            if (lotta != null) {
                liberaMob(ospite, lotta.intruso, false);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer ospite) {
            Lotta lotta = LOTTE.remove(ospite.getUUID());
            if (lotta != null) {
                liberaMob(ospite, lotta.intruso, false);
            }
        }
    }
}
