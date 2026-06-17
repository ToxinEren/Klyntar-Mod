package modKlyntar.power;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayersPowerProvider implements ICapabilityProvider, INBTSerializable<CompoundTag>{
	public static Capability<PlayersPower> PLAYERS_POWER = CapabilityManager.get(new CapabilityToken<PlayersPower>() { });
	
	private PlayersPower symbiote = null;
	private final LazyOptional<PlayersPower> optional = LazyOptional.of(this::createPlayersPower);
	
	private PlayersPower createPlayersPower() {
		if(this.symbiote == null) {
			this.symbiote = new PlayersPower();
		}
		return this.symbiote;
	}
	
	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		createPlayersPower().saveNBTData(nbt);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		createPlayersPower().loadNBTData(nbt);
		
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side) {
		if(cap == PLAYERS_POWER) {
			return optional.cast();
		}
		return LazyOptional.empty();
	}

}
