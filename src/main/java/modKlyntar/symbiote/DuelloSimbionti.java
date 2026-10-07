package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.network.ModNetwork;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il duello coi tentacoli prima dell'ingresso, dalla parte dell'ospite.
 *
 * <p>Nella mod Symbiote un simbionte rivale non entra nel corpo: lo combatte da fuori, a
 * frustate, e il simbionte dell'ospite risponde da solo a ogni colpo (il "rival reflex"). Qui
 * il duello viene prima dell'ingresso: Riot frusta da qualche blocco, a volte afferra e
 * scaraventa via, e quando la preda e' provata la aggancia da lontano e se la tira addosso (vedi
 * {@code BraccaOspiteGoal} in {@link SymbioteEntity}). Il simbionte dell'ospite risponde a ogni
 * frustata con la sua, piu' forte quanto piu' alto e' il bond: un Venom che si fida del suo ospite
 * lo difende meglio. Sotto il 40% il rivale lascia perdere e scappa, senza entrare.</p>
 *
 * <p>Il giocatore puo' anche colpirlo lui, coi pugni e con le abilita': finche' porta un
 * simbionte di un'altra famiglia, i colpi lo feriscono invece di farlo entrare. Per lasciarlo
 * entrare basta andargli addosso. E un rivale logorato entra piu' debole nel conflitto.</p>
 *
 * <p>Le frustate sono solo un disegno sui client ({@code FrustateSimbionte}): il danno lo fa il
 * server, il filamento parte e rientra per chiunque veda chi frusta.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class DuelloSimbionti {
    /** Un secondo e un quarto fra due risposte: un po' piu' svelto della frusta del rivale. */
    private static final long OGNI_RISPOSTA = 25L;
    private static final float DANNO_BASE = 2.0F;
    private static final float DANNO_PER_BOND = 1.5F;
    private static final long RICARICA_BATTUTA = 20L * 10L;

    private static final Map<UUID, Long> ULTIMA_RISPOSTA = new ConcurrentHashMap<>();

    /** Fin dove arrivano i tentacoli di un Riot fuso con un giocatore. */
    private static final double PRESA_RIOT = 8.0D;
    /** Quanto tira per tick, al massimo: circa tre secondi da otto blocchi. */
    private static final double TIRO_MASSIMO = 0.3D;
    /** Sotto questa distanza la preda tocca l'ospite ed entra. */
    private static final double CONTATTO = 1.6D;
    private static final int OGNI_RICERCA = 10;
    /** Ogni quanto ripartono i filamenti mentre tiene: cosi' sembra che non molli mai la presa. */
    private static final int OGNI_FILAMENTO = 8;
    /** Chi sta tenendo ogni Riot ospitato: l'id del mob. */
    private static final Map<UUID, Integer> PRESE = new ConcurrentHashMap<>();

    private DuelloSimbionti() {
    }

    /** Mostra a tutti quelli che la vedono una frustata da un'entita' all'altra. */
    public static void mostraFrustata(Entity da, Entity a, String forma) {
        ModNetwork.mandaFrustata(da, a, forma);
    }

    /**
     * Il rivale ha colpito l'ospite: il suo simbionte risponde con una frustata. Non quando e'
     * indebolito dal fuoco o dal suono: in quella finestra i suoi riflessi non lavorano.
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer ospite)
                || !(event.getSource().getEntity() instanceof SymbioteEntity rivale)
                || !rivale.isAlive() || !rivale.rivale(ospite)) {
            return;
        }
        String forma = SymbioteState.forma(ospite);
        if (forma.isEmpty() || SymbioteState.isVulnerabile(ospite)
                || ospite.distanceTo(rivale) > SymbioteEntity.PORTATA_FRUSTA + 1.0D) {
            return;
        }
        long ora = ospite.level().getGameTime();
        Long ultima = ULTIMA_RISPOSTA.get(ospite.getUUID());
        if (ultima != null && ora - ultima < OGNI_RISPOSTA) {
            return;
        }
        ULTIMA_RISPOSTA.put(ospite.getUUID(), ora);
        mostraFrustata(ospite, rivale, forma);
        if (ospite.getRandom().nextBoolean()) {
            mostraFrustata(ospite, rivale, forma);
        }
        ospite.level().playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                modKlyntar.sound.SuoniKlyntar.FEND_OFF_LASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        float danno = DANNO_BASE + DANNO_PER_BOND * ProfiliSimbionti.livelloBond(ospite, forma);
        rivale.hurt(ospite.damageSources().playerAttack(ospite), danno);
        VoceSimbionte.di(ospite, "duello_risposta", Tono.AGGRESSIVO, false, RICARICA_BATTUTA);
    }

    // ------------------------------------------------------------------ Riot fuso con un giocatore

    /**
     * Riot non lascia mai scappare nessuno, nemmeno quando e' fuso con un giocatore: un
     * simbionte libero di un'altra famiglia che gli passa entro otto blocchi, a vista, viene
     * afferrato dai suoi tentacoli e trascinato all'ospite. Mentre e' tenuto non scappa e non si
     * rifugia in un animale; al contatto entra nel corpo, e Riot - che e' dentro - lo affronta nel
     * conflitto per assorbirlo. Se l'ospite l'ha gia' logorato, entra piu' debole.
     *
     * <p>Non mentre il corpo e' gia' conteso, ne' quando Riot e' indebolito dal fuoco o dal suono.</p>
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || !(ospite.level() instanceof ServerLevel livello)) {
            return;
        }
        String forma = SymbioteState.forma(ospite);
        RegistroSimbionti.Simbionte suo = RegistroSimbionti.di(forma);
        if (suo == null || !suo.temperamento().nonLasciaScappare() || !ospite.isAlive() || ospite.isSpectator()
                || ConflittoSimbionti.inCorso(ospite) || SymbioteState.isVulnerabile(ospite)) {
            PRESE.remove(ospite.getUUID());
            return;
        }
        SymbioteEntity preda = null;
        Integer presa = PRESE.get(ospite.getUUID());
        if (presa != null && livello.getEntity(presa) instanceof SymbioteEntity tenuto && tenuto.isAlive()
                && tenuto.rivale(ospite) && tenuto.distanceTo(ospite) <= PRESA_RIOT + 2.0D) {
            preda = tenuto;
        } else if (ospite.tickCount % OGNI_RICERCA == 0) {
            preda = piuVicina(ospite, livello);
        }
        if (preda == null) {
            PRESE.remove(ospite.getUUID());
            return;
        }
        if (presa == null || presa != preda.getId()) {
            // l'aggancio: il suono dei tentacoli che partono, e Riot che lo dice
            livello.playSound(null, ospite.getX(), ospite.getY(), ospite.getZ(),
                    modKlyntar.sound.SuoniKlyntar.ABILITY_GRAB_TENTACLE.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
            VoceSimbionte.di(ospite, "duello_presa", Tono.AGGRESSIVO, true);
        }
        PRESE.put(ospite.getUUID(), preda.getId());
        preda.trattieni();

        Vec3 verso = ospite.position().subtract(preda.position());
        double distanza = verso.length();
        if (distanza <= CONTATTO) {
            PRESE.remove(ospite.getUUID());
            if (ConflittoSimbionti.entra(ospite, preda.forma(), preda.position().add(0.0D, 0.5D, 0.0D),
                    preda.getHealth() / preda.getMaxHealth())) {
                preda.discard();
            }
            return;
        }
        Vec3 tiro = verso.normalize().scale(Math.min(TIRO_MASSIMO, distanza * 0.12D));
        double su = verso.y > 0.6D ? 0.25D : preda.getDeltaMovement().y;
        preda.setDeltaMovement(tiro.x, su, tiro.z);
        preda.hasImpulse = true;
        if (ospite.tickCount % OGNI_FILAMENTO == 0) {
            mostraFrustata(ospite, preda, forma);
        }
    }

    /** Il simbionte libero rivale piu' vicino, entro la presa di Riot e a vista. */
    private static SymbioteEntity piuVicina(ServerPlayer ospite, ServerLevel livello) {
        SymbioteEntity migliore = null;
        double meglio = PRESA_RIOT * PRESA_RIOT;
        for (SymbioteEntity mob : livello.getEntitiesOfClass(SymbioteEntity.class,
                ospite.getBoundingBox().inflate(PRESA_RIOT), e -> e.isAlive() && e.rivale(ospite))) {
            double d = mob.distanceToSqr(ospite);
            if (d < meglio && ospite.hasLineOfSight(mob)) {
                meglio = d;
                migliore = mob;
            }
        }
        return migliore;
    }

    /** Il rivale ha lasciato il duello ed e' scappato: il simbionte dell'ospite se ne vanta. */
    public static void respinto(ServerPlayer ospite, SymbioteEntity rivale) {
        VoceSimbionte.di(ospite, "duello_vinto", Tono.AGGRESSIVO, true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ULTIMA_RISPOSTA.remove(event.getEntity().getUUID());
        PRESE.remove(event.getEntity().getUUID());
    }
}
