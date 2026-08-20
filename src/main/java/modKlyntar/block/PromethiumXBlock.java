package modKlyntar.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import modKlyntar.entity.custom.SmokeTrailEntity;
import modKlyntar.entity.custom.SymbioteEntity;

import modKlyntar.MyMod;

public class PromethiumXBlock extends FallingBlock {
    public static final BooleanProperty FULL = BooleanProperty.create("full");
    private static final VoxelShape COLLISION_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 16.0D);
    public static final int SMOKE_TRAIL_INTERVAL_TICKS = 10 * 20;
    /** quante volte su una il blocco pieno libera un simbionte */
    public static final float SYMBIOTE_SPAWN_CHANCE = 0.5F;

    public PromethiumXBlock() {
        super(BlockBehaviour.Properties.of()
            .strength(9.0f, 9.0f)
            .noOcclusion()); // This ensures the block is not considered fully opaque

        this.registerDefaultState(this.stateDefinition.any().setValue(FULL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    public boolean isOpaque(BlockState state) {
        return false;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            PromethiumXSmokeHandler.registerBlockSmoke(level, pos);
        }
    }

    public static void emitPromethiumSmoke(ServerLevel level, Vec3 smokePos) {
        SmokeTrailEntity smokeTrail = new SmokeTrailEntity(level, smokePos.x, smokePos.y, smokePos.z);
        level.addFreshEntity(smokeTrail);
    }

    public static void clearSmokeTimer(LevelAccessor level, BlockPos pos) {
        PromethiumXSmokeHandler.removeBlockSmoke(level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    public boolean isCorrectToolForDrops(BlockState state, Player player) {
        ItemStack heldItem = player.getMainHandItem();
        return heldItem.getItem() instanceof PickaxeItem && 
               (heldItem.is(Items.IRON_PICKAXE) || heldItem.is(Items.DIAMOND_PICKAXE) || heldItem.is(Items.NETHERITE_PICKAXE));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        // il simbionte esce una volta su due, non a ogni blocco rotto
        if (!level.isClientSide && state.getValue(FULL) && isCorrectToolForDrops(state, player)
                && level.getRandom().nextFloat() < SYMBIOTE_SPAWN_CHANCE) {
            spawnSymbiote(level, Vec3.atCenterOf(pos));
        }
    }

    private void spawnSymbiote(Level level, Vec3 pos) {
        // Implementa la logica per spawnare il Symbiote
        // Ad esempio:
        SymbioteEntity symbiote = new SymbioteEntity(MyMod.SYMBIOTE_ENTITY.get(), level);
        symbiote.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(symbiote);
    }

}
