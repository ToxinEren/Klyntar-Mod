package modKlyntar.block;

import modKlyntar.MyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PromethiumXSmokeHandler {
    private static final Map<UUID, ServerLevel> FALLING_PROMETHIUM = new ConcurrentHashMap<>();
    private static final Map<BlockSmokeKey, Integer> BLOCK_PROMETHIUM = new ConcurrentHashMap<>();

    public static void registerBlockSmoke(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level realLevel) {
            BLOCK_PROMETHIUM.put(new BlockSmokeKey(realLevel.dimension(), pos), 0);
        }
    }

    public static void removeBlockSmoke(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level realLevel) {
            BLOCK_PROMETHIUM.remove(new BlockSmokeKey(realLevel.dimension(), pos));
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (event.getEntity() instanceof FallingBlockEntity fallingBlock && isPromethium(fallingBlock.getBlockState())) {
            FALLING_PROMETHIUM.put(fallingBlock.getUUID(), serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        tickPlacedPromethium(serverLevel);
        tickFallingPromethium(serverLevel);
    }

    private static void tickPlacedPromethium(ServerLevel serverLevel) {
        for (Map.Entry<BlockSmokeKey, Integer> entry : BLOCK_PROMETHIUM.entrySet()) {
            BlockSmokeKey key = entry.getKey();
            if (key.dimension() != serverLevel.dimension()) {
                continue;
            }

            int ticks = entry.getValue();
            if (ticks > 0 && ticks % PromethiumXBlock.SMOKE_TRAIL_INTERVAL_TICKS == 0 && serverLevel.hasChunkAt(key.pos())) {
                Vec3 smokePos = Vec3.atCenterOf(key.pos()).add(0.0D, 0.85D, 0.0D);
                PromethiumXBlock.emitPromethiumSmoke(serverLevel, smokePos);
            }
            BLOCK_PROMETHIUM.replace(key, ticks, ticks + 1);
        }
    }

    private static void tickFallingPromethium(ServerLevel serverLevel) {
        for (Map.Entry<UUID, ServerLevel> entry : FALLING_PROMETHIUM.entrySet()) {
            if (entry.getValue() != serverLevel) {
                continue;
            }

            Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof FallingBlockEntity fallingBlock) || !isPromethium(fallingBlock.getBlockState())) {
                FALLING_PROMETHIUM.remove(entry.getKey());
                continue;
            }

            if (fallingBlock.tickCount % PromethiumXBlock.SMOKE_TRAIL_INTERVAL_TICKS == 0) {
                Vec3 smokePos = fallingBlock.position().add(0.0D, 0.85D, 0.0D);
                PromethiumXBlock.emitPromethiumSmoke(serverLevel, smokePos);
            }
        }
    }

    private static boolean isPromethium(BlockState state) {
        return state.is(MyMod.PROMETHIUMX_BLOCK.get());
    }

    private record BlockSmokeKey(ResourceKey<Level> dimension, BlockPos pos) {
        private BlockSmokeKey(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos.immutable();
        }
    }
}
