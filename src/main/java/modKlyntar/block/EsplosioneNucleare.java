package modKlyntar.block;

import modKlyntar.MyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo scoppio del dio: un cratere che si apre nel giro di qualche secondo.
 *
 * <p>Non e' un'esplosione vanilla. {@code Level.explode} spara 4096 raggi che perdono 0,225 di
 * intensita' a ogni passo di 0,3 blocchi: con potenza 40, quella della PromethiumX, ogni raggio
 * corre gia' cinquantatre blocchi. Moltiplicare per cento vorrebbe dire raggi da cinquemila
 * blocchi e decine di milioni di posizioni da abbattere in un tick solo — il server si pianta,
 * non esplode.</p>
 *
 * <p>Qui il cratere viene scavato a fette su piu' tick, con un tetto di blocchi per tick, e il
 * danno alle creature viene dato tutto al primo istante. {@link #RAGGIO} e' l'unica manopola:
 * il volume scavato cresce col cubo, quindi alzarlo costa molto in fretta.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class EsplosioneNucleare {

    /** Il raggio del cratere. La PromethiumX ne fa uno di circa 53: questo e' otto volte il volume. */
    public static final int RAGGIO = 64;
    /** Oltre questa distanza dal centro non si muore, si vola soltanto. */
    private static final double RAGGIO_LETALE = RAGGIO * 1.15D;
    /** Fin dove arrivano spinta, fuoco e accecamento. */
    private static final double RAGGIO_ONDA = RAGGIO * 2.6D;
    /** Quanti blocchi al massimo si abbattono in un tick. */
    private static final int BLOCCHI_PER_TICK = 6000;
    /**
     * Quante posizioni al massimo si esaminano in un tick.
     *
     * <p>Serve un tetto a parte perche' l'aria non consuma il budget dei blocchi: una sfera
     * scoppiata in cielo e' quasi tutta vuota, e senza questo limite un solo tick potrebbe
     * scandire i due milioni di posizioni del cubo che la contiene.</p>
     */
    private static final int POSIZIONI_PER_TICK = 120_000;
    /** Il danno al centro: nulla di vanilla ci sopravvive. */
    private static final float DANNO_MASSIMO = 2000.0F;

    private static final List<Scavo> IN_CORSO = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger("KlyntarNucleare");

    private EsplosioneNucleare() {
    }

    /** Un cratere a meta' scavo: si ricorda dov'e' arrivato fra un tick e l'altro. */
    private static final class Scavo {
        final ServerLevel livello;
        final BlockPos centro;
        /** l'indice della prossima posizione da esaminare dentro il cubo che contiene la sfera */
        long cursore;
        final long totale;
        final int lato;
        int tick;

        Scavo(ServerLevel livello, BlockPos centro) {
            this.livello = livello;
            this.centro = centro;
            this.lato = RAGGIO * 2 + 1;
            this.totale = (long) lato * lato * lato;
        }
    }

    /**
     * Fa saltare tutto attorno a {@code centro}.
     *
     * <p>Il danno e la botta d'aria sono immediati, il cratere si apre nei secondi successivi.</p>
     */
    public static void innesca(ServerLevel livello, BlockPos centro) {
        for (Scavo s : IN_CORSO) {
            if (s.livello == livello && s.centro.equals(centro)) {
                return;                       // gia' in corso: non si innesca due volte
            }
        }
        IN_CORSO.add(new Scavo(livello, centro));
        LOGGER.info("Esplosione nucleare innescata a {} (raggio {})", centro, RAGGIO);

        lampo(livello, centro);
        travolgi(livello, centro);
    }

    /** Il bagliore, il fungo e il boato. */
    private static void lampo(ServerLevel livello, BlockPos centro) {
        double x = centro.getX() + 0.5D, y = centro.getY() + 0.5D, z = centro.getZ() + 0.5D;
        livello.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 24,
                RAGGIO * 0.35D, RAGGIO * 0.25D, RAGGIO * 0.35D, 0.0D);
        livello.sendParticles(ParticleTypes.FLASH, x, y + 8, z, 8, 6.0D, 6.0D, 6.0D, 0.0D);
        // la colonna e il cappello del fungo
        for (int q = 0; q < 44; q++) {
            double h = q * (RAGGIO / 14.0D);
            double raggio = q < 30 ? 3.0D + q * 0.35D : RAGGIO * 0.55D;
            livello.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + h, z, 40,
                    raggio, 2.0D, raggio, 0.02D);
        }
        for (int q = 0; q < 8; q++) {
            livello.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                    64.0F, 0.32F + q * 0.05F);
        }
    }

    /** Il danno e la spinta, dati una volta sola nell'istante dello scoppio. */
    private static void travolgi(ServerLevel livello, BlockPos centro) {
        Vec3 fuoco = Vec3.atCenterOf(centro);
        AABB zona = new AABB(centro).inflate(RAGGIO_ONDA);
        for (Entity entita : livello.getEntities((Entity) null, zona, e -> true)) {
            double distanza = entita.position().distanceTo(fuoco);
            if (distanza > RAGGIO_ONDA) {
                continue;
            }
            Vec3 via = entita.position().subtract(fuoco);
            if (via.lengthSqr() < 1.0E-4D) {
                via = new Vec3(0.0D, 1.0D, 0.0D);
            }
            via = via.normalize();
            double spinta = Math.max(0.0D, 1.0D - distanza / RAGGIO_ONDA) * 4.5D;
            entita.setDeltaMovement(entita.getDeltaMovement()
                    .add(via.scale(spinta)).add(0.0D, spinta * 0.35D, 0.0D));
            entita.hurtMarked = true;

            if (entita instanceof LivingEntity vivo) {
                if (distanza <= RAGGIO_LETALE) {
                    float caduta = (float) (1.0D - distanza / RAGGIO_LETALE);
                    vivo.hurt(livello.damageSources().explosion(null, null),
                            DANNO_MASSIMO * Math.max(0.08F, caduta));
                } else {
                    vivo.hurt(livello.damageSources().explosion(null, null), 12.0F);
                }
                int durata = (int) (200 * Math.max(0.2D, 1.0D - distanza / RAGGIO_ONDA));
                vivo.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durata, 0));
                vivo.addEffect(new MobEffectInstance(MobEffects.CONFUSION, durata * 2, 0));
                vivo.addEffect(new MobEffectInstance(MobEffects.WITHER, durata, 1));
                vivo.setSecondsOnFire((int) (12 * Math.max(0.1D, 1.0D - distanza / RAGGIO_ONDA)));
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END
                || !(evento.level instanceof ServerLevel livello)
                || IN_CORSO.isEmpty()) {
            return;
        }
        IN_CORSO.removeIf(scavo -> scavo.livello == livello && avanza(scavo));
    }

    /** Scava un pezzo di cratere. Ritorna vero quando ha finito. */
    private static boolean avanza(Scavo scavo) {
        ServerLevel livello = scavo.livello;
        BlockState aria = Blocks.AIR.defaultBlockState();
        BlockState crosta = Blocks.BLACKSTONE.defaultBlockState();
        BlockState brace = Blocks.MAGMA_BLOCK.defaultBlockState();

        int fatti = 0;
        int esaminate = 0;
        BlockPos.MutableBlockPos cursore = new BlockPos.MutableBlockPos();
        double raggioQuadro = (double) RAGGIO * RAGGIO;

        while (fatti < BLOCCHI_PER_TICK && esaminate < POSIZIONI_PER_TICK
                && scavo.cursore < scavo.totale) {
            long indice = scavo.cursore++;
            esaminate++;
            int dz = (int) (indice % scavo.lato) - RAGGIO;
            int dx = (int) ((indice / scavo.lato) % scavo.lato) - RAGGIO;
            // dall'alto verso il basso, cosi' il cratere sprofonda invece di comparire
            int dy = RAGGIO - (int) (indice / ((long) scavo.lato * scavo.lato));

            double d2 = (double) dx * dx + (double) dy * dy + (double) dz * dz;
            if (d2 > raggioQuadro) {
                continue;
            }
            cursore.set(scavo.centro.getX() + dx, scavo.centro.getY() + dy,
                    scavo.centro.getZ() + dz);
            if (cursore.getY() < livello.getMinBuildHeight()
                    || cursore.getY() >= livello.getMaxBuildHeight()) {
                continue;
            }
            BlockState stato = livello.getBlockState(cursore);
            if (stato.isAir()) {
                continue;                     // il cielo non si scava
            }
            if (stato.is(Blocks.BEDROCK)) {
                continue;
            }
            fatti++;

            // il guscio esterno non sparisce: si vetrifica, ed e' quello che si vede da fuori
            double bordo = raggioQuadro * 0.86D;
            if (d2 > bordo) {
                livello.setBlock(cursore, livello.random.nextInt(6) == 0 ? brace : crosta, 2);
            } else {
                livello.setBlock(cursore, aria, 2);
            }
        }

        scavo.tick++;
        if (scavo.tick % 20 == 0) {
            double x = scavo.centro.getX() + 0.5D, y = scavo.centro.getY() + 0.5D,
                    z = scavo.centro.getZ() + 0.5D;
            livello.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 12, z, 120,
                    RAGGIO * 0.4D, 10.0D, RAGGIO * 0.4D, 0.05D);
            livello.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                    24.0F, 0.3F);
        }

        boolean finito = scavo.cursore >= scavo.totale;
        if (finito) {
            LOGGER.info("Cratere completato a {} in {} tick", scavo.centro, scavo.tick);
        }
        return finito;
    }
}
