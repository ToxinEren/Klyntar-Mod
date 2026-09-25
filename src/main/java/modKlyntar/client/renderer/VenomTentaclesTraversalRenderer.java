package modKlyntar.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import modKlyntar.MyMod;
import modKlyntar.player.SymbioteInvisibilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, value = Dist.CLIENT)
public final class VenomTentaclesTraversalRenderer {
    private static final ResourceLocation ARM_TEXTURE = new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/venom_tentacle_segment.png");
    /** la stessa texture in bianco, per la forma anti-venom */
    private static final ResourceLocation ARM_TEXTURE_ANTI = new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/antivenom_tentacle_segment.png");
    /** e in rosso, per la forma carnage */
    private static final ResourceLocation ARM_TEXTURE_CARN = new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/carnage_tentacle_segment.png");

    /** i tentacoli di Toxin, arancioni */

    private static final ResourceLocation ARM_TEXTURE_TOX = new ResourceLocation(MyMod.MOD_ID, "textures/models/tentacles_traversal/toxin_tentacle_segment.png");
    /**
     * Texture del giocatore che si sta disegnando. I tentacoli si disegnano anche per gli altri
     * giocatori, quindi non basta sapere la forma di chi guarda: la si legge dall'obiettivo che
     * il server tiene aggiornato per ciascuno.
     */
    private static ResourceLocation armTexture = ARM_TEXTURE;

    /** forma simbionte di ogni giocatore, aggiornata dal server via SyncSymbioteFormPacket */
    private static final Map<Integer, String> FORMS = new ConcurrentHashMap<>();

    public static void updateForm(int entityId, String form) {
        if (form == null || form.isEmpty()) {
            FORMS.remove(entityId);
        } else {
            FORMS.put(entityId, form);
        }
    }

    private static Method metodoPoteri;
    private static boolean poteriCercati;

    /**
     * Legge il superpotere che Palladium ha davvero assegnato al giocatore.
     *
     * <p>Piu' affidabile del messaggio mandato al momento della trasformazione: quello arriva
     * una sola volta, quindi chi era gia' trasformato al login, o chi riceve il potere per altre
     * vie, non verrebbe mai riconosciuto.</p>
     */
    private static String formaDaPalladium(Player player) {
        if (!poteriCercati) {
            poteriCercati = true;
            try {
                metodoPoteri = Class.forName("net.threetag.palladium.power.SuperpowerUtil")
                        .getMethod("getSuperpowerIds", LivingEntity.class);
            } catch (ReflectiveOperationException ignorata) {
                metodoPoteri = null;
            }
        }
        if (metodoPoteri == null) {
            return null;
        }
        try {
            if (metodoPoteri.invoke(null, player) instanceof Collection<?> poteri) {
                for (Object p : poteri) {
                    String id = String.valueOf(p);
                    // i piu' specifici per primi, anche se qui non si accavallano:
                    // ":venom" non combacia con "venomspidey", che finisce per "spidey"
                    if (id.endsWith(":allblack")) return "allblack";
                    if (id.endsWith(":venomspidey")) return "venomspidey";
                    if (id.endsWith(":antivenom")) return "antivenom";
                    if (id.endsWith(":carnage")) return "carnage";

                    if (id.endsWith(":toxin")) return "toxin";
                    if (id.endsWith(":venom")) return "venom";
                }
            }
        } catch (ReflectiveOperationException ignorata) {
            // Palladium assente o firma cambiata: si ricade sulla forma sincronizzata
        }
        return null;
    }

    /**
     * Sceglie pelle e tinta dei tentacoli secondo il simbionte indossato.
     *
     * <p>La tinta serve perche' la texture da sola non basta: il colore del vertice la
     * moltiplica, e lasciandolo sul grigio scuro di Venom ogni altra forma verrebbe spenta.</p>
     */
    private static void scegliAspetto(Player player) {
        armTexture = textureFor(player);
        int[] tinta;
        if (armTexture == ARM_TEXTURE_ANTI) {
            tinta = new int[]{ARM_COLOR_ANTI, ARM_COLOR_ANTI, ARM_COLOR_ANTI};
        } else if (armTexture == ARM_TEXTURE_CARN) {
            tinta = ARM_COLOR_CARN;
        } else if (armTexture == ARM_TEXTURE_TOX) {
            tinta = ARM_COLOR_TOX;
        } else {
            tinta = new int[]{ARM_COLOR, ARM_COLOR, ARM_COLOR};
        }
        armR = tinta[0];
        armG = tinta[1];
        armB = tinta[2];
        // il tubo di Venom ha la sua tinta; le altre forme hanno gia' una tinta chiara
        boolean venom = armTexture == ARM_TEXTURE;
        tuboR = venom ? TINTA_TUBO_VENOM : armR;
        tuboG = venom ? TINTA_TUBO_VENOM : armG;
        tuboB = venom ? TINTA_TUBO_VENOM : armB;
    }

    /** una texture per forma: bianca per anti-venom, rossa per carnage, arancione per toxin */
    /** La forma simbionte di questo giocatore, o {@code null} se non ne ha nessuna. */
    public static String formaDi(Player player) {
        String cache = FORMS.get(player.getId());
        return cache != null ? cache : formaDaPalladium(player);
    }

    public static ResourceLocation textureFor(Player player) {
        String forma = formaDaPalladium(player);
        if (forma == null) {
            forma = FORMS.get(player.getId());
        }
        if ("antivenom".equals(forma)) return ARM_TEXTURE_ANTI;
        if ("carnage".equals(forma)) return ARM_TEXTURE_CARN;
        if ("toxin".equals(forma)) return ARM_TEXTURE_TOX;
        return ARM_TEXTURE;
    }
    private static final int ARM_SEGMENTS = 18;
    private static final int VISIBLE_ARMS = 6;
    /**
     * Quanto le braccia della locomozione stanno davanti al centro del giocatore,
     * in frazione dell'altezza del modello.
     *
     * <p>L'animazione della locomozione porta il modello in avanti rispetto alla
     * posizione dell'entita': con l'origine dietro, i tentacoli restavano staccati.
     * E' l'unico numero da toccare: positivo le porta avanti, negativo indietro.</p>
     */
    private static final double SPORGENZA_AVANTI = -0.03D;
    private static final int ARM_COLOR = 18;
    /**
     * I vertici vengono tinti di questo colore, che moltiplica la texture: con 18 su tutti i
     * canali qualunque immagine esce nera. Per anti-venom serve il valore chiaro, altrimenti la
     * texture bianca non si vedrebbe comunque.
     */
    private static final int ARM_COLOR_ANTI = 235;
    /** il rosso di Carnage va espresso sui tre canali: un solo valore darebbe solo un grigio */
    private static final int[] ARM_COLOR_CARN = {190, 42, 46};

    /** l'arancio di Toxin */

