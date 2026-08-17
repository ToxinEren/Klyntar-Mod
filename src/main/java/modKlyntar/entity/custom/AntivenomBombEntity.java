package modKlyntar.entity.custom;

import com.mojang.logging.LogUtils;

import modKlyntar.MyMod;
import modKlyntar.player.VenomSymbiotePowersHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import org.slf4j.Logger;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * La bomba simbiotica dell'Antivenom Bomb: il modello del pack che vola davvero, al posto del
 * punto simulato che si usava prima.
 *
 * <p>La detonazione resta in {@link VenomSymbiotePowersHandler}, cosi' danno, contraccolpo e
 * particelle restano descritti in un posto solo.</p>
 */
public class AntivenomBombEntity extends Projectile implements GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.antivenom_bomb.idle");
    private static final int VITA_MASSIMA = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int vissuto;
    /** finche' e' vero la bomba sta nella mano e non vola: la si vede caricare */
    private boolean inMano;

    public AntivenomBombEntity(EntityType<? extends AntivenomBombEntity> type, Level level) {
        super(type, level);
    }

    /** nasce in mano, ferma: comincia a volare solo quando la si lancia */
    public AntivenomBombEntity(Level level, LivingEntity portatore) {
        this(MyMod.ANTIVENOM_BOMB_ENTITY.get(), level);
        this.setOwner(portatore);
        this.inMano = true;
        Vec3 mano = VenomSymbiotePowersHandler.puntoDiLancio(portatore);
        this.setPos(mano.x, mano.y, mano.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** la stacca dalla mano e le da' la spinta */
    public void lancia(Vec3 direzione, double velocita) {
        this.inMano = false;
        this.setDeltaMovement(direzione.normalize().scale(velocita));
        LOGGER.info("Bomba lanciata da {} {} {}, velocita {}",
                String.format("%.2f", this.getX()), String.format("%.2f", this.getY()),
                String.format("%.2f", this.getZ()), String.format("%.2f", velocita));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.inMano) {
            // resta incollata alla mano: niente urti, niente gravita'
            if (this.getOwner() instanceof LivingEntity portatore) {
                Vec3 mano = VenomSymbiotePowersHandler.puntoDiLancio(portatore);
                this.setPos(mano.x, mano.y, mano.z);
            }
            return;
        }

        HitResult colpo = net.minecraft.world.entity.projectile.ProjectileUtil
                .getHitResultOnMoveVector(this, e -> e != this.getOwner() && e.isAlive() && !e.isSpectator());
        if (colpo.getType() != HitResult.Type.MISS) {
            this.onHit(colpo);
            return;
        }

        this.setPos(this.getX() + this.getDeltaMovement().x,
                this.getY() + this.getDeltaMovement().y,
                this.getZ() + this.getDeltaMovement().z);
        // vola quasi dritta: una goccia di gravita' basta a darle un arco
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.012D, 0.0D));

        if (!this.level().isClientSide && ++this.vissuto > VITA_MASSIMA) {
            esplodi();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult risultato) {
        super.onHitBlock(risultato);
        esplodi();
    }

    @Override
    protected void onHitEntity(EntityHitResult risultato) {
        super.onHitEntity(risultato);
        esplodi();
    }

    private void esplodi() {
        if (this.level().isClientSide) {
            return;
        }
        LOGGER.info("Bomba esplosa dopo {} tick a {} {} {}", this.vissuto,
                String.format("%.2f", this.getX()), String.format("%.2f", this.getY()),
                String.format("%.2f", this.getZ()));
        VenomSymbiotePowersHandler.detonateBomb(this.level(), this.getOwner(), this.position());
        this.discard();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "bomba", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
