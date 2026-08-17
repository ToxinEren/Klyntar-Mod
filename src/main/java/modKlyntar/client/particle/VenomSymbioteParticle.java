package modKlyntar.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Ricalco della particella {@code spidermanaddon:venom_particle} dello Spider-Man pack.
 *
 * <p>I valori vengono dalla definizione Bedrock originale, convertiti da secondi a tick:
 * durata {@code 6.0 / (random(0,16) + 12)} secondi, velocita' iniziale fra 15 e 25 blocchi al
 * secondo, accelerazione verticale -12 e coefficiente di attrito 2. Il flipbook e' di otto
 * fotogrammi distesi sull'intera vita della particella.</p>
 */
public class VenomSymbioteParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private VenomSymbioteParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;

        // 6.0 / (random(0,16) + 12) secondi -> fra 4 e 10 tick
        double secondi = 6.0D / (this.random.nextDouble() * 16.0D + 12.0D);
        this.lifetime = Math.max(2, (int) Math.round(secondi * 20.0D));

        // initial_speed 15..25 blocchi al secondo, diviso 20 per passare al tick
        double velocita = (15.0D + this.random.nextDouble() * 10.0D) / 20.0D;
        double lunghezza = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (lunghezza < 1.0E-4D) {
            // emitter_shape_point sparge in tutte le direzioni quando non gliene viene data una
            vx = this.random.nextDouble() * 1.5D - 0.75D;
            vy = this.random.nextDouble() * 2.0D - 1.0D;
            vz = this.random.nextDouble() * 1.5D - 0.75D;
            lunghezza = Math.sqrt(vx * vx + vy * vy + vz * vz);
        }
        this.xd = vx / lunghezza * velocita;
        this.yd = vy / lunghezza * velocita;
        this.zd = vz / lunghezza * velocita;

        // size 0.25 + particle_random_1 * 0.05
        this.quadSize = 0.25F + this.random.nextFloat() * 0.05F;
        this.gravity = 0.0F;    // l'accelerazione la applichiamo noi, come nel pack
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // linear_acceleration [0,-12,0] in blocchi al secondo quadrato
        this.yd -= 12.0D / (20.0D * 20.0D);
        // linear_drag_coefficient 2
        double attrito = 1.0D - 2.0D / 20.0D;
        this.xd *= attrito;
        this.yd *= attrito;
        this.zd *= attrito;

        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);
        // la macchia sbiadisce sul finire, cosi' non sparisce di colpo
        this.alpha = 1.0F - (float) this.age / this.lifetime * 0.35F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new VenomSymbioteParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
