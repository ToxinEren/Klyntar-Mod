package modKlyntar.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import modKlyntar.entity.custom.SymbioteEntity;


import modKlyntar.MyMod;

public class PromethiumXBlock extends FallingBlock {
    public static final BooleanProperty FULL = BooleanProperty.create("full");

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

    public boolean isCorrectToolForDrops(BlockState state, Player player) {
        ItemStack heldItem = player.getMainHandItem();
        return heldItem.getItem() instanceof PickaxeItem && 
               (heldItem.is(Items.IRON_PICKAXE) || heldItem.is(Items.DIAMOND_PICKAXE) || heldItem.is(Items.NETHERITE_PICKAXE));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && state.getValue(FULL) && isCorrectToolForDrops(state, player)) {
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
