package modKlyntar.entity.custom;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
            this.level().playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE,
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

    /** Chi vale la pena raggiungere. Le sottoclassi possono ribaltare il criterio. */
    protected boolean bersaglioValido(Player giocatore) {
        return puoOspitare(giocatore);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new ApproachPlayerGoal(this, 1.0D));
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
                                .selector(e -> e instanceof Player p && puoOspitare(p)),
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

    static class WanderAroundGoal extends Goal {
        private final SymbioteEntity symbiote;
        private final double speed;
        private final TargetingConditions animalTargeting;

        public WanderAroundGoal(SymbioteEntity symbiote, double speed) {
            this.symbiote = symbiote;
            this.speed = speed;
            this.animalTargeting = TargetingConditions.forNonCombat().range(10.0D);
        }

        @Override
        public boolean canUse() {
            if (this.symbiote.hostAnimal == null) {
                this.symbiote.hostAnimal = this.symbiote.level().getNearestEntity(Animal.class, this.animalTargeting, this.symbiote, this.symbiote.getX(), this.symbiote.getY(), this.symbiote.getZ(), this.symbiote.getBoundingBox().inflate(10.0D));
                return this.symbiote.hostAnimal != null;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.symbiote.hostAnimal != null && !this.symbiote.hostAnimal.isDeadOrDying();
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

    private void despawnAndAttachToAnimal() {
        if (!this.level().isClientSide && this.hostAnimal != null) {
            this.hostAnimal.getPersistentData().putBoolean("InfectedBySymbiote", true);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    private void spawnAtAnimalLocation() {
        if (!this.level().isClientSide && this.hostAnimal != null) {
            SymbioteEntity newSymbiote = (SymbioteEntity) this.getType().create((ServerLevel) this.level());
            if (newSymbiote != null) {
                newSymbiote.moveTo(this.hostAnimal.getX(), this.hostAnimal.getY(), this.hostAnimal.getZ(), this.hostAnimal.getYRot(), this.hostAnimal.getXRot());
                this.level().addFreshEntity(newSymbiote);
            }
        }
    }

    public void doPlayerEffect(ServerPlayer player) {
        LOGGER.info("Symbiote infection triggered for {}", player.getGameProfile().getName());
        player.displayClientMessage(Component.literal("You have bonded with a symbiote."), false);
        PlayerPowerCapability.infectPlayer(player);
        // il livello di simbionte serve ad arrampicata e caduta; effetti e attributi
        // li mette gia' la trasformazione, che sa di che forma si tratta
        player.getCapability(PlayersPowerProvider.PLAYERS_POWER)
                .ifPresent(power -> power.addSymbiote(1));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!cercaOspite()) {
            return super.hurt(source, amount);
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || source.isCreativePlayer()) {
            return super.hurt(source, amount);
        } else if (source.getEntity() instanceof ServerPlayer player) {
            if (!puoOspitare(player)) {
                return super.hurt(source, amount);
            }
            this.doPlayerEffect(player);
            this.remove(RemovalReason.DISCARDED);
            return false;
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (!this.level().isClientSide && target instanceof ServerPlayer player && puoOspitare(player)) {
            this.doPlayerEffect(player);
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
        }
        if (this.hostAnimal == null) {
            for (Player player : this.level().players()) {
                if (bersaglioValido(player) && player.distanceTo(this) <= 1.0D) {
                    this.doHurtTarget(player);
                    break;
                }
            }
        } else if (this.hostAnimal.isDeadOrDying()) {
            this.spawnAtAnimalLocation();
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
