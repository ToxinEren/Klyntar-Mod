package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import modKlyntar.MyMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.client.renderer.renderlayer.PackRenderLayerManager;
import net.threetag.palladium.compat.geckolib.renderlayer.GeckoRenderLayer;
import net.threetag.palladium.power.ability.AbilityUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Il braccio del simbionte in prima persona, al posto di quello umano.
 *
 * <p>Si vedevano due braccia sovrapposte: quello vanilla del giocatore e quello che Palladium
 * disegna col suo render layer, grosso come in terza persona e quindi enorme a mezzo metro
 * dall'occhio. {@code hidebody} dichiara {@code affects_first_person} ma il braccio umano
 * restava li' lo stesso, gia' con Palladium 4.5.6.</p>
 *
 * <p>Annullare l'evento non basta: Forge lo lancia dentro {@code renderRightHand}, prima della
 * chiamata a {@code renderHand}, e Palladium appende il suo braccio a quest'ultima con un
 * mixin. Annullando spariscono <b>tutti e due</b> e in prima persona non resta niente. Qui
 * quindi si annulla e si disegna, nello stesso gesto.</p>
 *
 * <p>Quello che si disegna e' il braccio del giocatore - la stessa {@code ModelPart}, con la
 * stessa taglia e la stessa animazione del colpo - vestito con {@code black.png}, la pelle del
 * simbionte. E' la texture che il render layer {@code klyntars:black} usa gia' come
 * skin_overlay: sta nel formato della pelle del giocatore e non in quello del modello Bedrock,
 * quindi le coordinate combaciano senza doverle inventare.</p>
 *
 * <p><b>La terza persona non viene toccata.</b> {@code RenderArmEvent} lo lancia solo
 * {@code ItemInHandRenderer}, cioe' esiste soltanto quando si guarda dai propri occhi: da fuori
 * il corpo del simbionte resta quello di sempre, disegnato dal render layer.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class BraccioPrimaPersona {

    /**
     * Le ossa delle braccia nel modello del corpo: in prima persona si disegna solo quello che
     * sta sotto di loro. Sono i nomi del json del layer (bones.right_arm e bones.left_arm).
     */
    private static final Set<String> BRACCIA = Set.of("rightArm_player_anchor", "leftArm_player_anchor");

    /**
     * La scala a cui il corpo viene disegnato in terza persona: quella di PlayerRenderer.
     * In prima persona deve essere la stessa, o le braccia sembrano di un altro corpo.
     */
    private static final float SCALA_TERZA_PERSONA = 0.9375F;
    /**
     * Il divisore che Palladium usa in GeckoRenderLayer.renderArm: scala = occhi / 1.75, cioe'
     * pensa a un modello con gli occhi a 1.75 blocchi. Il corpo di Venom ha gli occhi il doppio
     * piu' in alto, e con gli occhi del giocatore a 3.55 usciva a scala 2.03 invece di 0.9375:
     * piu' del doppio, con la telecamera a meta' del busto.
     */
    private static final float DIVISORE_PALLADIUM = 1.75F;
    /**
     * Di quanti gradi le braccia vengono alzate in avanti, ruotandole attorno alla spalla.
     * Alla taglia giusta la telecamera sta negli occhi e i colpi passano all'altezza del petto,
     * sotto il campo visivo: senza questo del moveset si vedevano solo i pugni sul bordo in
     * basso. Si ruota attorno alla spalla e non attorno alla telecamera: cosi' la spalla resta
     * dov'e', sotto il bordo dello schermo, e il braccio ci resta attaccato invece di fluttuare.
     */
    private static final float ALZA_BRACCIA_GRADI = 45.0F;
    /**
     * Quanto ci mette l'alzata a entrare o uscire, in secondi. Con lo scudo alzato le braccia
     * tornano alla posa dell'animazione: lo scudo si vede gia' da solo, e alzato di altri 45
     * gradi copriva l'angolo in alto a destra. Il passaggio e' graduale perche' altrimenti,
     * a scudo appena alzato, il braccio scatterebbe giu' di colpo.
     */
    private static final float ALZATA_TRANSIZIONE = 0.2F;
    /** L'alzata di adesso, da 0 a 1, e quando e' stata aggiornata l'ultima volta. */
    private static float alzataAttuale = 1.0F;
    private static long alzataAggiornata;

    /** Le due braccia, sempre in questo ordine: 0 la destra, 1 la sinistra. */
    private static final String[] BRACCIA_IN_ORDINE = {"rightArm_player_anchor", "leftArm_player_anchor"};

    /**
     * I raccordi: la pelle, la tinta e la misura dei tubi che uniscono le spalle a sotto la
     * telecamera. La tinta e' bassa perche' il braccio in prima persona appare nero pieno, e un
     * raccordo piu' chiaro si vedrebbe come un pezzo a parte.
     */
    private static final ResourceLocation PELLE_RACCORDO =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/venom_tentacle_segment.png");
    private static final int TINTA_RACCORDO = 30;
    private static final float RAGGIO_SPALLA = 0.30F;
    private static final float RAGGIO_FONDO = 0.42F;
    private static final int LATI_RACCORDO = 12;
    private static final int ANELLI_RACCORDO = 8;
    /**
     * Dove finiscono i raccordi, nello spazio della telecamera: sotto e un filo dietro, dal
     * lato della spalla. Un punto dietro la telecamera non entra mai nell'inquadratura, qualunque
     * sia l'inclinazione dello sguardo, quindi il tubo esce sempre dal bordo dello schermo.
     */
    private static final float FONDO_LATO = 0.45F;
    private static final float FONDO_GIU = -1.2F;
    private static final float FONDO_DIETRO = 0.3F;

    /** Le spalle del fotogramma in corso, nello spazio della telecamera, e se sono state calcolate. */
    private static final Vector3f[] SPALLE = {new Vector3f(), new Vector3f()};
    private static final boolean[] SPALLA_PRONTA = new boolean[2];

    /** Vero solo mentre e' questa classe a chiedere a Palladium il corpo in prima persona. */
    private static boolean disegnoNostro;
    /** Il giocatore che si sta disegnando, per sapere dal mixin se ha lo scudo alzato. */
    private static Player giocatoreDisegnato;
    /** Come rimettere le rotazioni delle braccia dopo il disegno, riempito da dopoLeAnimazioni. */
    private static final List<Runnable> RIPRISTINA_ROTAZIONI = new ArrayList<>();

    /** La pelle del simbionte, nel formato della pelle del giocatore. */
    private static final ResourceLocation PELLE =
            new ResourceLocation(MyMod.MOD_ID, "textures/models/black.png");

    private BraccioPrimaPersona() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void suBraccio(RenderArmEvent evento) {
        if (evento.isCanceled()) {
            return;
        }
        Player giocatore = evento.getPlayer();
        if (giocatore != Minecraft.getInstance().player) {
            return;
        }
        // La guardia e' l'abilita', non l'objective della taglia: AbilityUtil legge lo stato che
        // Palladium sincronizza gia' ai client per i render layer, ed e' quindi vero esattamente
        // quando il corpo si vede. Un primo tentativo passava da corpoAttivo, che lato client
        // dipende da un pacchetto nostro: quel pacchetto non arrivava e non succedeva niente.
        String forma = VenomTentaclesTraversalRenderer.formaDi(giocatore);
        if (forma == null
                || !AbilityUtil.isEnabled(giocatore, new ResourceLocation(MyMod.MOD_ID, forma), "enablevenombody")) {
            return;
        }

        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(giocatore);
        if (!(renderer instanceof PlayerRenderer playerRenderer)
                || !(giocatore instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        evento.setCanceled(true);
        if (!disegnaCorpoIntero(playerRenderer, clientPlayer, evento.getArm(), evento.getPoseStack(),
                evento.getMultiBufferSource(), evento.getPackedLight())) {
            disegna(playerRenderer, clientPlayer, evento.getArm(), evento.getPoseStack(),
                    evento.getMultiBufferSource(), evento.getPackedLight());
        }
    }

    /**
     * Il corpo del simbionte in prima persona, animazioni comprese.
     *
     * <p>Il moveset del click sinistro e lo scudo sono animazioni GeckoLib del corpo: col solo
     * braccio piatto non si vedevano. Palladium sa gia' disegnare un layer GeckoLib intero
     * attorno alla telecamera ({@code render_full_model_in_first_person} nel json del layer),
     * ma lo fa dentro {@code renderHand}, che l'annullamento dell'evento salta. Qui lo si
     * chiede direttamente, e solo per i layer a corpo intero: gli altri (la pelle nera sopra
     * il braccio vanilla) ridisegnerebbero il braccio umano che si voleva togliere.</p>
     *
     * @return se almeno un layer a corpo intero e' attivo: altrimenti si ripiega sul braccio piatto
     */
    private static boolean disegnaCorpoIntero(PlayerRenderer renderer, AbstractClientPlayer giocatore,
                                              HumanoidArm braccio, PoseStack pila,
                                              MultiBufferSource buffer, int luce) {
        boolean[] disegnato = {false};
        PackRenderLayerManager.forEachLayer(giocatore, (contesto, layer) -> {
            if (layer instanceof GeckoRenderLayer gecko && gecko.renderFullModelInFirstPerson) {
                List<Runnable> ripristina = soloBraccia(gecko);
                SPALLA_PRONTA[0] = false;
                SPALLA_PRONTA[1] = false;
                disegnoNostro = true;
                giocatoreDisegnato = giocatore;
                try {
                    layer.renderArm(contesto, braccio, renderer, pila, allaTagliaGiusta(giocatore, buffer), luce);
                } finally {
                    disegnoNostro = false;
                    giocatoreDisegnato = null;
                    // il modello e' lo stesso della terza persona, anche per gli altri giocatori
                    ripristina.addAll(RIPRISTINA_ROTAZIONI);
                    RIPRISTINA_ROTAZIONI.clear();
                    for (int i = ripristina.size() - 1; i >= 0; i--) {
                        ripristina.get(i).run();
                    }
                }
                disegnaRaccordi(pila, buffer, luce);
                disegnato[0] = true;
            }
        });
        return disegnato[0];
    }

    /**
     * Chiamato dal mixin fra le animazioni e il disegno: alza le braccia attorno alla spalla.
     *
     * <p>Le animazioni hanno appena scritto le rotazioni delle ossa, e il disegno le usa subito
     * dopo: e' l'unico momento in cui un'aggiunta sopravvive. La rotazione di prima viene
     * rimessa a disegno finito, perche' le stesse ossa servono alla terza persona.</p>
     */
    public static void dopoLeAnimazioni(GeckoRenderLayer gecko) {
        if (!disegnoNostro || gecko.cachedModel == null) {
            return;
        }
        BakedGeoModel modello = gecko.getModel().getGeoModel().getBakedModel(gecko.cachedModel);
        if (modello == null) {
            return;
        }
        float quanto = alzataVerso(scudoAlzato(giocatoreDisegnato) ? 0.0F : 1.0F);
        if (quanto > 0.0F) {
            float alzata = (float) Math.toRadians(ALZA_BRACCIA_GRADI) * quanto;
            for (String nome : BRACCIA) {
                modello.getBone(nome).ifPresent(osso -> {
                    float prima = osso.getRotX();
                    RIPRISTINA_ROTAZIONI.add(() -> osso.setRotX(prima));
                    osso.setRotX(prima + alzata);
                });
            }
        }
        calcolaSpalle(modello);
    }

    /**
     * Dove stanno le spalle in questo fotogramma, nello spazio della telecamera.
     *
     * <p>Si rifa' la strada che fara' il disegno: la sistemazione di Palladium (inclinazione
     * dello sguardo, giu' fino ai piedi, scala occhi / 1.75), poi osso per osso dalla radice al
     * braccio gli stessi passi di GeckoLib (RenderUtils.prepMatrixForBone: posizione, perno,
     * rotazioni Z Y X, scala, via dal perno), infine la correzione di taglia di questa classe.
     * Il perno del braccio e' la spalla: la rotazione del braccio non lo sposta, quindi il
     * raccordo resta attaccato anche mentre il braccio si alza o colpisce.</p>
     */
    private static void calcolaSpalle(BakedGeoModel modello) {
        Player giocatore = giocatoreDisegnato;
        if (giocatore == null) {
            return;
        }
        float occhi = giocatore.getEyeHeight();
        if (occhi <= 0.0F) {
            return;
        }
        float inclinazione = Mth.lerp(Minecraft.getInstance().getFrameTime(), giocatore.xRotO, giocatore.getXRot());
        float scalaPalladium = occhi / DIVISORE_PALLADIUM;
        float fattore = SCALA_TERZA_PERSONA * DIVISORE_PALLADIUM / occhi;
        Vector3f piedi = new Vector3f(0.0F, -occhi, 0.0F).rotate(Axis.XP.rotationDegrees(inclinazione));
        for (int i = 0; i < BRACCIA_IN_ORDINE.length; i++) {
            GeoBone braccio = modello.getBone(BRACCIA_IN_ORDINE[i]).orElse(null);
            if (braccio == null) {
                continue;
            }
            List<GeoBone> catena = new ArrayList<>();
            for (GeoBone osso = braccio; osso != null; osso = osso.getParent()) {
                catena.add(0, osso);
            }
            PoseStack pila = new PoseStack();
            pila.mulPose(Axis.XP.rotationDegrees(inclinazione));
            pila.translate(0.0F, -occhi, 0.0F);
            pila.scale(scalaPalladium, scalaPalladium, scalaPalladium);
            for (GeoBone osso : catena) {
                preparaOsso(pila, osso);
            }
            Vector4f spalla = new Vector4f(braccio.getPivotX() / 16.0F, braccio.getPivotY() / 16.0F,
                    braccio.getPivotZ() / 16.0F, 1.0F).mul(pila.last().pose());
            SPALLE[i].set(piedi.x() + (spalla.x() - piedi.x()) * fattore,
                    piedi.y() + (spalla.y() - piedi.y()) * fattore,
                    piedi.z() + (spalla.z() - piedi.z()) * fattore);
            SPALLA_PRONTA[i] = true;
        }
    }

    /** Gli stessi passi di GeckoLib RenderUtils.prepMatrixForBone, sui getter che usa lui. */
    private static void preparaOsso(PoseStack pila, GeoBone osso) {
        pila.translate(-osso.getPosX() / 16.0F, osso.getPosY() / 16.0F, osso.getPosZ() / 16.0F);
        pila.translate(osso.getPivotX() / 16.0F, osso.getPivotY() / 16.0F, osso.getPivotZ() / 16.0F);
        if (osso.getRotZ() != 0.0F) {
            pila.mulPose(Axis.ZP.rotation(osso.getRotZ()));
        }
        if (osso.getRotY() != 0.0F) {
            pila.mulPose(Axis.YP.rotation(osso.getRotY()));
        }
        if (osso.getRotX() != 0.0F) {
            pila.mulPose(Axis.XP.rotation(osso.getRotX()));
        }
        pila.scale(osso.getScaleX(), osso.getScaleY(), osso.getScaleZ());
        pila.translate(-osso.getPivotX() / 16.0F, -osso.getPivotY() / 16.0F, -osso.getPivotZ() / 16.0F);
    }

    /**
     * I raccordi: per ogni braccio un tubo nero che dalla spalla scende sotto la telecamera.
     *
     * <p>Il busto non si puo' mostrare (guardando in basso riempiva lo schermo), e senza busto,
     * quando un colpo portava la spalla nell'inquadratura, il braccio finiva tagliato a mezz'aria.
     * Il tubo parte da dentro la spalla e va a un punto dietro la telecamera: non entra mai
     * tutto nell'inquadratura, quindi il braccio sembra continuare oltre il bordo dello schermo,
     * come la mano della prima persona vanilla. Con la spalla fuori campo il tubo resta fuori
     * campo anche lui e non si vede.</p>
     */
    private static void disegnaRaccordi(PoseStack pila, MultiBufferSource buffer, int luce) {
        if (!SPALLA_PRONTA[0] && !SPALLA_PRONTA[1]) {
            return;
        }
        pila.pushPose();
        // lo spazio della telecamera, come lo usa Palladium per il corpo
        pila.last().pose().identity();
        pila.last().normal().identity();
        Matrix4f matrice = pila.last().pose();
        Matrix3f normali = pila.last().normal();
        VertexConsumer consumatore = buffer.getBuffer(RenderType.entityCutoutNoCull(PELLE_RACCORDO));
        for (int i = 0; i < SPALLE.length; i++) {
            if (!SPALLA_PRONTA[i]) {
                continue;
            }
            Vector3f da = SPALLE[i];
            float lato = da.x() >= 0.0F ? FONDO_LATO : -FONDO_LATO;
            tubo(consumatore, matrice, normali, da, new Vector3f(lato, FONDO_GIU, FONDO_DIETRO), luce);
        }
        pila.popPose();
    }

    /** Un tubo dritto a {@link #LATI_RACCORDO} lati, che si allarga dalla spalla al fondo. */
    private static void tubo(VertexConsumer consumatore, Matrix4f matrice, Matrix3f normali,
                             Vector3f da, Vector3f a, int luce) {
        Vector3f asse = new Vector3f(a).sub(da);
        float lunghezza = asse.length();
        if (lunghezza < 1.0E-3F) {
            return;
        }
        asse.div(lunghezza);
        Vector3f aiuto = Math.abs(asse.y()) > 0.9F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f u = new Vector3f(asse).cross(aiuto).normalize();
        Vector3f v = new Vector3f(asse).cross(u).normalize();
        Vector3f[] prima = null;
        Vector3f[] versiPrima = null;
        float vPrima = 0.0F;
        for (int anello = 0; anello <= ANELLI_RACCORDO; anello++) {
            float t = anello / (float) ANELLI_RACCORDO;
            float raggio = RAGGIO_SPALLA + (RAGGIO_FONDO - RAGGIO_SPALLA) * t;
            Vector3f centro = new Vector3f(asse).mul(lunghezza * t).add(da);
            Vector3f[] punti = new Vector3f[LATI_RACCORDO + 1];
            Vector3f[] versi = new Vector3f[LATI_RACCORDO + 1];
            for (int k = 0; k <= LATI_RACCORDO; k++) {
                double angolo = Math.PI * 2.0D * k / LATI_RACCORDO;
                Vector3f verso = new Vector3f(u).mul((float) Math.cos(angolo))
                        .add(new Vector3f(v).mul((float) Math.sin(angolo)));
                versi[k] = verso;
                punti[k] = new Vector3f(verso).mul(raggio).add(centro);
            }
            float vOra = lunghezza * t;
            if (prima != null) {
                for (int k = 0; k < LATI_RACCORDO; k++) {
                    float u0 = k / (float) LATI_RACCORDO;
                    float u1 = (k + 1) / (float) LATI_RACCORDO;
                    vertice(consumatore, matrice, normali, prima[k], versiPrima[k], u0, vPrima, luce);
                    vertice(consumatore, matrice, normali, prima[k + 1], versiPrima[k + 1], u1, vPrima, luce);
                    vertice(consumatore, matrice, normali, punti[k + 1], versi[k + 1], u1, vOra, luce);
                    vertice(consumatore, matrice, normali, punti[k], versi[k], u0, vOra, luce);
                }
            }
            prima = punti;
            versiPrima = versi;
            vPrima = vOra;
        }
    }

    private static void vertice(VertexConsumer consumatore, Matrix4f matrice, Matrix3f normali,
                                Vector3f posizione, Vector3f verso, float u, float v, int luce) {
        consumatore.vertex(matrice, posizione.x(), posizione.y(), posizione.z())
                .color(TINTA_RACCORDO, TINTA_RACCORDO, TINTA_RACCORDO, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(luce)
                .normal(normali, verso.x(), verso.y(), verso.z())
                .endVertex();
    }

    /** Avvicina l'alzata all'obiettivo, alla velocita' di {@link #ALZATA_TRANSIZIONE}. */
    private static float alzataVerso(float obiettivo) {
        long adesso = System.nanoTime();
        float trascorso = alzataAggiornata == 0L ? 0.0F : (adesso - alzataAggiornata) / 1.0E9F;
        alzataAggiornata = adesso;
        float passo = Math.min(1.0F, trascorso / ALZATA_TRANSIZIONE);
        alzataAttuale += Mth.clamp(obiettivo - alzataAttuale, -passo, passo);
        return alzataAttuale;
    }

    /** Se il giocatore tiene alzato lo scudo del simbionte (il click destro tenuto). */
    private static boolean scudoAlzato(Player giocatore) {
        if (giocatore == null) {
            return false;
        }
        String forma = VenomTentaclesTraversalRenderer.formaDi(giocatore);
        return forma != null
                && AbilityUtil.isEnabled(giocatore, new ResourceLocation(MyMod.MOD_ID, forma), "venomblock");
    }

    /**
     * Riporta il corpo in prima persona alla taglia che ha in terza.
     *
     * <p>Palladium rimette la pila a zero prima di disegnare (la telecamera), ruota per
     * l'inclinazione dello sguardo, scende di {@code occhi} fino ai piedi e scala di
     * {@code occhi / 1.75}. Tutto quello che si fa sulla pila prima della chiamata si perde, e
     * il 1.75 e' scritto nel suo codice. Si corregge allora dopo: ogni vertice che esce viene
     * avvicinato ai piedi dello stesso fattore che manca alla scala. I piedi restano dove
     * Palladium li mette, cioe' {@code occhi} sotto la telecamera come in terza persona, e il
     * corpo intorno torna della sua misura: la telecamera finisce all'altezza degli occhi del
     * modello invece che nel petto.</p>
     */
    private static MultiBufferSource allaTagliaGiusta(AbstractClientPlayer giocatore, MultiBufferSource buffer) {
        float occhi = giocatore.getEyeHeight();
        if (occhi <= 0.0F) {
            return buffer;
        }
        float fattore = SCALA_TERZA_PERSONA * DIVISORE_PALLADIUM / occhi;
        float inclinazione = Mth.lerp(Minecraft.getInstance().getFrameTime(), giocatore.xRotO, giocatore.getXRot());
        Vector3f piedi = new Vector3f(0.0F, -occhi, 0.0F).rotate(Axis.XP.rotationDegrees(inclinazione));
        return tipo -> new VerticiRidotti(buffer.getBuffer(tipo), piedi, fattore);
    }

    /** Un VertexConsumer che avvicina ogni vertice a un centro di un fattore fisso. */
    private static final class VerticiRidotti implements VertexConsumer {
        private final VertexConsumer dentro;
        private final float cx;
        private final float cy;
        private final float cz;
        private final float fattore;
        VerticiRidotti(VertexConsumer dentro, Vector3f centro, float fattore) {
            this.dentro = dentro;
            this.cx = centro.x();
            this.cy = centro.y();
            this.cz = centro.z();
            this.fattore = fattore;
        }

        private Vector3f sposta(float x, float y, float z) {
            return new Vector3f(cx + (x - cx) * fattore, cy + (y - cy) * fattore, cz + (z - cz) * fattore);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            Vector3f p = sposta((float) x, (float) y, (float) z);
            dentro.vertex(p.x(), p.y(), p.z());
            return this;
        }

        /** Il percorso veloce che usa GeckoLib: stessa correzione, il resto passa intatto. */
        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                           int overlay, int luce, float nx, float ny, float nz) {
            Vector3f p = sposta(x, y, z);
            dentro.vertex(p.x(), p.y(), p.z(), r, g, b, a, u, v, overlay, luce, nx, ny, nz);
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            dentro.color(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            dentro.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            dentro.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            dentro.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            // una scala uniforme non cambia la direzione delle normali
            dentro.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            dentro.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            dentro.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            dentro.unsetDefaultColor();
        }
    }

    /**
     * Nasconde tutto il modello tranne le braccia, e restituisce come rimetterlo com'era.
     *
     * <p>La testa va via perche' la telecamera ci sta dentro. Anche il busto: provato, guardando
     * in basso riempiva lo schermo di nero, e negli attacchi il petto entrava in vista. Le
     * braccia restano attaccate lo stesso, perche' si alzano attorno alla spalla e la spalla
     * resta sotto il bordo dello schermo (vedi {@link #dopoLeAnimazioni}).</p>
     *
     * <p>In GeckoLib nascondere un osso nasconde anche i figli. Gli antenati delle braccia
     * (radice, vita, busto) perdono quindi solo i loro cubi e tengono visibili i figli; tutto
     * il resto (testa, gambe, ali, tentacoli) sparisce col suo sottoalbero. Palladium non rimette
     * a posto la visibilita' in questo percorso: la rimette solo in applyBaseTransformations, che
     * qui non viene chiamato.</p>
     */
    private static List<Runnable> soloBraccia(GeckoRenderLayer gecko) {
        List<Runnable> ripristina = new ArrayList<>();
        if (gecko.cachedModel == null) {
            return ripristina;
        }
        BakedGeoModel modello = gecko.getModel().getGeoModel().getBakedModel(gecko.cachedModel);
        if (modello == null) {
            return ripristina;
        }
        for (GeoBone osso : modello.topLevelBones()) {
            nascondiTranneBraccia(osso, ripristina);
        }
        return ripristina;
    }

    private static void nascondiTranneBraccia(GeoBone osso, List<Runnable> ripristina) {
        if (BRACCIA.contains(osso.getName())) {
            return;
        }
        boolean nascosto = osso.isHidden();
        boolean figliNascosti = osso.isHidingChildren();
        ripristina.add(() -> {
            osso.setHidden(nascosto);
            osso.setChildrenHidden(figliNascosti);
        });
        if (contieneBraccia(osso)) {
            osso.setHidden(true);
            osso.setChildrenHidden(false);
            for (GeoBone figlio : osso.getChildBones()) {
                nascondiTranneBraccia(figlio, ripristina);
            }
        } else {
            osso.setHidden(true);
        }
    }

    private static boolean contieneBraccia(GeoBone osso) {
        for (GeoBone figlio : osso.getChildBones()) {
            if (BRACCIA.contains(figlio.getName()) || contieneBraccia(figlio)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gli stessi passi di {@code PlayerRenderer.renderHand}, con la nostra texture.
     *
     * <p>Il modello va riportato alla posa neutra prima di disegnare: {@code setupAnim} lascia
     * dentro la posa dell'ultimo fotogramma in terza persona - accovacciato, a nuoto, col
     * braccio a meta' colpo - e in prima persona uscirebbe un braccio storto. La rotazione
     * sull'asse X si azzera perche' a inclinare il braccio ci pensa gia' la pila, che
     * {@code ItemInHandRenderer} ha preparato.</p>
     */
    private static void disegna(PlayerRenderer renderer, AbstractClientPlayer giocatore, HumanoidArm braccio,
                                PoseStack pila, MultiBufferSource buffer, int luce) {
        PlayerModel<AbstractClientPlayer> modello = renderer.getModel();
        modello.attackTime = 0.0F;
        modello.crouching = false;
        modello.swimAmount = 0.0F;
        modello.setupAnim(giocatore, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        boolean destro = braccio == HumanoidArm.RIGHT;
        ModelPart arto = destro ? modello.rightArm : modello.leftArm;
        ModelPart manica = destro ? modello.rightSleeve : modello.leftSleeve;
        // Il braccio va riacceso, e non e' una precauzione: hidebody spegne right_arm sul
        // modello del giocatore, che e' uno solo e condiviso. Passando dalla terza alla prima
        // persona quel visible=false resta appiccicato li' e il braccio non si disegna piu'.
        // Il renderHand di vanilla non ha il problema perche' chiama setModelProperties, che
        // rimette tutto visibile; quel metodo e' privato, quindi qui si riaccende a mano.
        arto.visible = true;
        manica.visible = giocatore.isModelPartShown(
                destro ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);
        arto.xRot = 0.0F;
        manica.xRot = 0.0F;

        // translucent e non solid: la pelle del simbionte ha pixel semitrasparenti sui bordi,
        // e con il tipo opaco verrebbero squadrati
        VertexConsumer consumatore = buffer.getBuffer(RenderType.entityTranslucent(PELLE));
        arto.render(pila, consumatore, luce, OverlayTexture.NO_OVERLAY);
        manica.render(pila, consumatore, luce, OverlayTexture.NO_OVERLAY);
    }
}
