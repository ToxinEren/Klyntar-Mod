package modKlyntar.entity.custom;

import modKlyntar.MyMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SmokeTrailEntity extends Entity {
    private static final EntityDataAccessor<Integer> REMAINING_TICKS = SynchedEntityData.defineId(SmokeTrailEntity.class, EntityDataSerializers.INT);
    private static final int DURATION_TICKS = 3 * 60 * 20; // Durata in ticks (4 minuti)

    public SmokeTrailEntity(EntityType<? extends SmokeTrailEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SmokeTrailEntity(Level level, double x, double y, double z) {
        this(MyMod.SMOKE_TRAIL_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(REMAINING_TICKS, DURATION_TICKS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(REMAINING_TICKS, DURATION_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.entityData.set(REMAINING_TICKS, compoundTag.getInt("RemainingTicks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("RemainingTicks", this.entityData.get(REMAINING_TICKS));
    }

    @Override
    public void tick() {
        super.tick();
        int ticks = this.entityData.get(REMAINING_TICKS);
        if (ticks > 0) {
            this.entityData.set(REMAINING_TICKS, ticks - 1);
            //this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        } else {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
