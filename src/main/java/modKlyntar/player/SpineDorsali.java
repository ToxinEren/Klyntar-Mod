package modKlyntar.player;

import modKlyntar.MyMod;
import modKlyntar.capability.PlayerPowerCapability;
import modKlyntar.sound.SuoniKlyntar;
import modKlyntar.symbiote.ProfiliSimbionti;
import modKlyntar.symbiote.RegistroSimbionti.Tratto;
import modKlyntar.symbiote.SymbioteState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

/**
 * Le spine dorsali, il tratto passivo di Riot (11/10/2026): chi porta un simbionte che ce l'ha
 * (Riot di natura, gli altri digerendo un Riot), da trasformato, se viene colpito alle spalle fa
 * uscire dalla schiena spine di living armor ferme, che pungono chi ha colpito e poi rientrano.
 *
 * <p>Il colpo deve essere da vicino (chi colpisce e' anche cio' che colpisce, non una freccia) e
 * arrivare dalla meta' di dietro del corpo. Le spine escono a ogni colpo cosi', se non sono gia'
 * fuori, e pungono sempre: {@value #DANNO} punti col tipo di danno delle spine. All'inizio
 * seguivano Spine III vanilla (45% di probabilita', da 1 a 4 punti), ma per un simbionte grosso
 * come Riot era un graffio: ToxinEren ha chiesto il 100% e 6 di danno (11/10/2026).</p>
 *
 * <p>Il disegno sta nel modello del corpo (ossa back_spike_*, spine_dorsali.py della skill
 * nuovo-simbionte): qui si mette {@value #OBIETTIVO} alla durata dell'animazione e lo si fa scendere
 * a ogni tick; finche' e' sopra zero l'abilita' backspikes del potere e' accesa, e nel suo primo
 * tick lancia l'animazione.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class SpineDorsali {
    public static final String OBIETTIVO = "Klyntar.BackSpikes";
    /** I tick dell'animazione animation.venom.back_spikes.out (1,1 secondi). */
    private static final int DURATA = 22;
    /** Il danno di ogni puntura, a ogni colpo alle spalle. */
    private static final float DANNO = 6.0F;
    /** Sotto questo coseno fra lo sguardo del corpo e chi colpisce, il colpo arriva alle spalle. */
    private static final double ALLE_SPALLE = -0.2D;

    private SpineDorsali() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer giocatore) || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource fonte = event.getSource();
        // non si risponde alle spine con le spine, e solo ai colpi da vicino
        if (fonte.is(DamageTypes.THORNS) || fonte.is(DamageTypeTags.IS_PROJECTILE)
                || !(fonte.getEntity() instanceof LivingEntity attaccante)
                || fonte.getDirectEntity() != attaccante || attaccante == giocatore) {
            return;
        }
        if (!trasformato(giocatore) || !ProfiliSimbionti.haTratto(giocatore, Tratto.SPINE_DORSALI)
                || !allaSchiena(giocatore, attaccante)) {
            return;
        }
        if (SymbioteState.getScore(giocatore, OBIETTIVO) <= 0) {
            SymbioteState.setScore(giocatore, OBIETTIVO, DURATA);
            giocatore.level().playSound(null, giocatore.getX(), giocatore.getY(), giocatore.getZ(),
                    SuoniKlyntar.ABILITY_SPIKES.get(), SoundSource.PLAYERS, 0.7F, 1.25F);
        }
        if (attaccante instanceof Player altro && !PvpRules.colpibile(giocatore, altro)) {
            return;
        }
        attaccante.hurt(giocatore.damageSources().thorns(giocatore), DANNO);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer giocatore)) {
            return;
        }
        int resto = SymbioteState.getScore(giocatore, OBIETTIVO);
        if (resto > 0) {
            SymbioteState.setScore(giocatore, OBIETTIVO, resto - 1);
        }
    }

    /** Il corpo del simbionte e' fuori: l'abilita' della trasformazione e' accesa. */
    private static boolean trasformato(ServerPlayer giocatore) {
        String forma = PlayerPowerCapability.formaSuPalladium(giocatore);
        return forma != null && !forma.isEmpty()
                && AbilityUtil.isEnabled(giocatore, new ResourceLocation(MyMod.MOD_ID, forma), "enablesymbiotebody");
    }

    /** Chi colpisce sta nella meta' di dietro, rispetto a dove guarda il busto. */
    private static boolean allaSchiena(ServerPlayer giocatore, LivingEntity attaccante) {
        Vec3 avanti = Vec3.directionFromRotation(0.0F, giocatore.yBodyRot);
        Vec3 verso = attaccante.position().subtract(giocatore.position()).multiply(1.0D, 0.0D, 1.0D);
        if (verso.lengthSqr() < 1.0E-4D) {
            return false;
        }
        return avanti.dot(verso.normalize()) < ALLE_SPALLE;
    }
}
