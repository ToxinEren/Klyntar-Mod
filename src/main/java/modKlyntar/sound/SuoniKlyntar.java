package modKlyntar.sound;

import modKlyntar.MyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Tutti i suoni della mod, uno per cosa che succede.
 *
 * <p>Ogni evento sta in {@code assets/klyntars/sounds.json}. Oggi quasi tutti rimandano al suono
 * vanilla che si sentiva prima ({@code "type": "event"}), quindi in gioco non cambia niente. Per
 * dare a una cosa un suono suo basta mettere un file in {@code assets/klyntars/sounds/} e
 * sostituire nel json il rimando col nome del file: codice, poteri e script non si toccano.</p>
 *
 * <p>I poteri Palladium li suonano per nome ({@code playsound klyntars:ability.rage}), il codice
 * Java da qui.</p>
 */
public final class SuoniKlyntar {
    public static final DeferredRegister<SoundEvent> SUONI =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MyMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ABILITY_ARMOR = registra("ability.armor");
    public static final RegistryObject<SoundEvent> ABILITY_UNLEASH = registra("ability.unleash");
    public static final RegistryObject<SoundEvent> ABILITY_SPIKES = registra("ability.spikes");
    public static final RegistryObject<SoundEvent> ABILITY_HIT = registra("ability.hit");
    public static final RegistryObject<SoundEvent> ABILITY_ROAR = registra("ability.roar");
    public static final RegistryObject<SoundEvent> ABILITY_SHIELD = registra("ability.shield");
    public static final RegistryObject<SoundEvent> ABILITY_ASSIMILATE = registra("ability.assimilate");
    public static final RegistryObject<SoundEvent> ABILITY_TRAVERSAL = registra("ability.traversal");
    public static final RegistryObject<SoundEvent> ABILITY_WINGS = registra("ability.wings");
    public static final RegistryObject<SoundEvent> ABILITY_FEND_OFF = registra("ability.fend_off");
    public static final RegistryObject<SoundEvent> ABILITY_GRAB_TENTACLE = registra("ability.grab_tentacle");
    public static final RegistryObject<SoundEvent> ABILITY_GRAB_PULL = registra("ability.grab_pull");
    public static final RegistryObject<SoundEvent> ABILITY_GRAB_PUSH = registra("ability.grab_push");
    public static final RegistryObject<SoundEvent> ABILITY_GRAB_THROW = registra("ability.grab_throw");
    public static final RegistryObject<SoundEvent> ABILITY_SYMBIOTE_PULL = registra("ability.symbiote_pull");
    public static final RegistryObject<SoundEvent> ABILITY_SYMBIOTE_STRIKE = registra("ability.symbiote_strike");
    public static final RegistryObject<SoundEvent> ABILITY_RAGE = registra("ability.rage");
    public static final RegistryObject<SoundEvent> ABILITY_BLAST = registra("ability.blast");
    public static final RegistryObject<SoundEvent> POWER_PULL_SLAM = registra("power.pull_slam");
    public static final RegistryObject<SoundEvent> POWER_STRIKE_HIT = registra("power.strike_hit");
    public static final RegistryObject<SoundEvent> POWER_STRIKE_IMPACT = registra("power.strike_impact");
    public static final RegistryObject<SoundEvent> POWER_TEMPEST_WAVE = registra("power.tempest_wave");
    public static final RegistryObject<SoundEvent> POWER_TEMPEST_IMPACT = registra("power.tempest_impact");
    public static final RegistryObject<SoundEvent> POWER_BOMB_THROW = registra("power.bomb_throw");
    public static final RegistryObject<SoundEvent> POWER_BOMB_DETONATE = registra("power.bomb_detonate");
    public static final RegistryObject<SoundEvent> POWER_BLAST_BURST = registra("power.blast_burst");
    public static final RegistryObject<SoundEvent> POWER_RAGE_START = registra("power.rage_start");
    public static final RegistryObject<SoundEvent> TRAVERSAL_EXTEND = registra("traversal.extend");
    public static final RegistryObject<SoundEvent> TRAVERSAL_GRIP = registra("traversal.grip");
    public static final RegistryObject<SoundEvent> TRAVERSAL_RETRACT = registra("traversal.retract");
    public static final RegistryObject<SoundEvent> FEND_OFF_LASH = registra("fend_off.lash");
    public static final RegistryObject<SoundEvent> FEND_OFF_SLAP = registra("fend_off.slap");
    public static final RegistryObject<SoundEvent> FEND_OFF_SLAM_GROUND = registra("fend_off.slam_ground");
    public static final RegistryObject<SoundEvent> FEND_OFF_SLAM_HIT = registra("fend_off.slam_hit");
    public static final RegistryObject<SoundEvent> FEND_OFF_CLASH_HIT = registra("fend_off.clash_hit");
    public static final RegistryObject<SoundEvent> FEND_OFF_CLASH_SQUISH = registra("fend_off.clash_squish");
    public static final RegistryObject<SoundEvent> VOICE_NEUTRAL = registra("voice.neutral");
    public static final RegistryObject<SoundEvent> VOICE_BOND_UP = registra("voice.bond_up");
    public static final RegistryObject<SoundEvent> VOICE_BOND_DOWN = registra("voice.bond_down");
    public static final RegistryObject<SoundEvent> VOICE_AGGRESSIVE = registra("voice.aggressive");
    public static final RegistryObject<SoundEvent> VOICE_WARNING = registra("voice.warning");
    public static final RegistryObject<SoundEvent> SYMBIOTE_GRAB = registra("symbiote.grab");
    public static final RegistryObject<SoundEvent> SYMBIOTE_EMERGE = registra("symbiote.emerge");
    public static final RegistryObject<SoundEvent> CAPSULE_FILL = registra("capsule.fill");
    public static final RegistryObject<SoundEvent> CAPSULE_THROW = registra("capsule.throw");
    public static final RegistryObject<SoundEvent> CAPSULE_BREAK = registra("capsule.break");
    public static final RegistryObject<SoundEvent> KNULL_FRAGMENT = registra("knull.fragment");
    public static final RegistryObject<SoundEvent> PROMETHIUM_TNT_PRIMED = registra("promethium.tnt_primed");

    private SuoniKlyntar() {
    }

    private static RegistryObject<SoundEvent> registra(String nome) {
        return SUONI.register(nome, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MyMod.MOD_ID, nome)));
    }
}
