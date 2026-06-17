package modKlyntar.entity.custom;

import modKlyntar.MyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class VenomPlayerEntity extends Player implements GeoEntity {
	private static BlockPos lastPos;
	private static float bob;
	private Inventory inventory;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public VenomPlayerEntity(Level level, ServerPlayer originalPlayer) {
        super(level, lastPos, bob, originalPlayer.getGameProfile());
        // Copia attributi dal player originale, se necessario
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
    }

	@Override
	public boolean isSpectator() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCreative() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
