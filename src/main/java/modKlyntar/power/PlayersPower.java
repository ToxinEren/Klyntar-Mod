package modKlyntar.power;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.TentacleSegmentEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayersPower {
	private int symbiote;
	private final int MIN_SYMBIOTE = 0;
	private final int MAX_SYMBIOTE = 1;
	
	public int getSymbioteLevel() {
		return symbiote;
	}
	
	public void addSymbiote(int add) {
		this.symbiote = Math.min(symbiote + add, MAX_SYMBIOTE);
	}
	
	public void subSymbiote(int sub) {
		this.symbiote = Math.max(symbiote - sub, MIN_SYMBIOTE);
	}
	
	public void copyFrom(PlayersPower source) {
		this.symbiote = source.symbiote;
	}
	
	public void saveNBTData(CompoundTag nbt) {
		nbt.putInt("symbiote", symbiote);
	}
	
	public void loadNBTData(CompoundTag nbt) {
		symbiote = nbt.getInt("symbiote");
	}
	
	
	public boolean isImmuneToFallDamage() {
        return symbiote == 1;
    }
	
	public boolean canClimbWalls() {
	    return symbiote == 1;
	}
	
	/**
	 * Questa capability tiene solo il livello di simbionte, che dice se il giocatore puo'
	 * arrampicarsi e se cade senza farsi male.
	 *
	 * <p>Effetti e attributi li assegna {@code PlayerPowerCapability.applyTransformation}, che
	 * sa anche di che forma si tratta. Prima li metteva pure questa classe, con valori diversi
	 * e arrivando dopo: vinceva lei, e i valori per forma non contavano nulla. Il fuoco che
	 * toglieva i bonus lo gestisce ora il sistema di indebolimento.</p>
	 */
	public void tick(Player player) {
    }

    
	private void generateTentacle(Player player) {
        Level level = player.level();
        if (!level.isClientSide) {
            Vec3 offset = new Vec3(0, 1.5, 0); // Posizione di partenza del tentacolo
            LivingEntity previousSegment = null;

            for (int i = 0; i < 10; i++) { // Crea 10 segmenti
                TentacleSegmentEntity segment = new TentacleSegmentEntity(MyMod.TENTACLE_SEGMENT.get(), level);
                segment.setParent(previousSegment == null ? player : previousSegment, offset);
                segment.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                level.addFreshEntity(segment);
                previousSegment = segment;
                offset = offset.add(0, -0.15, 0); // Modifica offset per i segmenti successivi
            }
        }
    }
	
}
