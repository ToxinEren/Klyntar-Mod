package modKlyntar.entity.custom;

import modKlyntar.MyMod;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * La capsula lanciata a mano, che vola come una palla di neve.
 *
 * <p>Dove tocca il vetro si spacca e il simbionte che c'era dentro esce libero.</p>
 */
public class ThrownCapsuleEntity extends ThrowableItemProjectile {
    /** quanti frammenti di vetro sparge l'impatto */
    private static final int FRAMMENTI = 24;

    public ThrownCapsuleEntity(EntityType<? extends ThrownCapsuleEntity> tipo, Level livello) {
        super(tipo, livello);
    }

    public ThrownCapsuleEntity(Level livello, LivingEntity lanciatore) {
        super(MyMod.THROWN_CAPSULE_ENTITY.get(), lanciatore, livello);
    }

    @Override
    protected Item getDefaultItem() {
        return MyMod.VENOM_CAPSULE.get();
    }

    @Override
    protected void onHit(HitResult risultato) {
        super.onHit(risultato);
        if (this.level().isClientSide) {
            return;
        }

        Vec3 impatto = risultato.getLocation();
        if (this.level() instanceof ServerLevel livello) {
            livello.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()),
                    impatto.x, impatto.y, impatto.z, FRAMMENTI, 0.25D, 0.25D, 0.25D, 0.05D);
            livello.playSound(null, impatto.x, impatto.y, impatto.z,
                    SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
            liberaSimbionte(livello, impatto);
        }
        this.discard();
    }

    private void liberaSimbionte(ServerLevel livello, Vec3 dove) {
        Entity simbionte = MyMod.SYMBIOTE_ENTITY.get().create(livello);
        if (simbionte == null) {
            return;
        }
        simbionte.moveTo(dove.x, dove.y, dove.z, this.getYRot(), 0.0F);
        livello.addFreshEntity(simbionte);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
