package modKlyntar.effect;

import modKlyntar.symbiote.SymbioteState;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Il veleno che spegne i simbionti.
 *
 * <p>Chi ne porta uno, mentre e' sotto questo effetto, prende veleno e nausea oltre alla
 * debolezza, e il simbionte smette di ripulirgli i malus. Anti-Venom e' l'unico immune:
 * e' il suo stesso veleno.</p>
 */
public class AntiVenomEffect extends MobEffect {
    /** ogni quanti tick si rinfrescano veleno e nausea */
    private static final int RINFRESCO = 40;
    /** durata dei malus figli: piu' lunga del rinfresco, cosi' non lampeggiano */
    private static final int DURATA_FIGLI = 80;

    public AntiVenomEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8F4FF);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplificatore) {
        if (SymbioteState.isAntiVenom(entity)) {
            return;
        }
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, DURATA_FIGLI, amplificatore, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DURATA_FIGLI, 0, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURATA_FIGLI, amplificatore, false, true));
    }

    @Override
    public boolean isDurationEffectTick(int durataRimasta, int amplificatore) {
        return durataRimasta % RINFRESCO == 0;
    }
}