    /**
     * Tinta neutra: la texture di toxin porta gia' i colori del body_venom del suo
     * modello, quindi il colore del vertice non deve ricolorarla una seconda volta.
     * Con una tinta colorata il risultato era texture x tinta, cioe' molto piu' scuro
     * e di un arancione diverso da quello del corpo.
     */
    private static final int[] ARM_COLOR_TOX = {255, 255, 255};
    private static int armR = ARM_COLOR, armG = ARM_COLOR, armB = ARM_COLOR;
    /**
     * La tinta del tubo dei bracci di Venom. Prima era {@link #ARM_COLOR}, 18 su 255: la texture
     * e' gia' scura di suo (fondo 18, venature fino a 58) e moltiplicata per 18/255 scendeva a
     * 1-4, nero pieno, senza venature e senza luce ne' ombra sul tubo tondo. I filamenti di
     * living armor restano su {@link #ARM_COLOR}: piu' scuri del braccio, gli fanno da rilievo.
     */
    private static final int TINTA_TUBO_VENOM = 170;
    private static int tuboR = TINTA_TUBO_VENOM, tuboG = TINTA_TUBO_VENOM, tuboB = TINTA_TUBO_VENOM;
    private static final float TENTACLE_THICKNESS_SCALE = 1.5F;
    private static final float TENTACLE_RADIUS = 0.085F * TENTACLE_THICKNESS_SCALE;
    private static final float TENTACLE_TIP_RADIUS = 0.018F * TENTACLE_THICKNESS_SCALE;
    private static final int TENTACLE_TIP_SEGMENTS = 3;
    /**
     * I lati della sezione del braccio. Era un quadrato: dodici lati si leggono tondi a
     * qualunque distanza, e la luce ci scivola sopra invece di spaccarsi su quattro facce.
     */
    private static final int LATI_BRACCIO = 12;
    /**
     * Il raggio del braccio tondo. Non e' {@link #TENTACLE_RADIUS}: quello era il mezzo lato di
     * un quadrato, e un quadrato visto da tutti i lati appare in media largo 4/pi volte un
     * cerchio dello stesso numero. Moltiplicandolo per 4/pi il braccio resta grosso com'era a
     * colpo d'occhio: un po' piu' largo di prima guardando una faccia, appena piu' stretto
     * guardando uno spigolo.
     */
    private static final float RAGGIO_BRACCIO = TENTACLE_RADIUS * 4.0F / (float) Math.PI;
    /** La punta, riportata dal quadrato al cerchio allo stesso modo. */
    private static final float RAGGIO_PUNTA = TENTACLE_TIP_RADIUS * 4.0F / (float) Math.PI;
    /**
     * Di quanto il braccio si gonfia a meta' e si allarga dove esce dal corpo, in frazioni del
     * raggio. Solo in aggiunta: prima della punta non c'e' punto piu' sottile di
     * {@link #RAGGIO_BRACCIO}.
     */
    private static final double PANCIA = 0.14D;
    private static final double COLLETTO = 0.12D;
    /** Dove il braccio comincia ad affusolarsi: gli stessi ultimi tre pezzi su diciotto di prima. */
    private static final double INIZIO_PUNTA = (ARM_SEGMENTS - TENTACLE_TIP_SEGMENTS) / (double) ARM_SEGMENTS;
    /**
     * Ogni quanti blocchi di braccio la texture ricomincia. Con sedici pixel attorno a circa un
     * blocco di circonferenza, un blocco anche in lunghezza tiene i texel quadrati e alla stessa
     * densita' del resto del mondo.
     */
    private static final double TEXTURE_OGNI = 1.0D;

    /** In quanti tick un braccio arriva all'appiglio: poco per i vicini, un po' di piu' per i lontani. */
    private static final double ALLUNGO_MIN = 3.0D;
    private static final double ALLUNGO_MAX = 7.0D;
    private static final double ALLUNGO_PER_BLOCCO = 0.15D;
    /** In quanti tick un braccio che ha mollato torna nel corpo. */
    private static final double RITIRO = 5.0D;

    /** I nodi della corda: quelli della mod di riferimento, sedici. */
    private static final int NODI = 16;
    /** La corda avanza a passi fissi di un quarto di tick, qualunque sia il frame rate. */
    private static final double PASSO = 0.25D;
    /** Quanta velocita' perde a ogni passo: senza, oscillerebbe per sempre. */
    private static final double SMORZA = 0.16D;
    /** Quanto la tira giu' il peso, in blocchi per tick al quadrato. */
    private static final double GRAVITA = 0.02D;
    /** Quante volte per passo si rimettono i nodi alla giusta distanza. */
    private static final int RILASSA = 5;
    /** Quanto la corda e' piu' lunga della distanza fra spalla e appiglio: un filo di lasco. */
    private static final double LASCO = 1.004D;

    /** Il minimo fra due suoni dello stesso tipo, per giocatore: la traversata riaggancia spesso. */
    private static final double PAUSA_SUONO = 3.0D;

    /** Un braccio della traversata, con la sua storia. */
    private static final class Braccio {
        /** L'appiglio che tiene adesso, o null. */
        Vec3 appiglio;
        /** Quando l'ha preso, in tick: da qui parte l'allungamento. */
        double nato;
        boolean partitoSuonato;
        boolean presaSuonata;
        /** L'appiglio appena lasciato, verso cui il braccio si sta ritirando. */
        Vec3 vecchio;
        double mollato;
        boolean ritiroSuonato;
        /** La corda: nodi di adesso e del passo prima, in coordinate del mondo. */
        Vec3[] nodi;
        Vec3[] nodiPrima;
        double ultimoPasso;
    }

    /** I bracci di un giocatore, uno per posto, e quando ha suonato l'ultima volta. */
    private static final class Corredo {
        final Braccio[] posti = new Braccio[VISIBLE_ARMS];
        double suonoAllungo = -1.0E9D;
        double suonoPresa = -1.0E9D;
        double suonoRitiro = -1.0E9D;

        Corredo() {
            for (int i = 0; i < posti.length; i++) {
                posti[i] = new Braccio();
            }
        }

