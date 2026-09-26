package modKlyntar.client;

import modKlyntar.sound.SuoniKlyntar;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La voce del simbionte sullo schermo: un riquadro scuro in alto, bordato del colore del tono,
 * che compare, resta qualche secondo e sfuma.
 *
 * <p>La battuta si sceglie qui, a caso fra le varianti che il file di lingua ha per la
 * situazione ({@code klyntars.voice.<situazione>.1}, {@code .2}, ...), mai la stessa due volte
 * di fila. Il nome del giocatore entra dove la battuta ha {@code %s}.</p>
 */
public final class VoceSimbionteClient {

    private static final long DURATA_MS = 4000L;
    private static final long COMPARSA_MS = 150L;
    private static final long SFUMATURA_MS = 750L;
    /** Oltre questa larghezza la battuta va a capo, in pixel della GUI. */
    private static final int LARGHEZZA_MASSIMA = 220;
    private static final int MARGINE = 6;
    private static final int INTERLINEA = 10;

    /** I colori dei toni, nello stesso ordine di VoceSimbionte.Tono. */
    private static final int[] COLORI = {
            0x9A9AA8,   // neutro: grigio
            0xBFE6FF,   // il legame sale: bianco azzurro
            0x8A6BD9,   // il legame scende: viola
            0xE23B3B,   // aggressivo: rosso
            0xF2B63A    // avviso: ambra
    };

    private static final Random CASO = new Random();
    /** Quante varianti ha ogni situazione nel file di lingua, contate una volta sola. */
    private static final Map<String, Integer> VARIANTI = new HashMap<>();
    private static final Map<String, Integer> ULTIMA_VARIANTE = new HashMap<>();

    private static String testo;
    private static int tono;
    private static long inizio;
    private static List<FormattedCharSequence> righe;

    private VoceSimbionteClient() {
    }

    /** Arriva dal server: il simbionte ha qualcosa da dire, col tono e la fiducia del momento. */
    public static void mostra(String situazione, int tonoRicevuto, int fiducia) {
        String chiave = scegliPerFiducia(situazione, fiducia);
        if (chiave == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        String nome = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();
        testo = I18n.get(chiave, nome);
        tono = Math.max(0, Math.min(COLORI.length - 1, tonoRicevuto));
        inizio = Util.getMillis();
        righe = null;
        suona(tono);
    }

    /**
     * Le battute del gradino di fiducia, se la situazione ne ha: "hostile" con Bond 1, "friend"
     * con Bond 3. Altrimenti, e sempre con Bond 2, quelle comuni.
     */
    private static String scegliPerFiducia(String situazione, int fiducia) {
        String gradino = fiducia <= 0 ? "hostile" : fiducia >= 2 ? "friend" : null;
        if (gradino != null) {
            String chiave = scegli(situazione + "." + gradino);
            if (chiave != null) {
                return chiave;
            }
        }
        return scegli(situazione);
    }

    /** Una variante a caso della situazione, diversa dall'ultima; null se non ne ha nessuna. */
    private static String scegli(String situazione) {
        String base = "klyntars.voice." + situazione + ".";
        int quante = VARIANTI.computeIfAbsent(situazione, s -> {
            int n = 0;
            while (I18n.exists(base + (n + 1))) {
                n++;
            }
            return n;
        });
        if (quante == 0) {
            return null;
        }
        int scelta = 1 + CASO.nextInt(quante);
        Integer ultima = ULTIMA_VARIANTE.get(situazione);
        if (quante > 1 && ultima != null && scelta == ultima) {
            scelta = scelta % quante + 1;
        }
        ULTIMA_VARIANTE.put(situazione, scelta);
        return base + scelta;
    }

    /** Un segnale breve e basso per tono: non parole, solo un verso del simbionte. */
    private static void suona(int tono) {
        SoundEvent suono;
        float altezza;
        float volume;
        switch (tono) {
            case 1 -> { suono = SuoniKlyntar.VOICE_BOND_UP.get(); altezza = 1.3F; volume = 0.6F; }
            case 2 -> { suono = SuoniKlyntar.VOICE_BOND_DOWN.get(); altezza = 0.8F; volume = 0.6F; }
            case 3 -> { suono = SuoniKlyntar.VOICE_AGGRESSIVE.get(); altezza = 1.5F; volume = 0.25F; }
            case 4 -> { suono = SuoniKlyntar.VOICE_WARNING.get(); altezza = 1.4F; volume = 0.6F; }
            default -> { suono = SuoniKlyntar.VOICE_NEUTRAL.get(); altezza = 0.55F; volume = 0.5F; }
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(suono, altezza, volume));
    }

    /** Il riquadro, disegnato sopra tutto il resto della GUI. */
    public static final IGuiOverlay OVERLAY = (gui, grafica, partialTick, larghezza, altezza) -> {
        if (testo == null) {
            return;
        }
        long trascorso = Util.getMillis() - inizio;
        if (trascorso > DURATA_MS + SFUMATURA_MS) {
            testo = null;
            return;
        }
        float opacita = trascorso < COMPARSA_MS ? trascorso / (float) COMPARSA_MS
                : trascorso > DURATA_MS ? 1.0F - (trascorso - DURATA_MS) / (float) SFUMATURA_MS
                : 1.0F;
        // sotto 5 su 255 Minecraft disegna il testo opaco: meglio non disegnarlo affatto
        int alfa = Math.round(opacita * 255.0F);
        if (alfa < 5) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        if (righe == null) {
            righe = font.split(Component.literal(testo), LARGHEZZA_MASSIMA);
        }
        int larga = 0;
        for (FormattedCharSequence riga : righe) {
            larga = Math.max(larga, font.width(riga));
        }
        int w = larga + MARGINE * 2;
        int h = righe.size() * INTERLINEA + MARGINE * 2 - 2;
        int x = (larghezza - w) / 2;
        // sotto le barre dei boss in alto, dove sta la fame del simbionte, e sopra il mirino
        int y = Math.max(40, altezza / 5);

        int colore = COLORI[tono];
        int fondo = (Math.round(alfa * 0.72F) << 24) | 0x07070A;
        int bordo = (alfa << 24) | colore;
        grafica.fill(x, y, x + w, y + h, fondo);
        grafica.fill(x, y, x + w, y + 1, bordo);
        grafica.fill(x, y + h - 1, x + w, y + h, bordo);
        grafica.fill(x, y, x + 2, y + h, bordo);
        grafica.fill(x + w - 1, y, x + w, y + h, bordo);
        int scritta = (alfa << 24) | 0xF2F2F2;
        for (int i = 0; i < righe.size(); i++) {
            grafica.drawString(font, righe.get(i), x + MARGINE, y + MARGINE + i * INTERLINEA, scritta, false);
        }
    };
}
