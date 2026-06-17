package modKlyntar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import modKlyntar.block.PromethiumXBlock;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.Iterator;

@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, bus = Bus.FORGE)
public class MeteorCommand {

    private static final Random RANDOM = new Random();
    private static final int SMOKE_DURATION_TICKS = 4 * 60 * 20; // Durata in ticks (4 minuti)
    private static final Map<BlockPos, Integer> smokingObsidianBlocks = new HashMap<>();

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
        double offsetY = Math.sin(angle) * fallDistance;
        
        // Calcola la posizione di partenza delle fireball
        Vec3 meteorPos1 = new Vec3(playerPos.x + offsetX, playerPos.y + offsetY, playerPos.z);
        Vec3 meteorPos2 = new Vec3(playerPos.x + offsetX + 4, playerPos.y + offsetY + 2, playerPos.z);
        Vec3 meteorPos3 = new Vec3(playerPos.x + offsetX + 6, playerPos.y + offsetY - 1, playerPos.z);

        // Calcola la posizione di impatto a 10 blocchi di fronte al giocatore
        Vec3 impactPos = playerPos.add(offsetX, 0, 10);
        Vec3 impactPos2 = playerPos.add(offsetX + 4, 0, 10);
        Vec3 impactPos3 = playerPos.add(offsetX + 6, 0, 10);

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
        BlockPos blockBelowImpactPos = impactPos.below(); // Ottieni la posizione un blocco sotto l'impatto

        // Spawna i blocchi di ossidiana e il symbiote
        spawnObsidianBlocks(projectile.level(), impactPos, 15);
        // spawnSymbiote(projectile.level(), impactPos, 2);
        spawnFire(projectile.level(), impactPos);

        // Piazza il blocco PromethiumX un blocco sotto l'impactPos
        //projectile.level().setBlock(blockBelowImpactPos, MyMod.PROMETHIUMX_BLOCK.get().defaultBlockState(), 3); // 3: Notify clients and prevent updates
        projectile.level().setBlock(blockBelowImpactPos, MyMod.PROMETHIUMX_BLOCK.get().defaultBlockState().setValue(PromethiumXBlock.FULL, true), 3); // 3: Notify clients and prevent updates

        projectile.remove(RemovalReason.DISCARDED);
    }

    private static void spawnObsidianBlocks(Level world, BlockPos centerPos, int count) {
        for (int i = 0; i < count; i++) {
            int xo = RANDOM.nextInt(5) - 2; // Random tra -2 e 2
            int zo = RANDOM.nextInt(5) - 2; // Random tra -2 e 2
            BlockPos spawnPos = centerPos.offset(xo, 0, zo);
            BlockPos belowPos = spawnPos.below();

            // Controlla che il blocco sotto non sia aria e che la posizione attuale non sia aria
            if (!world.getBlockState(belowPos).isAir() && !world.getBlockState(spawnPos).isAir()) {
                BlockState blockState = Blocks.OBSIDIAN.defaultBlockState();
                world.setBlock(spawnPos, blockState, 3); // 3: Notify clients and prevent updates

                // Aggiungi il blocco di ossidiana fumante alla mappa
                smokingObsidianBlocks.put(spawnPos, 0);
            }
        }
     }
    
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel) {
            ServerLevel world = (ServerLevel) event.level;
            Iterator<Map.Entry<BlockPos, Integer>> iterator = smokingObsidianBlocks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = iterator.next();
                BlockPos pos = entry.getKey();
                int ticks = entry.getValue();
                if (ticks < 20 * 60 * 20) { // 3 minuti in ticks
                    // Genera particelle di fumo sopra il blocco di ossidiana
                    Vec3 particlePos = Vec3.atCenterOf(pos).add(0, 1.0, 0);
                    world.sendParticles(ParticleTypes.LARGE_SMOKE, particlePos.x, particlePos.y, particlePos.z, 5, 0.0, 0.1, 0.0, 0.0);
                    entry.setValue(ticks + 1);
                } else {
                    iterator.remove(); // Rimuovi dopo 3 minuti
                }
            }
        }
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
        int count = 5; // Numero di fiamme da spawnare
        for (int i = 0; i < count; i++) {
            int xo = RANDOM.nextInt(5) - 2; // Random tra -2 e 2
            int zo = RANDOM.nextInt(5) - 2; // Random tra -2 e 2
            BlockPos spawnPos = centerPos.offset(xo, 0, zo);
            world.setBlock(spawnPos, Blocks.FIRE.defaultBlockState(), 11); // 11: Notify clients and prevent updates
        }
    }


}