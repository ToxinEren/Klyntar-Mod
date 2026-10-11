package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Uno strato del giocatore che non disegna niente: mentre il corpo viene disegnato, legge dove sta
 * il polsino del pugno e lo passa a {@link TentacoloPugno}, che fa partire il braccio-tentacolo da
 * li'. Cosi' la radice segue il braccio in qualunque posa (la carica, la spinta, il ritorno,
 * accucciato), con le rotazioni del corpo che Palladium mette sulla pila e non sulle parti.
 *
 * <p>Si leggono due punti sull'asse dell'avambraccio: la punta del polsino e un punto un quarto di
 * blocco piu' dentro. Il tentacolo parte da quello dentro e corre lungo il braccio fino alla punta,
 * sovrapposto al polsino, prima di piegare verso il bersaglio: cosi' fra i due non resta mai una
 * fessura, qualunque sia la direzione del colpo.</p>
 *
 * <p>In prima persona il giocatore non viene disegnato nel mondo: la mano la legge
 * {@link #primaPersona}, chiamato dal mixin sul disegno del braccio di Palladium
 * ({@code GeckoRenderLayerPrimaPersonaMixin}).</p>
 */
public final class ManoDelPugno extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /**
     * La punta del polsino nello spazio del braccio destro, in blocchi: al centro dell'avambraccio
     * e poco sotto la mano (il polsino scende fino a 11,2 pixel dalla spalla).
     */
    private static final Vector4f PUNTA = new Vector4f(-1.0F / 16.0F, 11.2F / 16.0F, 0.0F, 1.0F);
    /** Dove il tentacolo comincia, dentro il polsino: 3,7 pixel prima della punta, a meta' avambraccio. */
    private static final Vector4f DENTRO = new Vector4f(-1.0F / 16.0F, 7.5F / 16.0F, 0.0F, 1.0F);

    public ManoDelPugno(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> genitore) {
        super(genitore);
    }

    @Override
    public void render(PoseStack pila, MultiBufferSource buffer, int luce, AbstractClientPlayer giocatore,
                       float passo, float ampiezzaPasso, float parziale, float eta, float testaY, float testaX) {
        if (!TentacoloPugno.daSeguire(giocatore.getId())) {
            return;
        }
        Vector3f[] punti = leggi(getParentModel(), pila);
        TentacoloPugno.manoVista(giocatore.getId(), punti[0], punti[1]);
    }

    /**
     * In prima persona: la pila e' quella del braccio disegnato davanti alla camera, quindi i punti
     * escono nello spazio della mano (la camera all'origine, col campo visivo fisso della mano).
     *
     * <p>Si legge sempre, non solo a pugno partito: la mano si disegna dopo il mondo, e leggendola
     * solo da quando il tentacolo c'e' il suo primo fotogramma partiva da una stima sbagliata. Il
     * polsino si disegna dal primo tick del pugno, quindi al tick 8 la lettura c'e' gia'.</p>
     */
    public static void primaPersona(int giocatore, PlayerModel<?> modello, PoseStack pila) {
        Vector3f[] punti = leggi(modello, pila);
        TentacoloPugno.manoPrimaPersona(giocatore, punti[0], punti[1]);
    }

    /** La punta del polsino e il punto dentro, nello spazio della pila. */
    private static Vector3f[] leggi(PlayerModel<?> modello, PoseStack pila) {
        pila.pushPose();
        modello.rightArm.translateAndRotate(pila);
        Matrix4f braccio = pila.last().pose();
        Vector4f punta = new Vector4f(PUNTA).mul(braccio);
        Vector4f dentro = new Vector4f(DENTRO).mul(braccio);
        pila.popPose();
        return new Vector3f[]{new Vector3f(punta.x(), punta.y(), punta.z()),
                new Vector3f(dentro.x(), dentro.y(), dentro.z())};
    }
}