        /** Nessun braccio aggrappato e nessuno che si stia ancora ritirando. */
        boolean fermo(double ora) {
            for (Braccio b : posti) {
                if (b.appiglio != null || (b.vecchio != null && ora - b.mollato < RITIRO)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final Map<Integer, Corredo> BRACCIA = new ConcurrentHashMap<>();

    /**
     * Quanto si piega il braccio mentre parte, in frazione della sua lunghezza, per mossa di
     * Fend Off: la frusta di lato e un po' in alto, lo schiaffo largo e piatto come un
     * manrovescio, la presa quasi dritta.
     */
    private static final double[] CURVA_MOSSA = { 0.3D, 0.6D, 0.12D };
    private static final double[] ALZA_MOSSA = { 0.6D, 0.0D, 0.3D };
    /** Il numero della presa, come arriva dal server (VenomFendOffEnemiesHandler.Mossa). */
    private static final int MOSSA_PRESA = 2;

    /**
     * Un braccio di Fend Off: chi lo tira, a chi, con che mossa, da quale posto sulla schiena,
     * quando e' partito e per quanto. Tiene l'ultimo punto in cui ha visto il bersaglio: se
     * muore mentre e' tenuto, il braccio non torna al punto di partenza ma rientra da li'.
     */
    private static final class Frustata {
        final int giocatore;
        final int bersaglio;
        final int mossa;
        final int posto;
        final long partita;
        final double arriva;
        final double resta;
        final double rientra;
        Vec3 punto;
        /** Da quando rientra prima del tempo, perche' il bersaglio non c'e' piu'; -1 se no. */
        double rientraDa = -1.0D;

        Frustata(int giocatore, int bersaglio, Vec3 punto, int mossa, int posto, long partita,
                 int arriva, int resta, int rientra) {
            this.giocatore = giocatore;
            this.bersaglio = bersaglio;
            this.punto = punto;
            this.mossa = Math.max(0, Math.min(CURVA_MOSSA.length - 1, mossa));
            this.posto = posto;
            this.partita = partita;
            this.arriva = Math.max(1, arriva);
            this.resta = Math.max(0, resta);
            this.rientra = Math.max(1, rientra);
        }

        double durata() {
            return rientraDa >= 0.0D ? rientraDa + rientra : arriva + resta + rientra;
        }
    }

    private static final List<Frustata> FRUSTATE = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Il prossimo posto sulla schiena, per giocatore: frustate vicine partono da punti diversi. */
    private static final Map<Integer, Integer> PROSSIMO_POSTO = new ConcurrentHashMap<>();
    private static final Map<Integer, AnchorState> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<Integer, GrabTargetState> GRAB_TARGETS = new ConcurrentHashMap<>();
    private static final Map<Integer, CombatTargetState> COMBAT_TARGETS = new ConcurrentHashMap<>();

    private VenomTentaclesTraversalRenderer() {
    }

    public static void updateAnchors(int entityId, List<Vec3> anchors, boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (!active) {
            ANCHORS.remove(entityId);
            ritiraTutti(entityId, gameTime);
            return;
        }
        // i null contano: sono i posti vuoti, e List.copyOf non li accetterebbe
        List<Vec3> perPosto = anchors == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(anchors));
        ANCHORS.put(entityId, new AnchorState(perPosto, gameTime));

        Corredo corredo = BRACCIA.computeIfAbsent(entityId, id -> new Corredo());
        for (int posto = 0; posto < VISIBLE_ARMS; posto++) {
            Vec3 nuovo = posto < perPosto.size() ? perPosto.get(posto) : null;
            Braccio b = corredo.posti[posto];
            if (stessoAppiglio(nuovo, b.appiglio)) {
                continue;
            }
            if (b.appiglio != null) {
                // il vecchio appiglio si ritira mentre il nuovo, se c'e', si allunga
                b.vecchio = b.appiglio;
                b.mollato = gameTime;
                b.ritiroSuonato = false;
            }
            b.appiglio = nuovo;
            if (nuovo != null) {
                b.nato = gameTime;
                b.partitoSuonato = false;
                b.presaSuonata = false;
                b.nodi = null;
            }
        }
    }

    /** Chi smette di aggrapparsi ritira tutte le braccia, invece di farle sparire. */
    private static void ritiraTutti(int entityId, long gameTime) {
        Corredo corredo = BRACCIA.get(entityId);
        if (corredo == null) {
            return;
        }
        for (Braccio b : corredo.posti) {
            if (b.appiglio != null) {
                b.vecchio = b.appiglio;
                b.mollato = gameTime;
                b.ritiroSuonato = false;
                b.appiglio = null;
            }
        }
    }

    private static boolean stessoAppiglio(Vec3 a, Vec3 b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.distanceToSqr(b) < 1.0E-4D;
    }
    public static void aggiungiFrustata(int giocatore, int bersaglio, Vec3 punto,
                                        int mossa, int arriva, int resta, int rientra) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int posto = PROSSIMO_POSTO.merge(giocatore, 1, (vecchio, uno) -> (vecchio + 1) % VISIBLE_ARMS);
        FRUSTATE.add(new Frustata(giocatore, bersaglio, punto, mossa, posto, gameTime, arriva, resta, rientra));
    }

    public static void updateGrabTarget(int entityId, Vec3 target) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (target == null) {
            GRAB_TARGETS.remove(entityId);
            return;
        }
        GRAB_TARGETS.put(entityId, new GrabTargetState(target, gameTime));
    }
    public static void updateCombatTargets(int entityId, List<Vec3> targets) {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (targets == null || targets.isEmpty()) {
            COMBAT_TARGETS.remove(entityId);
            return;
        }
        COMBAT_TARGETS.put(entityId, new CombatTargetState(List.copyOf(targets), gameTime));
    }

    public static List<Vec3> getAnchors(int entityId) {
        AnchorState state = ANCHORS.get(entityId);
        if (state == null) {
            return Collections.emptyList();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.getGameTime() - state.gameTime > 8L) {
            return Collections.emptyList();
        }
        // la lista arriva per posto, con i buchi: chi la usa conta le braccia aggrappate
        List<Vec3> aggrappati = new ArrayList<>(state.anchors.size());
        for (Vec3 appiglio : state.anchors) {
            if (appiglio != null) {
                aggrappati.add(appiglio);
            }
        }
        return aggrappati;
    }

