package modKlyntar.client;

import com.mojang.blaze3d.platform.NativeImage;
import modKlyntar.MyMod;
import modKlyntar.symbiote.RegistroSimbionti;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Disegna tentacoli in pixel: tubi ombreggiati con un contorno scuro, il lucido in alto e le fibre
 * della texture dei segmenti dei tentacoli della mod. Serve alle barre organiche della fame e del
 * conflitto ({@link BarreOrganicheClient}), che si ridisegnano piu' volte al secondo per muoversi.
 *
 * <p>Si lavora su una {@link Tela} di colori ARGB a doppia definizione (2 texel per pixel
 * dell'interfaccia): le forme restano a pixel, ma abbastanza fini da leggersi come tentacoli.</p>
 */
public final class DisegnoTentacoli {

    /** Una tela di pixel ARGB. */
    public static final class Tela {
        public final int larga;
        public final int alta;
        public final int[] argb;

        public Tela(int larga, int alta) {
            this.larga = larga;
            this.alta = alta;
            this.argb = new int[larga * alta];
        }

        public void pulisci() {
            java.util.Arrays.fill(argb, 0);
        }

        /** Oltre questa x i tubi non disegnano (la parte vuota della barra della fame). */
        int limiteX = Integer.MAX_VALUE;

        void metti(int x, int y, int r, int g, int b, int a) {
            if (x < 0 || y < 0 || x >= larga || y >= alta) {
                return;
            }
            argb[y * larga + x] = (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        /** Copia la tela in un'immagine nativa (che vuole i colori ABGR). */
        public void copiaIn(NativeImage immagine) {
            for (int y = 0; y < alta; y++) {
                for (int x = 0; x < larga; x++) {
                    int c = argb[y * larga + x];
                    int a = c >>> 24, r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                    immagine.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
        }
    }

    /** La pelle di un simbionte: colore medio, colore del lucido, contorno, e le fibre (16x16, luminanza). */
    public static final class Pelle {
        final int[] base;
        final int[] lucido;
        final int[] bordo;
        final float forzaLucido;
        final float[] fibre;

        Pelle(int[] base, int[] lucido, int[] bordo, float forzaLucido, float[] fibre) {
            this.base = base;
            this.lucido = lucido;
            this.bordo = bordo;
            this.forzaLucido = forzaLucido;
            this.fibre = fibre;
        }
    }

    private static final Map<String, Pelle> PELLI = new HashMap<>();

    private DisegnoTentacoli() {
    }

    static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    /** La pelle della famiglia di questa forma: Venom nero bluastro, Riot grigio acciaio, gli altri dal loro colore. */
    public static Pelle pelle(String forma) {
        String famiglia = forma == null || forma.isEmpty() ? "venom" : RegistroSimbionti.famiglia(forma);
        return PELLI.computeIfAbsent(famiglia, f -> switch (f) {
            case "venom" -> new Pelle(new int[]{26, 26, 36}, new int[]{150, 170, 220}, new int[]{4, 4, 8}, 0.55F,
                    fibre("venom"));
            case "riot" -> new Pelle(new int[]{104, 110, 120}, new int[]{205, 214, 226}, new int[]{30, 32, 38}, 0.45F,
                    fibre("riot"));
            default -> {
                int c = RegistroSimbionti.colore(f);
                int[] base = {(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
                int[] lucido = {base[0] + (255 - base[0]) * 3 / 5, base[1] + (255 - base[1]) * 3 / 5,
                        base[2] + (255 - base[2]) * 3 / 5};
                yield new Pelle(base, lucido, new int[]{base[0] / 5, base[1] / 5, base[2] / 5}, 0.5F, fibre("venom"));
            }
        });
    }

    /** Le fibre della texture dei segmenti dei tentacoli, come scarto di luminanza dal centro. */
    private static float[] fibre(String forma) {
        ResourceLocation dove = new ResourceLocation(MyMod.MOD_ID,
                "textures/models/tentacles_traversal/" + forma + "_tentacle_segment.png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(dove).orElseThrow().open();
             NativeImage img = NativeImage.read(in)) {
            float[] f = new float[256];
            float centro = lum(img.getPixelRGBA(7, 7));
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    f[y * 16 + x] = lum(img.getPixelRGBA(x % img.getWidth(), y % img.getHeight())) - centro;
                }
            }
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    /** Luminanza di un pixel ABGR. */
    private static float lum(int abgr) {
        return ((abgr & 0xFF) + ((abgr >> 8) & 0xFF) + ((abgr >> 16) & 0xFF)) / 3.0F;
    }

    // ------------------------------------------------------------------ i tubi

    /**
     * Un tubo lungo il percorso, con il raggio che va da r0 a r1. Prima tutti i bordi e poi tutti
     * gli interni: cosi' il contorno resta solo fuori, e un disco non ridisegna il bordo dentro
     * quello prima.
     */
    public static void tubo(Tela t, float[] xs, float[] ys, int n, float r0, float r1, Pelle p) {
        tubo(t, xs, ys, n, r0, r1, p, Integer.MAX_VALUE);
    }

    /** Come sopra, ma niente oltre maxX: la' la barra e' vuota e i tentacoli non ci vanno. */
    public static void tubo(Tela t, float[] xs, float[] ys, int n, float r0, float r1, Pelle p, int maxX) {
        int giaLimite = t.limiteX;
        t.limiteX = maxX;
        for (int i = 0; i < n; i++) {
            float r = r0 + (r1 - r0) * (i / (float) Math.max(1, n - 1));
            if (r < 0.45F) {
                continue;
            }
            disco(t, xs[i], ys[i], r, p.bordo[0], p.bordo[1], p.bordo[2]);
        }
        for (int i = 0; i < n; i++) {
            float r = r0 + (r1 - r0) * (i / (float) Math.max(1, n - 1));
            if (r < 0.45F) {
                continue;
            }
            interno(t, xs[i], ys[i], r, i, p);
        }
        t.limiteX = giaLimite;
    }

    private static void disco(Tela t, float cx, float cy, float r, int cr, int cg, int cb) {
        int raggio = (int) Math.ceil(r) + 1;
        int bx = (int) Math.floor(cx), by = (int) Math.floor(cy);
        for (int dy = -raggio; dy <= raggio; dy++) {
            for (int dx = -raggio; dx <= raggio; dx++) {
                float ex = bx + dx + 0.5F - cx, ey = by + dy + 0.5F - cy;
                if (ex * ex + ey * ey <= r * r && bx + dx <= t.limiteX) {
                    t.metti(bx + dx, by + dy, cr, cg, cb, 255);
                }
            }
        }
    }

    private static void interno(Tela t, float cx, float cy, float r, int i, Pelle p) {
        float dentro = r - 0.85F;
        int raggio = (int) Math.ceil(r) + 1;
        int bx = (int) Math.floor(cx), by = (int) Math.floor(cy);
        for (int dy = -raggio; dy <= raggio; dy++) {
            for (int dx = -raggio; dx <= raggio; dx++) {
                float ex = bx + dx + 0.5F - cx, ey = by + dy + 0.5F - cy;
                if (ex * ex + ey * ey > dentro * dentro || bx + dx > t.limiteX) {
                    continue;
                }
                float ny = ey / r;                                    // -1 sopra, +1 sotto
                float luce = (0.55F + 0.45F * (1.0F - (ny + 1.0F) / 2.0F)) * 1.15F;
                float m = 0.0F;
                if (p.fibre != null) {
                    int fx = Math.floorMod(i / 2, 16), fy = Math.floorMod((by + dy) * 2, 16);
                    m = p.fibre[fy * 16 + fx] * 0.9F;
                }
                float r1 = p.base[0] * luce + m, g1 = p.base[1] * luce + m, b1 = p.base[2] * luce + m;
                if (ny > -0.75F && ny < -0.35F) {                    // la striscia lucida
                    r1 += (p.lucido[0] - r1) * p.forzaLucido;
                    g1 += (p.lucido[1] - g1) * p.forzaLucido;
                    b1 += (p.lucido[2] - b1) * p.forzaLucido;
                }
                t.metti(bx + dx, by + dy, (int) r1, (int) g1, (int) b1, 255);
            }
        }
    }

    // ------------------------------------------------------------------ i percorsi

    /** Un percorso, riempito dai metodi qui sotto. */
    public static final class Percorso {
        public float[] xs = new float[256];
        public float[] ys = new float[256];
        public int n;

        void aggiungi(float x, float y) {
            if (n == xs.length) {
                xs = java.util.Arrays.copyOf(xs, n * 2);
                ys = java.util.Arrays.copyOf(ys, n * 2);
            }
            xs[n] = x;
            ys[n] = y;
            n++;
        }

        public Percorso vuoto() {
            n = 0;
            return this;
        }
    }

    /** Un filamento che parte in una direzione e si arriccia sempre di piu' verso la punta. */
    public static Percorso ricciolo(Percorso p, float x0, float y0, float ang, float lung, float curva) {
        p.vuoto();
        float passo = 0.5F, x = x0, y = y0, a = ang;
        for (float s = 0.0F; s < lung; s += passo) {
            p.aggiungi(x, y);
            float k = s / lung;
            a += curva * passo * (0.02F + 0.22F * k * k);
            x += (float) Math.cos(a) * passo;
            y += (float) Math.sin(a) * passo;
        }
        return p;
    }

    /** Un percorso quasi dritto, con un'onda leggera. */
    public static Percorso retto(Percorso p, float x0, float x1, float y, float onda, float fase) {
        p.vuoto();
        for (float x = x0; x <= x1; x += 0.5F) {
            p.aggiungi(x, y + onda * (float) Math.sin(fase + x * 0.045F));
        }
        return p;
    }

    // ------------------------------------------------------------------ la barra della fame

    /** Posizioni e misure dei filamenti dei bordi: fisse, cosi' non saltano da un fotogramma all'altro. */
    private static final float[][] RICCIOLI = new float[8][4];

    static {
        Random caso = new Random(7);
        for (int i = 0; i < RICCIOLI.length; i++) {
            RICCIOLI[i][0] = caso.nextFloat() * 12.0F - 6.0F;            // scarto in x
            RICCIOLI[i][1] = caso.nextFloat() * 1.2F - 0.6F;             // scarto d'angolo
            RICCIOLI[i][2] = 13.0F + caso.nextFloat() * 6.0F;            // lunghezza
            RICCIOLI[i][3] = caso.nextBoolean() ? 1.0F : -1.0F;          // verso del ricciolo
        }
    }

    /**
     * La barra della fame, avvolta dai tentacoli: la membrana scura dove e' vuota (con le vene, che
     * pulsano quando la fame e' bassa), il riempimento di carne del simbionte, i filamenti che
     * spuntano dai bordi e si arricciano e due tentacoli che la avvolgono. I tentacoli stanno solo
     * sulla parte piena: man mano che la fame scende, spariscono. tempo in secondi, per l'animazione.
     */
    public static void fame(Tela t, String forma, float livello, float tempo) {
        t.pulisci();
        Pelle p = pelle(forma);
        Percorso q = new Percorso();
        int w = t.larga;
        float yc = t.alta / 2.0F, h = 14.0F;
        float battito = livello < 0.25F ? (float) Math.max(0.0, Math.sin(tempo * 5.0)) : 0.0F;
        membrana(t, yc, h, battito);
        float fine = 6.0F + (w - 12.0F) * Math.max(0.0F, Math.min(1.0F, livello));
        if (fine > 6.0F + h / 2.0F) {
            retto(q, 6.0F + h / 2.0F - 2.0F, fine, yc, 0.8F, tempo * 0.8F);
            tubo(t, q.xs, q.ys, q.n, h / 2.0F - 0.5F, h / 2.0F - 1.2F, p);
        }
        int limite = (int) fine;
        // i filamenti dei bordi, solo dove la barra e' piena
        for (int i = 0; i < RICCIOLI.length; i++) {
            float[] r = RICCIOLI[i];
            boolean su = i % 2 == 0;
            float x = 16.0F + i * (w - 32.0F) / (RICCIOLI.length - 1) + r[0];
            if (x + 10.0F > fine) {
                continue;
            }
            float y0 = su ? yc - h / 2.0F + 1.0F : yc + h / 2.0F - 1.0F;
            float ang = (float) ((su ? -Math.PI / 2 : Math.PI / 2) + r[1] + 0.18F * Math.sin(tempo * 1.6F + i * 1.7F));
            float curva = r[3] * 2.6F * (1.0F + 0.18F * (float) Math.sin(tempo * 2.1F + i));
            ricciolo(q, x, y0, ang, r[2], curva);
            tubo(t, q.xs, q.ys, q.n, 3.0F, 0.5F, p, limite);
        }
        // due tentacoli che avvolgono la barra
        for (float frazione : new float[]{0.18F, 0.62F}) {
            q.vuoto();
            float x0 = w * frazione;
            if (x0 + 32.0F > fine) {
                continue;
            }
            float ondeggia = 1.5F * (float) Math.sin(tempo * 1.3F + frazione * 9.0F);
            for (int k = 0; k < 80; k++) {
                float s = k / 79.0F;
                q.aggiungi(x0 + 26.0F * s + 3.0F * (float) Math.sin(s * 6.0F) + ondeggia * s,
                        yc - h / 2.0F - 6.0F + (h + 12.0F) * s);
            }
            tubo(t, q.xs, q.ys, q.n, 2.6F, 1.6F, p, limite);
        }
    }

    /** La parte vuota: una membrana scura semitrasparente, con le vene rosse; battito da 0 a 1. */
    private static void membrana(Tela t, float yc, float h, float battito) {
        int w = t.larga;
        int alto = (int) (yc - h / 2.0F), basso = (int) (yc + h / 2.0F);
        int vena = (int) (90 + 120 * battito);
        for (int x = 6; x < w - 6; x++) {
            float v1 = yc + 2.5F * (float) Math.sin(x * 0.07F) + 1.5F * (float) Math.sin(x * 0.19F);
            float v2 = yc - 3.0F + 2.0F * (float) Math.sin(x * 0.11F + 2.0F);
            for (int y = alto; y < basso; y++) {
                float u = (y - alto) / h;
                if (u < 0.12F || u > 0.88F) {
                    t.metti(x, y, 5, 4, 7, 235);
                } else if (Math.abs(y - v1) < 0.8F) {
                    t.metti(x, y, vena, 14, 22, 220);
                } else if (Math.abs(y - v2) < 0.6F && (x / 9) % 3 != 0) {
                    t.metti(x, y, vena * 2 / 3, 10, 16, 210);
                } else {
                    t.metti(x, y, 16, 9, 14, 190);
                }
            }
        }
        // i capi arrotondati
        for (int dx = 0; dx < 6; dx++) {
            for (int y = alto; y < basso; y++) {
                if (Math.hypot(dx - 6.0, y - yc) < h / 2.0) {
                    t.metti(dx, y, 5, 4, 7, 235);
                    t.metti(w - 1 - dx, y, 5, 4, 7, 235);
                }
            }
        }
    }

    // ------------------------------------------------------------------ la barra del conflitto

    /**
     * La barra del conflitto: due tentacoli, uno per lato, che si spingono fino al punto dello
     * scontro e li' si attorcigliano uno sull'altro. Chi tiene piu' corpo ha il tentacolo piu' lungo.
     */
    public static void conflitto(Tela t, String dentro, String intruso, float equilibrio, float tempo) {
        t.pulisci();
        Pelle pd = pelle(dentro), pi = pelle(intruso);
        Percorso q = new Percorso();
        int w = t.larga;
        float yc = t.alta / 2.0F;
        float confine = 6.0F + (w - 12.0F) * Math.max(0.0F, Math.min(1.0F, equilibrio));
        tentacolo(t, q, 8.0F, confine + 10.0F, yc, pd, tempo * 1.2F, 9.0F, 2.0F);
        tentacolo(t, q, w - 8.0F, confine - 10.0F, yc, pi, 1.3F + tempo * 1.2F, 9.0F, 2.0F);
        // le punte si attorcigliano: l'intreccio ruota
        for (int chi = 0; chi < 2; chi++) {
            q.vuoto();
            float fase = chi * (float) Math.PI + tempo * 2.5F;
            for (int k = 0; k < 100; k++) {
                float s = k / 99.0F;
                q.aggiungi(confine - 18.0F + 36.0F * s, yc + 10.0F * (float) Math.sin(fase + s * 3.0F * Math.PI));
            }
            tubo(t, q.xs, q.ys, q.n, 3.6F, 1.4F, chi == 0 ? pd : pi);
        }
    }

    /** Un tentacolo che si assottiglia da x0 a x1, ondeggiando. */
    private static void tentacolo(Tela t, Percorso q, float x0, float x1, float yc, Pelle p, float fase,
                                  float r0, float r1) {
        q.vuoto();
        float verso = x1 > x0 ? 1.0F : -1.0F;
        float lung = Math.abs(x1 - x0);
        int n = Math.max(2, (int) (lung / 0.5F));
        for (int k = 0; k < n; k++) {
            float s = k / (float) (n - 1);
            q.aggiungi(x0 + verso * lung * s, yc + 2.6F * (float) Math.sin(fase + s * 2.2F * Math.PI) * (0.3F + s));
        }
        tubo(t, q.xs, q.ys, q.n, r0, r1, p);
    }
}
