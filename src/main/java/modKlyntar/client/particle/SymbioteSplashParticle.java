package modKlyntar.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Ricalco di {@code spidermanaddon:antivenom_symbiote_splash}, lo schizzo grosso che la bomba
 * lascia all'impatto.
 *
 * <p>Dalla definizione Bedrock: dieci particelle in un colpo solo, vita fra 7 e 10 secondi,
 * velocita' iniziale fra 2 e 5, gravita' piena e attrito basso. Sono macchie pesanti che volano
 * via e ricadono, non fumo: per questo durano tanto e vanno fermate a terra.</p>
 */
public class SymbioteSplashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private SymbioteSplashParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;

        // Math.random(7.0, 10.0) secondi
        this.lifetime = (int) ((7.0D + this.random.nextDouble() * 3.0D) * 20.0D);

        double velocita = (2.0D + this.random.nextDouble() * 3.0D) / 20.0D;
        double lunghezza = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (lunghezza < 1.0E-4D) {
            vx = this.random.nextDouble() * 2.0D - 1.0D;
            vy = this.random.nextDouble() * 1.5D;
            vz = this.random.nextDouble() * 2.0D - 1.0D;
            lunghezza = Math.sqrt(vx * vx + vy * vy + vz * vz);
        }
        this.xd = vx / lunghezza * velocita;
        this.yd = vy / lunghezza * velocita;
        this.zd = vz / lunghezza * velocita;

        // il pack dichiara 0.4, ma la' l'UV finge un atlante doppio del reale e le unita' non
        // sono quelle di Java: preso alla lettera veniva una macchiolina
        this.quadSize = 1.1F + this.random.nextFloat() * 0.5F;
        this.gravity = 0.0F;
        this.hasPhysics = true;   // deve poter atterrare
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

        // linear_acceleration [0,-9.81,0] con attrito 0.5
        this.yd -= 9.81D / (20.0D * 20.0D);
        double attrito = 1.0D - 0.5D / 20.0D;
        this.xd *= attrito;
        this.yd *= attrito;
        this.zd *= attrito;

        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            // la macchia si posa e resta li' a seccare
            this.xd = 0.0D;
            this.zd = 0.0D;
        }
        this.setSpriteFromAge(this.sprites);
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
            return new SymbioteSplashParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
