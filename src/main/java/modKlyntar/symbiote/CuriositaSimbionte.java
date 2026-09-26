package modKlyntar.symbiote;

import modKlyntar.MyMod;
import modKlyntar.entity.custom.SymbioteEntity;
import modKlyntar.symbiote.VoceSimbionte.Tono;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La curiosita': il simbionte commenta quello che ha intorno, e intanto insegna.
 *
 * <p>Ogni commento spiega qualcosa della mod senza dirlo in un manuale: la campana e il fuoco
 * lo spaventano (le sue debolezze), il Warden lo terrorizza (il sonic boom), un ragno lo
 * incuriosisce (divorato con Regeneration porta a Venomspidey), la notte e il buio gli
 * piacciono. La prima volta che incontra una cosa lo dice appena puo', da tutorial; poi la
 * ripete di rado, cosi' non diventa una nenia. Le cose gia' viste si ricordano nei dati del
 * giocatore, e sopravvivono alla chiusura del gioco.</p>
 *
 * <p>Finche' non si fida (Bond 1) guarda anche chi incontra con l'occhio di chi cerca casa: un
 * altro giocatore meglio corazzato, un golem di ferro, un ravager, e lo dice.</p>
 */
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID)
public final class CuriositaSimbionte {
    private static final int OGNI = 40;
    private static final long RICARICA_PRIMA_VOLTA = 20L * 60L * 2L;
    private static final long RICARICA_DOPO = 20L * 60L * 10L;
    private static final String VISTI = "Klyntar.SymbioteSeen";

    private static final double RAGGIO_WARDEN = 24.0D;
    private static final double RAGGIO_CREATURE = 12.0D;
    private static final double RAGGIO_VILLAGGIO = 16.0D;
    private static final int RAGGIO_CAMPANA = 8;
    private static final int RAGGIO_FUOCO = 5;
    /** Quanta armatura in piu' serve a un altro giocatore per sembrare al simbionte un ospite migliore. */
    private static final int ARMATURA_IN_PIU = 4;
    /** Sotto questa quota e al buio completo il simbionte si sente a casa. */
    private static final int QUOTA_PROFONDA = 40;

    private static final Map<UUID, Boolean> ERA_NOTTE = new ConcurrentHashMap<>();

