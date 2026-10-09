package modKlyntar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import modKlyntar.MyMod;
import modKlyntar.client.renderer.VenomTentaclesTraversalRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Le barre organiche: la fame del simbionte avvolta dai tentacoli, e il conflitto come due
 * tentacoli che si attorcigliano. Il disegno sta in {@link DisegnoTentacoli}; qui le texture
 * dinamiche, che si ridisegnano al massimo trenta volte al secondo per far muovere i filamenti.
 *
 * <p>La fame resta la barra dei boss che manda il server (la logica e' quella di sempre): qui se ne
 * salta il disegno standard e al suo posto si mette quello organico, con la stessa scritta sopra.
 * La barra si riconosce dal nome, che comincia per "Symbiote Hunger".</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class BarreOrganicheClient {
    /** La texture e' a doppia definizione: 2 texel per pixel dell'interfaccia. */
    private static final int D = 2;
    private static final int LARGA_GUI = 182;
    private static final int FAME_ALTA_GUI = 30;
    private static final int CONFLITTO_ALTA_GUI = 24;
    private static final long OGNI_MS = 33L;
    /** Quanto la barra organica della fame sta sotto il punto della barra standard. */
    private static final int FAME_SPOSTA = 1;

    private static Texture fame;
    private static Texture conflitto;

    private BarreOrganicheClient() {
    }

    /** Una texture dinamica con la sua tela e l'ora dell'ultimo disegno. */
    private static final class Texture {
        final DisegnoTentacoli.Tela tela;
        final DynamicTexture dinamica;
        final ResourceLocation dove;
        long ultima;

        Texture(String nome, int largaGui, int altaGui) {
            this.tela = new DisegnoTentacoli.Tela(largaGui * D, altaGui * D);
            this.dinamica = new DynamicTexture(tela.larga, tela.alta, true);
            this.dove = new ResourceLocation(MyMod.MOD_ID, "dinamica/" + nome);
            Minecraft.getInstance().getTextureManager().register(dove, dinamica);
        }

        boolean daRidisegnare(long adesso) {
            if (adesso - ultima < OGNI_MS) {
                return false;
            }
            ultima = adesso;
            return true;
        }

        void carica() {
            tela.copiaIn(dinamica.getPixels());
            dinamica.upload();
        }

        void disegna(GuiGraphics grafica, int x, int y, float alfa) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alfa);
            grafica.blit(dove, x, y, tela.larga / D, tela.alta / D, 0.0F, 0.0F, tela.larga, tela.alta,
                    tela.larga, tela.alta);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static float tempo() {
        return (Util.getMillis() % 1_000_000L) / 1000.0F;
    }

    // ------------------------------------------------------------------ la fame

    /**
     * La barra dei boss della fame: si salta quella standard e si disegna quella organica, con la
     * scritta sopra. Lo spazio che occupa e' piu' alto, e le barre che vengono dopo scendono.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Component nome = event.getBossEvent().getName();
        if (!nome.getString().startsWith("Symbiote Hunger")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        event.setCanceled(true);
        if (fame == null) {
            fame = new Texture("barra_fame", LARGA_GUI, FAME_ALTA_GUI);
        }
        String forma = VenomTentaclesTraversalRenderer.formaDi(minecraft.player);
        if (fame.daRidisegnare(Util.getMillis())) {
            DisegnoTentacoli.fame(fame.tela, forma, event.getBossEvent().getProgress(), tempo());
            fame.carica();
        }
        GuiGraphics grafica = event.getGuiGraphics();
        int x = event.getX(), y = event.getY();
        int larghezzaNome = minecraft.font.width(nome);
        grafica.drawString(minecraft.font, nome, x + LARGA_GUI / 2 - larghezzaNome / 2, y - 9, 0xFFFFFF);
        fame.disegna(grafica, x, y + FAME_SPOSTA, 1.0F);
        event.setIncrement(event.getIncrement() + FAME_ALTA_GUI - 4);
    }

    // ------------------------------------------------------------------ il conflitto

    /** L'altezza della barra del conflitto, nell'interfaccia. */
    public static int altezzaConflitto() {
        return CONFLITTO_ALTA_GUI;
    }

    /** I due tentacoli del conflitto, larghi quanto una barra dei boss, a partire da (x, y). */
    public static void disegnaConflitto(GuiGraphics grafica, int x, int y, String dentro, String intruso,
                                        float equilibrio, float alfa) {
        if (conflitto == null) {
            conflitto = new Texture("barra_conflitto", LARGA_GUI, CONFLITTO_ALTA_GUI);
        }
        if (conflitto.daRidisegnare(Util.getMillis())) {
            DisegnoTentacoli.conflitto(conflitto.tela, dentro, intruso, equilibrio, tempo());
            conflitto.carica();
        }
        conflitto.disegna(grafica, x, y, alfa);
    }
}
