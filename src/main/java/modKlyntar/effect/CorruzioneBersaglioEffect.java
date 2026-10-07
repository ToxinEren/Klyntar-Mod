package modKlyntar.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * La corruzione del bersaglio: un debuff simile al wither, il potere di corruzione della spada.
 *
 * <p>Dal documento di design (stadio 2 di All-Black). Un punto di danno che ignora l'armatura,
 * ogni due secondi, piu' spesso col livello. Puo' uccidere, a differenza del veleno.</p>
 *
 * <p><b>Segnalibro.</b> La applichera' l'abilita' di All-Black, sbloccata dallo stadio 2.</p>
 */
public final class CorruzioneBersaglioEffect extends MobEffect {
    private static final int COLORE = 0x2A1838;
    private static final int INTERVALLO = 40;

    public CorruzioneBersaglioEffect() {
        super(MobEffectCategory.HARMFUL, COLORE);
    }

    @Override
    public void applyEffectTick(LivingEntity bersaglio, int livello) {
        bersaglio.hurt(bersaglio.damageSources().wither(), 1.0F);
    }

    @Override
    public boolean isDurationEffectTick(int durata, int livello) {
        int ogni = Math.max(10, INTERVALLO >> livello);
        return durata % ogni == 0;
    }
}