    private CuriositaSimbionte() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer ospite)
                || ospite.tickCount % OGNI != 0 || !SymbioteState.haSimbionte(ospite)
                || !(ospite.level() instanceof ServerLevel livello)) {
            return;
        }
        // prima i pericoli: se c'e' un Warden, il resto puo' aspettare
        if (vicino(livello, ospite, Warden.class, RAGGIO_WARDEN)) {
            nota(ospite, "cur_warden", Tono.AVVISO);
            return;
        }
        if (bloccoVicino(livello, ospite, RAGGIO_CAMPANA, stato -> stato.is(Blocks.BELL))) {
            nota(ospite, "cur_campana", Tono.AVVISO);
            return;
        }
        if (bloccoVicino(livello, ospite, RAGGIO_FUOCO, CuriositaSimbionte::brucia)) {
            nota(ospite, "cur_fuoco", Tono.AVVISO);
            return;
        }
        // poi le cose interessanti
        boolean diffidente = VoceSimbionte.fiducia(ospite) == VoceSimbionte.DIFFIDENTE;
        if ("venom".equals(SymbioteState.forma(ospite)) && vicino(livello, ospite, Spider.class, RAGGIO_CREATURE)) {
            nota(ospite, "cur_ragno", Tono.NEUTRO);
        } else if (vicino(livello, ospite, SymbioteEntity.class, RAGGIO_CREATURE)) {
            nota(ospite, "cur_simbionte", Tono.NEUTRO);
        } else if (altroOspiteVicino(livello, ospite)) {
            nota(ospite, "cur_ospite", Tono.NEUTRO);
        } else if (diffidente && ospitePiuForteVicino(livello, ospite)) {
            // con Bond 1 il simbionte misura chi incontra: magari quello li' e' un corpo migliore
            nota(ospite, "cur_ospite_forte", Tono.AGGRESSIVO);
        } else if (diffidente && (vicino(livello, ospite, IronGolem.class, RAGGIO_CREATURE)
                || vicino(livello, ospite, Ravager.class, RAGGIO_CREATURE))) {
            nota(ospite, "cur_bestia_forte", Tono.AGGRESSIVO);
        } else if (vicino(livello, ospite, Villager.class, RAGGIO_VILLAGGIO)) {
            nota(ospite, "cur_villaggio", Tono.NEUTRO);
        }
        // l'ambiente: la notte appena scesa, il buio profondo
        boolean notte = livello.isNight() && livello.dimensionType().hasSkyLight();
        Boolean eraNotte = ERA_NOTTE.put(ospite.getUUID(), notte);
        if (notte && Boolean.FALSE.equals(eraNotte)) {
            nota(ospite, "cur_notte", Tono.NEUTRO);
        } else if (ospite.getY() < QUOTA_PROFONDA
                && livello.getMaxLocalRawBrightness(ospite.blockPosition()) == 0) {
            nota(ospite, "cur_buio", Tono.NEUTRO);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ERA_NOTTE.remove(event.getEntity().getUUID());
    }

    /** Commenta: presto se e' la prima volta, di rado le altre. */
    private static void nota(ServerPlayer ospite, String situazione, Tono tono) {
        CompoundTag visti = ospite.getPersistentData().getCompound(VISTI);
        boolean giaVisto = visti.getBoolean(situazione);
        if (VoceSimbionte.di(ospite, situazione, tono, false, giaVisto ? RICARICA_DOPO : RICARICA_PRIMA_VOLTA)
                && !giaVisto) {
            visti.putBoolean(situazione, true);
            ospite.getPersistentData().put(VISTI, visti);
        }
    }

    private static boolean vicino(ServerLevel livello, ServerPlayer ospite, Class<? extends Entity> tipo, double raggio) {
        AABB zona = ospite.getBoundingBox().inflate(raggio);
        return !livello.getEntitiesOfClass(tipo, zona, e -> e.isAlive() && e != ospite).isEmpty();
    }

    private static boolean altroOspiteVicino(ServerLevel livello, ServerPlayer ospite) {
        AABB zona = ospite.getBoundingBox().inflate(RAGGIO_CREATURE);
        return !livello.getEntitiesOfClass(Player.class, zona,
                p -> p != ospite && p.isAlive() && !p.isSpectator() && SymbioteState.haSimbionte(p)).isEmpty();
    }

    /** Un altro giocatore, senza simbionte, piu' corazzato o piu' robusto dell'ospite. */
    private static boolean ospitePiuForteVicino(ServerLevel livello, ServerPlayer ospite) {
        AABB zona = ospite.getBoundingBox().inflate(RAGGIO_CREATURE);
        return !livello.getEntitiesOfClass(Player.class, zona,
                p -> p != ospite && p.isAlive() && !p.isSpectator() && !SymbioteState.haSimbionte(p)
                        && (p.getArmorValue() >= ospite.getArmorValue() + ARMATURA_IN_PIU
                        || p.getMaxHealth() > ospite.getMaxHealth())).isEmpty();
    }

    private static boolean brucia(BlockState stato) {
        return stato.is(BlockTags.FIRE) || stato.is(Blocks.LAVA)
                || (stato.getBlock() instanceof CampfireBlock && stato.getValue(CampfireBlock.LIT));
    }

    private static boolean bloccoVicino(ServerLevel livello, ServerPlayer ospite, int raggio,
                                        java.util.function.Predicate<BlockState> cerca) {
        BlockPos centro = ospite.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-raggio, -raggio, -raggio),
                centro.offset(raggio, raggio, raggio))) {
            if (cerca.test(livello.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }
}
