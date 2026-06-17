package modKlyntar.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.EnumSet;
import java.util.List;

public class TentacleSegmentEntity extends Mob {
    private static final EntityDataAccessor<Integer> PARENT_ID = SynchedEntityData.defineId(TentacleSegmentEntity.class, EntityDataSerializers.INT);
    private LivingEntity parent;
    private Vec3 offset;

    public TentacleSegmentEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    public void setParent(LivingEntity parent, Vec3 offset) {
        this.parent = parent;
        this.offset = offset;
        this.entityData.set(PARENT_ID, parent.getId());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ID, 0);
    }

    @Override
	public void readAdditionalSaveData(CompoundTag tag) {
        // No additional data
    }

    @Override
	public void addAdditionalSaveData(CompoundTag tag) {
        // No additional data
    }

    @Override
    public void tick() {
        super.tick();
        if (parent == null && this.level().isClientSide) {
            int parentId = this.entityData.get(PARENT_ID);
            this.parent = (LivingEntity) this.level().getEntity(parentId);
        }
        if (parent != null) {
            Vec3 parentPos = parent.position().add(offset);
            this.setPos(parentPos.x, parentPos.y, parentPos.z);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new GrabNearbyMobsGoal(this));
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    static class GrabNearbyMobsGoal extends Goal {
        private final TentacleSegmentEntity tentacleSegment;

        public GrabNearbyMobsGoal(TentacleSegmentEntity tentacleSegment) {
            this.tentacleSegment = tentacleSegment;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            List<Mob> mobs = tentacleSegment.level().getEntitiesOfClass(Mob.class, tentacleSegment.getBoundingBox().inflate(5));
            return !mobs.isEmpty();
        }

        @Override
        public void start() {
            List<Mob> mobs = tentacleSegment.level().getEntitiesOfClass(Mob.class, tentacleSegment.getBoundingBox().inflate(5));
            if (!mobs.isEmpty()) {
                Mob target = mobs.get(0);
                tentacleSegment.moveTo(target.getX(), target.getY(), target.getZ());
                target.hurt(tentacleSegment.damageSources().mobAttack(tentacleSegment), 5.0F);
            }
        }
    }
}
