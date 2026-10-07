package modKlyntar.entity.custom;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import modKlyntar.capability.PlayerPowerCapability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import modKlyntar.power.PlayersPower;
import modKlyntar.power.PlayersPowerProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class SymbioteEntity extends Mob implements GeoEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Animal hostAnimal;

    /** L'ospite che i tentacoli hanno agganciato, o 0. Sincronizzato: serve al disegno. */
    private static final EntityDataAccessor<Integer> AGGANCIATO =
            SynchedEntityData.defineId(SymbioteEntity.class, EntityDataSerializers.INT);

    /**
     * Che simbionte e': "venom" o "riot". Sincronizzato, perche' il client lo disegna del suo
     * colore, e salvato col mondo. E' la forma che prende chi ci si lega.
     */
    private static final EntityDataAccessor<String> FORMA =
            SynchedEntityData.defineId(SymbioteEntity.class, EntityDataSerializers.STRING);
    public static final String FORMA_BASE = "venom";
    private static final String CHIAVE_FORMA = "Forma";
    /** Fino a quando scappa: uscito da un corpo, per un po' non cerca nessuno e si allontana. */
    private long fugaFinoA;
    private static final int DURATA_FUGA = 20 * 20;
    /** Chi sta braccando ({@link BraccaOspiteGoal}), e se ha deciso di colpire: lo sente il simbionte della preda. */
    private java.util.UUID braccato;
    private boolean colpisce;

    // ---- il duello coi tentacoli, prima di entrare (dalla mod Symbiote: WildHostBrain)
    /** Fin dove arriva una frustata. */
    public static final double PORTATA_FRUSTA = 7.0D;
    /** Fin dove arriva lo strattone, i tentacoli che agganciano da lontano e tirano. */
    public static final double PORTATA_STRATTONE = 10.0D;
    private static final int OGNI_FRUSTATA = 30;
    private static final int OGNI_STRATTONE = 140;
    private static final int DURATA_STRATTONE = 16;
    private static final float DANNO_FRUSTA = 3.0F;
    /** Riot e' il piu' forte fisicamente: frusta piu' forte. */
    private static final float DANNO_FRUSTA_FORTE = 4.0F;
    /** Una frustata su tre afferra e scaraventa via. */
    private static final int LANCIO_OGNI = 3;
    /** Sotto il 40% della vita il simbionte lascia il duello e scappa. */
    public static final float RITIRATA = 0.4F;
    /** Sotto un quarto, chi vuole assorbire (Riot) lo divora. */
    private static final float DIVORABILE = 0.25F;
    /** Lontano dai colpi per dieci secondi, si rigenera di mezzo cuore ogni due. */
    private static final int RIPOSO = 200;
    private static final int OGNI_CURA = 40;
    /** Oltre questa distanza smette di rispondere a chi l'ha colpito. */
    private static final double MEMORIA_NEMICO = 16.0D;
    private long ultimaFrustata = Long.MIN_VALUE / 2;
    private long ultimoStrattone = Long.MIN_VALUE / 2;
    private long strattoneFinoA;
    private long ultimoColpoSubito = Long.MIN_VALUE / 2;
    /** Chi l'ha colpito per ultimo, simbionte o ospite di un altro simbionte: gli risponde. */
    private int nemico;
    /** Fin quando lo tengono i tentacoli di un Riot fuso con un giocatore. */
    private long trattenutoFinoA;

    /** Entro quanto i tentacoli arrivano ad agganciare un ospite. */
    public static final double PORTATA_PRESA = 3.0D;
    /** Sotto questa distanza smette di tirare: al resto pensa il contatto. */
    private static final double PRESA_FINITA = 1.2D;
    /** Quanta parte dello scarto si copre in un tick: piu' alto, piu' secco lo strattone. */
    private static final double AVVICINAMENTO = 0.22D;
    /** Il tetto alla trazione, perche' da lontano non parta come una fionda. */
    private static final double TRAZIONE_MASSIMA = 0.42D;

    /**
     * Quanto sono usciti i tentacoli della presa. Lo tiene e lo muove <b>solo il client</b>,
     * per non farli scattare fuori di colpo: sul server resta a zero e non serve a niente.
     */
    public float uscitaTentacoli;
    /** L'ultimo agganciato, per ritrarre i filamenti da dove stavano invece che di scatto. */
    public int ultimoAgganciato;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AGGANCIATO, 0);
        this.entityData.define(FORMA, FORMA_BASE);
    }

    public String forma() {
        return this.entityData.get(FORMA);
    }

    /** Una forma che non esiste come mob (o non e' ancora nella mod) torna Venom: e' il simbionte di base. */
    public void setForma(String forma) {
        this.entityData.set(FORMA, modKlyntar.symbiote.RegistroSimbionti.formaDelMob(forma) ? forma : FORMA_BASE);
    }

    /** Il temperamento del simbionte di questo mob, dal registro. */
    private modKlyntar.symbiote.RegistroSimbionti.Temperamento temperamento() {
        modKlyntar.symbiote.RegistroSimbionti.Simbionte s = modKlyntar.symbiote.RegistroSimbionti.di(forma());
        return s == null ? modKlyntar.symbiote.RegistroSimbionti.Temperamento.PROTETTIVO : s.temperamento();
    }

    // ------------------------------------------------------------------ il duello

    /** Chi gli da' la caccia non lascia scappare nessuno: un Riot libero, o un giocatore fuso con Riot. */
    private static boolean nonLasciaScappare(Entity chi) {
        if (chi instanceof SymbioteEntity cacciatore) {
            return cacciatore.temperamento().nonLasciaScappare();
        }
        if (chi instanceof Player giocatore) {
            modKlyntar.symbiote.RegistroSimbionti.Simbionte s =
                    modKlyntar.symbiote.RegistroSimbionti.di(SymbioteState.forma(giocatore));
            return s != null && s.temperamento().nonLasciaScappare();
        }
        return false;
    }

    /** E' un rivale: un altro simbionte libero, o chi ne porta uno di un'altra famiglia. */
    public boolean rivale(Entity chi) {
        if (!cercaOspite() || chi == null || chi == this) {
            return false;
        }
        if (chi instanceof SymbioteEntity altro) {
            return altro.cercaOspite() && !modKlyntar.symbiote.RegistroSimbionti.famiglia(altro.forma())
                    .equals(modKlyntar.symbiote.RegistroSimbionti.famiglia(forma()));
        }
        return chi instanceof Player giocatore && altroSimbionte(giocatore);
    }

    public boolean puoFrustare() {
        return this.level().getGameTime() - this.ultimaFrustata >= OGNI_FRUSTATA;
    }

    public boolean puoStrattonare() {
        return puoStrattonare(false);
    }

    /** Allo stremo, chi non molla la preda ritenta lo strattone appena il precedente e' finito. */
    public boolean puoStrattonare(boolean disperato) {
        long attesa = disperato ? DURATA_STRATTONE + 20L : OGNI_STRATTONE;
        return this.level().getGameTime() - this.ultimoStrattone >= attesa;
    }

    /**
     * Una frustata coi tentacoli: il danno, e il filamento che i client vedono partire e
     * rientrare. Un giocatore, una volta su tre, viene afferrato e scaraventato via; un altro
     * simbionte ridotto allo stremo, se questo vuole assorbire, viene divorato.
     */
    public void frusta(LivingEntity bersaglio) {
        if (!(this.level() instanceof ServerLevel livello)) {
            return;
        }
        this.ultimaFrustata = livello.getGameTime();
        modKlyntar.symbiote.DuelloSimbionti.mostraFrustata(this, bersaglio, forma());
        livello.playSound(null, this.blockPosition(), modKlyntar.sound.SuoniKlyntar.FEND_OFF_LASH.get(),
                SoundSource.HOSTILE, 0.9F, 0.9F + this.random.nextFloat() * 0.2F);
        float danno = temperamento().nonLasciaScappare() ? DANNO_FRUSTA_FORTE : DANNO_FRUSTA;
        // il corpo che vuole prendersi non lo uccide: le frustate si fermano a mezzo cuore
        if (bersaglio instanceof Player) {
            danno = Math.min(danno, bersaglio.getHealth() - 1.0F);
        }
        if (danno > 0.0F) {
            bersaglio.hurt(this.damageSources().mobAttack(this), danno);
        }
        if (bersaglio instanceof Player giocatore && this.random.nextInt(LANCIO_OGNI) == 0) {
            Vec3 via = giocatore.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            via = via.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : via.normalize();
            giocatore.setDeltaMovement(via.x * 1.15D, 0.62D, via.z * 1.15D);
            giocatore.hurtMarked = true;
            livello.playSound(null, giocatore.blockPosition(), modKlyntar.sound.SuoniKlyntar.ABILITY_GRAB_THROW.get(),
                    SoundSource.HOSTILE, 1.0F, 0.85F);
        }
        if (bersaglio instanceof SymbioteEntity altro && altro.isAlive() && temperamento().vuoleAssorbire()
                && altro.getHealth() <= altro.getMaxHealth() * DIVORABILE) {
            divora(altro);
        }
    }

    /** Lo strattone: i tentacoli agganciano la preda da lontano e se la tirano addosso. */
    public void strattona(Player preda) {
        long ora = this.level().getGameTime();
        this.braccato = preda.getUUID();
        this.ultimoStrattone = ora;
        this.strattoneFinoA = ora + DURATA_STRATTONE;
        this.level().playSound(null, this.blockPosition(), modKlyntar.sound.SuoniKlyntar.ABILITY_GRAB_PULL.get(),
                SoundSource.HOSTILE, 1.0F, 1.1F);
    }

    /** Inghiotte un altro simbionte sconfitto: quello sparisce, questo torna in forze. */
    private void divora(SymbioteEntity altro) {
        if (!(this.level() instanceof ServerLevel livello)) {
            return;
        }
        livello.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                altro.getX(), altro.getY() + 0.4D, altro.getZ(), 20, 0.3D, 0.3D, 0.3D, 0.05D);
        livello.playSound(null, altro.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.0F, 0.5F);
        LOGGER.info("Symbiote {} devoured symbiote {}", forma(), altro.forma());
        altro.discard();
        this.setHealth(this.getMaxHealth());
        this.nemico = 0;
    }

    /**
     * Lo tengono i tentacoli di un Riot fuso con un giocatore, che se lo tira addosso: per tutto
     * il tempo non scappa, non si rifugia in un animale e non cerca nessuno. Va rinnovato ogni
     * tick da chi lo tiene; appena la presa molla, torna libero.
     */
    public void trattieni() {
        this.trattenutoFinoA = this.level().getGameTime() + 5L;
        this.fugaFinoA = 0L;
        this.hostAnimal = null;
        this.getNavigation().stop();
    }

    public boolean trattenuto() {
        return this.level().getGameTime() < this.trattenutoFinoA;
    }

    /** Sta braccando questo giocatore? */
    public boolean bracca(Player giocatore) {
        return giocatore.getUUID().equals(this.braccato);
    }

    /** Ha smesso di girargli intorno e gli sta venendo addosso? */
    public boolean staColpendo(Player giocatore) {
        return this.colpisce && bracca(giocatore);
    }

    public boolean inFuga() {
        return this.level().getGameTime() < this.fugaFinoA;
    }

    /** E' appena uscito da un corpo: si allontana dall'ospite e per un po' non cerca nessuno. */
    public void scappaDa(Entity ospite) {
        this.fugaFinoA = this.level().getGameTime() + DURATA_FUGA;
        Vec3 via = this.position().subtract(ospite.position()).multiply(1.0D, 0.0D, 1.0D);
        if (via.lengthSqr() < 1.0E-4D) {
            via = new Vec3(1.0D, 0.0D, 0.0D);
        }
        via = via.normalize();
        this.setDeltaMovement(via.x * 0.8D, 0.35D, via.z * 0.8D);
        this.hasImpulse = true;
        Vec3 meta = this.position().add(via.scale(10.0D));
        this.getNavigation().moveTo(meta.x, meta.y, meta.z, 1.3D);
    }

    /**
     * Un ospite gia' legato a un simbionte di un'altra famiglia. I simbionti si cacciano fra loro:
     * i dominanti lo cercano apposta, per entrare e lottare con quello che c'e' gia'.
     */
    private boolean altroSimbionte(Player giocatore) {
        String sua = SymbioteState.forma(giocatore);
        return !sua.isEmpty() && !giocatore.isCreative() && !giocatore.isSpectator()
                && !modKlyntar.symbiote.RegistroSimbionti.famiglia(sua)
                .equals(modKlyntar.symbiote.RegistroSimbionti.famiglia(forma()));
    }

    /**
     * Entra nel corpo del giocatore: se e' libero si lega, se ha gia' un simbionte scatta il
     * conflitto. Restituisce se il mob e' entrato, e quindi va tolto dal mondo.
     */
    public boolean tentaLegame(ServerPlayer giocatore) {
        if (!giocatore.isAlive() || giocatore.isDeadOrDying()) {
            return false;
        }
        if (puoOspitare(giocatore)) {
            doPlayerEffect(giocatore);
            return true;
        }
        return altroSimbionte(giocatore)
                && modKlyntar.symbiote.ConflittoSimbionti.entra(giocatore, forma(), this.position().add(0.0D, 0.5D, 0.0D),
                this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(CHIAVE_FORMA, forma());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(CHIAVE_FORMA)) {
            setForma(tag.getString(CHIAVE_FORMA));
        }
    }

    /**
     * Chi i tentacoli stanno tirando, o {@code null}.
     *
     * <p>Passa dai dati sincronizzati e non da un pacchetto nostro: cosi' ogni client sa chi
     * e' agganciato, anche quando non e' lui, e i filamenti partono dalla parte giusta per
     * tutti quelli che guardano.</p>
     */
    public Player ospiteAgganciato() {
        int id = this.entityData.get(AGGANCIATO);
        return id != 0 && this.level().getEntity(id) instanceof Player preda ? preda : null;
    }

    /**
     * La presa: i tentacoli agganciano chi si avvicina e se lo tirano addosso.
     *
     * <p><b>Non lo solleva</b>, ed e' la differenza voluta con la presa di All-Black sul
     * cadavere: li' il simbionte alzava la preda per rivestirla, qui la trascina e basta.
     * Della velocita' si riscrivono percio' le sole componenti orizzontali, e quella
     * verticale resta dov'e': la decide la gravita', come per chiunque altro.</p>
     *
     * <p>Lo tira con la velocita' invece di riposizionarlo. Il teletrasporto a ogni tick e'
     * piu' diretto ma porta con se' la rotazione e inchioda la telecamera.</p>
     */
    private void tickPresa() {
        Player preda = cercaPreda();
        this.entityData.set(AGGANCIATO, preda == null ? 0 : preda.getId());
        if (preda == null) {
            return;
        }

        Vec3 scarto = this.position().subtract(preda.position());
        if (scarto.length() < PRESA_FINITA) {
            return;                       // e' arrivato: alla fusione pensa il contatto
        }

        Vec3 trazione = new Vec3(scarto.x, 0.0D, scarto.z).scale(AVVICINAMENTO);
        if (trazione.length() > TRAZIONE_MASSIMA) {
            trazione = trazione.normalize().scale(TRAZIONE_MASSIMA);
        }
        Vec3 moto = preda.getDeltaMovement();
        preda.setDeltaMovement(trazione.x, moto.y, trazione.z);
        // senza questo il server si tiene la velocita' per se' e il giocatore non si muove
        preda.hurtMarked = true;
        preda.fallDistance = 0.0F;
        // la lentezza gli toglie il passo: senza, camminando contrasterebbe la trazione
        preda.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));

        if (this.tickCount % 10 == 0) {
            this.level().playSound(null, this.blockPosition(), modKlyntar.sound.SuoniKlyntar.SYMBIOTE_GRAB.get(),
                    SoundSource.HOSTILE, 0.7F, 0.5F);
        }
    }

    /**
     * L'ospite piu' vicino a tiro.
     *
     * <p>Solo per chi un ospite lo cerca davvero: il frammento di Grendel no, lui da' la
     * caccia ai suoi e non deve trascinare nessuno. E nemmeno mentre sta dietro a un
     * animale, che a quel punto e' la sua preda.</p>
     */
    private Player cercaPreda() {
        if (!cercaOspite() || this.hostAnimal != null) {
            return null;
        }
        // durante lo strattone i tentacoli tengono la preda braccata anche da lontano
        if (this.braccato != null && this.level().getGameTime() < this.strattoneFinoA) {
            Player braccata = this.level().getPlayerByUUID(this.braccato);
            if (braccata != null && bersaglioValido(braccata) && braccata.distanceTo(this) <= PORTATA_STRATTONE
                    && this.hasLineOfSight(braccata)) {
                return braccata;
            }
        }
        Player vicino = this.level().getNearestPlayer(
                TargetingConditions.forNonCombat().range(PORTATA_PRESA)
                        .selector(e -> e instanceof Player p && bersaglioValido(p)),
                this);
        return vicino != null && this.hasLineOfSight(vicino) ? vicino : null;
    }

    public SymbioteEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    /**
     * Un ospite valido: chi porta gia' un simbionte non ne accetta un altro.
     *
     * <p>Vale per tutte le forme, comprese quelle derivate, perche' la verifica passa dal
     * superpotere di Palladium e non da un elenco di nomi.</p>
     */
    public static boolean puoOspitare(Player giocatore) {
        return !giocatore.isCreative() && !giocatore.isSpectator()
                && !SymbioteState.haSimbionte(giocatore);
    }

    /**
     * Se questo mob cerca un ospite da infettare.
     *
     * <p>Il frammento di Grendel no: non cerca ospiti, consegna il Knull's Bond a chi un
     * simbionte ce l'ha gia'.</p>
     */
    protected boolean cercaOspite() {
        return true;
    }

    /**
     * Chi vale la pena raggiungere: chi un simbionte non ce l'ha, e - per i simbionti che
     * attaccano (Riot) - anche chi ne porta uno di un'altra famiglia. Mai mentre scappa. Le
     * sottoclassi possono ribaltare il criterio.
     */
    protected boolean bersaglioValido(Player giocatore) {
        if (inFuga() || trattenuto()) {
            return false;
        }
        // un corpo gia' conteso da due simbionti non ne cerca un terzo
        return puoOspitare(giocatore) || (temperamento().attacca() && altroSimbionte(giocatore)
                && !modKlyntar.symbiote.ConflittoSimbionti.inCorso(giocatore));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new ApproachPlayerGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new BraccaOspiteGoal(this));
        this.goalSelector.addGoal(1, new CacciaSimbionteGoal(this));
        this.goalSelector.addGoal(2, new WanderAroundGoal(this, 1.0D));
    }

    static class ApproachPlayerGoal extends Goal {
        private final SymbioteEntity symbiote;
        private Player targetPlayer;
        private final double speed;

        public ApproachPlayerGoal(SymbioteEntity symbiote, double speed) {
            this.symbiote = symbiote;
            this.speed = speed;
        }

        @Override
        public boolean canUse() {
            if (this.symbiote.hostAnimal == null) {
                this.targetPlayer = this.symbiote.level().getNearestPlayer(
                        TargetingConditions.forNonCombat().range(10.0D)
                                .selector(e -> e instanceof Player p && this.symbiote.bersaglioValido(p)),
                        this.symbiote);
                return this.targetPlayer != null && puoOspitare(this.targetPlayer)
                    && this.targetPlayer.distanceTo(this.symbiote) > 1.0D;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPlayer != null && puoOspitare(this.targetPlayer)
                    && this.targetPlayer.distanceTo(this.symbiote) > 1.0D;
        }

        @Override
        public void tick() {
            if (this.targetPlayer != null) {
                this.symbiote.getNavigation().moveTo(this.targetPlayer, this.speed);
                if (this.targetPlayer.distanceTo(this.symbiote) <= 1.0D) {
                    this.symbiote.doHurtTarget(this.targetPlayer);
                }
            }
        }
    }

    /**
     * Come un simbionte che attacca (Riot) da' la caccia a chi ne porta gia' uno di un'altra
     * famiglia. Il ritmo viene dalla mod Symbiote (WildHostBrain): prima lo guarda, fermo; poi lo
     * bracca restando a nove blocchi; colpisce quando trova un'apertura - le spalle girate, la
     * vita sotto il 60%, il buio - o quando ha aspettato abbastanza. Ferito sotto il 40% (fuoco,
     * suono) lascia perdere e si ritira.
     *
     * <p>Chi un simbionte non ce l'ha non lo bracca: lo raggiunge dritto, come sempre
     * ({@link ApproachPlayerGoal}). Sotto i tre blocchi c'e' la presa coi tentacoli, come sempre.</p>
     */
    static class BraccaOspiteGoal extends Goal {
        private enum Fase { OSSERVA, BRACCA, COLPISCE }

        private static final double NOTA = 24.0D;
        private static final double PERSO = 30.0D;
        private static final double TIENE = 9.0D;
        private static final double MARGINE = 2.0D;
        private static final double APERTURA = 12.0D;
        private static final int OSSERVA_TICK = 60;
        private static final int PAZIENZA = 600;
        private static final int SENZA_VISTA = 200;
        private static final float VITA_APERTA = 0.6F;
        private static final int BUIO = 6;
        /** Nel duello resta fra quattro e sei blocchi: dentro la frusta, fuori dalla presa. */
        private static final double DUELLO_VICINO = 4.0D;
        private static final double DUELLO_LONTANO = 6.0D;
        private static final int PAZIENZA_DUELLO = 200;

        private final SymbioteEntity symbiote;
        private Player preda;
        private Fase fase;
        private long dal;
        private long vistoIl;

        BraccaOspiteGoal(SymbioteEntity symbiote) {
            this.symbiote = symbiote;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean predaValida(Player giocatore) {
            return !puoOspitare(giocatore) && this.symbiote.bersaglioValido(giocatore);
        }

        @Override
        public boolean canUse() {
            if (this.symbiote.hostAnimal != null || !this.symbiote.cercaOspite() || this.symbiote.inFuga()) {
                return false;
            }
            this.preda = this.symbiote.level().getNearestPlayer(
                    TargetingConditions.forNonCombat().range(NOTA)
                            .selector(e -> e instanceof Player p && predaValida(p)),
                    this.symbiote);
            return this.preda != null && this.symbiote.hasLineOfSight(this.preda);
        }

        @Override
        public boolean canContinueToUse() {
            if (this.preda == null || !this.preda.isAlive() || !predaValida(this.preda)
                    || this.symbiote.hostAnimal != null) {
                return false;
            }
            long ora = this.symbiote.level().getGameTime();
            return this.symbiote.distanceTo(this.preda) <= PERSO && ora - this.vistoIl <= SENZA_VISTA;
        }

        @Override
        public void start() {
            long ora = this.symbiote.level().getGameTime();
            this.fase = Fase.OSSERVA;
            this.dal = ora;
            this.vistoIl = ora;
            this.symbiote.braccato = this.preda.getUUID();
            this.symbiote.colpisce = false;
        }

        @Override
        public void stop() {
            this.preda = null;
            this.symbiote.braccato = null;
            this.symbiote.colpisce = false;
            // se si sta ritirando la strada della fuga resta la sua
            if (!this.symbiote.inFuga()) {
                this.symbiote.getNavigation().stop();
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            long ora = this.symbiote.level().getGameTime();
            double distanza = this.symbiote.distanceTo(this.preda);
            if (this.symbiote.hasLineOfSight(this.preda)) {
                this.vistoIl = ora;
            }
            this.symbiote.getLookControl().setLookAt(this.preda, 30.0F, 30.0F);
            boolean stremato = this.symbiote.getHealth() / this.symbiote.getMaxHealth() < RITIRATA;
            if (stremato && !this.symbiote.temperamento().nonLasciaScappare()) {
                this.symbiote.scappaDa(this.preda);
                return;
            }
            long tenuto = ora - this.dal;
            if (stremato && this.fase != Fase.COLPISCE) {
                cambia(Fase.COLPISCE, ora);
                tenuto = 0L;
            }
            switch (this.fase) {
                case OSSERVA -> {
                    this.symbiote.getNavigation().stop();
                    if (tenuto > OSSERVA_TICK) {
                        cambia(apertura(distanza, tenuto) ? Fase.COLPISCE : Fase.BRACCA, ora);
                    }
                }
                case BRACCA -> {
                    if (apertura(distanza, tenuto)) {
                        cambia(Fase.COLPISCE, ora);
                    } else {
                        tieniDistanza(distanza);
                    }
                }
                case COLPISCE -> duello(distanza, tenuto, stremato);
            }
        }

        /**
         * Il duello coi tentacoli. Resta alla distanza della frusta, abbastanza vicino da colpire
         * e non abbastanza da farsi prendere, e frusta. Quando la preda e' provata - meno del 60%
         * di vita, o dieci secondi di duello - la aggancia da lontano e se la tira addosso: sotto
         * i tre blocchi c'e' la presa, poi il contatto, e l'ingresso. Chi gli va addosso da solo
         * si fa prendere subito: e' il modo di lasciarlo entrare.
         */
        private void duello(double distanza, long tenuto, boolean stremato) {
            SymbioteEntity s = this.symbiote;
            if (distanza > DUELLO_LONTANO) {
                s.getNavigation().moveTo(this.preda, 1.25D);
            } else if (distanza < DUELLO_VICINO && tenuto < PAZIENZA_DUELLO) {
                Vec3 via = s.position().subtract(this.preda.position()).multiply(1.0D, 0.0D, 1.0D);
                via = via.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : via.normalize();
                Vec3 meta = s.position().add(via.scale(3.0D));
                s.getNavigation().moveTo(meta.x, meta.y, meta.z, 1.1D);
            } else {
                s.getNavigation().stop();
            }
            if (!s.hasLineOfSight(this.preda) || distanza <= PORTATA_PRESA) {
                return;
            }
            boolean provata = stremato || tenuto > PAZIENZA_DUELLO
                    || this.preda.getHealth() / this.preda.getMaxHealth() <= VITA_APERTA;
            if (provata && distanza <= PORTATA_STRATTONE && s.puoStrattonare(stremato)) {
                s.strattona(this.preda);
            } else if (distanza <= PORTATA_FRUSTA && s.puoFrustare()) {
                s.frusta(this.preda);
            }
        }

        private void cambia(Fase nuova, long ora) {
            this.fase = nuova;
            this.dal = ora;
            this.symbiote.colpisce = nuova == Fase.COLPISCE;
        }

        /** Il momento buono: le spalle girate, la preda gia' ferita, il buio, o la pazienza finita. */
        private boolean apertura(double distanza, long tenuto) {
            if (distanza > APERTURA) {
                return false;
            }
            Vec3 sguardo = this.preda.getLookAngle();
            Vec3 verso = this.symbiote.position().subtract(this.preda.position());
            if (verso.lengthSqr() > 1.0E-4D && sguardo.dot(verso.normalize()) < 0.1D) {
                return true;
            }
            if (this.preda.getHealth() / this.preda.getMaxHealth() <= VITA_APERTA) {
                return true;
            }
            if (this.symbiote.level().getMaxLocalRawBrightness(this.symbiote.blockPosition()) <= BUIO) {
                return true;
            }
            return tenuto > PAZIENZA;
        }

        /** Resta a nove blocchi: si avvicina se e' lontano, si allontana se gli e' finito addosso. */
        private void tieniDistanza(double distanza) {
            if (Math.abs(distanza - TIENE) < MARGINE) {
                this.symbiote.getNavigation().stop();
                return;
            }
            Vec3 via = this.symbiote.position().subtract(this.preda.position()).multiply(1.0D, 0.0D, 1.0D);
            if (via.lengthSqr() < 1.0E-4D) {
                via = new Vec3(1.0D, 0.0D, 0.0D);
            }
            Vec3 meta = distanza > TIENE
                    ? this.preda.position().add(via.normalize().scale(TIENE))
                    : this.symbiote.position().add(via.normalize().scale(4.0D));
            this.symbiote.getNavigation().moveTo(meta.x, meta.y, meta.z, distanza > TIENE ? 0.85D : 0.9D);
        }
    }

    /**
     * Un simbionte che attacca (Riot) attacca a vista gli altri simbionti liberi, come un Venom
     * non ancora fuso: lo insegue e lo frusta coi tentacoli. L'altro risponde (vedi
     * {@link #rispondi}) e sotto il 40% scappa; se lo riprende allo stremo, lo divora.
     */
    static class CacciaSimbionteGoal extends Goal {
        private static final double NOTA = 24.0D;
        private static final double PERSO = 30.0D;
        private static final int SENZA_VISTA = 100;

        private final SymbioteEntity symbiote;
        private SymbioteEntity preda;
        private long vistoIl;

        CacciaSimbionteGoal(SymbioteEntity symbiote) {
            this.symbiote = symbiote;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean pronto() {
            return this.symbiote.hostAnimal == null && this.symbiote.cercaOspite() && !this.symbiote.inFuga()
                    && !this.symbiote.trattenuto() && this.symbiote.temperamento().attacca()
                    && (this.symbiote.temperamento().nonLasciaScappare()
                    || this.symbiote.getHealth() >= this.symbiote.getMaxHealth() * RITIRATA);
        }

        @Override
        public boolean canUse() {
            if (!pronto()) {
                return false;
            }
            SymbioteEntity migliore = null;
            double meglio = NOTA * NOTA;
            for (SymbioteEntity altro : this.symbiote.level().getEntitiesOfClass(SymbioteEntity.class,
                    this.symbiote.getBoundingBox().inflate(NOTA), e -> e.isAlive() && this.symbiote.rivale(e))) {
                double d = altro.distanceToSqr(this.symbiote);
                if (d < meglio && this.symbiote.hasLineOfSight(altro)) {
                    meglio = d;
                    migliore = altro;
                }
            }
            this.preda = migliore;
            return migliore != null;
        }

        @Override
        public boolean canContinueToUse() {
            return pronto() && this.preda != null && this.preda.isAlive()
                    && this.symbiote.distanceTo(this.preda) <= PERSO
                    && this.symbiote.level().getGameTime() - this.vistoIl <= SENZA_VISTA;
        }

        @Override
        public void start() {
            this.vistoIl = this.symbiote.level().getGameTime();
        }

        @Override
        public void stop() {
            this.preda = null;
            if (!this.symbiote.inFuga()) {
                this.symbiote.getNavigation().stop();
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            boolean vede = this.symbiote.hasLineOfSight(this.preda);
            if (vede) {
                this.vistoIl = this.symbiote.level().getGameTime();
            }
            this.symbiote.getLookControl().setLookAt(this.preda, 30.0F, 30.0F);
            double distanza = this.symbiote.distanceTo(this.preda);
            if (distanza > 3.0D) {
                // chi scappa corre a 1.3: chi non lascia scappare nessuno corre di piu'
                this.symbiote.getNavigation().moveTo(this.preda,
                        this.symbiote.temperamento().nonLasciaScappare() ? 1.45D : 1.2D);
            } else {
                this.symbiote.getNavigation().stop();
            }
            if (vede && distanza <= PORTATA_FRUSTA && this.symbiote.puoFrustare()) {
                this.symbiote.frusta(this.preda);
            }
        }
    }

    static class WanderAroundGoal extends Goal {
        private final SymbioteEntity symbiote;
        private final double speed;
        private final TargetingConditions animalTargeting;

        public WanderAroundGoal(SymbioteEntity symbiote, double speed) {
            this.symbiote = symbiote;
            this.speed = speed;
            // mentre bracca un ospite non si distrae con un animale
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
            // un animale che ha gia' un simbionte dentro non si prende: il marchio e' un si'/no,
            // alla morte ne uscirebbe uno solo e il secondo andrebbe perso. Si guarda il marchio,
            // che e' la fonte di verita', e per sicurezza anche l'effetto
            this.animalTargeting = TargetingConditions.forNonCombat().range(10.0D)
                    .selector(e -> !e.getPersistentData().getBoolean(SymbioteParasiteHandler.MARCHIO)
                            && !e.hasEffect(modKlyntar.effect.ModEffects.SYMBIOTE_PARASITE.get()));
        }

        @Override
        public boolean canUse() {
            if (this.symbiote.hostAnimal == null && !this.symbiote.trattenuto()) {
                this.symbiote.hostAnimal = this.symbiote.level().getNearestEntity(Animal.class, this.animalTargeting, this.symbiote, this.symbiote.getX(), this.symbiote.getY(), this.symbiote.getZ(), this.symbiote.getBoundingBox().inflate(10.0D));
                return this.symbiote.hostAnimal != null;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.symbiote.trattenuto() && this.symbiote.hostAnimal != null
                    && !this.symbiote.hostAnimal.isDeadOrDying();
        }

        @Override
        public void tick() {
            if (this.symbiote.hostAnimal != null) {
                this.symbiote.getNavigation().moveTo(this.symbiote.hostAnimal, this.speed);
                if (this.symbiote.hostAnimal.distanceTo(this.symbiote) <= 1.0D) {
                    this.symbiote.despawnAndAttachToAnimal();
                }
                Player nearestPlayer = this.symbiote.level().getNearestPlayer(this.symbiote.hostAnimal, 10.0D);
                if (nearestPlayer != null && !nearestPlayer.isCreative()) {
                    this.symbiote.hostAnimal.getNavigation().moveTo(nearestPlayer, this.speed);
                }
            }
        }
    }

    /**
     * Entra nell'animale. Il marchio dice che il simbionte e' li' dentro - lo legge
     * SymbioteParasiteHandler alla morte dell'ospite per farlo uscire - e l'effetto lo
     * consuma da dentro un punto ogni dieci secondi, finche' cede. La durata e' finita e
     * non -1 di proposito: con l'infinito di Minecraft l'effetto non scatterebbe mai.
     */
    private void despawnAndAttachToAnimal() {
        if (!this.level().isClientSide && this.hostAnimal != null) {
            this.hostAnimal.getPersistentData().putBoolean(SymbioteParasiteHandler.MARCHIO, true);
            // quando l'animale muore il simbionte esce: deve uscire lo stesso, non sempre Venom
            this.hostAnimal.getPersistentData().putString(SymbioteParasiteHandler.MARCHIO_FORMA, forma());
            this.hostAnimal.addEffect(new MobEffectInstance(
                    modKlyntar.effect.ModEffects.SYMBIOTE_PARASITE.get(), Integer.MAX_VALUE, 0, false, true));
            this.remove(RemovalReason.DISCARDED);
        }
    }


    public void doPlayerEffect(ServerPlayer player) {
        LOGGER.info("Symbiote infection triggered for {}", player.getGameProfile().getName());
        player.displayClientMessage(Component.literal("You have bonded with a symbiote."), false);
        PlayerPowerCapability.infectPlayer(player, forma());
        // il livello di simbionte serve ad arrampicata e caduta; effetti e attributi
        // li mette gia' la trasformazione, che sa di che forma si tratta
        player.getCapability(PlayersPowerProvider.PLAYERS_POWER)
                .ifPresent(power -> power.addSymbiote(1));
    }

    /**
     * Un simbionte non si ammazza a spadate. Lo feriscono solo le sue debolezze: il fuoco,
     * e il suono, cioe' il sonic boom del Warden e la campana, che colpisce con lo stesso tipo
     * di danno. Tutto il resto scivola via, frecce e pugni compresi.
     *
     * <p>Due eccezioni di servizio: il giocatore in creativa, per poter pulire, e i danni che
     * bypassano l'invulnerabilita' - {@code /kill} e il vuoto - altrimenti nemmeno il comando
     * riuscirebbe a toglierlo di mezzo. Vale per tutti i simbionti, frammento di Grendel
     * compreso: e' lo stesso corpo.</p>
     *
     * <p>Il tocco di chi puo' ospitarlo non e' un colpo ma la fusione, come prima.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || source.is(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.isCreativePlayer()) {
            return ferito(super.hurt(source, amount), null);
        }
        // nel duello i simbionti si fanno male fra loro: un altro simbionte libero, o chi ne
        // porta uno di un'altra famiglia, coi pugni e con le abilita'. Non entra: si combatte
        if (!this.level().isClientSide && rivale(source.getEntity())) {
            float danno = Math.min(amount, this.getHealth() - 1.0F);
            return ferito(danno > 0.0F && super.hurt(source, danno), source.getEntity());
        }
        // chi lo colpisce se lo prende: libero, si lega; con un simbionte addosso, scatta il conflitto
        if (cercaOspite() && !inFuga() && source.getEntity() instanceof ServerPlayer player
                && this.tentaLegame(player)) {
            this.remove(RemovalReason.DISCARDED);
        }
        return false;
    }

    /**
     * Dopo un colpo andato a segno: si ricorda chi e' stato, per rispondergli, e sotto il 40%
     * lascia il duello e scappa. Se a respingerlo e' stato l'ospite di un altro simbionte, il
     * simbionte dell'ospite lo dice.
     */
    private boolean ferito(boolean colpito, Entity chi) {
        if (!colpito || this.level().isClientSide) {
            return colpito;
        }
        this.ultimoColpoSubito = this.level().getGameTime();
        if (chi != null) {
            this.nemico = chi.getId();
            if (this.isAlive() && !inFuga() && this.getHealth() < this.getMaxHealth() * RITIRATA) {
                if (temperamento().nonLasciaScappare()) {
                    // Riot non molla: allo stremo si getta sull'ospite per entrarci, anche malconcio
                    if (chi instanceof Player preda && bersaglioValido(preda) && preda.distanceTo(this) <= PORTATA_STRATTONE
                            && this.hasLineOfSight(preda) && puoStrattonare(true)) {
                        strattona(preda);
                    }
                } else if (!nonLasciaScappare(chi) && !trattenuto()) {
                    scappaDa(chi);
                    this.nemico = 0;
                    if (chi instanceof ServerPlayer ospite) {
                        modKlyntar.symbiote.DuelloSimbionti.respinto(ospite, this);
                    }
                }
                // a Riot non si scappa: chi e' nel suo mirino combatte fino alla fine
            }
        }
        return true;
    }

    /**
     * Risponde a chi l'ha colpito: lo frusta finche' e' a tiro e lui ha le forze. E' la difesa
     * di un Venom libero attaccato da Riot - protettivo, combatte solo se minacciato - e di
     * chiunque venga colpito da un rivale mentre fa altro.
     */
    private void rispondi() {
        if (this.nemico == 0 || inFuga()) {
            return;
        }
        Entity chi = this.level().getEntity(this.nemico);
        if (!(chi instanceof LivingEntity bersaglio) || !bersaglio.isAlive() || !rivale(bersaglio)
                || bersaglio.distanceTo(this) > MEMORIA_NEMICO) {
            this.nemico = 0;
            return;
        }
        if (bersaglio.distanceTo(this) <= PORTATA_FRUSTA && puoFrustare() && this.hasLineOfSight(bersaglio)) {
            this.getLookControl().setLookAt(bersaglio, 30.0F, 30.0F);
            frusta(bersaglio);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!this.level().isClientSide && target instanceof ServerPlayer player && bersaglioValido(player)
                && altroSimbionte(player)) {
            if (this.tentaLegame(player)) {
                this.remove(RemovalReason.DISCARDED);
                return true;
            }
            return false;
        }
        boolean hurt = super.doHurtTarget(target);
        if (!this.level().isClientSide && target instanceof ServerPlayer player && bersaglioValido(player)
                && this.tentaLegame(player)) {
            this.remove(RemovalReason.DISCARDED);
            return true;
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            tickPresa();
            rispondi();
            if (this.level().getGameTime() - this.ultimoColpoSubito > RIPOSO && this.tickCount % OGNI_CURA == 0
                    && this.getHealth() < this.getMaxHealth()) {
                this.heal(1.0F);
            }
        }
        if (this.hostAnimal == null) {
            for (Player player : this.level().players()) {
                if (bersaglioValido(player) && player.distanceTo(this) <= 1.0D) {
                    this.doHurtTarget(player);
                    break;
                }
            }
        } else if (this.hostAnimal.isDeadOrDying()) {
            // l'animale e' morto prima che lo raggiungesse: il simbionte e' ancora qui, torna
            // a cercare. Prima ne faceva nascere un secondo sul cadavere, e diventavano due
            this.hostAnimal = null;
        }
    }

    private <E extends GeoEntity> PlayState predicate(AnimationState<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("tentacles"));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(RawAnimation.begin().thenLoop("tentacles"));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