    /**
     * Sparire vuol dire sparire tutto, tentacoli compresi.
     *
     * <p>Questi non passano dal renderer del giocatore ma si disegnano in spazio-mondo, quindi
     * l'annullamento di {@code RenderPlayerEvent} che nasconde corpo e strati non li tocca:
     * senza questo controllo resterebbero appesi in aria a tradire chi e' invisibile.
     * Vale per chiunque, non solo per il proprio giocatore: cosi' regge anche in
     * multiplayer.</p>
     */
    private static boolean nascostoDallInvisibilita(Player giocatore) {
        return SymbioteInvisibilityHandler.invisibileDaSimbionte(giocatore);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || (ANCHORS.isEmpty() && BRACCIA.isEmpty() && FRUSTATE.isEmpty()
                        && GRAB_TARGETS.isEmpty() && COMBAT_TARGETS.isEmpty())) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        long gameTime = minecraft.level.getGameTime();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        List<Integer> staleEntries = new ArrayList<>();
        for (Map.Entry<Integer, AnchorState> entry : ANCHORS.entrySet()) {
            if (gameTime - entry.getValue().gameTime > 8L) {
                staleEntries.add(entry.getKey());
            }
        }
        // se i pacchetti smettono di arrivare le braccia si ritirano, non restano appese
        for (Integer id : staleEntries) {
            ANCHORS.remove(id);
            ritiraTutti(id, gameTime);
        }
        double ora = gameTime + event.getPartialTick();
        List<Integer> finiti = new ArrayList<>();
        for (Map.Entry<Integer, Corredo> entry : BRACCIA.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof Player player)) {
                finiti.add(entry.getKey());
                continue;
            }
            Corredo corredo = entry.getValue();
            if (!nascostoDallInvisibilita(player)) {
                scegliAspetto(player);
                renderPlayerArms(player, corredo, event.getPartialTick(), poseStack, buffer, gameTime, ora);
                buffer.endBatch(RenderType.entityCutoutNoCull(armTexture));
            }
            if (corredo.fermo(ora)) {
                finiti.add(entry.getKey());
            }
        }
        finiti.forEach(BRACCIA::remove);

        for (Frustata frustata : FRUSTATE) {
            double eta = ora - frustata.partita;
            if (eta >= frustata.durata() || eta < -2.0D) {
                FRUSTATE.remove(frustata);
                continue;
            }
            Entity entity = minecraft.level.getEntity(frustata.giocatore);
            if (entity instanceof Player player && !nascostoDallInvisibilita(player)) {
                scegliAspetto(player);
                disegnaFrustata(player, frustata, Math.max(0.0D, eta), event.getPartialTick(), poseStack, buffer, gameTime);
                buffer.endBatch(RenderType.entityCutoutNoCull(armTexture));
            }
        }
        List<Integer> staleGrabTargets = new ArrayList<>();
        for (Map.Entry<Integer, GrabTargetState> entry : GRAB_TARGETS.entrySet()) {
            GrabTargetState state = entry.getValue();
            if (gameTime - state.gameTime > 8L) {
                staleGrabTargets.add(entry.getKey());
                continue;
            }
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity instanceof Player player && !nascostoDallInvisibilita(player)) {
                scegliAspetto(player);
                renderPlayerGrabTentacle(player, state.target, event.getPartialTick(), poseStack, buffer, gameTime);
                buffer.endBatch(RenderType.entityCutoutNoCull(armTexture));
            }
        }
        List<Integer> staleCombatTargets = new ArrayList<>();
        for (Map.Entry<Integer, CombatTargetState> entry : COMBAT_TARGETS.entrySet()) {
            CombatTargetState state = entry.getValue();
            if (gameTime - state.gameTime > 8L) {
                staleCombatTargets.add(entry.getKey());
                continue;
            }
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity instanceof Player player && !nascostoDallInvisibilita(player)) {
                scegliAspetto(player);
                renderPlayerCombatTentacles(player, state.targets, event.getPartialTick(), poseStack, buffer, gameTime);
                buffer.endBatch(RenderType.entityCutoutNoCull(armTexture));
            }
        }
        poseStack.popPose();
        staleGrabTargets.forEach(GRAB_TARGETS::remove);
        staleCombatTargets.forEach(COMBAT_TARGETS::remove);
    }

    private static void renderPlayerArms(Player player, Corredo corredo, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffer, long gameTime, double ora) {
        Vec3 playerPos = player.getPosition(partialTick);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 horizontalLook = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        // le braccia devono uscire dal torso del modello indossato, non da un'altezza
        // fissa: il corpo del simbionte e' alto piu' del doppio di un giocatore vanilla,
        // e con gli offset originali i tentacoli spuntavano all'altezza della vita
        double altezza = player.getBbHeight();
        double scala = altezza / 1.8D;
        double[] sideOffsets = new double[] { -0.18D, 0.18D, -0.11D, 0.11D, -0.04D, 0.04D };
        // frazioni dell'altezza del modello, non offset fissi: la fascia del torso.
        // Scalare i valori vanilla non andava bene perche' 1.72 su un giocatore alto
        // 1.8 e' gia' sopra gli occhi, e sul simbionte finiva dietro la testa
        double[] heightOffsets = new double[] { 0.56D, 0.56D, 0.63D, 0.63D, 0.70D, 0.70D };
        for (int i = 0; i < sideOffsets.length; i++) {
            sideOffsets[i] *= scala;
            heightOffsets[i] *= altezza;
        }

        Level livello = player.level();
        Vec3 avanti = horizontalLook.scale(SPORGENZA_AVANTI * altezza);
        // il posto decide la spalla e l'altezza, e non cambia finche' il braccio vive
        for (int posto = 0; posto < VISIBLE_ARMS; posto++) {
            Braccio b = corredo.posti[posto];
            if (b.appiglio == null && b.vecchio == null) {
                continue;
            }
            Vec3 root = playerPos.add(right.scale(sideOffsets[posto]))
                    .add(avanti).add(0.0D, heightOffsets[posto], 0.0D);
            VertexConsumer consumatore = buffer.getBuffer(RenderType.entityCutoutNoCull(armTexture));

            // quello che ha mollato torna nel corpo, dritto e sempre piu' corto
            if (b.vecchio != null) {
                double r = (ora - b.mollato) / RITIRO;
                if (r >= 1.0D) {
                    b.vecchio = null;
                } else {
                    if (!b.ritiroSuonato) {
                        b.ritiroSuonato = true;
                        if (ora - corredo.suonoRitiro >= PAUSA_SUONO) {
                            corredo.suonoRitiro = ora;
                            suona(livello, root, SoundEvents.HONEY_BLOCK_SLIDE, 0.3F, 1.3F);
                        }
                    }
                    double resta = 1.0D - liscio(Math.max(0.0D, r));
                    Vec3 punta = root.add(b.vecchio.subtract(root).scale(resta));
                    if (punta.distanceToSqr(root) > 0.01D) {
                        disegnaTubo(consumatore, poseStack, dritto(root, punta, ARM_SEGMENTS + 1), punta.subtract(root));
                    }
                }
            }
            if (b.appiglio == null) {
                continue;
            }

            // quello che ha appena preso l'appiglio ci arriva, veloce all'inizio e piano in fondo
            double distanza = b.appiglio.distanceTo(root);
            double durata = Math.max(ALLUNGO_MIN, Math.min(ALLUNGO_MAX, ALLUNGO_MIN + distanza * ALLUNGO_PER_BLOCCO));
            double e = Math.max(0.0D, Math.min(1.0D, (ora - b.nato) / durata));
            if (!b.partitoSuonato && ora - corredo.suonoAllungo >= PAUSA_SUONO) {
                corredo.suonoAllungo = ora;
                suona(livello, root, SoundEvents.SLIME_SQUISH_SMALL, 0.35F, 1.5F);
            }
            b.partitoSuonato = true;
            if (e >= 1.0D && !b.presaSuonata) {
                b.presaSuonata = true;
                if (ora - corredo.suonoPresa >= PAUSA_SUONO) {
                    corredo.suonoPresa = ora;
                    suona(livello, b.appiglio, SoundEvents.SLIME_BLOCK_PLACE, 0.5F, 0.95F);
                }
            }
            double arrivato = 1.0D - Math.pow(1.0D - e, 3.0D);
            Vec3 fine = root.add(b.appiglio.subtract(root).scale(arrivato));
            if (fine.distanceToSqr(root) < 0.01D) {
                continue;
            }
            Vec3[] punti = corda(livello, b, root, fine, ora);
            disegnaTubo(consumatore, poseStack, punti, fine.subtract(root));
            livingArmorLungo(punti, posto, poseStack, buffer, gameTime);
        }
    }

    /** Ease in-out: parte e arriva piano. */
    private static double liscio(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    /** Punti in fila fra due estremi, per i bracci senza corda. */
    private static Vec3[] dritto(Vec3 da, Vec3 a, int quanti) {
        Vec3[] punti = new Vec3[quanti];
        for (int i = 0; i < quanti; i++) {
            punti[i] = da.add(a.subtract(da).scale(i / (double) (quanti - 1)));
        }
        return punti;
    }

    /**
     * La corda del braccio: sedici nodi con peso e inerzia, fissati alla spalla e all'appiglio.
     *
     * <p>Prima il braccio era una retta ricalcolata a ogni fotogramma: dove il giocatore andava,
     * il braccio era gia' li'. Adesso i nodi hanno memoria del passo prima, quindi quando la
     * spalla si sposta il centro del braccio resta indietro e ondeggia prima di assestarsi.
     * Integrazione di Verlet a passi fissi, poi i nodi si rimettono alla distanza giusta, poi
     * chi e' finito dentro un blocco ne viene spinto fuori.</p>
     */
    private static Vec3[] corda(Level livello, Braccio b, Vec3 root, Vec3 fine, double ora) {
        if (b.nodi == null) {
            b.nodi = dritto(root, fine, NODI);
            b.nodiPrima = b.nodi.clone();
            b.ultimoPasso = ora;
        }
        double trascorso = ora - b.ultimoPasso;
        b.ultimoPasso = ora;
        // una pausa lunga o il tempo che torna indietro non devono far esplodere la corda
        int passi = trascorso <= 0.0D ? 0 : (int) Math.min(8, Math.ceil(trascorso / PASSO));
        double peso = GRAVITA * PASSO * PASSO;
        for (int passo = 0; passo < passi; passo++) {
            b.nodi[0] = root;
            b.nodi[NODI - 1] = fine;
            for (int i = 1; i < NODI - 1; i++) {
                Vec3 velocita = b.nodi[i].subtract(b.nodiPrima[i]).scale(1.0D - SMORZA);
                b.nodiPrima[i] = b.nodi[i];
                b.nodi[i] = b.nodi[i].add(velocita).add(0.0D, -peso, 0.0D);
            }
            double riposo = fine.distanceTo(root) * LASCO / (NODI - 1);
            for (int giro = 0; giro < RILASSA; giro++) {
                for (int i = 0; i < NODI - 1; i++) {
                    Vec3 tratto = b.nodi[i + 1].subtract(b.nodi[i]);
                    double lunghezza = tratto.length();
                    if (lunghezza < 1.0E-6D) {
                        continue;
                    }
                    Vec3 correzione = tratto.scale((lunghezza - riposo) / lunghezza);
                    boolean fisso0 = i == 0;
                    boolean fisso1 = i + 1 == NODI - 1;
                    if (fisso0 && !fisso1) {
                        b.nodi[i + 1] = b.nodi[i + 1].subtract(correzione);
                    } else if (!fisso0 && fisso1) {
                        b.nodi[i] = b.nodi[i].add(correzione);
                    } else if (!fisso0) {
                        b.nodi[i] = b.nodi[i].add(correzione.scale(0.5D));
                        b.nodi[i + 1] = b.nodi[i + 1].subtract(correzione.scale(0.5D));
                    }
                }
            }
            for (int i = 1; i < NODI - 1; i++) {
                b.nodi[i] = fuoriDaiBlocchi(livello, b.nodi[i]);
            }
        }
        b.nodi[0] = root;
        b.nodi[NODI - 1] = fine;
        return b.nodi.clone();
    }

    /**
     * Un nodo della corda finito dentro un blocco esce dalla faccia piu' vicina, con un margine
     * pari al raggio del braccio perche' non ci resti dentro nemmeno la pelle. Senza, il braccio
     * che gira attorno a uno spigolo lo attraversava.
     */
    private static Vec3 fuoriDaiBlocchi(Level livello, Vec3 p) {
        if (livello == null) {
            return p;
        }
        BlockPos pos = BlockPos.containing(p);
        BlockState stato = livello.getBlockState(pos);
        if (stato.isAir()) {
            return p;
        }
        VoxelShape forma = stato.getCollisionShape(livello, pos);
        if (forma.isEmpty()) {
            return p;
        }
        for (AABB scatola : forma.toAabbs()) {
            AABB s = scatola.move(pos);
            if (!s.contains(p)) {
                continue;
            }
            double[] verso = { p.x - s.minX, s.maxX - p.x, p.y - s.minY, s.maxY - p.y, p.z - s.minZ, s.maxZ - p.z };
            int migliore = 0;
            for (int i = 1; i < verso.length; i++) {
                if (verso[i] < verso[migliore]) {
                    migliore = i;
                }
            }
            double m = RAGGIO_BRACCIO;
            switch (migliore) {
                case 0: return new Vec3(s.minX - m, p.y, p.z);
                case 1: return new Vec3(s.maxX + m, p.y, p.z);
                case 2: return new Vec3(p.x, s.minY - m, p.z);
                case 3: return new Vec3(p.x, s.maxY + m, p.z);
                case 4: return new Vec3(p.x, p.y, s.minZ - m);
                default: return new Vec3(p.x, p.y, s.maxZ + m);
            }
        }
        return p;
    }

    /** Un suono sul client di chi guarda: ogni client riceve gli appigli e suona da se'. */
    private static void suona(Level livello, Vec3 dove, SoundEvent suono, float volume, float tono) {
        if (livello == null) {
            return;
        }
        float variazione = 0.85F + livello.random.nextFloat() * 0.3F;
        livello.playLocalSound(dove.x, dove.y, dove.z, suono, SoundSource.PLAYERS, volume, tono * variazione, false);
    }

    /**
     * Un braccio di Fend Off: parte dalla schiena piegato secondo la mossa, arriva dritto sul
     * bersaglio, resta e rientra, coi tempi che manda il server.
     *
     * <p>La punta segue il bersaglio mentre si muove: il colpo sul server scende proprio quando
     * il braccio arriva, e se la punta restasse dov'era il bersaglio alla partenza si vedrebbe
     * colpire l'aria. Per la presa vale ancora di piu': il bersaglio viene sollevato e sbattuto,
     * e il braccio lo accompagna.</p>
     */
    private static void disegnaFrustata(Player player, Frustata frustata, double eta, float partialTick,
                                        PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        Vec3 playerPos = player.getPosition(partialTick);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 horizontalLook = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        double altezza = player.getBbHeight();
        double scala = altezza / 1.8D;
        // dalla schiena, a coppie ai due lati, su tre altezze del torso
        double[] lati = { -0.2D, 0.2D, -0.12D, 0.12D, -0.04D, 0.04D };
        double[] quote = { 0.60D, 0.60D, 0.68D, 0.68D, 0.76D, 0.76D };
        int posto = frustata.posto;
        Vec3 root = playerPos.add(right.scale(lati[posto] * scala))
                .add(horizontalLook.scale(-0.2D * scala))
                .add(0.0D, quote[posto] * altezza, 0.0D);

        Entity entita = player.level().getEntity(frustata.bersaglio);
        if (entita instanceof LivingEntity vivo && vivo.isAlive()) {
            frustata.punto = vivo.getPosition(partialTick).add(0.0D, vivo.getBbHeight() * 0.55D, 0.0D);
        } else if (frustata.rientraDa < 0.0D && eta > frustata.arriva) {
            // il bersaglio non c'e' piu' (morto, o fuori tracciamento): si rientra da qui
            frustata.rientraDa = eta;
        }
        Vec3 bersaglio = frustata.punto;

        double arrivato;
        if (frustata.rientraDa >= 0.0D) {
            arrivato = 1.0D - liscio(Math.min(1.0D, (eta - frustata.rientraDa) / frustata.rientra));
        } else if (eta < frustata.arriva) {
            arrivato = 1.0D - Math.pow(1.0D - eta / frustata.arriva, 3.0D);
        } else if (eta < frustata.arriva + frustata.resta) {
            arrivato = 1.0D;
        } else {
            arrivato = 1.0D - liscio(Math.min(1.0D, (eta - frustata.arriva - frustata.resta) / frustata.rientra));
        }
        Vec3 fine = root.add(bersaglio.subtract(root).scale(arrivato));
        Vec3 tratto = fine.subtract(root);
        if (tratto.lengthSqr() < 0.01D) {
            return;
        }
        // piegata di lato mentre parte, dritta quando colpisce: il lato e' quello della spalla
        double verso = lati[posto] < 0.0D ? -1.0D : 1.0D;
        Vec3 fianco = safeNormalize(tratto.cross(new Vec3(0.0D, 1.0D, 0.0D)), right).scale(verso);
        // la presa resta un filo piegata anche mentre tiene, come un braccio che regge un peso
        double piega = frustata.mossa == MOSSA_PRESA ? Math.max(1.0D - arrivato, 0.25D) : 1.0D - arrivato;
        Vec3 controllo = root.add(tratto.scale(0.5D))
                .add(fianco.add(0.0D, ALZA_MOSSA[frustata.mossa], 0.0D)
                        .scale(tratto.length() * CURVA_MOSSA[frustata.mossa] * piega));
        Vec3[] punti = new Vec3[ARM_SEGMENTS + 1];
        for (int i = 0; i <= ARM_SEGMENTS; i++) {
            double t = i / (double) ARM_SEGMENTS;
            double u = 1.0D - t;
            punti[i] = root.scale(u * u).add(controllo.scale(2.0D * u * t)).add(fine.scale(t * t));
        }
        disegnaTubo(buffer.getBuffer(RenderType.entityCutoutNoCull(armTexture)), poseStack, punti, tratto);
        livingArmorLungo(punti, 12 + posto, poseStack, buffer, gameTime);
    }

    private static void renderPlayerGrabTentacle(Player player, Vec3 target, float partialTick, PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        Vec3 playerPos = player.getPosition(partialTick);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 horizontalLook = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vec3 back = horizontalLook.scale(-0.36D);
        Vec3 start = playerPos.add(back).add(0.0D, 1.45D, 0.0D).add(horizontalLook.scale(0.16D));
        renderArmPath(start, target, 7, poseStack, buffer, gameTime);
    }
    private static void renderPlayerCombatTentacles(Player player, List<Vec3> targets, float partialTick, PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        Vec3 playerPos = player.getPosition(partialTick);
        float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;
        double yawRadians = Math.toRadians(bodyYaw);
        Vec3 horizontalLook = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        Vec3 back = horizontalLook.scale(-0.42D);
        double[] sideOffsets = new double[] { -0.2D, 0.2D, -0.12D, 0.12D, -0.04D, 0.04D };
        double[] heightOffsets = new double[] { 1.18D, 1.18D, 1.48D, 1.48D, 1.75D, 1.75D };

        for (int i = 0; i < targets.size() && i < VISIBLE_ARMS; i++) {
            Vec3 start = playerPos.add(right.scale(sideOffsets[i])).add(back).add(0.0D, heightOffsets[i], 0.0D);
            renderArmPath(start, targets.get(i), 12 + i, poseStack, buffer, gameTime);
        }
    }

    private static void renderArmPath(Vec3 start, Vec3 end, int armIndex, PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        Vec3[] punti = puntiDellaCurva(start, end, armIndex, gameTime);
        if (punti == null) {
            return;
        }
        disegnaTubo(buffer.getBuffer(RenderType.entityCutoutNoCull(armTexture)), poseStack, punti, end.subtract(start));
        livingArmorLungo(punti, armIndex, poseStack, buffer, gameTime);
    }

    /** Quanti filamenti spuntano lungo ogni braccio della traversata. */
    private static final int FILAMENTI_PER_BRACCIO = 7;
    /** Corti: devono leggersi come simbionte vivo sul braccio, non come altri tentacoli. */
    private static final double FILAMENTO_LUNGHEZZA = 0.26D;

    /**
     * La living armor sui bracci, di traversata, presa e combattimento.
     *
     * <p>I punti non sono stimati: sono gli stessi su cui e' costruito il tubo — la corda
     * per la traversata, la curva con l'onda per gli altri — quindi ogni radice cade
     * esattamente sul braccio, qualunque cosa faccia l'animazione.</p>
     *
     * <p>Partono appena dentro il raggio del tentacolo ed escono perpendicolari, alternando i
     * quattro versi attorno alla sezione, cosi' spuntano tutt'intorno e non su un lato solo.</p>
     */
    private static void livingArmorLungo(Vec3[] punti, int armIndex, PoseStack poseStack,
                                         MultiBufferSource buffer, long gameTime) {
        Vec3 delta = punti[punti.length - 1].subtract(punti[0]);
        if (delta.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 lato = safeNormalize(delta.cross(new Vec3(0.0D, 1.0D, 0.0D)), new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 alto = safeNormalize(delta.cross(lato), new Vec3(0.0D, 1.0D, 0.0D));
        Level livello = Minecraft.getInstance().level;

        for (int k = 0; k < FILAMENTI_PER_BRACCIO; k++) {
            // si resta lontani dalle due estremita': alla radice il braccio esce dal corpo,
            // in punta si assottiglia e un filamento sporgerebbe nel vuoto
            double progress = 0.18D + 0.64D * (k / (double) (FILAMENTI_PER_BRACCIO - 1));
            Vec3 sulBraccio = lungo(punti, progress);

            // quattro versi attorno alla sezione
            int giro = (k + armIndex) % 4;
            Vec3 fuori = giro == 0 ? lato : giro == 1 ? alto : giro == 2 ? lato.reverse() : alto.reverse();

            int seme = armIndex * 31 + k;
            double t = gameTime * 0.09D + seme * 1.7D;
            double respiro = 0.55D + 0.45D * Math.sin(t * 0.6D);
            // la superficie e' quella del tubo, che ha uno spessore suo in ogni punto: il
            // filamento sporge oltre di essa di quanto sporgeva prima, e ha lo stesso spessore
            double superficie = raggioBraccio(progress);
            Vec3 radice = sulBraccio.add(fuori.scale(superficie * 0.6D));
            Vec3 punta = sulBraccio.add(fuori.scale(superficie + FILAMENTO_LUNGHEZZA * respiro));

            disegnaFilamento(radice, punta, seme, armTexture,
                    TENTACLE_RADIUS * 0.22F, TENTACLE_RADIUS * 0.05F, 8, 0.05D,
                    poseStack, buffer, gameTime, luceIn(livello, sulBraccio, LightTexture.FULL_BRIGHT));
        }
    }

    /** Il punto a una frazione della lunghezza del braccio, fra i due nodi che la contengono. */
    private static Vec3 lungo(Vec3[] punti, double frazione) {
        double f = Math.max(0.0D, Math.min(1.0D, frazione)) * (punti.length - 1);
        int i = Math.min(punti.length - 2, (int) Math.floor(f));
        return punti[i].add(punti[i + 1].subtract(punti[i]).scale(f - i));
    }

    /**
     * Lo stesso tentacolo dei bracci di tentacles_traversal, ma con la texture passata da fuori.
     *
     * <p>Serve a chi non e' un giocatore — per esempio il cadavere nel cratere di All-Black —
     * e vuole disegnare un tentacolo senza passare per lo stato del simbionte di qualcuno.
     * Le coordinate sono nello spazio in cui si trova la {@code poseStack} al momento della
     * chiamata, quindi da una block entity si passano coordinate locali al blocco.</p>
     */
    public static void disegnaTentacolo(Vec3 start, Vec3 end, int armIndex, ResourceLocation texture,
                                        PoseStack poseStack, MultiBufferSource buffer, long gameTime) {
        Vec3[] punti = puntiDellaCurva(start, end, armIndex, gameTime);
        if (punti != null) {
            disegnaTubo(buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), poseStack, punti, end.subtract(start));
        }
    }

    /**
     * La curva dei tentacoli di presa e di combattimento: la retta con la sua onda leggera,
     * campionata a diciotto passi. Null se i due estremi coincidono.
     */
    private static Vec3[] puntiDellaCurva(Vec3 start, Vec3 end, int armIndex, long gameTime) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 0.01D) {
            return null;
        }
        Vec3 sideWave = safeNormalize(delta.cross(new Vec3(0.0D, 1.0D, 0.0D)), new Vec3(1.0D, 0.0D, 0.0D)).scale(0.06D);
        Vec3[] punti = new Vec3[ARM_SEGMENTS + 1];
        for (int segment = 0; segment <= ARM_SEGMENTS; segment++) {
            double progress = segment / (double) ARM_SEGMENTS;
            double pulse = Math.sin((gameTime * 0.35D) + progress * Math.PI * 2.0D + armIndex * 0.8D);
            double wave = pulse * 0.28D * (1.0D - Math.abs(progress - 0.5D));
            punti[segment] = start.add(delta.scale(progress)).add(sideWave.scale(wave));
        }
        return punti;
    }

    /**
     * Il braccio: un tubo tondo attorno ai punti della curva, con gli anelli cuciti fra loro.
     *
     * <p>Prima ogni pezzo era una scatola a se', orientata sul suo tratto: dove la curva
     * piegava le scatole si aprivano a ventaglio e fra l'una e l'altra restava uno scalino.
     * Qui ogni punto ha un solo anello, condiviso dal pezzo prima e da quello dopo, quindi il
     * braccio e' continuo. L'orientamento dell'anello si trasporta da un punto al successivo
     * invece di ricavarlo ogni volta dall'alto del mondo, cosi' la texture non si attorciglia
     * quando il braccio passa per la verticale.</p>
     *
     * <p>La texture si avvolge: un giro attorno alla sezione e un ripetersi ogni
     * {@link #TEXTURE_OGNI} blocchi lungo il braccio. Prima ricominciava intera su ogni faccia
     * di ogni pezzo, quindi si stirava sui bracci lunghi e si schiacciava su quelli corti.</p>
     *
     * <p>I punti sono in coordinate del mondo, ed e' li' che si legge la luce.</p>
     */
    private static void disegnaTubo(VertexConsumer consumatore, PoseStack pila, Vec3[] punti, Vec3 delta) {
        int n = punti.length;
        Vec3[] tangenti = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 tratto = punti[Math.min(i + 1, n - 1)].subtract(punti[Math.max(i - 1, 0)]);
            tangenti[i] = safeNormalize(tratto, safeNormalize(delta, new Vec3(0.0D, 1.0D, 0.0D)));
        }
        Vec3 alto = Math.abs(tangenti[0].y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 normale = safeNormalize(tangenti[0].cross(alto), new Vec3(1.0D, 0.0D, 0.0D));

        Level livello = Minecraft.getInstance().level;
        Matrix4f matrice = pila.last().pose();
        Matrix3f matriceNormali = pila.last().normal();

        Vec3[] anelloPrima = null;
        Vec3[] versiPrima = null;
        float vPrima = 0.0F;
        int lucePrima = luceIn(livello, punti[0], LightTexture.FULL_BRIGHT);
        double percorso = 0.0D;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                // trasporto: si toglie alla normale di prima la parte lungo la nuova tangente
                Vec3 t = tangenti[i];
                Vec3 proiettata = normale.subtract(t.scale(normale.dot(t)));
                normale = proiettata.lengthSqr() < 1.0E-6D
                        ? safeNormalize(t.cross(alto), new Vec3(1.0D, 0.0D, 0.0D))
                        : proiettata.normalize();
                percorso += punti[i].distanceTo(punti[i - 1]);
            }
            Vec3 binormale = tangenti[i].cross(normale);
            float raggio = raggioBraccio(i / (double) (n - 1));
            Vec3[] anello = new Vec3[LATI_BRACCIO + 1];
            Vec3[] versi = new Vec3[LATI_BRACCIO + 1];
            for (int k = 0; k <= LATI_BRACCIO; k++) {
                double angolo = Math.PI * 2.0D * k / LATI_BRACCIO;
                Vec3 verso = normale.scale(Math.cos(angolo)).add(binormale.scale(Math.sin(angolo)));
                versi[k] = verso;
                anello[k] = punti[i].add(verso.scale(raggio));
            }
            float v = (float) (percorso / TEXTURE_OGNI);
            int luce = luceIn(livello, punti[i], lucePrima);
            if (anelloPrima != null) {
                for (int k = 0; k < LATI_BRACCIO; k++) {
                    float u0 = k / (float) LATI_BRACCIO;
                    float u1 = (k + 1) / (float) LATI_BRACCIO;
                    vertice(consumatore, matrice, matriceNormali, anelloPrima[k], versiPrima[k], u0, vPrima, lucePrima);
                    vertice(consumatore, matrice, matriceNormali, anelloPrima[k + 1], versiPrima[k + 1], u1, vPrima, lucePrima);
                    vertice(consumatore, matrice, matriceNormali, anello[k + 1], versi[k + 1], u1, v, luce);
                    vertice(consumatore, matrice, matriceNormali, anello[k], versi[k], u0, v, luce);
                }
            }
            anelloPrima = anello;
            versiPrima = versi;
            vPrima = v;
            lucePrima = luce;
        }
    }

    /**
     * Lo spessore lungo il braccio, da 0 alla radice a 1 in punta: un colletto dove esce dal
     * corpo, una pancia larga a meta', e l'affusolarsi degli ultimi tre pezzi come prima.
     */
    private static float raggioBraccio(double t) {
        double pancia = PANCIA * Math.pow(Math.sin(Math.PI * Math.min(t / INIZIO_PUNTA, 1.0D)), 2);
        double colletto = COLLETTO * Math.pow(1.0D - t, 6);
        double corpo = RAGGIO_BRACCIO * (1.0D + pancia + colletto);
        if (t <= INIZIO_PUNTA) {
            return (float) corpo;
        }
        double q = (t - INIZIO_PUNTA) / (1.0D - INIZIO_PUNTA);
        double liscio = q * q * (3.0D - 2.0D * q);
        return (float) (corpo + (RAGGIO_PUNTA - corpo) * liscio);
    }

    /**
     * La luce del mondo in un punto del braccio.
     *
     * <p>Prima era sempre piena: di notte o in una grotta il braccio restava chiaro come a
     * mezzogiorno. La punta sta sulla faccia del blocco a cui si aggrappa, e se cade dentro il
     * blocco la luce li' e' zero: in quel caso vale quella del punto precedente.</p>
     */
    private static int luceIn(Level livello, Vec3 punto, int precedente) {
        if (livello == null) {
            return LightTexture.FULL_BRIGHT;
        }
        BlockPos pos = BlockPos.containing(punto);
        if (livello.getBlockState(pos).isSolidRender(livello, pos)) {
            return precedente;
        }
        return LevelRenderer.getLightColor(livello, pos);
    }

    private static void vertice(VertexConsumer consumatore, Matrix4f matrice, Matrix3f matriceNormali,
                                Vec3 posizione, Vec3 verso, float u, float v, int luce) {
        consumatore.vertex(matrice, (float) posizione.x, (float) posizione.y, (float) posizione.z)
                .color(tuboR, tuboG, tuboB, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(luce)
                .normal(matriceNormali, (float) verso.x, (float) verso.y, (float) verso.z)
                .endVertex();
    }

    /**
     * Un tentacolo sottile e affusolato, con il raggio scelto da chi chiama.
     *
     * <p>Separato da {@link #disegnaTentacolo} apposta: quello serve i bracci della tentacles_traversal
     * e il suo spessore e' tarato sul giocatore, mentre qui servono filamenti molto piu' fini e
     * molto piu' numerosi. Il raggio cala lungo tutta la corsa invece che solo in punta, con un
     * rigonfiamento a meta', che e' quello che li fa sembrare organici e non tubi.</p>
     */
    /**
     * Come sopra, ma con la tinta detta esplicitamente.
     *
     * <p>Serve perche' {@code addQuad} colora i vertici con i campi statici
     * {@code armR/armG/armB}, che valgono per l'ultimo giocatore disegnato. Chi disegna
     * filamenti che devono restare neri — la living armor di All-Black sulla tuta, in prima
     * persona e sul cadavere — senza questo si ritroverebbe il rosso di Carnage addosso non
     * appena un altro giocatore in quella forma viene disegnato prima. In singleplayer non si
     * nota mai; in multiplayer succede subito.</p>
     */
    public static void disegnaFilamento(Vec3 inizio, Vec3 fine, int indice, ResourceLocation texture,
                                        float raggioBase, float raggioPunta, int segmenti,
                                        double ampiezza, PoseStack pila, MultiBufferSource buffer,
                                        long tempo, int r, int g, int b) {
        int pr = armR, pg = armG, pb = armB;
        armR = r; armG = g; armB = b;
        try {
            disegnaFilamento(inizio, fine, indice, texture, raggioBase, raggioPunta, segmenti,
                    ampiezza, pila, buffer, tempo);
        } finally {
            armR = pr; armG = pg; armB = pb;
        }
    }

    /** Il nero del simbionte: la tinta con cui vanno disegnati i filamenti di All-Black. */
    public static final int NERO_SIMBIONTE = ARM_COLOR;

    public static void disegnaFilamento(Vec3 inizio, Vec3 fine, int indice, ResourceLocation texture,
                                        float raggioBase, float raggioPunta, int segmenti,
                                        double ampiezza, PoseStack pila, MultiBufferSource buffer,
                                        long tempo) {
        disegnaFilamento(inizio, fine, indice, texture, raggioBase, raggioPunta, segmenti,
                ampiezza, pila, buffer, tempo, LightTexture.FULL_BRIGHT);
    }

    /**
     * Con la luce detta da chi chiama. La usano solo i filamenti sui bracci, che devono
     * scurirsi insieme al braccio: gli altri (tuta, prima persona, mob) restano com'erano.
     */
    private static void disegnaFilamento(Vec3 inizio, Vec3 fine, int indice, ResourceLocation texture,
                                         float raggioBase, float raggioPunta, int segmenti,
                                         double ampiezza, PoseStack pila, MultiBufferSource buffer,
                                         long tempo, int luce) {
        VertexConsumer consumatore = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        Vec3 delta = fine.subtract(inizio);
        if (delta.lengthSqr() < 1.0E-4D) {
            return;
        }
        Vec3 lato = safeNormalize(delta.cross(new Vec3(0.0D, 1.0D, 0.0D)), new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 alto = safeNormalize(lato.cross(delta), new Vec3(0.0D, 1.0D, 0.0D));

        for (int passo = 1; passo <= segmenti; passo++) {
            double q = passo / (double) segmenti;
            double qPrec = (passo - 1) / (double) segmenti;
            Vec3 corrente = puntoFilamento(inizio, delta, lato, alto, q, indice, tempo, ampiezza);
            Vec3 precedente = puntoFilamento(inizio, delta, lato, alto, qPrec, indice, tempo, ampiezza);
            addTaperedSegment(consumatore, pila, precedente, corrente,
                    raggioFilamento(qPrec, raggioBase, raggioPunta),
                    raggioFilamento(q, raggioBase, raggioPunta),
                    luce);
        }
    }

    /** Il raggio lungo la corsa: cala dalla radice alla punta con un rigonfiamento a meta'. */
    private static float raggioFilamento(double q, float base, float punta) {
        float lineare = (float) (base + (punta - base) * Math.pow(q, 0.75D));
        float pancia = (float) (Math.sin(q * Math.PI) * (base - punta) * 0.35D);
        return Math.max(punta * 0.5F, lineare + pancia);
    }

    /** Un punto lungo il filamento, con due ondulazioni sfasate che lo fanno serpeggiare. */
    private static Vec3 puntoFilamento(Vec3 inizio, Vec3 delta, Vec3 lato, Vec3 alto,
                                       double q, int indice, long tempo, double ampiezza) {
        double t = tempo * 0.18D + indice * 1.7D;
        double smorza = Math.sin(q * Math.PI * 0.85D);      // fermo alla radice, libero in punta
        double onda1 = Math.sin(t + q * Math.PI * 2.4D) * ampiezza * smorza;
        double onda2 = Math.cos(t * 0.73D + q * Math.PI * 1.7D) * ampiezza * 0.7D * smorza;
        return inizio.add(delta.scale(q)).add(lato.scale(onda1)).add(alto.scale(onda2));
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() < 1.0E-4D) {
            return fallback;
        }
        return vector.normalize();
    }

    private static void addTaperedSegment(VertexConsumer consumer, PoseStack poseStack, Vec3 start, Vec3 end, float startRadius, float endRadius, int light) {
        Vec3 forward = end.subtract(start);
        if (forward.lengthSqr() < 1.0E-5D) {
            return;
        }
        forward = forward.normalize();
        Vec3 upHint = Math.abs(forward.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 rightDirection = forward.cross(upHint).normalize();
        Vec3 upDirection = rightDirection.cross(forward).normalize();
        Vec3 startRight = rightDirection.scale(startRadius);
        Vec3 startUp = upDirection.scale(startRadius);
        Vec3 endRight = rightDirection.scale(endRadius);
        Vec3 endUp = upDirection.scale(endRadius);

        Vec3 a = start.add(startRight).add(startUp);
        Vec3 b = start.add(startRight).subtract(startUp);
        Vec3 c = start.subtract(startRight).subtract(startUp);
        Vec3 d = start.subtract(startRight).add(startUp);
        Vec3 e = end.add(endRight).add(endUp);
        Vec3 f = end.add(endRight).subtract(endUp);
        Vec3 g = end.subtract(endRight).subtract(endUp);
        Vec3 h = end.subtract(endRight).add(endUp);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        addQuad(consumer, matrix, normal, a, e, f, b, rightDirection, light);
        addQuad(consumer, matrix, normal, d, c, g, h, rightDirection.scale(-1.0D), light);
        addQuad(consumer, matrix, normal, a, d, h, e, upDirection, light);
        addQuad(consumer, matrix, normal, b, f, g, c, upDirection.scale(-1.0D), light);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normalVector, int light) {
        float nx = (float) normalVector.x;
        float ny = (float) normalVector.y;
        float nz = (float) normalVector.z;
        consumer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(armR, armG, armB, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(armR, armG, armB, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(armR, armG, armB, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(armR, armG, armB, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
    }

    private record AnchorState(List<Vec3> anchors, long gameTime) {
    }

    private record GrabTargetState(Vec3 target, long gameTime) {
    }

    private record CombatTargetState(List<Vec3> targets, long gameTime) {
    }
}
