package modKlyntar.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Il simbionte dentro un animale, che lo consuma da dentro.
 *
 * <p>Sul modello del wither: un punto di danno ogni dieci secondi, che ignora l'armatura come
 * il wither. Non e' letale in fretta - una mucca da dieci cuori regge piu' di tre minuti - ma
 * prima o poi l'ospite cede, e alla sua morte il simbionte esce. Che muoia per questo o per
 * mano di qualcuno non fa differenza: a farlo uscire pensa {@code SymbioteParasiteHandler}
 * sull'evento di morte, non l'effetto.</p>
 */
public final class SymbioteParasiteEffect extends MobEffect {

    /** ogni quanti tick toglie un punto: dieci secondi */
    private static final int INTERVALLO = 10 * 20;
    /** il colore del wither, cosi' si legge subito come "sta morendo da dentro" */
    private static final int COLORE = 0x352A27;

    public SymbioteParasiteEffect() {
        super(MobEffectCategory.HARMFUL, COLORE);
    }

    @Override
    public void applyEffectTick(LivingEntity ospite, int amplifier) {
        ospite.hurt(ospite.damageSources().wither(), 1.0F);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // La durata scende di uno a tick: si scatta ogni volta che tocca un multiplo
        // dell'intervallo. Per questo la durata va data finita e non con -1, l'infinito di
        // Minecraft: con -1 il resto non e' mai zero e l'effetto non morde mai.
        return duration % INTERVALLO == 0;
    }
}
