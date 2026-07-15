package modKlyntar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import modKlyntar.block.PromethiumXBlock;
import modKlyntar.block.PromethiumXSmokeHandler;
import modKlyntar.entity.custom.GhastProjectileEntity;

import modKlyntar.entity.custom.SmokeTrailEntity;
import modKlyntar.entity.custom.SymbioteEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, bus = Bus.FORGE)
public class MeteorCommand {

    private static final Random RANDOM = new Random();
    private static final int SMOKE_DURATION_TICKS = 4 * 60 * 20; // Durata in ticks (4 minuti)
    private static final double METEOR_SPACING = 14.0D;
    private static final int IMPACT_SYNC_RADIUS = 10;
    private static final int IMPACT_SYNC_HEIGHT_ABOVE = 5;
    private static final int IMPACT_SYNC_HEIGHT_BELOW = 14;
    private static final Map<BlockPos, Integer> smokingObsidianBlocks = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("spawnMeteor")
                        .requires(source -> source.hasPermission(2))
                        .executes(MeteorCommand::spawnMeteor)
        );
    }

    private static int spawnMeteor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel world = player.serverLevel();
        Vec3 playerPos = player.position();
        
        player.displayClientMessage(Component.literal("Some meteors are crashing on this planet"), false);

        // Posizione di caduta dei proiettili
        double fallDistance = 60.0; // Distanza di caduta
        double angle = Math.toRadians(45); // Angolo di caduta (45 gradi)
        double offsetX = Math.cos(angle) * fallDistance;
        double spawnY = world.getMaxBuildHeight() - 4.0D;
        
        // Calcola la posizione di partenza delle fireball
        Vec3 meteorPos1 = new Vec3(playerPos.x + offsetX, spawnY, playerPos.z);
        Vec3 meteorPos2 = new Vec3(playerPos.x + offsetX + METEOR_SPACING, spawnY - 3.0D, playerPos.z);
        Vec3 meteorPos3 = new Vec3(playerPos.x + offsetX + METEOR_SPACING * 2.0D, spawnY - 6.0D, playerPos.z);

        // Calcola la posizione di impatto a 10 blocchi di fronte al giocatore
        Vec3 impactPos = playerPos.add(offsetX, 0, 10);
        Vec3 impactPos2 = playerPos.add(offsetX + METEOR_SPACING, 0, 10);
        Vec3 impactPos3 = playerPos.add(offsetX + METEOR_SPACING * 2.0D, 0, 10);

        // Spawna i proiettili di Ghast
        spawnGhastProjectile(world, meteorPos1, impactPos);
        spawnGhastProjectile(world, meteorPos2, impactPos2);
        spawnGhastProjectile(world, meteorPos3, impactPos3);

        return 1;
    }


    private static void spawnGhastProjectile(ServerLevel world, Vec3 startPos, Vec3 targetPos) {
        GhastProjectileEntity ghastProjectile = new GhastProjectileEntity(world, startPos.x, startPos.y, startPos.z, targetPos.x, targetPos.y, targetPos.z);
        ghastProjectile.setDeltaMovement(0, -0.15, 0); // Direzione di caduta (velocità negativa verso il basso, più lenta)
        world.addFreshEntity(ghastProjectile);

        // Aggiungi un'entità di scia di fumo per ogni proiettile
        SmokeTrailEntity smokeTrail = new SmokeTrailEntity(world, startPos.x, startPos.y, startPos.z);
        world.addFreshEntity(smokeTrail);
    }

    public static void handleProjectileImpact(GhastProjectileEntity projectile, BlockHitResult blockHitResult) {
        BlockPos impactPos = blockHitResult.getBlockPos();

        // Spawna i blocchi di ossidiana e il symbiote
        spawnObsidianBlocks(projectile.level(), impactPos, 15);
        // spawnSymbiote(projectile.level(), impactPos, 2);
        spawnFire(projectile.level(), impactPos);

        BlockPos promethiumPos = findImpactSurface(projectile.level(), impactPos);
        if (promethiumPos != null) {
            projectile.level().setBlock(promethiumPos, MyMod.PROMETHIUMX_BLOCK.get().defaultBlockState().setValue(PromethiumXBlock.FULL, true), 3);
            PromethiumXSmokeHandler.registerBlockSmoke(projectile.level(), promethiumPos);
        }

        syncImpactArea(projectile.level(), impactPos);
        projectile.remove(RemovalReason.DISCARDED);
    }

    private static void spawnObsidianBlocks(Level world, BlockPos centerPos, int count) {
        for (int i = 0; i < count; i++) {
            int xo = RANDOM.nextInt(13) - 6;
            int zo = RANDOM.nextInt(13) - 6;
            BlockPos spawnPos = findImpactSurface(world, centerPos.offset(xo, 0, zo));
            if (spawnPos != null) {
                world.setBlock(spawnPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                smokingObsidianBlocks.put(spawnPos, 0);
                if (world instanceof ServerLevel serverLevel) {
                    emitObsidianSmoke(serverLevel, spawnPos);
                }
            }
        }
     }
    
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel) {
            ServerLevel world = (ServerLevel) event.level;
            for (Map.Entry<BlockPos, Integer> entry : smokingObsidianBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                int ticks = entry.getValue();
                if (ticks > 0 && ticks % 10 == 0) {
                    emitObsidianSmoke(world, pos);
                }
                smokingObsidianBlocks.replace(pos, ticks, ticks + 1);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (state.is(Blocks.OBSIDIAN)) {
            smokingObsidianBlocks.remove(pos);
        } else if (state.is(MyMod.PROMETHIUMX_BLOCK.get())) {
            PromethiumXBlock.clearSmokeTimer(event.getLevel(), pos);
            PromethiumXSmokeHandler.removeBlockSmoke(event.getLevel(), pos);
        }
    }

    private static void emitObsidianSmoke(ServerLevel world, BlockPos pos) {
        Vec3 particlePos = Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D);
        world.sendParticles(ParticleTypes.LARGE_SMOKE, particlePos.x, particlePos.y, particlePos.z, 10, 0.25D, 0.15D, 0.25D, 0.02D);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, particlePos.x, particlePos.y, particlePos.z, 2, 0.15D, 0.05D, 0.15D, 0.01D);
    }

    private static void spawnSymbiote(Level world, BlockPos centerPos, int count) {
        for (int i = 0; i < count; i++) {
            int xo = RANDOM.nextInt(5) - 2; // Random tra -2 e 2
            int zo = RANDOM.nextInt(5) - 2; // Random tra -2 e 2
            BlockPos spawnPos = centerPos.offset(xo, 0, zo);
            SymbioteEntity symbiote = MyMod.SYMBIOTE_ENTITY.get().create(world);
            if (symbiote != null) {
                symbiote.moveTo(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ(), 0.0F, 0.0F);
                world.addFreshEntity(symbiote);
            }
        }
    }


    private static void spawnFire(Level world, BlockPos centerPos) {
        int count = 12; // Numero di fiamme da spawnare
        for (int i = 0; i < count; i++) {
            int xo = RANDOM.nextInt(13) - 6;
            int zo = RANDOM.nextInt(13) - 6;
            BlockPos surface = findImpactSurface(world, centerPos.offset(xo, 0, zo));
            if (surface != null) {
                BlockPos firePos = surface.above();
                if (world.getBlockState(firePos).isAir()) {
                    world.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
    }

    private static BlockPos findImpactSurface(Level world, BlockPos origin) {
        for (int y = 4; y >= -12; y--) {
            BlockPos pos = origin.offset(0, y, 0);
            BlockPos below = pos.below();
            if (world.getBlockState(pos).isAir() && !world.getBlockState(below).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static void syncImpactArea(Level world, BlockPos centerPos) {
        if (!(world instanceof ServerLevel)) {
            return;
        }
        for (int x = -IMPACT_SYNC_RADIUS; x <= IMPACT_SYNC_RADIUS; x++) {
            for (int y = -IMPACT_SYNC_HEIGHT_BELOW; y <= IMPACT_SYNC_HEIGHT_ABOVE; y++) {
                for (int z = -IMPACT_SYNC_RADIUS; z <= IMPACT_SYNC_RADIUS; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    world.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }
    }


}
