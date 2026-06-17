package modKlyntar.power;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.TentacleSegmentEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayersPower {
	private int symbiote;
	private final int MIN_SYMBIOTE = 0;
	private final int MAX_SYMBIOTE = 1;
	private int tickCounter = 0;
	
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
	
	public void applyPowers(Player player) {
        if (symbiote == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 4, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, Integer.MAX_VALUE, 4, false, false));


            player.getAttributes().getInstance(Attributes.ATTACK_DAMAGE).setBaseValue(8.0D);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D); // Setting base max health to 40.0
            player.setHealth(40.0F);
            player.getAttribute(Attributes.ARMOR).setBaseValue(15.0D);
            
         // Genera il tentacolo
            //generateTentacle(player);
        }
    }
	
	public boolean isImmuneToFallDamage() {
        return symbiote == 1;
    }
	
	public boolean canClimbWalls() {
	    return symbiote == 1;
	}
	
	public void tick(Player player) {
        if (symbiote == 1) {
            // Controlla se il giocatore è in fiamme o nella lava
            if (player.isOnFire() || player.isInLava()) {
                // Rimuovi i poteri
                removePowers(player);
                return;
            }

            tickCounter++;
            
            // Aggiorna i poteri ogni 3 minuti (3600 tick)
            if (tickCounter >= 1600) {
                applyPowers(player);
                tickCounter = 0; // Reset del contatore
            }
        }
    }

    public void removePowers(Player player) {
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.JUMP);

        player.getAttributes().getInstance(Attributes.ATTACK_DAMAGE).setBaseValue(2.0D); // Valore predefinito
        player.getAttribute(Attributes.ARMOR).setBaseValue(8.0D); // Valore predefinito
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
